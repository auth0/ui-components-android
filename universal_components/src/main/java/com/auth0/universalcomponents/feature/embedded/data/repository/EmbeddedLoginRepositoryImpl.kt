package com.auth0.universalcomponents.feature.embedded.data.repository

import com.auth0.android.embedded.EmbeddedAuthClient
import com.auth0.android.embedded.EmbeddedAuthError
import com.auth0.android.embedded.EmbeddedAuthException
import com.auth0.android.embedded.authorize.IdentifierType
import com.auth0.android.embedded.authorize.NextAction
import com.auth0.android.embedded.authorize.OtpChannel
import com.auth0.android.embedded.authorize.OtpType
import com.auth0.android.request.Request
import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.feature.embedded.data.mapper.toLoginOption
import com.auth0.universalcomponents.feature.embedded.data.mapper.toVerifyFailure
import com.auth0.universalcomponents.feature.embedded.data.mapper.withLoginErrorMapping
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginException
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption
import com.auth0.universalcomponents.feature.embedded.domain.repository.EmbeddedLoginRepository
import com.auth0.universalcomponents.utils.logging.Logger

/**
 * The only class that talks to the SDK: authorize → identify → challengeEmail → verifyOtp.
 * Each step before the last answers with `insufficient_authorization` and the next actions to take.
 * [connection] is fixed for the whole flow; every resend, edited email and Try again opens a new session with it.
 */
internal class EmbeddedLoginRepositoryImpl(
    private val client: EmbeddedAuthClient,
    private val connection: String,
) : EmbeddedLoginRepository {

    private var nextActions: List<NextAction> = emptyList()

    /** Lets the first [startEmailOtp] reuse the session [discover] opened. */
    private var isDiscoverySessionUnused = false

    override suspend fun discover(): List<LoginOption> = withLoginErrorMapping {
        if (connection.isBlank()) throw EmbeddedLoginException(EmbeddedLoginError.ConnectionMissing)
        runStep(client.authorize(connection))
        isDiscoverySessionUnused = true
        nextActions.map { it.toLoginOption() }
    }

    override suspend fun startEmailOtp(email: String): Unit = withLoginErrorMapping {
        if (!isDiscoverySessionUnused) runStep(client.authorize(connection))
        isDiscoverySessionUnused = false
        nextActions = emptyList()
        runStep(client.identify(email, IdentifierType.EMAIL))
        val challenge = nextActions.firstNotNullOfOrNull { it as? NextAction.ChallengeEmail }
        when {
            challenge != null -> runStep(client.challengeEmail(challenge.index))
            // Carry on as if a code went out, so the screen never tells whether the email has an account.
            nextActions.none { it is NextAction.VerifyOtp && it.channel == OtpChannel.EMAIL } ->
                Logger.w(TAG, "No email code was sent; the server offered ${nextActions.map { it.capability.value }}")
        }
    }

    override suspend fun verifyOtp(code: String): Credentials = withLoginErrorMapping {
        try {
            client.verifyOtp(code, OtpType.OOB).await()
        } catch (e: EmbeddedAuthException) {
            throw e.toVerifyFailure()
        }
    }

    /** Runs one step and keeps the next actions it answers with; any other failure is left to the caller to map. */
    private suspend fun runStep(request: Request<Void?, EmbeddedAuthException>) {
        try {
            request.await()
        } catch (e: EmbeddedAuthException) {
            val error = e.error as? EmbeddedAuthError.InsufficientAuthorization ?: throw e
            nextActions = error.nextActions
        }
    }

    private companion object {
        const val TAG = "EmbeddedRepository"
    }
}
