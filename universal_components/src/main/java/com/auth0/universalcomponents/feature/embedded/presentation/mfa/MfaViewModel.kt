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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Holds the MFA state and runs the work [MfaReducer] asks for; it makes no decisions itself.
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
    private var pushCheck: Job? = null

    fun onIntent(intent: MfaIntent) = accept(MfaReducer.reduce(_state.value, intent))

    private fun onResult(result: MfaResult) = accept(MfaReducer.reduce(_state.value, result))

    private fun accept(update: MfaUpdate) {
        _state.value = update.state
        update.effect?.let { _effects.trySend(it) }
        update.command?.let(::run)
    }

    private fun run(command: MfaCommand) {
        when (command) {
            MfaCommand.StopPush -> pushCheck?.cancel()
            is MfaCommand.CheckPush -> checkPushAfter(command.afterMs)
            is MfaCommand.SendPush -> viewModelScope.launch {
                // The flow is one step at a time: a push check in flight must finish before the next step starts.
                pushCheck?.cancelAndJoin()
                onResult(repository.sendPush(command.factor).toResult { MfaResult.PushSent(it) })
            }
        }
    }

    /**
     * Waits, then checks once. Stopping cancels the wait at once, but lets a check already in flight finish,
     * so the SDK keeps the session that check returns; its answer is then dropped.
     */
    private fun checkPushAfter(delayMs: Long) {
        pushCheck?.cancel()
        pushCheck = viewModelScope.launch {
            delay(delayMs)
            val checked = withContext(NonCancellable) { repository.checkPush() }
            ensureActive()
            onResult(
                checked.toResult { status ->
                    when (status) {
                        is PushStatus.Approved -> MfaResult.SignedIn(status.credentials)
                        is PushStatus.Pending -> MfaResult.PushPending(status.retryInMs)
                    }
                },
            )
        }
    }

    /** A failure is logged once here, with its cause, so it can be traced in a live app. */
    private inline fun <T> Result<T, LoginFailure>.toResult(onSuccess: (T) -> MfaResult): MfaResult = when (this) {
        is Result.Success -> onSuccess(data)
        is Result.Error -> {
            Logger.e(TAG, "MFA step failed: ${error.error}", error.cause)
            MfaResult.Failed(error)
        }
    }

    private companion object {
        const val TAG = "MfaViewModel"
    }
}
