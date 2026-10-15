package com.auth0.universalcomponents.feature.embedded.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.domain.network.onError
import com.auth0.universalcomponents.domain.network.onSuccess
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption
import com.auth0.universalcomponents.feature.embedded.domain.repository.EmbeddedLoginRepository
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
 * Drives the email-code flow: [LoginEvent]s in, [LoginStep]s out. Each event counts only on the screen it belongs to.
 * The repository returns each failure as a [LoginFailure], so nothing here is caught.
 */
internal class LoginFlowViewModel(private val repository: EmbeddedLoginRepository) : ViewModel() {

    private val _loginStep = MutableStateFlow<LoginStep>(LoginStep.Loading)
    val loginStep: StateFlow<LoginStep> = _loginStep.asStateFlow()

    private val _authenticated = Channel<Credentials>(Channel.BUFFERED)
    val authenticated: Flow<Credentials> = _authenticated.receiveAsFlow()

    /** False before the first Start, and again once the flow signed in or was left; the next Start begins anew. */
    private var isRunning = false
    private var inFlight: Job? = null
    private var countdown: Job? = null

    fun send(event: LoginEvent) {
        val current = _loginStep.value
        when (event) {
            // A Start mid-flow (after a rotation, say) keeps the user's place.
            LoginEvent.Start -> if (!isRunning) {
                isRunning = true
                launchStep { discover() }
            }

            is LoginEvent.SubmitIdentifier -> if (current is LoginStep.CollectOtpIdentifier) {
                launchStep { sendCode(event.email, isResend = false) }
            }

            is LoginEvent.SubmitOtp -> if (current is LoginStep.CollectOtpCode) {
                launchStep { verify(current.identifier, event.code) }
            }

            LoginEvent.Resend -> if (current is LoginStep.CollectOtpCode && !current.isCountingDown) {
                launchStep { sendCode(current.identifier, isResend = true) }
            }

            LoginEvent.Retry -> if (current is LoginStep.Failed) launchStep { retry(current) }
        }
    }

    /** Code entry goes back to email entry (true); anywhere else leaves the flow (false). */
    fun goBack(): Boolean {
        val current = _loginStep.value
        // Stay put until the request in flight answers, rather than dropping it halfway.
        if (current.isBusy) return true
        if (current is LoginStep.CollectOtpCode) {
            _loginStep.value = LoginStep.CollectOtpIdentifier(current.identifier)
            return true
        }
        isRunning = false
        return false
    }

    private suspend fun discover() {
        _loginStep.value = LoginStep.Loading
        repository.discover()
            .onSuccess { options ->
                _loginStep.value =
                    if (LoginOption.EmailOtp in options) LoginStep.CollectOtpIdentifier() else LoginStep.NoMethodsAvailable
            }
            .onError { failure -> _loginStep.value = failed(failure, retryTo = null) }
    }

    /**
     * Each code sent restarts the resend countdown. A failure here, including no code going out,
     * is one for good: Try again returns to email entry with the email kept.
     */
    private suspend fun sendCode(email: String, isResend: Boolean) {
        _loginStep.value = if (isResend) {
            LoginStep.CollectOtpCode(email, isResending = true)
        } else {
            LoginStep.CollectOtpIdentifier(email, isSubmitting = true)
        }
        repository.startEmailOtp(email)
            .onSuccess {
                _loginStep.value = LoginStep.CollectOtpCode(email, resendSecondsLeft = RESEND_COOLDOWN_SECONDS)
                startResendCountdown()
            }
            .onError { failure -> _loginStep.value = failed(failure, retryTo = LoginStep.CollectOtpIdentifier(email)) }
    }

    /**
     * A wrong or expired code stays on code entry with the error under the field.
     * After a failure for good, Try again returns to code entry only if the session survived, else to email entry.
     */
    private suspend fun verify(email: String, code: String) {
        updateCodeStep { copy(error = null, isSubmitting = true) }
        repository.verifyOtp(code)
            .onSuccess { credentials ->
                _loginStep.value = LoginStep.Authenticated
                isRunning = false
                _authenticated.send(credentials)
            }
            .onError { failure ->
                when (failure.error) {
                    EmbeddedLoginError.InvalidCode ->
                        updateCodeStep { copy(error = failure.error, isSubmitting = false) }

                    // That code can't be used any more, so a new one can be asked for at once.
                    EmbeddedLoginError.CodeExpired ->
                        updateCodeStep { copy(error = failure.error, isSubmitting = false, resendSecondsLeft = 0) }

                    else -> _loginStep.value = failed(
                        failure,
                        retryTo = if (failure.canRetryStep) {
                            LoginStep.CollectOtpCode(email)
                        } else {
                            LoginStep.CollectOtpIdentifier(email)
                        },
                    )
                }
            }
    }

    /** Counts the resend wait down a second at a time; it stops at zero, or once code entry is left. */
    private fun startResendCountdown() {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            while (_loginStep.value.isCountingDown) {
                delay(ONE_SECOND_MILLIS)
                updateCodeStep { copy(resendSecondsLeft = (resendSecondsLeft - 1).coerceAtLeast(0)) }
            }
        }
    }

    /** Changes code entry in place, so its countdown carries on; does nothing once that step is left. */
    private inline fun updateCodeStep(change: LoginStep.CollectOtpCode.() -> LoginStep.CollectOtpCode) {
        val current = _loginStep.value as? LoginStep.CollectOtpCode ?: return
        _loginStep.value = current.change()
    }

    /** The full-screen error. Logged once here, with its cause, so a failure in a live app can be traced. */
    private fun failed(failure: LoginFailure, retryTo: LoginStep?): LoginStep {
        Logger.e(TAG, "Login step failed: ${failure.error}", failure.cause)
        return LoginStep.Failed(failure.error, retryTo)
    }

    private suspend fun retry(failed: LoginStep.Failed) {
        val target = failed.retryTo
        if (target != null) _loginStep.value = target else discover()
    }

    /** Runs one step at a time; a second tap while one is in flight is dropped. */
    private fun launchStep(block: suspend () -> Unit) {
        if (inFlight?.isActive == true) return
        inFlight = viewModelScope.launch { block() }
    }

    private companion object {
        const val TAG = "LoginFlowViewModel"
        const val RESEND_COOLDOWN_SECONDS = 60
        const val ONE_SECOND_MILLIS = 1_000L

        /** Resend is still waiting out its cooldown. */
        val LoginStep.isCountingDown: Boolean
            get() = this is LoginStep.CollectOtpCode && resendSecondsLeft > 0

        /** A request for this screen is in flight. */
        val LoginStep.isBusy: Boolean
            get() = when (this) {
                is LoginStep.CollectOtpIdentifier -> isSubmitting
                is LoginStep.CollectOtpCode -> isSubmitting || isResending
                else -> false
            }
    }
}
