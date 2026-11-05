package com.auth0.universalcomponents.feature.mfachallenge.presentation

import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.mfachallenge.MfaError
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.CodeStatus
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.PushStatus
import com.auth0.universalcomponents.feature.mfachallenge.domain.repository.MfaRepository
import com.auth0.universalcomponents.utils.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

/**
 * One run of MFA: holds the state on show, runs the work [MfaReducer] asks for, and reports the end through
 * [onEffect]. It makes no decisions itself and knows nothing about the first factor, so any host can own it:
 * embedded login today, a standalone MFA component or step-up later. Its work runs in a child of [hostScope]
 * that [stop] cancels, so the host ends MFA with one call.
 *
 * [state] is what [MfaHost] draws, and taps come back through [onIntent]. It is ready as soon as the engine exists,
 * but nothing runs until [start]: the host stores the engine first, so an effect sent while starting finds it.
 * After [stop], [state] keeps its last value (the screen may still be animating out) and taps are ignored.
 *
 * Checks carry on while the app is in the background, so an approval given in another app is seen as soon as the
 * server has it and the screen is up to date when the user comes back.
 *
 * The engine is confined to the main thread: [hostScope] must run on it (the ViewModel's does), and so must every
 * call. Its state is plain, not atomic, because a single thread is what keeps the reducer's read-then-write of [state]
 * safe; making one flag atomic would not make the rest so.
 */
internal class MfaEngine(
    private val reducer: MfaReducer,
    private val repository: MfaRepository,
    hostScope: CoroutineScope,
    private val timeSource: TimeSource = ElapsedRealtimeSource,
    private val onEffect: (MfaEffect) -> Unit,
) {

    private val job = SupervisorJob(hostScope.coroutineContext[Job])
    private val scope = CoroutineScope(hostScope.coroutineContext + job)

    private val initialUpdate = reducer.start()
    private val _state = MutableStateFlow(initialUpdate.state)
    val state: StateFlow<MfaState> = _state.asStateFlow()

    /** The wait before the next push check. The check itself runs on its own, so cancelling this never cuts it off. */
    private var pushCheckTimer: Job? = null

    /**
     * The push being sent, from the moment it waits its turn at the SDK. Cancelling it drops a push the user has left
     * (it is never sent if it hasn't started), and a call already on its way finishes but is not heard.
     */
    private var pushSendJob: Job? = null

    /**
     * The phone code being sent, tracked for the same reason: when a check that was still on its way ends the round,
     * a code queued behind it is for a screen the user never sees, so it is dropped rather than texted or called.
     */
    private var phoneSendJob: Job? = null

    /** The one-second wait before the resend countdown moves on. */
    private var resendCountdownJob: Job? = null

    /** True while the app is in the background; only the main thread reads or sets it. */
    private var isInBackground = false

    /** Set by the first [start]; the step alone can't tell, since the list looks the same before and after. */
    private var isStarted = false

    /** Runs the first step's work, once: the host calls it right after it stores the engine, and a repeat is ignored. */
    fun start() {
        if (isStarted) return
        isStarted = true
        applyUpdate(initialUpdate)
    }

    fun onIntent(intent: MfaIntent) = applyUpdate(reducer.handleIntent(_state.value, intent))

    /** System back or the top-bar arrow; MFA decides what it means on the screen on show. */
    fun back() = onIntent(MfaIntent.Back)

    /**
     * Told by whoever draws MFA when the app leaves the foreground. Checks carry on, but a network failure while in
     * the background doesn't count against the push.
     */
    fun onAppBackgrounded() {
        isInBackground = true
    }

    /**
     * Told when the app returns to the foreground: the user may have approved in another app meanwhile, so a waiting
     * push is checked at once instead of after the rest of its wait. Does nothing if the app never left.
     */
    fun onAppForegrounded() {
        if (!isInBackground) return
        isInBackground = false
        applyUpdate(reducer.handleAppForegrounded(_state.value))
    }

    /** False once MFA has ended, one way or another; the host then owns what is left on screen. */
    val isRunning: Boolean
        get() = job.isActive

    /**
     * Ends this run: nothing it started reaches the host any more. It can't be undone, so the host builds a new engine
     * for the next visit rather than starting this one again. Nothing is left to clean up: the waits and the countdown
     * are cancelled with the job, and a call already on its way can't be recalled but its answer is dropped.
     */
    fun stop() = job.cancel()

    private fun onOutcome(outcome: MfaOutcome) = applyUpdate(reducer.handleOutcome(_state.value, outcome))

    /** An effect means MFA is over, so the engine stops itself once it has told the host; no timer outlives it. */
    private fun applyUpdate(update: MfaUpdate) {
        if (!job.isActive) return
        _state.value = update.state
        update.commands.forEach(::runCommand)
        update.effect?.let { effect ->
            stop()
            onEffect(effect)
        }
    }

    private fun runCommand(command: MfaCommand) {
        when (command) {
            // A check still on its way finishes first: the repository runs one SDK step at a time. A send that is
            // replaced, or abandoned, is cancelled, so an old push can't answer for a newer one.
            is MfaCommand.SendPush -> {
                pushSendJob?.cancel()
                pushSendJob = scope.launch {
                    val sent = repository.sendPush(command.factor)
                    onOutcome(sent.toOutcome(MfaOutcome::SendFailed) { MfaOutcome.PushSent(it) })
                }
            }

            // Nothing replaces a code on its way, since the screen stays off limits until it is out, but a new round
            // replaces the whole menu, so [MfaCommand.CancelChallengeWork] drops a code that is still waiting.
            is MfaCommand.SendPhoneCode -> {
                phoneSendJob?.cancel()
                phoneSendJob = scope.launch {
                    val sent = repository.sendPhoneCode(command.factor)
                    onOutcome(sent.toOutcome(MfaOutcome::SendFailed) { MfaOutcome.PhoneCodeSent(canResend = it) })
                }
            }

            is MfaCommand.VerifyCode -> scope.launch {
                val verified = repository.verifyCode(command.code)
                onOutcome(
                    verified.toOutcome(MfaOutcome::CodeFailed) { status ->
                        when (status) {
                            is CodeStatus.Verified -> MfaOutcome.SignedIn(status.credentials)
                            CodeStatus.WrongCode -> MfaOutcome.CodeRejected
                            is CodeStatus.AnotherFactor -> MfaOutcome.AnotherFactor(status.factors)
                        }
                    },
                )
            }

            is MfaCommand.SchedulePushCheck -> {
                pushCheckTimer?.cancel()
                pushCheckTimer = scope.launch {
                    delay(command.afterMs)
                    applyUpdate(reducer.handlePushCheckDue(_state.value))
                }
            }

            // Not tied to the wait, so leaving the push screen never cuts a check off: an approval that lands after
            // the user picked another device isn't lost. The reducer drops answers that no longer fit the screen.
            MfaCommand.CheckPush -> scope.launch { onOutcome(checkPushStatus()) }

            MfaCommand.CancelChallengeWork -> {
                pushCheckTimer?.cancel()
                pushSendJob?.cancel()
                phoneSendJob?.cancel()
            }

            MfaCommand.CountDownResend -> countDownResend()
        }
    }

    private suspend fun checkPushStatus(): MfaOutcome {
        val checked = repository.checkPushStatus()
        // Read when the answer arrives: that is when a failure is judged.
        val whileInBackground = isInBackground
        return checked.toOutcome({ MfaOutcome.CheckFailed(it, whileInBackground) }) { status ->
            when (status) {
                is PushStatus.Approved -> MfaOutcome.SignedIn(status.credentials)
                is PushStatus.Pending -> MfaOutcome.PushPending(status)
                is PushStatus.AnotherFactor -> MfaOutcome.AnotherFactor(status.factors)
                is PushStatus.Rejected -> MfaOutcome.PushRejected(status.cause)
            }
        }
    }

    /**
     * Replaces any tick still waiting, so a new countdown never loses a second to an old one. A tick counts the whole
     * seconds that really passed (at least the one asked for), so an app that was frozen in the background catches up.
     */
    private fun countDownResend() {
        resendCountdownJob?.cancel()
        val startedAt = timeSource.markNow()
        resendCountdownJob = scope.launch {
            delay(ONE_SECOND_MILLIS)
            val passed = (startedAt.elapsedNow().inWholeMilliseconds + HALF_SECOND_MILLIS) / ONE_SECOND_MILLIS
            applyUpdate(reducer.handleSecondPassed(_state.value, seconds = passed.toInt()))
        }
    }

    /** A failure is logged once here, with its cause, so it can be traced in a live app. */
    private inline fun <T> Result<T, MfaError>.toOutcome(
        onFailure: (MfaError) -> MfaOutcome,
        onSuccess: (T) -> MfaOutcome,
    ): MfaOutcome = when (this) {
        is Result.Success -> onSuccess(data)
        is Result.Error -> {
            Logger.e(TAG, "MFA step failed: $error", error)
            onFailure(error)
        }
    }

    private companion object {
        const val TAG = "MfaEngine"
        const val ONE_SECOND_MILLIS = 1_000L
        const val HALF_SECOND_MILLIS = 500L
    }
}
