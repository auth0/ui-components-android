package com.auth0.universalcomponents.feature.embedded.data.repository

import com.auth0.android.embedded.EmbeddedAuthError
import com.auth0.android.embedded.EmbeddedAuthError.InsufficientAuthorization.Reason
import com.auth0.android.embedded.EmbeddedAuthException
import com.auth0.android.embedded.authorize.NextAction
import com.auth0.android.request.Request
import com.auth0.universalcomponents.data.FakeRequestImpl
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure
import com.google.common.truth.Truth.assertThat

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
): Request<T, EmbeddedAuthException> {
    val exception = sdkInstance<EmbeddedAuthException>(
        String::class.java to code,
        String::class.java to description,
        Int::class.java to statusCode,
        EmbeddedAuthError::class.java to error,
        String::class.java to null,
        Throwable::class.java to null,
    )
    return FakeRequestImpl(exception = exception)
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
