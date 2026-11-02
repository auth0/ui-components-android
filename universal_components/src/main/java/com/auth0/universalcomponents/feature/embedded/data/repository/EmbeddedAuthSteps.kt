package com.auth0.universalcomponents.feature.embedded.data.repository

import com.auth0.android.embedded.EmbeddedAuthClient
import com.auth0.android.embedded.EmbeddedAuthError
import com.auth0.android.embedded.EmbeddedAuthException
import com.auth0.android.request.Request
import com.auth0.universalcomponents.feature.embedded.data.mapper.toLoginFailure
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * Wraps the [EmbeddedAuthClient] of one sign-in. Every SDK call from the first-factor and MFA repositories goes
 * through [run], which does two things:
 *
 * 1. **Runs one call at a time.** The client keeps the sign-in session inside itself and every answer changes it,
 *    so two calls must never overlap. A call waits until the one before it has answered, and a call that has started
 *    always finishes even if its caller has left, so the session is never left half-updated (that caller just
 *    doesn't get the answer).
 * 2. **Turns the SDK result into a [StepAnswer].** A successful result becomes [StepAnswer.Done]; "another step is
 *    needed" (which the SDK throws as an exception) becomes [StepAnswer.Next]; anything else becomes
 *    [StepAnswer.Failed].
 */
internal class EmbeddedAuthSteps(private val client: EmbeddedAuthClient) {

    private val oneAtATime = Mutex()

    suspend fun <T> run(step: EmbeddedAuthClient.() -> Request<T, EmbeddedAuthException>): StepAnswer<T> {
        val answer = oneAtATime.withLock { withContext(NonCancellable) { answerOf { client.step() } } }
        currentCoroutineContext().ensureActive()
        return answer
    }

    /**
     * Runs one step and says how it answered. "Not finished yet" arrives from the SDK as an exception but isn't a
     * failure, so it comes back as [StepAnswer.Next]. This is the only place an exception is caught.
     */
    // Intentionally catches all exceptions to name them in our terms; CancellationException is rethrown first.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> answerOf(request: () -> Request<T, EmbeddedAuthException>): StepAnswer<T> = try {
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
}
