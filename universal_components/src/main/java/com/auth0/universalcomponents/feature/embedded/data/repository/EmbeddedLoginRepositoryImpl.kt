package com.auth0.universalcomponents.feature.embedded.data.repository

import com.auth0.android.embedded.EmbeddedAuthClient
import com.auth0.android.embedded.EmbeddedAuthError
import com.auth0.android.embedded.EmbeddedAuthError.InsufficientAuthorization.Reason
import com.auth0.android.embedded.EmbeddedAuthException
import com.auth0.android.embedded.authorize.IdentifierType
import com.auth0.android.embedded.authorize.NextAction
import com.auth0.android.embedded.authorize.OtpChannel
import com.auth0.android.embedded.authorize.OtpType
import com.auth0.android.request.Request
import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.domain.network.EmptyResult
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.embedded.data.mapper.toLoginFailure
import com.auth0.universalcomponents.feature.embedded.data.mapper.toLoginOption
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption
import com.auth0.universalcomponents.feature.embedded.domain.repository.EmbeddedLoginRepository
import com.auth0.universalcomponents.utils.logging.Logger
import kotlin.coroutines.cancellation.CancellationException

/**
 * The only class that talks to the SDK: authorize → identify → challengeEmail → verifyOtp.
 * Each step before the last answers with `insufficient_authorization` and the next actions to take.
 * [connection] is fixed for the whole flow; every resend, edited email and Try again opens a new session with it.
 */
internal class EmbeddedLoginRepositoryImpl(
    private val client: EmbeddedAuthClient,
    private val connection: String,
) : EmbeddedLoginRepository {

    /** Lets the first [startEmailOtp] reuse the session [discover] opened. */
    private var isDiscoverySessionUnused = false

    override suspend fun discover(): Result<List<LoginOption>, LoginFailure> {
        if (connection.isBlank()) return Result.Error(LoginFailure(EmbeddedLoginError.ConnectionMissing))
        val opened = runStep { client.authorize(connection) }
        if (opened is StepAnswer.Failed) return Result.Error(opened.failure)
        isDiscoverySessionUnused = true
        return Result.Success(opened.nextActions.map { it.toLoginOption() })
    }

    override suspend fun startEmailOtp(email: String): EmptyResult<LoginFailure> {
        if (!isDiscoverySessionUnused) {
            val opened = runStep { client.authorize(connection) }
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

    override suspend fun verifyOtp(code: String): Result<Credentials, LoginFailure> =
        when (val verified = runStep { client.verifyOtp(code, OtpType.OOB) }) {
            is StepAnswer.Done -> Result.Success(verified.value)
            // A wrong code also answers "not finished yet"; only its reason tells it apart from a further step, such as MFA.
            is StepAnswer.Next -> if (verified.reason in INVALID_CODE_REASONS) {
                Result.Error(LoginFailure(EmbeddedLoginError.InvalidCode))
            } else {
                Logger.w(TAG, "The server asked for a further step: ${verified.actions.map { it.capability.value }}")
                Result.Error(LoginFailure(EmbeddedLoginError.ExtraStepUnsupported))
            }

            is StepAnswer.Failed -> Result.Error(verified.failure)
        }

    /**
     * Runs one step and says how it answered. "Not finished yet" arrives from the SDK as an exception but isn't a failure,
     * so it comes back as [StepAnswer.Next]. This is the only place an exception is caught; cancellation goes through.
     */
    // Intentionally catches all exceptions to name them in our terms; CancellationException is rethrown first.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> runStep(request: () -> Request<T, EmbeddedAuthException>): StepAnswer<T> = try {
        StepAnswer.Done(request().await())
    } catch (e: CancellationException) {
        throw e
    } catch (e: EmbeddedAuthException) {
        val error = e.error
        if (error is EmbeddedAuthError.InsufficientAuthorization) {
            StepAnswer.Next(error.nextActions, error.reason)
        } else {
            StepAnswer.Failed(e.toLoginFailure())
        }
    } catch (e: Exception) {
        StepAnswer.Failed(e.toLoginFailure())
    }

    /** How one SDK step answered: finished, "not finished yet, do one of [Next.actions]", or failed. */
    private sealed interface StepAnswer<out T> {
        data class Done<T>(val value: T) : StepAnswer<T>
        data class Next(val actions: List<NextAction>, val reason: Reason) : StepAnswer<Nothing>
        data class Failed(val failure: LoginFailure) : StepAnswer<Nothing>
    }

    private companion object {
        const val TAG = "EmbeddedRepository"

        val INVALID_CODE_REASONS = setOf(Reason.INVALID_CODE, Reason.INVALID_IDENTIFIER_OR_CODE)

        /** The actions a step asks for next; none if it finished. */
        val StepAnswer<*>.nextActions: List<NextAction>
            get() = (this as? StepAnswer.Next)?.actions.orEmpty()
    }
}
