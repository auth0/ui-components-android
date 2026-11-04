package com.auth0.universalcomponents.feature.mfachallenge.data.repository

import com.auth0.android.embedded.EmbeddedAuthError
import com.auth0.android.embedded.EmbeddedAuthError.InsufficientAuthorization.Reason
import com.auth0.android.embedded.EmbeddedAuthException
import com.auth0.android.embedded.authorize.NextAction
import com.auth0.universalcomponents.data.embeddedauth.EmbeddedAuthSteps
import com.auth0.universalcomponents.data.embeddedauth.StepAnswer
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.mfachallenge.MfaError
import com.auth0.universalcomponents.feature.mfachallenge.data.mapper.mfaError
import com.auth0.universalcomponents.feature.mfachallenge.data.mapper.toMfaFactors
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.PushStatus
import com.auth0.universalcomponents.feature.mfachallenge.domain.repository.MfaRepository
import com.auth0.universalcomponents.utils.logging.Logger

/**
 * The second factor against the SDK. [steps] must be the first factor's: its client holds the session MFA continues.
 * The server's answers are read as iOS reads them, so both platforms follow the same flow.
 */
internal class MfaRepositoryImpl(private val steps: EmbeddedAuthSteps) : MfaRepository {

    override suspend fun sendPush(factor: MfaFactor.Push): Result<PushStatus.Pending, MfaError> =
        when (val sent = steps.run { challengePush(factor.index) }) {
            // The push is out once the server asks for its approval to be checked.
            is StepAnswer.Next -> sent.verifyOob()
                ?.let { Result.Success(PushStatus.Pending(it.pollInMs.toLong(), canResend = sent.offersPush)) }
                ?: unexpected("The push was sent without a way to check it")
            is StepAnswer.Failed -> Result.Error(sent.mfaError)
            is StepAnswer.Done -> unexpected("Sending a push signed in")
        }

    override suspend fun checkPushStatus(): Result<PushStatus, MfaError> =
        when (val checked = steps.run { verifyOob() }) {
            is StepAnswer.Done -> Result.Success(PushStatus.Approved(checked.value))
            is StepAnswer.Next -> when {
                // Not an error: the user just hasn't answered yet. A pending answer always carries the next check, so
                // an answer that still has one is pending whatever its reason: the SDK reads any reason it doesn't
                // know as "none". Logged at debug level so the polling can be traced.
                checked.isStillWaiting -> {
                    Logger.d(TAG, "Push not answered yet (${checked.reason})")
                    Result.Success(
                        PushStatus.Pending(
                            retryInMs = checked.verifyOob()?.pollInMs?.toLong(),
                            canResend = checked.offersPush,
                            isSlowDown = checked.reason == Reason.SLOW_DOWN,
                        ),
                    )
                }

                // Approved, and the policy asks for another factor: the answer carries the next round's menu.
                checked.reason == Reason.NONE -> {
                    Logger.i(TAG, "Push approved; the server asks for another factor")
                    Result.Success(PushStatus.AnotherFactor(checked.actions.toMfaFactors()))
                }

                // No code is sent with a push check, so a wrong-code answer can't be for us.
                else -> unexpected("A push check was answered as a wrong code")
            }

            is StepAnswer.Failed -> if (checked.isRejected) {
                Logger.i(TAG, "Push denied by the user")
                Result.Success(PushStatus.Rejected)
            } else {
                Result.Error(checked.mfaError)
            }
        }

    /** The server says the user denied the push: an answer to the check, not a failure of it. */
    private val StepAnswer.Failed.isRejected: Boolean
        get() = (error as? EmbeddedAuthException)?.error == EmbeddedAuthError.AuthorizationRejected

    /** The "check the push" step the server offered, with the wait it wants before the next check. */
    private fun StepAnswer.Next.verifyOob(): NextAction.VerifyOob? =
        actions.firstNotNullOfOrNull { it as? NextAction.VerifyOob }

    private val StepAnswer.Next.isStillWaiting: Boolean
        get() = reason == Reason.AUTHORIZATION_PENDING || reason == Reason.SLOW_DOWN ||
            (reason == Reason.NONE && verifyOob() != null)

    /** Every answer says again whether the push may be sent again: the server stops offering it at its limit. */
    private val StepAnswer.Next.offersPush: Boolean
        get() = actions.any { it is NextAction.ChallengePush }

    private fun unexpected(message: String): Result<Nothing, MfaError> {
        Logger.w(TAG, message)
        return Result.Error(MfaError.Unexpected())
    }

    private companion object {
        private const val TAG = "MfaRepository"
    }
}
