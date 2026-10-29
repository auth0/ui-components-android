package com.auth0.universalcomponents.feature.embedded.data.repository

import com.auth0.android.callback.Callback
import com.auth0.android.embedded.EmbeddedAuthError
import com.auth0.android.embedded.EmbeddedAuthError.InsufficientAuthorization.Reason
import com.auth0.android.embedded.EmbeddedAuthException
import com.auth0.android.embedded.authorize.NextAction
import com.auth0.android.request.Request
import com.auth0.universalcomponents.data.FakeRequestImpl
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred

/** A step that answers "not finished yet, do one of [next]", the way every step before the last does. */
internal fun <T> step(vararg next: NextAction, reason: Reason = Reason.NONE): Request<T, EmbeddedAuthException> =
    failingWith(
        EmbeddedAuthError.InsufficientAuthorization(next.toList(), reason),
        code = "insufficient_authorization",
    )

internal fun <T> failingWith(
    error: EmbeddedAuthError,
    code: String = "access_denied",
    description: String = "",
    statusCode: Int = 0,
): Request<T, EmbeddedAuthException> = FakeRequestImpl(exception = sdkException(error, code, description, statusCode))

internal fun sdkException(
    error: EmbeddedAuthError,
    code: String = "access_denied",
    description: String = "",
    statusCode: Int = 0,
): EmbeddedAuthException = sdkInstance(
    String::class.java to code,
    String::class.java to description,
    Int::class.java to statusCode,
    EmbeddedAuthError::class.java to error,
    String::class.java to null,
    Throwable::class.java to null,
)

/**
 * A step held "in flight" until the test [answer]s or [fail]s it. [finished] tells a step that got its answer apart
 * from one that was cut off while waiting.
 */
internal class InFlightRequest<T> : Request<T, EmbeddedAuthException> {

    private val outcome = CompletableDeferred<T>()

    var finished = false
        private set

    fun answer(value: T) = outcome.complete(value)

    fun fail(exception: EmbeddedAuthException) = outcome.completeExceptionally(exception)

    override suspend fun await(): T = try {
        outcome.await()
    } finally {
        finished = outcome.isCompleted
    }

    override fun execute(): T = error("Only await() is used")

    override fun start(callback: Callback<T, EmbeddedAuthException>) = error("Only await() is used")

    override fun addHeader(name: String, value: String): Request<T, EmbeddedAuthException> = this

    override fun addParameter(name: String, value: String): Request<T, EmbeddedAuthException> = this

    override fun addParameter(name: String, value: Any): Request<T, EmbeddedAuthException> = this

    override fun addParameters(parameters: Map<String, String>): Request<T, EmbeddedAuthException> = this
}

/** The SDK's constructors are internal to Kotlin but public on the JVM, so tests build real instances. */
internal inline fun <reified T> sdkInstance(vararg args: Pair<Class<*>, Any?>): T =
    T::class.java.getConstructor(*args.map { it.first }.toTypedArray())
        .newInstance(*args.map { it.second }.toTypedArray())

/** Every failure comes back from a repository as a [LoginFailure], never thrown. */
internal fun Result<*, LoginFailure>.failure(): LoginFailure {
    assertThat(this).isInstanceOf(Result.Error::class.java)
    return (this as Result.Error).error
}
