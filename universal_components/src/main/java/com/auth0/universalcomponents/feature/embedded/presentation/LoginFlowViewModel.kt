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
import com.auth0.universalcomponents.presentation.ui.mfa.phone.Country
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
 * Drives the code flow, by email or by phone: [LoginIntent]s in, [LoginState]s out. Each event counts only on the
 * screen it belongs to.
 * A second factor runs in an [MfaEngine] owned here, so the hand-off and its end are plain calls on this ViewModel.
 * The repositories return each failure as a [LoginFailure], so nothing here is caught.
 */
internal class LoginFlowViewModel(
    private val repository: EmbeddedLoginRepository,
    private val mfaRepository: MfaRepository,
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Loading)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)

    /**
     * What happens once, not what the screen draws: signing in, and MFA ending with an error (for `onMfaError`; the
     * user leaving MFA is not one). The error event is not a second record of the ending: [finishMfa] derives it from
     * [mfaResult], in one place.
     */
    val events: Flow<LoginEvent> = _events.receiveAsFlow()

    private val _mfaResult = MutableStateFlow<MfaResult?>(null)

    /**
     * How the last MFA ended; null while one runs, and again once the next starts. It is kept, so a headless app that
     * is redrawn (a rotation) still reads it, which an event could not offer. Only [finishMfa] sets it.
     */
    val mfaResult: StateFlow<MfaResult?> = _mfaResult.asStateFlow()

    /** False before the first Start, and again once the flow signed in or was left; the next Start begins anew. */
    private var isRunning = false

    private var runningStep: Job? = null
    private var resendCountdownJob: Job? = null

    /** The method the flow starts on and Back returns to; set by [discover]. */
    private var primaryMethod: LoginOption = LoginOption.EmailOtp

    /** Whether the server also offers a phone, so the first screen can offer to switch to it. */
    private var isPhoneOffered = false

    /** The country the phone number was last submitted with, so every way back to its screen shows it again. */
    private var typedCountry: Country? = null

    fun send(event: LoginIntent) {
        val current = _loginState.value
        when (event) {
            // A Start mid-flow (after a rotation, say) keeps the user's place.
            LoginIntent.Start -> if (!isRunning) {
                isRunning = true
                launchStep { discover() }
            }

            is LoginIntent.SubmitIdentifier ->
                if (current is LoginState.CollectOtpIdentifier && current.method == event.method) {
                    typedCountry = event.country
                    launchStep { sendCode(event.method, event.value, isResend = false) }
                }

            // A tap on a button that was just replaced, or one while a code is sent, is not a switch.
            is LoginIntent.SwitchMethod ->
                if (current is LoginState.CollectOtpIdentifier && !current.isSubmitting &&
                    event.method == current.otherMethod
                ) {
                    _loginState.value = identifierStep(event.method)
                }

            is LoginIntent.SubmitOtp -> if (current is LoginState.CollectOtpCode) {
                launchStep { verify(current.method, current.identifier, event.code) }
            }

            LoginIntent.Resend -> if (current is LoginState.CollectOtpCode && !current.isCountingDown) {
                launchStep { sendCode(current.method, current.identifier, isResend = true) }
            }

            LoginIntent.Retry -> if (current is LoginState.Failed) launchStep { retry(current) }

            // Its own event, not Back: a second tap on the screen still sliding out must not leave the login.
            LoginIntent.LeaveMfa -> if (current is LoginState.Mfa) leaveMfa(current)

            // An app that doesn't take over leaves the user on MFA's own "not available" screen.
            is LoginIntent.MfaErrorAnswered ->
                if (current is LoginState.AwaitingApp && event.handling == MfaErrorHandling.NotHandled) {
                    _loginState.value = LoginState.Mfa(current.engine, current.identifier, current.method)
                }
        }
    }

    /**
     * Code entry goes back to the screen the identifier was typed on, and a secondary method's screen back to the
     * primary one; MFA decides its own Back (true); anywhere else leaves (false). Once MFA has ended, its error screen
     * is all that is left of it, so Back leaves it, as its "Start over" does. So does Back while waiting for the app:
     * the user doesn't wait for an app that never answers.
     */
    fun goBack(): Boolean {
        val current = _loginState.value
        if (current is LoginState.Mfa) {
            if (current.engine.isRunning) current.engine.back() else leaveMfa(current)
            return true
        }
        if (current is LoginState.AwaitingApp) {
            _loginState.value = identifierStep(current.method, current.identifier)
            return true
        }
        // Stay put until the request in flight answers, rather than dropping it halfway.
        if (current.isBusy) return true
        if (current is LoginState.CollectOtpCode) {
            _loginState.value = identifierStep(current.method, current.identifier)
            return true
        }
        if (current is LoginState.CollectOtpIdentifier && current.method != primaryMethod) {
            _loginState.value = identifierStep(primaryMethod)
            return true
        }
        isRunning = false
        return false
    }

    private suspend fun discover() {
        _loginState.value = LoginState.Loading
        repository.discover()
            .onSuccess { options ->
                // Email ranks above phone, whatever order the server lists them in.
                val primary = FIRST_FACTORS.firstOrNull { it in options }
                isPhoneOffered = LoginOption.PhoneOtp in options
                if (primary != null) primaryMethod = primary
                _loginState.value = if (primary != null) identifierStep(primary) else LoginState.NoMethodsAvailable
            }
            .onError { failure -> _loginState.value = failed(failure, retryTo = null) }
    }

    /**
     * Each code sent restarts the resend countdown. A failure here, including no code going out,
     * is one for good: Try again returns to the identifier entry with what was typed kept.
     */
    private suspend fun sendCode(method: LoginOption, identifier: String, isResend: Boolean) {
        _loginState.value = if (isResend) {
            LoginState.CollectOtpCode(identifier, method, isResending = true)
        } else {
            identifierStep(method, identifier, isSubmitting = true)
        }
        repository.startOtp(method, identifier)
            .onSuccess {
                _loginState.value =
                    LoginState.CollectOtpCode(identifier, method, resendSecondsLeft = RESEND_COOLDOWN_SECONDS)
                startResendCountdown()
            }
            .onError { failure ->
                _loginState.value = failed(failure, retryTo = identifierStep(method, identifier))
            }
    }

    /**
     * A wrong or expired code stays on code entry with the error under the field.
     * After a failure for good, Try again returns to code entry only if the session survived, else to the identifier
     * entry.
     * A second factor hands over to the MFA flow, which carries on with the same session.
     */
    private suspend fun verify(method: LoginOption, identifier: String, code: String) {
        updateCodeStep { copy(error = null, isSubmitting = true) }
        repository.verifyOtp(code)
            .onSuccess { credentials -> signedIn(credentials) }
            .onError { failure ->
                when (val error = failure.error) {
                    is EmbeddedLoginError.MfaRequired -> startMfa(method, identifier, error.factors)

                    EmbeddedLoginError.InvalidCode ->
                        updateCodeStep { copy(error = failure.error, isSubmitting = false) }

                    // That code can't be used any more, so a new one can be asked for at once.
                    EmbeddedLoginError.CodeExpired ->
                        updateCodeStep { copy(error = failure.error, isSubmitting = false, resendSecondsLeft = 0) }

                    else -> _loginState.value = failed(
                        failure,
                        retryTo = if (failure.canRetryStep) {
                            LoginState.CollectOtpCode(identifier, method)
                        } else {
                            identifierStep(method, identifier)
                        },
                    )
                }
            }
    }

    /**
     * MFA runs in its own engine, a new one for each visit so nothing from an old one reaches the screen. It stops
     * itself when it ends. It is shown as [LoginState.Mfa] and draws and takes its own taps, so all login sees is Back
     * and how it ends, decided here. [identifier] is what the user typed for the first factor with [method], kept by
     * login (not in the MFA state) to pre-fill it if the user starts over.
     */
    private fun startMfa(method: LoginOption, identifier: String, factors: List<MfaFactor>) {
        _mfaResult.value = null
        val engine = MfaEngine(
            reducer = MfaReducer(factors),
            repository = mfaRepository,
            hostScope = viewModelScope,
            onEffect = { effect -> endMfa(method, identifier, effect) },
        )
        // Shown before it starts, so an end reached while starting replaces this step rather than being overwritten.
        _loginState.value = LoginState.Mfa(engine, identifier, method)
        engine.start()
    }

    /** Back to the identifier entry with what was typed kept; the next code opens a new session. */
    private fun leaveMfa(mfa: LoginState.Mfa) {
        mfa.engine.stop()
        // An ending already recorded (an error screen being left) stays what it was.
        if (_mfaResult.value == null) finishMfa(MfaResult.Ended(MfaError.Cancelled()))
        _loginState.value = identifierStep(mfa.method, mfa.identifier)
    }

    private fun endMfa(method: LoginOption, identifier: String, effect: MfaEffect) {
        when (effect) {
            is MfaEffect.Authenticated -> {
                finishMfa(MfaResult.SignedIn)
                viewModelScope.launch { signedIn(effect.credentials) }
            }
            // What was typed is kept; the next code opens a new session.
            MfaEffect.Restart -> {
                finishMfa(MfaResult.Ended(MfaError.Cancelled()))
                _loginState.value = identifierStep(method, identifier)
            }
            // Its error screen stays on show until the user starts over. The engine already logged the failure, once.
            is MfaEffect.Failed -> finishMfa(MfaResult.Ended(effect.error))
        }
    }

    /**
     * The one place an MFA ending is recorded: [mfaResult] keeps it, and an error is also told to the app, once, as
     * a [LoginEvent.MfaFailed]. The user leaving (Cancelled) is a result but not an error, so it is not told.
     */
    private fun finishMfa(result: MfaResult) {
        _mfaResult.value = result
        if (result is MfaResult.Ended && result.error !is MfaError.Cancelled) tellApp(result.error)
    }

    /**
     * When no factor can be used the app may take over, so the error screen is held back behind a spinner until it
     * answers. Every other error already has its screen, which the answer doesn't change: it doesn't wait.
     */
    private fun tellApp(error: MfaError) {
        val mfa = _loginState.value as? LoginState.Mfa
        if (mfa != null && error is MfaError.NoEligibleFactors) {
            _loginState.value = LoginState.AwaitingApp(mfa.engine, mfa.identifier, mfa.method)
        }
        _events.trySend(LoginEvent.MfaFailed(error))
    }

    private suspend fun signedIn(credentials: Credentials) {
        _loginState.value = LoginState.Authenticated
        isRunning = false
        _events.send(LoginEvent.Authenticated(credentials))
    }

    /** Counts the resend wait down a second at a time; it stops at zero, or once code entry is left. */
    private fun startResendCountdown() {
        resendCountdownJob?.cancel()
        resendCountdownJob = viewModelScope.launch {
            while (_loginState.value.isCountingDown) {
                delay(ONE_SECOND_MILLIS)
                updateCodeStep { copy(resendSecondsLeft = (resendSecondsLeft - 1).coerceAtLeast(0)) }
            }
        }
    }

    /** Changes code entry in place, so its countdown carries on; does nothing once that step is left. */
    private inline fun updateCodeStep(change: LoginState.CollectOtpCode.() -> LoginState.CollectOtpCode) {
        val current = _loginState.value as? LoginState.CollectOtpCode ?: return
        _loginState.value = current.change()
    }

    /** The identifier screen for [method]; the way to the phone is offered on the primary screen only, as designed. */
    private fun identifierStep(method: LoginOption, value: String = "", isSubmitting: Boolean = false) =
        LoginState.CollectOtpIdentifier(
            method = method,
            value = value,
            otherMethod = LoginOption.PhoneOtp.takeIf { isPhoneOffered && method == primaryMethod && method != it },
            isSubmitting = isSubmitting,
            country = typedCountry.takeIf { method == LoginOption.PhoneOtp && value.isNotEmpty() },
        )

    /** The full-screen error. Logged once here, with its cause, so a failure in a live app can be traced. */
    private fun failed(failure: LoginFailure, retryTo: LoginState?): LoginState {
        Logger.e(TAG, "Login step failed: ${failure.error}", failure.cause)
        return LoginState.Failed(failure.error, retryTo)
    }

    private suspend fun retry(failed: LoginState.Failed) {
        val target = failed.retryTo
        if (target != null) _loginState.value = target else discover()
    }

    /** Runs one step at a time; a second tap while one is in flight is dropped. */
    private fun launchStep(block: suspend () -> Unit) {
        if (runningStep?.isActive == true) return
        runningStep = viewModelScope.launch { block() }
    }

    private companion object {
        const val TAG = "LoginFlowViewModel"
        const val RESEND_COOLDOWN_SECONDS = 60
        const val ONE_SECOND_MILLIS = 1_000L

        /** The first factors these screens can draw, most phishing-resistant first; the first offered is primary. */
        val FIRST_FACTORS = listOf(LoginOption.EmailOtp, LoginOption.PhoneOtp)

        /** Resend is still waiting out its cooldown. */
        val LoginState.isCountingDown: Boolean
            get() = this is LoginState.CollectOtpCode && resendSecondsLeft > 0

        /** A request for this screen is in flight. */
        val LoginState.isBusy: Boolean
            get() = when (this) {
                is LoginState.CollectOtpIdentifier -> isSubmitting
                is LoginState.CollectOtpCode -> isSubmitting || isResending
                else -> false
            }
    }
}
