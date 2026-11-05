package com.auth0.universalcomponents.feature.embedded.data.repository

import com.auth0.android.embedded.EmbeddedAuthError
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
import com.auth0.universalcomponents.domain.network.EmptyResult
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.embedded.data.mapper.failure
import com.auth0.universalcomponents.feature.embedded.data.mapper.toLoginOption
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption
import com.auth0.universalcomponents.feature.embedded.domain.repository.EmbeddedLoginRepository
import com.auth0.universalcomponents.feature.mfachallenge.data.mapper.toMfaFactors
import com.auth0.universalcomponents.utils.logging.Logger

/**
 * The first factor against the SDK: authorize → identify → challengeEmail or challengePhone → verifyOtp.
 * Each step before the last answers with `insufficient_authorization` and the next actions to take.
 * [connection] is fixed for the whole flow; every resend, edited email and Try again opens a new session with it.
 * [steps] holds the client and its session, so MFA continues on the same instance.
 */
internal class EmbeddedLoginRepositoryImpl(
    private val steps: EmbeddedAuthSteps,
    private val connection: String,
) : EmbeddedLoginRepository {

    /** Lets the first [startOtp] reuse the session [discover] opened. */
    private var isDiscoverySessionUnused = false

    override suspend fun discover(): Result<List<LoginOption>, LoginFailure> {
        if (connection.isBlank()) return Result.Error(LoginFailure(EmbeddedLoginError.ConnectionMissing))
        val opened = steps.run { authorize(connection, capabilities = CAPABILITIES) }
        if (opened is StepAnswer.Failed) return Result.Error(opened.failure)
        isDiscoverySessionUnused = true
        return Result.Success(opened.nextActions.map { it.toLoginOption() })
    }

    override suspend fun startOtp(method: LoginOption, identifier: String): EmptyResult<LoginFailure> {
        val identifierType = when (method) {
            LoginOption.EmailOtp -> IdentifierType.EMAIL
            LoginOption.PhoneOtp -> IdentifierType.PHONE
            is LoginOption.Unsupported -> return Result.Error(LoginFailure(EmbeddedLoginError.CodeNotSent(method)))
        }
        if (!isDiscoverySessionUnused) {
            val opened = steps.run { authorize(connection, capabilities = CAPABILITIES) }
            if (opened is StepAnswer.Failed) return Result.Error(opened.failure)
        }
        isDiscoverySessionUnused = false
        val identified = steps.run { identify(identifier, identifierType) }
        if (identified is StepAnswer.Failed) {
            // A rejected address or number can be fixed by the user, so it is not the generic failure.
            val isRejected = (identified.error as? EmbeddedAuthException)?.error == EmbeddedAuthError.InvalidRequest
            return Result.Error(
                if (isRejected) LoginFailure(EmbeddedLoginError.InvalidIdentifier(method)) else identified.failure,
            )
        }
        val nextActions = identified.nextActions
        val sent = when (method) {
            LoginOption.PhoneOtp -> sendPhoneCode(nextActions)
            else -> sendEmailCode(nextActions)
        }
        return when (sent) {
            // No code is on its way, so the code screen would wait for nothing.
            null -> {
                Logger.w(TAG, "No $method code was sent; the server offered ${nextActions.map { it.capability.value }}")
                Result.Error(LoginFailure(EmbeddedLoginError.CodeNotSent(method)))
            }

            is StepAnswer.Failed -> Result.Error(sent.failure)
            else -> Result.Success(Unit)
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
        val phone = nextActions.firstNotNullOfOrNull { it as? NextAction.ChallengePhone }
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

    override suspend fun verifyOtp(code: String): Result<Credentials, LoginFailure> =
        when (val verified = steps.run { verifyOtp(code, OtpType.OOB) }) {
            is StepAnswer.Done -> Result.Success(verified.value)
            // A wrong code also answers "not finished yet"; only its reason tells it apart from a further step.
            is StepAnswer.Next -> if (verified.reason in INVALID_CODE_REASONS) {
                Result.Error(LoginFailure(EmbeddedLoginError.InvalidCode))
            } else {
                // As in the SDK, a second factor comes back as an error, carrying the factors on offer. MFA decides
                // what happens when none of them can be used here, so the app hears about it the same way.
                val factors = verified.actions.toMfaFactors()
                if (factors.isEmpty()) {
                    val offered = verified.actions.map { it.capability.value }
                    Logger.w(TAG, "No usable second factor; the server offered $offered")
                    Result.Error(LoginFailure(EmbeddedLoginError.MfaRequired.NoUsableFactor))
                } else {
                    Result.Error(LoginFailure(EmbeddedLoginError.MfaRequired(factors)))
                }
            }

            // The server can also say outright that the account has no usable second factor: MFA tells the user.
            is StepAnswer.Failed -> if (verified.isNoEligibleFactors) {
                Result.Error(LoginFailure(EmbeddedLoginError.MfaRequired.NoUsableFactor))
            } else {
                Result.Error(verified.failure)
            }
        }

    private val StepAnswer.Failed.isNoEligibleFactors: Boolean
        get() = (error as? EmbeddedAuthException)?.error == EmbeddedAuthError.NoEligibleFactors

    private companion object {
        const val TAG = "EmbeddedRepository"

        /** For a code the server sent on its own, as it said in its menu: nothing is left to do. */
        val ALREADY_SENT: StepAnswer<Unit> = StepAnswer.Done(Unit)

        val PHONE_CHANNELS = setOf(OtpChannel.SMS, OtpChannel.VOICE)

        /**
         * What these screens can drive, told to the server so it only offers those: a code sent to an email or a phone,
         * then a push or a code sent to a phone. The same as iOS, which sends only what is plugged in.
         */
        val CAPABILITIES = setOf(
            EmbeddedCapability.IDENTIFY_EMAIL,
            EmbeddedCapability.CHALLENGE_EMAIL,
            EmbeddedCapability.IDENTIFY_PHONE,
            EmbeddedCapability.VERIFY_OTP,
            EmbeddedCapability.CHALLENGE_PUSH,
            EmbeddedCapability.CHALLENGE_PHONE,
            EmbeddedCapability.VERIFY_OOB,
        )
    }
}
