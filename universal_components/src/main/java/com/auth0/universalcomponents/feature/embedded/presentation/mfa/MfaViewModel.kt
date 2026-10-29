package com.auth0.universalcomponents.feature.embedded.presentation.mfa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.embedded.domain.model.PushStatus
import com.auth0.universalcomponents.feature.embedded.domain.repository.MfaRepository
import com.auth0.universalcomponents.utils.logging.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Holds the MFA state and runs the work [MfaIntentHandler] asks for; it makes no decisions itself.
 * [MfaIntent]s come in, [MfaState] and one-off [MfaEffect]s go out.
 */
internal class MfaViewModel(
    factors: List<MfaFactor>,
    private val repository: MfaRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MfaState(factors))
    val state: StateFlow<MfaState> = _state.asStateFlow()

    private val _effects = Channel<MfaEffect>(Channel.BUFFERED)
    val effects: Flow<MfaEffect> = _effects.receiveAsFlow()

    /** The wait between push checks, and the check itself. */
    private var pushCheckJob: Job? = null

    /** The one-second wait before the resend countdown moves on. */
    private var resendCountdownJob: Job? = null

    fun onIntent(intent: MfaIntent) = applyUpdate(MfaIntentHandler.handleIntent(_state.value, intent))

    private fun onServerResult(result: MfaResult) = applyUpdate(MfaIntentHandler.handleResult(_state.value, result))

    private fun applyUpdate(update: MfaUpdate) {
        _state.value = update.state
        update.effect?.let { _effects.trySend(it) }
        update.commands.forEach(::runCommand)
    }

    private fun runCommand(command: MfaCommand) {
        when (command) {
            MfaCommand.StopPush -> pushCheckJob?.cancel()
            is MfaCommand.CheckPush -> schedulePushCheck(command.afterMs)
            MfaCommand.CountDownResend -> countDownResend()
            // A check still in flight finishes first: the repository runs one SDK step at a time.
            is MfaCommand.SendPush -> viewModelScope.launch {
                pushCheckJob?.cancel()
                onServerResult(repository.sendPush(command.factor).toMfaResult { MfaResult.PushSent(it) })
            }
        }
    }

    /**
     * Waits, then checks once. Stopping cancels the wait at once; a check already in flight still finishes in the
     * repository, so the SDK keeps the session it returns, but its answer never reaches this ViewModel.
     */
    private fun schedulePushCheck(delayMs: Long) {
        pushCheckJob?.cancel()
        pushCheckJob = viewModelScope.launch {
            delay(delayMs)
            val checkResult = repository.checkPush()
            onServerResult(
                checkResult.toMfaResult { status ->
                    when (status) {
                        is PushStatus.Approved -> MfaResult.SignedIn(status.credentials)
                        is PushStatus.Pending -> MfaResult.PushPending(status.retryInMs)
                    }
                },
            )
        }
    }

    /** Replaces any tick still waiting, so a new countdown never loses a second to an old one. */
    private fun countDownResend() {
        resendCountdownJob?.cancel()
        resendCountdownJob = viewModelScope.launch {
            delay(ONE_SECOND_MILLIS)
            applyUpdate(MfaIntentHandler.handleSecondPassed(_state.value))
        }
    }

    /** A failure is logged once here, with its cause, so it can be traced in a live app. */
    private inline fun <T> Result<T, LoginFailure>.toMfaResult(onSuccess: (T) -> MfaResult): MfaResult = when (this) {
        is Result.Success -> onSuccess(data)
        is Result.Error -> {
            Logger.e(TAG, "MFA step failed: ${error.error}", error.cause)
            MfaResult.Failed(error)
        }
    }

    private companion object {
        const val TAG = "MfaViewModel"
        const val ONE_SECOND_MILLIS = 1_000L
    }
}
