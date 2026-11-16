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
 * Drives the first factor, by an email with a code or a password, a phone number with a code, or a username with its
 * password: [LoginIntent]s in, [LoginState]s out. Each event counts only on the screen it belongs to.
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

    /** The first screen's method, the one the tenant lists first; Back from another method's screen returns to it. */
    private var primaryMethod: LoginOption = LoginOption.EmailOtp

    /** The methods this flow can draw, in the tenant's order. The first is [primaryMethod]; the rest are its buttons. */
    private var methods: List<LoginOption> = emptyList()

    /** The country the phone number was last submitted with, so every way back to its screen shows it again. */
    private var typedCountry: Country? = null

    /**
     * The last code sent, to whom, and how long until another may be asked for. It outlives the screens: a code asked
     * for from the password screen waits as long as Resend does, so switching between the two can't send one on
     * every tap, and it keeps counting while the user is elsewhere.
     */
    private data class CodeWait(val method: LoginOption, val identifier: String, val secondsLeft: Int)

    private var codeWait: CodeWait? = null

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
                    launchStep { submitIdentifier(event.method, event.value) }
                }

            // A tap on a button that was just replaced, or one while a request is on its way, is not a switch.
            is LoginIntent.SwitchMethod ->
                if (current is LoginState.CollectOtpIdentifier && !current.isSubmitting &&
                    event.method in current.otherMethods
                ) {
                    _loginState.value = identifierStep(event.method)
                }

            is LoginIntent.SubmitOtp -> if (current is LoginState.CollectOtpCode) {
                launchStep { verify(current.method, current.identifier, event.code) }
            }

            is LoginIntent.SubmitPassword -> if (current is LoginState.CollectPassword) {
                launchStep { verifyPassword(current.method, current.identifier, event.password) }
            }

            LoginIntent.Resend ->
                if (current is LoginState.CollectOtpCode && current.resendSecondsLeft == 0 && !current.isBusy) {
                    launchStep { resend(current) }
                }

            LoginIntent.UsePassword ->
                if (current is LoginState.CollectOtpCode && current.canUsePassword && !current.isBusy) {
                    launchStep { usePassword(current) }
                }

            LoginIntent.SendCode ->
                if (current is LoginState.CollectPassword && current.canSendCode &&
                    current.resendSecondsLeft == 0 && !current.isBusy
                ) {
                    launchStep { sendCodeInstead(current) }
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
     * Code or password entry goes back to the screen the identifier was typed on, and a secondary method's screen back
     * to the first one; MFA decides its own Back (true); anywhere else leaves (false). Once MFA has ended, its error
     * screen is all that is left of it, so Back leaves it, as its "Start over" does. So does Back while waiting for the
     * app: the user doesn't wait for an app that never answers.
     */
    fun goBack(): Boolean {
        val current = _loginState.value
        if (current is LoginState.Mfa) {
            if (current.engine.isRunning) current.engine.back() else leaveMfa(current)
            return true
        }
        if (current is LoginState.AwaitingApp) {
            _loginState.value = firstScreen(current.method, current.identifier)
            return true
        }
        // Stay put until the request in flight answers, rather than dropping it halfway.
        if (current.isBusy) return true
        if (current is LoginState.CollectOtpCode) {
            _loginState.value = identifierStep(current.method, current.identifier)
            return true
        }
        if (current is LoginState.CollectPassword) {
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
                // The tenant's order: the first it lists is the first screen, the rest are its buttons.
                methods = options.filter { it in FIRST_FACTORS }.distinct()
                val primary = methods.firstOrNull()
                if (primary != null) primaryMethod = primary
                _loginState.value = if (primary != null) identifierStep(primary) else LoginState.NoMethodsAvailable
            }
            .onError { failure -> _loginState.value = failed(failure, retryTo = null) }
    }

    /**
     * Tells the server who is signing in, then follows what it lists first: a code is sent at once, or the password is
     * asked for. The other, when offered, is the alternative on that screen.
     */
    private suspend fun submitIdentifier(method: LoginOption, identifier: String) {
        _loginState.value = identifierStep(method, identifier, isSubmitting = true)
        repository.identify(method, identifier)
            .onSuccess { offer -> follow(offer, method, identifier) }
            .onError { failure -> requestFailed(failure, method, identifier) }
    }

    private suspend fun follow(offer: List<LoginOption>, method: LoginOption, identifier: String) {
        when (offer.firstOrNull()) {
            // Nothing to send and nothing to ask for.
            null -> requestFailed(LoginFailure(EmbeddedLoginError.CodeNotSent(method)), method, identifier)

            LoginOption.Password -> _loginState.value = LoginState.CollectPassword(
                identifier = identifier,
                method = method,
                canSendCode = method in offer,
                resendSecondsLeft = waitLeft(method, identifier),
            )

            else -> sendCode(method, identifier, canUsePassword = LoginOption.Password in offer)
        }
    }

    /**
     * Sends a code to someone the sign-in already knows, and goes to code entry, which offers the password when
     * [canUsePassword]. Each code sent restarts the resend countdown. A failure here, including no code going out, is
     * one for good: Try again returns to the identifier entry with what was typed kept.
     */
    private suspend fun sendCode(method: LoginOption, identifier: String, canUsePassword: Boolean) {
        repository.startOtp(method, identifier)
            .onSuccess {
                codeWait = CodeWait(method, identifier, RESEND_COOLDOWN_SECONDS)
                _loginState.value = LoginState.CollectOtpCode(
                    identifier = identifier,
                    method = method,
                    resendSecondsLeft = RESEND_COOLDOWN_SECONDS,
                    canUsePassword = canUsePassword,
                )
                startResendCountdown()
            }
            .onError { failure -> requestFailed(failure, method, identifier) }
    }

    /** Resend: a new code to the same person. The password stays on offer when it was. */
    private suspend fun resend(current: LoginState.CollectOtpCode) {
        _loginState.value = current.copy(error = null, isResending = true)
        sendCode(current.method, current.identifier, current.canUsePassword)
    }

    /** "Email me a code" on the password screen; the code screen that follows offers the password again. */
    private suspend fun sendCodeInstead(current: LoginState.CollectPassword) {
        _loginState.value = current.copy(error = null, isSendingCode = true)
        sendCode(current.method, current.identifier, canUsePassword = true)
    }

    /**
     * "Continue with a password" on the code screen: the server is asked again who this is, on a new session, so the
     * password is asked for on one that still offers it. The code that was sent goes with the old one.
     */
    private suspend fun usePassword(current: LoginState.CollectOtpCode) {
        val (method, identifier) = current.method to current.identifier
        _loginState.value = current.copy(error = null, isOpeningPassword = true)
        repository.identify(method, identifier)
            .onSuccess { offer ->
                if (LoginOption.Password in offer) {
                    _loginState.value = LoginState.CollectPassword(
                        identifier = identifier,
                        method = method,
                        canSendCode = method in offer,
                        resendSecondsLeft = waitLeft(method, identifier),
                    )
                } else {
                    requestFailed(LoginFailure(EmbeddedLoginError.PasswordNotAvailable), method, identifier)
                }
            }
            .onError { failure -> requestFailed(failure, method, identifier) }
    }

    /**
     * The failure of a request made from the screen on show: a rate limit stays there, since that screen works again
     * once the wait is over; what the user can fix by typing again is shown under the identifier's field; anything else
     * is the full-screen error, whose Try again returns to the identifier entry with what was typed kept.
     */
    private fun requestFailed(failure: LoginFailure, method: LoginOption, identifier: String) {
        val error = failure.error
        _loginState.value = when {
            error.isRateLimit -> _loginState.value.settled(error)
            error.isFixedOnIdentifier -> identifierStep(method, identifier, error = error)
            else -> failed(failure, retryTo = identifierStep(method, identifier))
        }
    }

    /**
     * A wrong, expired or no longer valid code stays on code entry with the error under the field. Any other failure is
     * the full-screen error, and Try again returns to code entry: if the server ended the sign-in, the next Verify says
     * so and Resend is open. A second factor hands over to the MFA flow, which carries on with the same session.
     */
    private suspend fun verify(method: LoginOption, identifier: String, code: String) {
        updateCodeStep { copy(error = null, isSubmitting = true) }
        repository.verifyOtp(code)
            .onSuccess { credentials -> signedIn(credentials) }
            .onError { failure ->
                when (val problem = failure.error) {
                    is EmbeddedLoginError.MfaRequired -> startMfa(method, identifier, problem.factors)

                    EmbeddedLoginError.InvalidCode -> updateCodeStep { copy(error = problem, isSubmitting = false) }

                    // That code can't be used any more, so a new one can be asked for at once.
                    EmbeddedLoginError.CodeExpired, EmbeddedLoginError.CodeNoLongerValid -> {
                        endWait(method, identifier)
                        updateCodeStep { copy(error = problem, isSubmitting = false, resendSecondsLeft = 0) }
                    }

                    else -> if (problem.isRateLimit) {
                        updateCodeStep { copy(error = problem, isSubmitting = false) }
                    } else {
                        _loginState.value = failed(failure, retryTo = _loginState.value.settled(null))
                    }
                }
            }
    }

    /**
     * A wrong password, or too many tries, stays on password entry with the error under the field. Any other failure
     * is the full-screen error, and Try again returns to password entry. A second factor hands over to the MFA flow.
     */
    private suspend fun verifyPassword(method: LoginOption, identifier: String, password: String) {
        updatePasswordStep { copy(error = null, isSubmitting = true) }
        repository.verifyPassword(method, identifier, password)
            .onSuccess { credentials -> signedIn(credentials) }
            .onError { failure ->
                when (val problem = failure.error) {
                    is EmbeddedLoginError.MfaRequired -> startMfa(method, identifier, problem.factors)

                    else -> if (problem is EmbeddedLoginError.InvalidPassword || problem.isRateLimit) {
                        updatePasswordStep { copy(error = problem, isSubmitting = false) }
                    } else {
                        _loginState.value = failed(failure, retryTo = _loginState.value.settled(null))
                    }
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

    /** Back to the first screen: the code or password is spent, so the first factor starts again. */
    private fun leaveMfa(mfa: LoginState.Mfa) {
        mfa.engine.stop()
        // An ending already recorded (an error screen being left) stays what it was.
        if (_mfaResult.value == null) finishMfa(MfaResult.Ended(MfaError.Cancelled()))
        _loginState.value = firstScreen(mfa.method, mfa.identifier)
    }

    private fun endMfa(method: LoginOption, identifier: String, effect: MfaEffect) {
        when (effect) {
            is MfaEffect.Authenticated -> {
                finishMfa(MfaResult.SignedIn)
                viewModelScope.launch { signedIn(effect.credentials) }
            }
            // The first screen, with what was typed kept when it is the same method.
            MfaEffect.Restart -> {
                finishMfa(MfaResult.Ended(MfaError.Cancelled()))
                _loginState.value = firstScreen(method, identifier)
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

    /** How long until another code may be asked for [identifier]; zero when none was sent to them, or it is over. */
    private fun waitLeft(method: LoginOption, identifier: String): Int =
        codeWait?.takeIf { it.method == method && it.identifier == identifier }?.secondsLeft ?: 0

    /** The code can't be used any more, so a new one may be asked for at once. */
    private fun endWait(method: LoginOption, identifier: String) {
        val wait = codeWait ?: return
        if (wait.method == method && wait.identifier == identifier) codeWait = wait.copy(secondsLeft = 0)
    }

    /** Counts the wait down a second at a time, whichever screen is on show; it stops at zero. */
    private fun startResendCountdown() {
        resendCountdownJob?.cancel()
        resendCountdownJob = viewModelScope.launch {
            while ((codeWait?.secondsLeft ?: 0) > 0) {
                delay(ONE_SECOND_MILLIS)
                val wait = codeWait ?: return@launch
                codeWait = wait.copy(secondsLeft = (wait.secondsLeft - 1).coerceAtLeast(0))
                showWait()
            }
        }
    }

    /** Puts the wait on the code or password screen of the person it is for. */
    private fun showWait() {
        when (val current = _loginState.value) {
            is LoginState.CollectOtpCode ->
                _loginState.value = current.copy(resendSecondsLeft = waitLeft(current.method, current.identifier))

            is LoginState.CollectPassword ->
                _loginState.value = current.copy(resendSecondsLeft = waitLeft(current.method, current.identifier))

            else -> Unit
        }
    }

    /** Changes code entry in place, so its countdown carries on; does nothing once that step is left. */
    private inline fun updateCodeStep(change: LoginState.CollectOtpCode.() -> LoginState.CollectOtpCode) {
        val current = _loginState.value as? LoginState.CollectOtpCode ?: return
        _loginState.value = current.change()
    }

    /** Changes password entry in place; does nothing once that step is left. */
    private inline fun updatePasswordStep(change: LoginState.CollectPassword.() -> LoginState.CollectPassword) {
        val current = _loginState.value as? LoginState.CollectPassword ?: return
        _loginState.value = current.change()
    }

    /**
     * The identifier screen for [method]; the other methods are offered on the first screen only, as designed, and a
     * method opened from there is its own screen ([LoginState.CollectOtpIdentifier.isFirst] is false).
     */
    private fun identifierStep(
        method: LoginOption,
        value: String = "",
        isSubmitting: Boolean = false,
        error: EmbeddedLoginError? = null,
    ): LoginState.CollectOtpIdentifier {
        val isFirst = method == primaryMethod
        return LoginState.CollectOtpIdentifier(
            method = method,
            value = value,
            otherMethods = if (isFirst) methods.filter { it != method } else emptyList(),
            isSubmitting = isSubmitting,
            country = typedCountry.takeIf { method == LoginOption.PhoneOtp && value.isNotEmpty() },
            error = error,
            isFirst = isFirst,
        )
    }

    /** The first screen, pre-filled with what was typed when it asks for the same [method]. */
    private fun firstScreen(method: LoginOption, identifier: String) =
        identifierStep(primaryMethod, value = if (method == primaryMethod) identifier else "")

    /** The same screen with nothing on its way, showing [error] if any. */
    private fun LoginState.settled(error: EmbeddedLoginError?): LoginState = when (this) {
        is LoginState.CollectOtpIdentifier -> copy(error = error, isSubmitting = false)
        is LoginState.CollectOtpCode ->
            copy(error = error, isSubmitting = false, isResending = false, isOpeningPassword = false)
        is LoginState.CollectPassword -> copy(error = error, isSubmitting = false, isSendingCode = false)
        else -> this
    }

    /** The full-screen error. Logged once here, with its cause, so a failure in a live app can be traced. */
    private fun failed(failure: LoginFailure, retryTo: LoginState?): LoginState {
        Logger.e(TAG, "Login step failed: ${failure.error}", failure.cause)
        return LoginState.Failed(failure.error, retryTo)
    }

    private suspend fun retry(failed: LoginState.Failed) {
        val target = failed.retryTo
        if (target != null) _loginState.value = target.withCurrentWait() else discover()
    }

    /** A screen kept for Try again shows the wait as it stands now, not as it stood when it was left. */
    private fun LoginState.withCurrentWait(): LoginState = when (this) {
        is LoginState.CollectOtpCode -> copy(resendSecondsLeft = waitLeft(method, identifier))
        is LoginState.CollectPassword -> copy(resendSecondsLeft = waitLeft(method, identifier))
        else -> this
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

        /** The ways in these screens can draw; whichever the tenant lists first is the first screen. */
        val FIRST_FACTORS = setOf(LoginOption.EmailOtp, LoginOption.PhoneOtp, LoginOption.Username)

        /** The server wants the user to wait; the screen they are on works again once it is over. */
        val EmbeddedLoginError.isRateLimit: Boolean
            get() = this == EmbeddedLoginError.TooManyWrongCodes ||
                this == EmbeddedLoginError.TooManyAttempts ||
                this == EmbeddedLoginError.TooManyLogins

        /** What the user can fix on the identifier screen, so it shows there rather than on the error screen. */
        val EmbeddedLoginError.isFixedOnIdentifier: Boolean
            get() = this is EmbeddedLoginError.CodeNotSent ||
                this == EmbeddedLoginError.PasswordNotAvailable ||
                this is EmbeddedLoginError.InvalidPassword

        /** A request for this screen is in flight. */
        val LoginState.isBusy: Boolean
            get() = when (this) {
                is LoginState.CollectOtpIdentifier -> isSubmitting
                is LoginState.CollectOtpCode -> isSubmitting || isResending || isOpeningPassword
                is LoginState.CollectPassword -> isSubmitting || isSendingCode
                else -> false
            }
    }
}
