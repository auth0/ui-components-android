package com.auth0.universalcomponents.feature.embedded.data.repository

import com.auth0.android.embedded.EmbeddedAuthError
import com.auth0.android.embedded.EmbeddedAuthError.InsufficientAuthorization.Reason
import com.auth0.android.embedded.EmbeddedAuthException
import com.auth0.android.embedded.authorize.EmbeddedCapability
import com.auth0.android.embedded.authorize.IdentifierType
import com.auth0.android.embedded.authorize.NextAction
import com.auth0.android.embedded.authorize.OtpChannel
import com.auth0.android.embedded.authorize.OtpType
import com.auth0.android.embedded.authorize.PhoneDeliveryMethod
import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.data.embeddedauth.EmbeddedAuthSteps
import com.auth0.universalcomponents.data.embeddedauth.INVALID_CODE_REASONS
import com.auth0.universalcomponents.data.embeddedauth.StepAnswer
import com.auth0.universalcomponents.data.embeddedauth.nextActions
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.embedded.EmbeddedLoginPreference
import com.auth0.universalcomponents.feature.embedded.data.mapper.failure
import com.auth0.universalcomponents.feature.embedded.data.mapper.toLoginOption
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption
import com.auth0.universalcomponents.feature.embedded.domain.repository.EmbeddedLoginRepository
import com.auth0.universalcomponents.feature.mfachallenge.data.mapper.toMfaFactors
import com.auth0.universalcomponents.utils.isAsciiDigit
import com.auth0.universalcomponents.utils.logging.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The first factor against the SDK: authorize → identify → challengeEmail or challengePhone → verifyOtp, or
 * identify → verifyPassword. Each step before the last answers with `insufficient_authorization` and the next
 * actions to take.
 * [connection] is fixed for the whole flow; every resend, edited identifier and Try again opens a new session with it.
 * [steps] holds the client and its session, so MFA continues on the same instance. [preference] decides which of a code
 * and a password leads when the server offers both.
 */
internal class EmbeddedLoginRepositoryImpl(
    private val steps: EmbeddedAuthSteps,
    private val connection: String,
    private val preference: EmbeddedLoginPreference = EmbeddedLoginPreference.Server,
) : EmbeddedLoginRepository {

    /** Who a sign-in is for: what it takes to open it again. */
    private data class Identity(val method: LoginOption, val identifier: String)

    /**
     * Where the sign-in stands with the server. The SDK ends it on every failure except a continuation or a
     * network error, so this says whether there is anything left to continue.
     */
    private sealed interface Loop {
        /** Who the sign-in is for, once the server knows. */
        val identity: Identity? get() = null

        /** Nothing to continue: the next identifier opens a sign-in. */
        data object Closed : Loop

        /** Alive with nobody identified yet (discovery just opened it), or a re-open that stopped before that. */
        data class Opened(override val identity: Identity?) : Loop

        /** The server has the identifier and has said everything it offers. */
        data class Identified(override val identity: Identity) : Loop

        /** Alive, but a wrong password narrowed the offer to the password alone, so a code needs a new sign-in. */
        data class PasswordOnly(override val identity: Identity) : Loop

        /** A code was sent on this sign-in, which is where it is checked. */
        data class CodeSent(override val identity: Identity) : Loop

        /** The server ended it. Only a new sign-in for the same person continues. */
        data class Ended(override val identity: Identity) : Loop
    }

    private var loop: Loop = Loop.Closed

    /** What the server offered last. */
    private var lastNextActions: List<NextAction> = emptyList()

    /**
     * One call from the screens at a time, from its first SDK step to its last: a sign-in is authorize → identify →
     * challenge, and [loop] and [lastNextActions] are read and written all along it. The lock in [steps] only keeps
     * two SDK calls apart, so a second caller could still slip in between the steps of the first. A public function
     * never calls another, so this is never taken twice.
     */
    private val oneAtATime = Mutex()

    override suspend fun discover(): Result<List<LoginOption>, LoginFailure> = oneAtATime.withLock {
        if (connection.isBlank()) return@withLock Result.Error(LoginFailure(EmbeddedLoginError.ConnectionMissing))
        val opened = steps.run { authorize(connection, capabilities = CAPABILITIES) }
        if (opened is StepAnswer.Failed) {
            loop = Loop.Closed
            return@withLock Result.Error(opened.failure)
        }
        loop = Loop.Opened(null)
        lastNextActions = opened.nextActions
        Result.Success(opened.nextActions.map { it.toLoginOption() })
    }

    override suspend fun identify(
        method: LoginOption,
        identifier: String,
    ): Result<List<LoginOption>, LoginFailure> = oneAtATime.withLock {
        submit(Identity(method, identifier))?.let { return@withLock Result.Error(it) }
        val offer = offerTo(method)
        // A username has nothing after it but a password: the server offering none is the same as a wrong one.
        if (method == LoginOption.Username && LoginOption.Password !in offer) {
            return@withLock Result.Error(LoginFailure(EmbeddedLoginError.InvalidPassword(method)))
        }
        Result.Success(offer)
    }

    override suspend fun startOtp(
        method: LoginOption,
        identifier: String,
    ): Result<String?, LoginFailure> = oneAtATime.withLock {
        if (method != LoginOption.EmailOtp && method != LoginOption.PhoneOtp) {
            return@withLock Result.Error(LoginFailure(EmbeddedLoginError.CodeNotSent(method)))
        }
        val identity = Identity(method, identifier)
        // identify is good for one code: a code right after it goes on the same session, and anything else (a
        // resend, or a code after a wrong password or an ended session) starts over.
        if (loop != Loop.Identified(identity)) submit(identity)?.let { return@withLock Result.Error(it) }
        val phone = lastNextActions.phoneToCode().takeIf { method == LoginOption.PhoneOtp }
        val sent = when (method) {
            LoginOption.PhoneOtp -> sendPhoneCode(lastNextActions)
            else -> sendEmailCode(lastNextActions)
        }
        when (sent) {
            // No code is on its way, so the code screen would wait for nothing.
            null -> {
                val offered = lastNextActions.map { it.capability.value }
                Logger.w(TAG, "No $method code was sent; the server offered $offered")
                Result.Error(LoginFailure(EmbeddedLoginError.CodeNotSent(method)))
            }

            is StepAnswer.Failed -> Result.Error(sent.recorded())
            else -> {
                if (sent is StepAnswer.Next) lastNextActions = sent.actions
                loop = Loop.CodeSent(identity)
                Result.Success(if (method == LoginOption.PhoneOtp) sentToPhone(phone) else null)
            }
        }
    }

    /**
     * Opens the sign-in (the one [discover] opened, the first time) and tells the server who it is for, keeping what
     * it offers next. Null when that worked, else why not. A method nobody signs in as (the password, one these screens
     * can't drive) fails before anything is opened.
     */
    private suspend fun submit(identity: Identity): LoginFailure? {
        val identifierType = identity.method.identifierType
            ?: return LoginFailure(EmbeddedLoginError.Unknown("Cannot identify with ${identity.method}"))
        if (loop !is Loop.Opened) {
            val opened = steps.run { authorize(connection, capabilities = CAPABILITIES) }
            if (opened is StepAnswer.Failed) return opened.recorded()
        }
        // From here a failure still knows who this is for, so the next try can open it again.
        loop = Loop.Opened(identity)
        lastNextActions = emptyList()
        val identified = steps.run { identify(identity.identifier, identifierType) }
        if (identified is StepAnswer.Failed) {
            val failure = identified.recorded()
            // A rejected address or number can be fixed by the user, so it is not the generic failure. The SDK has no
            // error just for it: this step takes only the identifier, so a malformed request is taken to be about it,
            // and the server's own words go to the log through the cause.
            val rejection = (identified.error as? EmbeddedAuthException)?.takeIf {
                it.error == EmbeddedAuthError.InvalidRequest
            }
            return when {
                rejection == null -> failure
                // A username has no format to blame, so what the server said about it is shown as it came, as on iOS.
                identity.method == LoginOption.Username -> rejection.description.takeIf(String::isNotBlank)
                    ?.let { LoginFailure(EmbeddedLoginError.Server(it), cause = rejection) } ?: failure

                else -> LoginFailure(EmbeddedLoginError.InvalidIdentifier(identity.method), cause = rejection)
            }
        }
        lastNextActions = identified.nextActions
        loop = Loop.Identified(identity)
        return null
    }

    /**
     * What the server offers [method]: a code to this email or phone, and a password, for the methods in
     * [PASSWORD_METHODS]. A password the server lists for any other method is left out, as if it had not been offered.
     * A number that only takes calls counts as a code only when no password is offered (the call is the way left), which
     * for a phone is always. In the server's order, unless the app's [preference] puts the other first (see [led]).
     */
    private fun offerTo(method: LoginOption): List<LoginOption> {
        val hasPassword = method in PASSWORD_METHODS && lastNextActions.any { it is NextAction.VerifyPassword }
        return lastNextActions.mapNotNull { action ->
            when {
                action is NextAction.VerifyPassword -> LoginOption.Password.takeIf { hasPassword }
                method == LoginOption.EmailOtp && action is NextAction.ChallengeEmail -> LoginOption.EmailOtp
                method == LoginOption.EmailOtp && action is NextAction.VerifyOtp && action.channel == OtpChannel.EMAIL ->
                    LoginOption.EmailOtp

                method == LoginOption.PhoneOtp && action is NextAction.VerifyOtp && action.channel in PHONE_CHANNELS ->
                    LoginOption.PhoneOtp

                method == LoginOption.PhoneOtp && action is NextAction.ChallengePhone &&
                    (!hasPassword || action.canText) -> LoginOption.PhoneOtp

                else -> null
            }
        }.distinct().led()
    }

    /**
     * The first of the offer is what the screens lead with. When both a code and a password are on offer, the app's
     * [preference] picks which: the server's order stands for [EmbeddedLoginPreference.Server]. With only one of the two
     * on offer nothing changes, since a preference can't add what the server did not list.
     */
    private fun List<LoginOption>.led(): List<LoginOption> {
        if (size < 2 || LoginOption.Password !in this) return this
        return when (preference) {
            EmbeddedLoginPreference.Server -> this
            EmbeddedLoginPreference.Code -> sortedBy { it == LoginOption.Password }
            EmbeddedLoginPreference.Password -> sortedByDescending { it == LoginOption.Password }
        }
    }

    /** The step that sends the email code; null when the server offers none, [ALREADY_SENT] when it went out itself. */
    private suspend fun sendEmailCode(nextActions: List<NextAction>): StepAnswer<*>? {
        val challenge = nextActions.firstNotNullOfOrNull { it as? NextAction.ChallengeEmail }
        return when {
            challenge != null -> steps.run { challengeEmail(challenge.index) }
            nextActions.any { it is NextAction.VerifyOtp && it.channel == OtpChannel.EMAIL } -> ALREADY_SENT
            else -> null
        }
    }

    /**
     * As [sendEmailCode], for a phone: a text message, or a call when that is all the server can do for the number
     * (a list with no way named is a text, as in the SDK).
     */
    private suspend fun sendPhoneCode(nextActions: List<NextAction>): StepAnswer<*>? {
        val phone = nextActions.phoneToCode()
        return when {
            phone != null -> {
                val ways = phone.deliveryMethods.ifEmpty { listOf(PhoneDeliveryMethod.TEXT) }
                val way = if (PhoneDeliveryMethod.TEXT in ways) PhoneDeliveryMethod.TEXT else ways.first()
                steps.run { challengePhone(phone.index, way) }
            }

            nextActions.any { it is NextAction.VerifyOtp && it.channel in PHONE_CHANNELS } -> ALREADY_SENT
            else -> null
        }
    }

    /**
     * The phone a code goes to: the first one that can take a text, else the first there is (the call is the way left). A
     * voice-only phone listed ahead of a phone that can text is not the one to call.
     */
    private fun List<NextAction>.phoneToCode(): NextAction.ChallengePhone? {
        val phones = filterIsInstance<NextAction.ChallengePhone>()
        return phones.firstOrNull { it.canText } ?: phones.firstOrNull()
    }

    /**
     * Where the server says the code went, as it hides the number: what it names once the code is out (a code it has
     * sent lists its destination), else the number of the phone [phone] that was asked. Null when it names none, or
     * only one it hid completely (`*********`): that tells the user nothing, so the screen shows what they typed.
     */
    private fun sentToPhone(phone: NextAction.ChallengePhone?): String? {
        val named = lastNextActions
            .filterIsInstance<NextAction.VerifyOtp>()
            .firstOrNull { it.channel in PHONE_CHANNELS }
            ?.identifier
        return named?.takeIf { it.showsDigit() } ?: phone?.identifier?.takeIf { it.showsDigit() }
    }

    private fun String.showsDigit(): Boolean = any(Char::isAsciiDigit)

    override suspend fun verifyOtp(code: String): Result<Credentials, LoginFailure> = oneAtATime.withLock {
        // The code belongs to a sign-in the server ended (too many wrong codes, a timeout): only a new code helps.
        if (loop is Loop.Ended) return@withLock Result.Error(LoginFailure(EmbeddedLoginError.CodeNoLongerValid))
        settle(INVALID_CODE_REASONS, EmbeddedLoginError.InvalidCode) {
            steps.run { verifyOtp(code, OtpType.OOB) }
        }
    }

    override suspend fun verifyPassword(
        method: LoginOption,
        identifier: String,
        password: String,
    ): Result<Credentials, LoginFailure> = oneAtATime.withLock {
        val identity = Identity(method, identifier)
        // Anything but a sign-in the server has identified (it ended on a rate limit, a breach or a timeout, or a
        // re-open stopped half way) gets a new one for the same person first.
        if (loop != Loop.Identified(identity) && loop != Loop.PasswordOnly(identity)) {
            submit(identity)?.let { return@withLock Result.Error(it) }
            // What the server answers to the new sign-in may no longer include a password.
            if (lastNextActions.none { it is NextAction.VerifyPassword }) {
                return@withLock Result.Error(LoginFailure(passwordGone(method)))
            }
        }
        val result = settle(INVALID_PASSWORD_REASONS, EmbeddedLoginError.InvalidPassword(method)) {
            steps.run { verifyPassword(password) }
        }
        // Alive, but the server now offers nothing but the password.
        if (result is Result.Error && result.error.error is EmbeddedLoginError.InvalidPassword) {
            (loop as? Loop.Identified)?.let { loop = Loop.PasswordOnly(it.identity) }
        }
        result
    }

    /**
     * What to tell the user when the server no longer offers a password for [method]. A username has nothing after it
     * but a password, so, as in [identify], that reads as a wrong one; an email can be signed in with a code instead.
     */
    private fun passwordGone(method: LoginOption): EmbeddedLoginError =
        if (method == LoginOption.Username) {
            EmbeddedLoginError.InvalidPassword(method)
        } else {
            EmbeddedLoginError.PasswordNotAvailable
        }

    /**
     * Turns the answer of the step that checks a code or a password into a result. A rejection the user can fix on
     * their screen ([rejectedReasons], named [rejection]) comes back as it is and keeps the session; "not finished
     * yet" with no reason is a second factor; any other failure ends the session unless the SDK keeps it.
     */
    private suspend fun settle(
        rejectedReasons: Set<Reason>,
        rejection: EmbeddedLoginError,
        attempt: suspend () -> StepAnswer<Credentials>,
    ): Result<Credentials, LoginFailure> = when (val answer = attempt()) {
        is StepAnswer.Done -> {
            loop = Loop.Closed
            Result.Success(answer.value)
        }

        is StepAnswer.Next -> {
            lastNextActions = answer.actions
            when (answer.reason) {
                in rejectedReasons -> Result.Error(LoginFailure(rejection))
                // As in the SDK, a second factor comes back as an error, carrying the factors on offer. MFA decides
                // what happens when none of them can be used here, so the app hears about it the same way.
                Reason.NONE -> secondFactor(answer.actions)
                else -> Result.Error(LoginFailure(EmbeddedLoginError.Unknown("Unexpected reason ${answer.reason}")))
            }
        }

        // The server can also say outright that the account has no usable second factor: MFA tells the user.
        is StepAnswer.Failed -> if (answer.isNoEligibleFactors) {
            loop = Loop.Closed
            Result.Error(LoginFailure(EmbeddedLoginError.MfaRequired.NoUsableFactor))
        } else {
            Result.Error(answer.recorded())
        }
    }

    private fun secondFactor(actions: List<NextAction>): Result<Nothing, LoginFailure> {
        loop = Loop.Closed
        val factors = actions.toMfaFactors()
        return if (factors.isEmpty()) {
            Logger.w(TAG, "No usable second factor; the server offered ${actions.map { it.capability.value }}")
            Result.Error(LoginFailure(EmbeddedLoginError.MfaRequired.NoUsableFactor))
        } else {
            Result.Error(LoginFailure(EmbeddedLoginError.MfaRequired(factors)))
        }
    }

    /** The failure in our terms; unless the SDK keeps the session after it, that session has ended. */
    private fun StepAnswer.Failed.recorded(): LoginFailure {
        val failure = failure
        if (!failure.canRetryStep) loop = loop.identity?.let(Loop::Ended) ?: Loop.Closed
        return failure
    }

    private val StepAnswer.Failed.isNoEligibleFactors: Boolean
        get() = (error as? EmbeddedAuthException)?.error == EmbeddedAuthError.NoEligibleFactors

    private val LoginOption.identifierType: IdentifierType?
        get() = when (this) {
            LoginOption.EmailOtp -> IdentifierType.EMAIL
            LoginOption.PhoneOtp -> IdentifierType.PHONE
            LoginOption.Username -> IdentifierType.USERNAME
            LoginOption.Password, is LoginOption.Unsupported -> null
        }

    /** A list with no way named is a text, as in the SDK. */
    private val NextAction.ChallengePhone.canText: Boolean
        get() = deliveryMethods.isEmpty() || PhoneDeliveryMethod.TEXT in deliveryMethods

    private companion object {
        const val TAG = "EmbeddedRepository"

        /** For a code the server sent on its own, as it said in its menu: nothing is left to do. */
        val ALREADY_SENT: StepAnswer<Unit> = StepAnswer.Done(Unit)

        val PHONE_CHANNELS = setOf(OtpChannel.SMS, OtpChannel.VOICE)

        /**
         * The methods a password can be offered for. A phone number's password is not supported yet: the server may list
         * one for it, but a phone is offered its code only. Adding [LoginOption.PhoneOtp] here is what turns it back on;
         * the screens, the ViewModel and the wording for it are still in place.
         */
        val PASSWORD_METHODS = setOf(LoginOption.EmailOtp, LoginOption.Username)

        /** The reason the server gives when the username or password was wrong; it doesn't say which. */
        val INVALID_PASSWORD_REASONS = setOf(Reason.INVALID_IDENTIFIER_OR_PASSWORD)

        /**
         * What these screens can drive, told to the server so it only offers those: a code sent to an email or a phone,
         * a username with its password, then a push, a code sent to a phone or an email, the code of an authenticator
         * app, or a recovery code (typed, then confirmed once its replacement is saved). One fixed set, sent once when
         * the sign-in opens, for the whole sign-in.
         */
        val CAPABILITIES = setOf(
            EmbeddedCapability.IDENTIFY_EMAIL,
            EmbeddedCapability.CHALLENGE_EMAIL,
            EmbeddedCapability.IDENTIFY_PHONE,
            EmbeddedCapability.IDENTIFY_USERNAME,
            EmbeddedCapability.VERIFY_OTP,
            EmbeddedCapability.VERIFY_PASSWORD,
            EmbeddedCapability.CHALLENGE_PUSH,
            EmbeddedCapability.CHALLENGE_PHONE,
            EmbeddedCapability.VERIFY_OOB,
            EmbeddedCapability.VERIFY_RECOVERY_CODE,
            EmbeddedCapability.CONFIRM_RECOVERY_CODE,
        )
    }
}
