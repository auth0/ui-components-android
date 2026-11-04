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
import com.auth0.universalcomponents.feature.mfachallenge.MfaError
import com.auth0.universalcomponents.feature.mfachallenge.MfaErrorHandling
import com.auth0.universalcomponents.feature.mfachallenge.MfaResult
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.mfachallenge.domain.repository.MfaRepository
import com.auth0.universalcomponents.feature.mfachallenge.presentation.MfaEffect
import com.auth0.universalcomponents.feature.mfachallenge.presentation.MfaEngine
import com.auth0.universalcomponents.feature.mfachallenge.presentation.MfaReducer
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
 * A second factor runs in an [MfaEngine] owned here, so the hand-off and its end are plain calls on this ViewModel.
 * The repositories return each failure as a [LoginFailure], so nothing here is caught.
 */
internal class LoginFlowViewModel(
    private val repository: EmbeddedLoginRepository,
    private val mfaRepository: MfaRepository,
) : ViewModel() {

    private val _loginStep = MutableStateFlow<LoginStep>(LoginStep.Loading)
    val loginStep: StateFlow<LoginStep> = _loginStep.asStateFlow()

    private val _authenticated = Channel<Credentials>(Channel.BUFFERED)
    val authenticated: Flow<Credentials> = _authenticated.receiveAsFlow()

    private val _mfaFailed = Channel<MfaError>(Channel.BUFFERED)

    /** Each time MFA ends with an error, once. The user leaving MFA is not one. */
    val mfaFailed: Flow<MfaError> = _mfaFailed.receiveAsFlow()

    private val _mfaResult = MutableStateFlow<MfaResult?>(null)

    /** How the last MFA ended; null while one runs, and again once the next starts. Only a headless app reads it. */
    val mfaResult: StateFlow<MfaResult?> = _mfaResult.asStateFlow()

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

            // Its own event, not Back: a second tap on the screen still sliding out must not leave the login.
            LoginEvent.LeaveMfa -> if (current is LoginStep.Mfa) leaveMfa(current)

            // An app that doesn't take over leaves the user on MFA's own "not available" screen.
            is LoginEvent.MfaErrorAnswered ->
                if (current is LoginStep.AwaitingApp && event.handling == MfaErrorHandling.NotHandled) {
                    _loginStep.value = LoginStep.Mfa(current.engine, current.identifier)
                }
        }
    }

    /**
     * Code entry goes back to email entry, and MFA decides its own Back (true); anywhere else leaves (false). Once MFA
     * has ended, its error screen is all that is left of it, so Back leaves it, as its "Start over" does. So does Back
     * while waiting for the app: the user doesn't wait for an app that never answers.
     */
    fun goBack(): Boolean {
        val current = _loginStep.value
        if (current is LoginStep.Mfa) {
            if (current.engine.isRunning) current.engine.back() else leaveMfa(current)
            return true
        }
        if (current is LoginStep.AwaitingApp) {
            _loginStep.value = LoginStep.CollectOtpIdentifier(current.identifier)
            return true
        }
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
     * A second factor hands over to the MFA flow, which carries on with the same session.
     */
    private suspend fun verify(email: String, code: String) {
        updateCodeStep { copy(error = null, isSubmitting = true) }
        repository.verifyOtp(code)
            .onSuccess { credentials -> signedIn(credentials) }
            .onError { failure ->
                when (val error = failure.error) {
                    is EmbeddedLoginError.MfaRequired -> startMfa(email, error.factors)

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

    /**
     * MFA runs in its own engine, a new one for each visit so nothing from an old one reaches the screen. It stops
     * itself when it ends. It is shown as [LoginStep.Mfa] and draws and takes its own taps, so all login sees is Back
     * and how it ends, decided here. [identifier] is what the user typed for the first factor, kept by login (not in
     * the MFA state) to pre-fill it if the user starts over.
     */
    private fun startMfa(identifier: String, factors: List<MfaFactor>) {
        _mfaResult.value = null
        val engine = MfaEngine(
            reducer = MfaReducer(factors),
            repository = mfaRepository,
            hostScope = viewModelScope,
            onEffect = { effect -> endMfa(identifier, effect) },
        )
        // Shown before it starts, so an end reached while starting replaces this step rather than being overwritten.
        _loginStep.value = LoginStep.Mfa(engine, identifier)
        engine.start()
    }

    /** Back to email entry with what was typed kept; the next code opens a new session. */
    private fun leaveMfa(mfa: LoginStep.Mfa) {
        mfa.engine.stop()
        if (_mfaResult.value == null) _mfaResult.value = MfaResult.Ended(MfaError.Cancelled())
        _loginStep.value = LoginStep.CollectOtpIdentifier(mfa.identifier)
    }

    private fun endMfa(identifier: String, effect: MfaEffect) {
        when (effect) {
            is MfaEffect.Authenticated -> {
                _mfaResult.value = MfaResult.SignedIn
                viewModelScope.launch { signedIn(effect.credentials) }
            }
            // What was typed is kept; the next code opens a new session.
            MfaEffect.Restart -> {
                _mfaResult.value = MfaResult.Ended(MfaError.Cancelled())
                _loginStep.value = LoginStep.CollectOtpIdentifier(identifier)
            }
            // Its error screen stays on show until the user starts over. The engine already logged the failure, once.
            is MfaEffect.Failed -> mfaFailed(effect.error)
        }
    }

    /**
     * Tells the app, once. When no factor can be used, the app may take over, so the error screen is held back
     * behind a spinner until it answers; every other error already has its screen, which the answer doesn't change.
     */
    private fun mfaFailed(error: MfaError) {
        _mfaResult.value = MfaResult.Ended(error)
        val mfa = _loginStep.value as? LoginStep.Mfa
        if (mfa != null && error is MfaError.NoEligibleFactors) {
            _loginStep.value = LoginStep.AwaitingApp(mfa.engine, mfa.identifier)
        }
        viewModelScope.launch { _mfaFailed.send(error) }
    }

    private suspend fun signedIn(credentials: Credentials) {
        _loginStep.value = LoginStep.Authenticated
        isRunning = false
        _authenticated.send(credentials)
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
