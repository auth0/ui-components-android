package com.auth0.universalcomponents.feature.embedded.data

import com.auth0.android.Auth0Exception
import com.auth0.android.embedded.EmbeddedAuthClient
import com.auth0.android.embedded.EmbeddedAuthError
import com.auth0.android.embedded.EmbeddedAuthError.InsufficientAuthorization.Reason
import com.auth0.android.embedded.EmbeddedAuthException
import com.auth0.android.embedded.authorize.IdentifierType
import com.auth0.android.embedded.authorize.NextAction
import com.auth0.android.embedded.authorize.OtpChannel
import com.auth0.android.embedded.authorize.OtpType
import com.auth0.android.request.Request
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.domain.model.AuthFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.AuthFailureException
import com.auth0.universalcomponents.feature.embedded.domain.model.AuthOutcome
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedAuthRequestFailed
import com.auth0.universalcomponents.feature.embedded.domain.model.FailureMessage
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaChallenge
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.embedded.domain.repository.AuthInteracting

/**
 * The only class that talks to the SDK: authorize → identify → challengeEmail → verifyOtp.
 * Each step before the last answers with `insufficient_authorization` and the next actions to take.
 */
internal class Auth0EmbeddedAuthInteractor(
    private val client: EmbeddedAuthClient,
    private val connection: String,
) : AuthInteracting {

    private var lastNextActions: List<NextAction> = emptyList()

    /** Lets the first [startOtp] reuse the session [discover] opened. */
    private var hasFreshLoop = false

    override suspend fun discover(): List<LoginOption> {
        if (connection.isBlank()) {
            throw EmbeddedAuthRequestFailed(FailureMessage.Local(R.string.embedded_login_error_connection_missing))
        }
        runStep(client.authorize(connection))
        hasFreshLoop = true
        return lastNextActions.map { it.toOption() }
    }

    override suspend fun startOtp(email: String) {
        if (!hasFreshLoop) runStep(client.authorize(connection))
        hasFreshLoop = false
        lastNextActions = emptyList()
        runStep(client.identify(email, IdentifierType.EMAIL))
        val challenge = lastNextActions.firstNotNullOfOrNull { it as? NextAction.ChallengeEmail }
        when {
            challenge != null -> runStep(client.challengeEmail(challenge.index))
            lastNextActions.none { it is NextAction.VerifyOtp && it.channel == OtpChannel.EMAIL } ->
                throw AuthFailureException(AuthFailure.CODE_NOT_SENT)
        }
    }

    override suspend fun verifyOtp(code: String): AuthOutcome = verify(code, OtpType.OOB)

    override suspend fun verifyMfa(code: String, factor: MfaFactor): AuthOutcome =
        verify(code, if (factor == MfaFactor.OTP) OtpType.TOTP else OtpType.OOB)

    /** Runs one step and keeps the next actions it answers with. */
    private suspend fun runStep(request: Request<Void?, EmbeddedAuthException>) {
        try {
            request.await()
        } catch (e: EmbeddedAuthException) {
            val error = e.error as? EmbeddedAuthError.InsufficientAuthorization ?: throw e.toUserFacing()
            lastNextActions = error.nextActions
        }
    }

    private suspend fun verify(code: String, type: OtpType): AuthOutcome {
        try {
            return AuthOutcome.Authenticated(client.verifyOtp(code, type).await())
        } catch (e: EmbeddedAuthException) {
            if (e.error == EmbeddedAuthError.ChallengeExpired) throw AuthFailureException(AuthFailure.CODE_EXPIRED)
            val error = e.error as? EmbeddedAuthError.InsufficientAuthorization ?: throw e.toUserFacing()
            lastNextActions = error.nextActions
            // Checked first, so a code step shown again isn't mistaken for a step-up.
            if (error.reason == Reason.INVALID_CODE || error.reason == Reason.INVALID_IDENTIFIER_OR_CODE) { //MMG
                throw AuthFailureException(AuthFailure.INVALID_CODE)
            }
            val challenge = error.nextActions.firstNotNullOfOrNull { it.toChallenge() } ?: throw e.toUserFacing()
            return AuthOutcome.MfaRequired(challenge)
        }
    }

    private companion object {

        /** Maps a server action to a sign-in method. */
        fun NextAction.toOption(): LoginOption = when {
            this is NextAction.IdentifyEmail -> LoginOption.EmailOtp
            this is NextAction.VerifyOtp && channel == OtpChannel.EMAIL -> LoginOption.EmailOtp
            else -> LoginOption.Unsupported(capability.value)
        }

        /** A step-up code request; SMS and voice aren't supported. */
        fun NextAction.toChallenge(): MfaChallenge? {
            if (this !is NextAction.VerifyOtp) return null
            return when (channel) {
                OtpChannel.EMAIL -> MfaChallenge(MfaFactor.EMAIL, identifier)
                OtpChannel.TOTP -> MfaChallenge(MfaFactor.OTP, identifier)
                else -> null
            }
        }

        /** Never the SDK's own text, which is debug output; ours for known errors, else the server's words. */
        fun EmbeddedAuthException.toUserFacing(): EmbeddedAuthRequestFailed {
            // The SDK keeps the session only for these; any other error ends it.
            val canRetryStep = error == EmbeddedAuthError.Network || statusCode in 500..599
            val local = when (error) {
                EmbeddedAuthError.Network -> R.string.embedded_login_error_network
                EmbeddedAuthError.ChallengeExpired -> R.string.embedded_login_error_code_expired
                EmbeddedAuthError.TooManyWrongOtpAttempts -> R.string.embedded_login_error_too_many_wrong_codes
                EmbeddedAuthError.TooManyAttempts -> R.string.embedded_login_error_too_many_attempts
                EmbeddedAuthError.TooManyLogins -> R.string.embedded_login_error_too_many_logins
                else -> null
            }
            if (local != null) return EmbeddedAuthRequestFailed(FailureMessage.Local(local), canRetryStep)
            val fromServer = code !in SDK_ERROR_CODES && description.isNotBlank()
            return EmbeddedAuthRequestFailed(if (fromServer) FailureMessage.Server(description) else null, canRetryStep)
        }

        /** Codes the SDK makes up when the server sent no usable error. */
        val SDK_ERROR_CODES = setOf(
            Auth0Exception.UNKNOWN_ERROR,
            Auth0Exception.NON_JSON_ERROR,
            Auth0Exception.EMPTY_BODY_ERROR,
        )
    }
}
