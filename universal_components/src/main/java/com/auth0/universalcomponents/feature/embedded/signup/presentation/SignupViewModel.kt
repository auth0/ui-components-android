package com.auth0.universalcomponents.feature.embedded.signup.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupError
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupFailure
import com.auth0.universalcomponents.feature.embedded.signup.domain.repository.EmbeddedSignupRepository
import com.auth0.universalcomponents.utils.ElapsedRealtimeSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

/**
 * Holds the sign-up's [state], runs the work [SignupReducer] asks for and tells the host what to do through [effects].
 * It makes no decisions itself: it is the store of the loop described in [SignupContract], as the engine is for MFA.
 * Every failure comes back from the repository as a value, already logged with its cause, so nothing is caught here.
 *
 * It is confined to the main thread: [viewModelScope] runs there, and so must every call. Its state is plain, not
 * atomic, because a single thread is what keeps the reducer's read-then-write of [state] safe.
 * [timeSource] measures the wait before another code may be asked for; it keeps counting while the device sleeps.
 */
internal class SignupViewModel(
    private val repository: EmbeddedSignupRepository,
    private val reducer: SignupReducer = SignupReducer(),
    private val timeSource: TimeSource = ElapsedRealtimeSource,
) : ViewModel() {

    private val _state = MutableStateFlow(reducer.initialState)
    val state: StateFlow<SignupState> = _state.asStateFlow()

    private val _effects = Channel<SignupEffect>(Channel.BUFFERED)

    /** What happens once, not what the screen draws: the credentials for the app, and leaving. Never repeated by a rotation. */
    val effects: Flow<SignupEffect> = _effects.receiveAsFlow()

    /** The call on its way, if any. The reducer lets only one be out at a time, and the repository runs it alone. */
    private var work: Job? = null

    /** The one-second wait before the resend countdown moves on. */
    private var countdown: Job? = null

    fun send(intent: SignupIntent) = apply(reducer.handleIntent(_state.value, intent))

    private fun onOutcome(outcome: SignupOutcome) = apply(reducer.handleOutcome(_state.value, outcome))

    private fun apply(update: SignupUpdate) {
        _state.value = update.state
        update.commands.forEach(::run)
        update.effect?.let { _effects.trySend(it) }
    }

    private fun run(command: SignupCommand) {
        when (command) {
            SignupCommand.Open -> launchWork {
                onOutcome(repository.open().asOutcome(SignupOutcome::OpenFailed) { SignupOutcome.Opened(it) })
            }

            is SignupCommand.Identify -> launchWork {
                val identified = repository.identify(command.identifier)
                onOutcome(
                    identified.asOutcome(SignupOutcome::RequestFailed) {
                        SignupOutcome.Identified(command.identifier, it)
                    },
                )
            }

            is SignupCommand.SendCode -> launchWork {
                val sent = repository.sendCode(command.challenge, command.delivery)
                onOutcome(
                    sent.asOutcome(SignupOutcome::RequestFailed) {
                        SignupOutcome.CodeSent(command.identifier, command.challenge, command.delivery)
                    },
                )
            }

            is SignupCommand.ReplaceExpiredCode -> launchWork {
                val replaced = repository.replaceExpiredCode(command.identifier, command.delivery)
                onOutcome(
                    replaced.asOutcome(SignupOutcome::RequestFailed) {
                        SignupOutcome.CodeSent(command.identifier, it, command.delivery)
                    },
                )
            }

            is SignupCommand.Verify -> launchWork {
                val verified = repository.verify(command.code)
                onOutcome(verified.asOutcome(SignupOutcome::RequestFailed) { SignupOutcome.Verified(it) })
            }

            SignupCommand.CreateAccount -> launchWork {
                val created = repository.createAccount()
                onOutcome(created.asOutcome(SignupOutcome::CreateFailed) { SignupOutcome.AccountCreated(it) })
            }

            SignupCommand.CountDownResend -> countDownResend()

            SignupCommand.CancelWork -> {
                work?.cancel()
                countdown?.cancel()
            }
        }
    }

    /** A call already on its way is the SDK's to finish; cancelling only stops its answer from being heard. */
    private fun launchWork(call: suspend () -> Unit) {
        work = viewModelScope.launch { call() }
    }

    /**
     * Replaces any tick still waiting, so a new countdown never loses a second to an old one. A tick counts the whole
     * seconds that really passed (at least the one asked for), so a device that slept catches up.
     */
    private fun countDownResend() {
        countdown?.cancel()
        val startedAt = timeSource.markNow()
        countdown = viewModelScope.launch {
            delay(ONE_SECOND_MILLIS)
            val passed = (startedAt.elapsedNow().inWholeMilliseconds + HALF_SECOND_MILLIS) / ONE_SECOND_MILLIS
            apply(reducer.handleSecondsPassed(_state.value, seconds = passed.toInt().coerceAtLeast(1)))
        }
    }

    private inline fun <D> Result<D, SignupFailure>.asOutcome(
        onFailure: (SignupError) -> SignupOutcome,
        onSuccess: (D) -> SignupOutcome,
    ): SignupOutcome = when (this) {
        is Result.Success -> onSuccess(data)
        is Result.Error -> onFailure(error.error)
    }

    private companion object {
        const val ONE_SECOND_MILLIS = 1_000L
        const val HALF_SECOND_MILLIS = 500L
    }
}
