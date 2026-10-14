package com.auth0.universalcomponents.feature.embedded.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.domain.model.AuthFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.AuthFailureException
import com.auth0.universalcomponents.feature.embedded.domain.model.AuthOutcome
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedAuthRequestFailed
import com.auth0.universalcomponents.feature.embedded.domain.model.FailureMessage
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginEvent
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginStep
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaChallenge
import com.auth0.universalcomponents.feature.embedded.domain.repository.AuthInteracting
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Drives the email-code flow: [LoginEvent]s in, [LoginStep]s out. */
@Suppress("TooGenericExceptionCaught")
internal class LoginFlowViewModel(private val interactor: AuthInteracting) : ViewModel() {

    private val _step = MutableStateFlow<LoginStep>(LoginStep.Loading)
    val step: StateFlow<LoginStep> = _step.asStateFlow()

    /** A recoverable failure, shown under the field on the current screen. */
    private val _inputError = MutableStateFlow<AuthFailure?>(null)
    val inputError: StateFlow<AuthFailure?> = _inputError.asStateFlow()

    /** A new code is being sent; only the resend row is busy, the Verify button is not. */
    private val _isResending = MutableStateFlow(false)
    val isResending: StateFlow<Boolean> = _isResending.asStateFlow()

    private val _authenticated = Channel<Credentials>(Channel.BUFFERED)
    val authenticated: Flow<Credentials> = _authenticated.receiveAsFlow()

    // Outputs of earlier calls that later calls need.
    private var isEmailOtpOffered = false
    private var email: String? = null
    private var challenge: MfaChallenge? = null

    /** Where Retry returns to; null runs discovery again. */
    private var retryStep: LoginStep? = null

    private var started = false

    /** The last flow signed in or was left; the next Start begins a new one instead of showing its end. */
    private var ended = false
    private var isSubmitting = false
    private var inFlight: Job? = null

    fun send(event: LoginEvent) {
        when (event) {
            LoginEvent.Start -> {
                // A Start mid-flow (after a rotation, say) keeps the user's place.
                if (started && !ended) return
                if (ended) startOver()
                started = true
                launchStep { discover() }
            }

            is LoginEvent.SubmitIdentifier -> launchStep { sendCode(event.email) }

            is LoginEvent.SubmitOtp -> {
                val email = email ?: return
                launchStep { verify(LoginStep.CollectOtpCode(email)) { interactor.verifyOtp(event.code) } }
            }

            is LoginEvent.SubmitMfa -> {
                val challenge = challenge ?: return
                launchStep {
                    verify(LoginStep.StepUpMfa(challenge)) { interactor.verifyMfa(event.code, challenge.factor) }
                }
            }

            LoginEvent.Retry -> launchStep { retry() }
        }
    }

    /** Sends a new code to the same email. */
    fun resend() {
        val email = email ?: return
        launchStep { sendCode(email, isResend = true) }
    }

    /** Code entry goes back to email entry (true); anywhere else leaves the flow (false). */
    fun goBack(): Boolean {
        // Stay put until the request in flight answers, rather than dropping it halfway.
        if (isSubmitting) return true
        _inputError.value = null
        val current = _step.value as? LoginStep.CollectOtpCode
        if (current == null) {
            ended = true
            return false
        }
        _step.value = LoginStep.CollectOtpIdentifier(current.identifier)
        return true
    }

    /** Forgets the ended flow; [discover] opens a new session, which drops the old one in the SDK. */
    private fun startOver() {
        ended = false
        isEmailOtpOffered = false
        email = null
        challenge = null
        retryStep = null
        _inputError.value = null
        _step.value = LoginStep.Loading
    }

    private suspend fun discover() {
        _step.value = LoginStep.Loading
        try {
            isEmailOtpOffered = LoginOption.EmailOtp in interactor.discover()
            _step.value = if (isEmailOtpOffered) LoginStep.CollectOtpIdentifier() else LoginStep.NoMethodsAvailable
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fail(e, returnTo = null)
        }
    }

    private suspend fun sendCode(email: String, isResend: Boolean = false) {
        if (!isEmailOtpOffered) return
        _inputError.value = null
        if (isResend) _isResending.value = true else _step.value = LoginStep.Loading
        submitting {
            try {
                interactor.startOtp(email)
                this.email = email
                _step.value = LoginStep.CollectOtpCode(email)
            } catch (e: AuthFailureException) {
                // A failed resend stays on code entry, where Edit and Resend are both at hand.
                _inputError.value = e.failure
                _step.value = if (isResend) LoginStep.CollectOtpCode(email) else LoginStep.CollectOtpIdentifier(email)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(e, returnTo = LoginStep.CollectOtpIdentifier(email))
            } finally {
                _isResending.value = false
            }
        }
    }

    /**
     * Shared by the email code and the step-up: a rejected code stays on [codeStep] with an inline error.
     * After a failure for good, Retry returns to [codeStep] only if the session survived, else to email entry.
     */
    private suspend fun verify(codeStep: LoginStep, call: suspend () -> AuthOutcome) {
        _inputError.value = null
        _step.value = LoginStep.Loading
        submitting {
            try {
                when (val outcome = call()) {
                    is AuthOutcome.Authenticated -> {
                        _step.value = LoginStep.Authenticated
                        ended = true
                        _authenticated.send(outcome.credentials)
                    }

                    is AuthOutcome.MfaRequired -> {
                        challenge = outcome.challenge
                        _step.value = LoginStep.StepUpMfa(outcome.challenge)
                    }
                }
            } catch (e: AuthFailureException) {
                _inputError.value = e.failure
                _step.value = codeStep
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A code can only be retried while the session lives; otherwise a new code is needed.
                val canRetryStep = (e as? EmbeddedAuthRequestFailed)?.canRetryStep == true
                fail(e, returnTo = if (canRetryStep) codeStep else LoginStep.CollectOtpIdentifier(email.orEmpty()))
            }
        }
    }

    /** Shows the full-screen error; only our own error types carry text, so SDK debug output never shows. */
    private fun fail(error: Exception, returnTo: LoginStep?) {
        retryStep = returnTo
        val message = (error as? EmbeddedAuthRequestFailed)?.displayMessage
            ?: FailureMessage.Local(R.string.embedded_login_error_generic)
        _step.value = LoginStep.Failed(message)
    }

    private suspend fun retry() {
        if (_step.value !is LoginStep.Failed) return
        val target = retryStep
        if (target != null) {
            retryStep = null
            _step.value = target
        } else {
            discover()
        }
    }

    private suspend fun submitting(block: suspend () -> Unit) {
        isSubmitting = true
        try {
            block()
        } finally {
            isSubmitting = false
        }
    }

    /** Runs one step at a time; a second tap while one is in flight is dropped. */
    private fun launchStep(block: suspend () -> Unit) {
        if (inFlight?.isActive == true) return
        inFlight = viewModelScope.launch { block() }
    }
}
