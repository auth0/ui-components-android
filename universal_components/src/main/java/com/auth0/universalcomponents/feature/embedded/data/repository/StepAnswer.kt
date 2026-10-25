package com.auth0.universalcomponents.feature.embedded.data.repository

import com.auth0.android.embedded.EmbeddedAuthError
import com.auth0.android.embedded.EmbeddedAuthError.InsufficientAuthorization.Reason
import com.auth0.android.embedded.EmbeddedAuthException
import com.auth0.android.embedded.authorize.NextAction
import com.auth0.android.request.Request
import com.auth0.universalcomponents.feature.embedded.data.mapper.toLoginFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure
import kotlin.coroutines.cancellation.CancellationException

/** How one SDK step answered: finished, "not finished yet, do one of [Next.actions]", or failed. */
internal sealed interface StepAnswer<out T> {
    data class Done<T>(val value: T) : StepAnswer<T>
    data class Next(val actions: List<NextAction>, val reason: Reason) : StepAnswer<Nothing>
    data class Failed(val failure: LoginFailure) : StepAnswer<Nothing>
}

/** The reasons the server gives when the code (or identifier) the user typed was wrong. */
internal val INVALID_CODE_REASONS = setOf(Reason.INVALID_CODE, Reason.INVALID_IDENTIFIER_OR_CODE)

/** The actions a step asks for next; none if it finished. */
internal val StepAnswer<*>.nextActions: List<NextAction>
    get() = (this as? StepAnswer.Next)?.actions.orEmpty()

/**
 * Runs one step and says how it answered. "Not finished yet" arrives from the SDK as an exception but isn't a failure,
 * so it comes back as [StepAnswer.Next]. This is the only place an exception is caught; cancellation goes through.
 */
// Intentionally catches all exceptions to name them in our terms; CancellationException is rethrown first.
@Suppress("TooGenericExceptionCaught")
internal suspend fun <T> runStep(request: () -> Request<T, EmbeddedAuthException>): StepAnswer<T> = try {
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
