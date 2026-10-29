package com.auth0.universalcomponents.feature.embedded.data.repository

import com.auth0.android.embedded.EmbeddedAuthError.InsufficientAuthorization.Reason
import com.auth0.android.embedded.authorize.NextAction
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.embedded.domain.model.PushStatus
import com.auth0.universalcomponents.feature.embedded.domain.repository.MfaRepository
import com.auth0.universalcomponents.utils.logging.Logger

/** The second factor against the SDK. [steps] must be the first factor's: its client holds the session MFA continues. */
internal class MfaRepositoryImpl(private val steps: EmbeddedAuthSteps) : MfaRepository {

    override suspend fun sendPush(factor: MfaFactor.Push): Result<Long, LoginFailure> =
        when (val sent = steps.run { challengePush(factor.index) }) {
            is StepAnswer.Next -> sent.pollDelay()?.let { Result.Success(it) }
                ?: unexpected("The push was sent without a way to check it")
            is StepAnswer.Failed -> Result.Error(sent.failure)
            is StepAnswer.Done -> unexpected("Sending a push signed in")
        }

    override suspend fun checkPush(): Result<PushStatus, LoginFailure> =
        when (val checked = steps.run { verifyOob() }) {
            is StepAnswer.Done -> Result.Success(PushStatus.Approved(checked.value))
            // Not an error: the user just hasn't answered yet. Logged at debug level so the polling can be traced.
            is StepAnswer.Next -> if (checked.reason in PENDING_REASONS) {
                val wait = checked.pollDelay()
                val next = wait?.let { "$it ms" } ?: "the same wait"
                Logger.d(TAG, "Push not answered yet (${checked.reason}); next check in $next")
                Result.Success(PushStatus.Pending(wait))
            } else {
                furtherStep(checked)
            }
            is StepAnswer.Failed -> Result.Error(checked.failure)
        }

    private fun StepAnswer.Next.pollDelay(): Long? =
        actions.firstNotNullOfOrNull { it as? NextAction.VerifyOob }?.pollInMs?.toLong()

    private fun furtherStep(answer: StepAnswer.Next): Result<Nothing, LoginFailure> {
        Logger.w(TAG, "The server asked for a further step: ${answer.actions.map { it.capability.value }}")
        return Result.Error(LoginFailure(EmbeddedLoginError.ExtraStepUnsupported))
    }

    private fun unexpected(message: String): Result<Nothing, LoginFailure> {
        Logger.w(TAG, message)
        return Result.Error(LoginFailure(EmbeddedLoginError.Unknown(message)))
    }

    private companion object {
        private const val TAG = "MfaRepository"

        /** The push hasn't been answered yet, or we checked too soon. */
        private val PENDING_REASONS = setOf(Reason.AUTHORIZATION_PENDING, Reason.SLOW_DOWN)
    }
}
