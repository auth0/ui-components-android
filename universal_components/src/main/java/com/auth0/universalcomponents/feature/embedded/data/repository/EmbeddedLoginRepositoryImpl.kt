package com.auth0.universalcomponents.feature.embedded.data.repository

import com.auth0.android.embedded.EmbeddedAuthClient
import com.auth0.android.embedded.authorize.EmbeddedCapability
import com.auth0.android.embedded.authorize.IdentifierType
import com.auth0.android.embedded.authorize.NextAction
import com.auth0.android.embedded.authorize.OtpChannel
import com.auth0.android.embedded.authorize.OtpType
import com.auth0.universalcomponents.domain.network.EmptyResult
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.embedded.data.mapper.toLoginOption
import com.auth0.universalcomponents.feature.embedded.data.mapper.toMfaFactors
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption
import com.auth0.universalcomponents.feature.embedded.domain.model.VerifyOutcome
import com.auth0.universalcomponents.feature.embedded.domain.repository.EmbeddedLoginRepository
import com.auth0.universalcomponents.utils.logging.Logger

/**
 * The first factor against the SDK: authorize → identify → challengeEmail → verifyOtp.
 * Each step before the last answers with `insufficient_authorization` and the next actions to take.
 * [connection] is fixed for the whole flow; every resend, edited email and Try again opens a new session with it.
 * [client] holds the session, so MFA continues on the same instance.
 */
internal class EmbeddedLoginRepositoryImpl(
    private val client: EmbeddedAuthClient,
    private val connection: String,
) : EmbeddedLoginRepository {

    /** Lets the first [startEmailOtp] reuse the session [discover] opened. */
    private var isDiscoverySessionUnused = false

    override suspend fun discover(): Result<List<LoginOption>, LoginFailure> {
        if (connection.isBlank()) return Result.Error(LoginFailure(EmbeddedLoginError.ConnectionMissing))
        val opened = runStep { authorize() }
        if (opened is StepAnswer.Failed) return Result.Error(opened.failure)
        isDiscoverySessionUnused = true
        return Result.Success(opened.nextActions.map { it.toLoginOption() })
    }

    override suspend fun startEmailOtp(email: String): EmptyResult<LoginFailure> {
        if (!isDiscoverySessionUnused) {
            val opened = runStep { authorize() }
            if (opened is StepAnswer.Failed) return Result.Error(opened.failure)
        }
        isDiscoverySessionUnused = false
        val identified = runStep { client.identify(email, IdentifierType.EMAIL) }
        if (identified is StepAnswer.Failed) return Result.Error(identified.failure)
        val nextActions = identified.nextActions
        val challenge = nextActions.firstNotNullOfOrNull { it as? NextAction.ChallengeEmail }
        return when {
            challenge != null -> {
                val sent = runStep { client.challengeEmail(challenge.index) }
                if (sent is StepAnswer.Failed) Result.Error(sent.failure) else Result.Success(Unit)
            }

            nextActions.any { it is NextAction.VerifyOtp && it.channel == OtpChannel.EMAIL } -> Result.Success(Unit)

            // No code is on its way, so the code screen would wait for nothing.
            else -> {
                Logger.w(TAG, "No email code was sent; the server offered ${nextActions.map { it.capability.value }}")
                Result.Error(LoginFailure(EmbeddedLoginError.CodeNotSent))
            }
        }
    }

    override suspend fun verifyOtp(code: String): Result<VerifyOutcome, LoginFailure> =
        when (val verified = runStep { client.verifyOtp(code, OtpType.OOB) }) {
            is StepAnswer.Done -> Result.Success(VerifyOutcome.SignedIn(verified.value))
            // A wrong code also answers "not finished yet"; only its reason tells it apart from a further step.
            is StepAnswer.Next -> if (verified.reason in INVALID_CODE_REASONS) {
                Result.Error(LoginFailure(EmbeddedLoginError.InvalidCode))
            } else {
                val factors = verified.actions.toMfaFactors()
                if (factors.isNotEmpty()) {
                    Result.Success(VerifyOutcome.MfaRequired(factors))
                } else {
                    val offered = verified.actions.map { it.capability.value }
                    Logger.w(TAG, "The server asked for a further step: $offered")
                    Result.Error(LoginFailure(EmbeddedLoginError.ExtraStepUnsupported))
                }
            }

            is StepAnswer.Failed -> Result.Error(verified.failure)
        }

    /** Tells the server only the steps these screens can draw, so it never offers one we can't show. */
    private fun authorize() = client.authorize(connection, capabilities = SUPPORTED_CAPABILITIES)

    private companion object {
        const val TAG = "EmbeddedRepository"

        val SUPPORTED_CAPABILITIES = setOf(
            EmbeddedCapability.IDENTIFY_EMAIL,
            EmbeddedCapability.CHALLENGE_EMAIL,
            EmbeddedCapability.VERIFY_OTP,
            EmbeddedCapability.CHALLENGE_PUSH,
            EmbeddedCapability.VERIFY_OOB,
        )
    }
}
