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
 * The one SDK client of a sign-in, and the only way to call it. The client keeps the session, and every answer it
 * gets rewrites that session, so the SDK asks for one step at a time. Both the first factor and MFA run their steps
 * here, which is what makes the rule hold across them:
 * - a step waits until the one before it has answered;
 * - a step that has started always finishes, so the session stays right; a caller that has left just never gets
 *   the answer.
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
