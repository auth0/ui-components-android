package com.auth0.universalcomponents.feature.embedded.data.repository

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
import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.data.FakeRequestImpl
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginException
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class EmbeddedLoginRepositoryImplTest {

    private lateinit var client: EmbeddedAuthClient
    private lateinit var repository: EmbeddedLoginRepositoryImpl

    @Before
    fun setup() {
        client = mockk()
        repository = EmbeddedLoginRepositoryImpl(client, CONNECTION)
        every { client.authorize(CONNECTION, any(), any(), any()) } returns step(NextAction.IdentifyEmail)
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returns step(challengeEmail(at = 0))
        every { client.challengeEmail(any()) } returns step(verifyOtp(OtpChannel.EMAIL, EMAIL))
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // region discover

    @Test
    fun `discover - blank connection - fails with connection missing and calls nothing`() = runTest {
        val failure = failureOf { EmbeddedLoginRepositoryImpl(client, " ").discover() }

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.ConnectionMissing)
        verify(exactly = 0) { client.authorize(any(), any(), any(), any()) }
    }

    @Test
    fun `discover - email offered - returns an email code option`() = runTest {
        assertThat(repository.discover()).containsExactly(LoginOption.EmailOtp)
    }

    @Test
    fun `discover - code already being sent - returns an email code option`() = runTest {
        every { client.authorize(CONNECTION, any(), any(), any()) } returns
            step(verifyOtp(OtpChannel.EMAIL, null))

        assertThat(repository.discover()).containsExactly(LoginOption.EmailOtp)
    }

    @Test
    fun `discover - unsupported method offered - returns it as unsupported`() = runTest {
        every { client.authorize(CONNECTION, any(), any(), any()) } returns step(NextAction.IdentifyPhone)

        assertThat(repository.discover())
            .containsExactly(LoginOption.Unsupported(NextAction.IdentifyPhone.capability.value))
    }

    @Test
    fun `discover - network error - fails with network`() = runTest {
        every { client.authorize(CONNECTION, any(), any(), any()) } returns failingWith(EmbeddedAuthError.Network)

        val failure = failureOf { repository.discover() }

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.Network)
    }

    // endregion

    // region startEmailOtp

    @Test
    fun `startEmailOtp - right after discover - reuses that session`() = runTest {
        repository.discover()
        repository.startEmailOtp(EMAIL)

        verify(exactly = 1) { client.authorize(CONNECTION, any(), any(), any()) }
    }

    @Test
    fun `startEmailOtp - resend - opens a new session first`() = runTest {
        repository.discover()
        repository.startEmailOtp(EMAIL)
        repository.startEmailOtp(EMAIL)

        verify(exactly = 2) { client.authorize(CONNECTION, any(), any(), any()) }
    }

    @Test
    fun `startEmailOtp - email challenge offered - asks the server to send the code`() = runTest {
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returns step(challengeEmail(at = 2))

        repository.startEmailOtp(EMAIL)

        verify(exactly = 1) { client.challengeEmail(2) }
    }

    @Test
    fun `startEmailOtp - code already sent - doesn't send another`() = runTest {
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returns step(verifyOtp(OtpChannel.EMAIL, EMAIL))

        repository.startEmailOtp(EMAIL)

        verify(exactly = 0) { client.challengeEmail(any()) }
    }

    @Test
    fun `startEmailOtp - no way to send a code - carries on without saying so`() = runTest {
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returns step(NextAction.IdentifyPhone)

        repository.startEmailOtp(EMAIL)

        verify(exactly = 0) { client.challengeEmail(any()) }
    }

    // endregion

    // region verify

    @Test
    fun `verifyOtp - code accepted - returns the credentials`() = runTest {
        val credentials = mockk<Credentials>()
        every { client.verifyOtp(CODE, OtpType.OOB) } returns FakeRequestImpl(response = credentials)

        assertThat(repository.verifyOtp(CODE)).isEqualTo(credentials)
    }

    @Test
    fun `verifyOtp - invalid code - fails with invalid code`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returns
            step(verifyOtp(OtpChannel.EMAIL, EMAIL), reason = Reason.INVALID_CODE)

        val failure = failureOf { repository.verifyOtp(CODE) }

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.InvalidCode)
    }

    @Test
    fun `verifyOtp - invalid identifier or code - fails with invalid code, not as another step`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returns
            step(verifyOtp(OtpChannel.EMAIL, EMAIL), reason = Reason.INVALID_IDENTIFIER_OR_CODE)

        val failure = failureOf { repository.verifyOtp(CODE) }

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.InvalidCode)
    }

    @Test
    fun `verifyOtp - code expired - fails with code expired`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returns failingWith(EmbeddedAuthError.ChallengeExpired)

        val failure = failureOf { repository.verifyOtp(CODE) }

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.CodeExpired)
    }

    @Test
    fun `verifyOtp - server asks for another step - fails for good as unsupported`() = runTest {
        listOf(
            verifyOtp(OtpChannel.TOTP, null),
            verifyOtp(OtpChannel.EMAIL, MASKED_EMAIL),
            verifyOtp(OtpChannel.SMS, null),
        ).forEach { nextAction ->
            every { client.verifyOtp(CODE, OtpType.OOB) } returns step(nextAction)

            val failure = failureOf { repository.verifyOtp(CODE) }

            assertThat(failure.error).isEqualTo(EmbeddedLoginError.ExtraStepUnsupported)
            assertThat(failure.canRetryStep).isFalse()
        }
    }

    // endregion

    // region errors

    @Test
    fun `errors - known SDK errors - named in our terms, never with the SDK's text`() = runTest {
        val expected = mapOf(
            EmbeddedAuthError.TooManyWrongOtpAttempts to EmbeddedLoginError.TooManyWrongCodes,
            EmbeddedAuthError.TooManyAttempts to EmbeddedLoginError.TooManyAttempts,
            EmbeddedAuthError.TooManyLogins to EmbeddedLoginError.TooManyLogins,
        )
        expected.forEach { (sdkError, loginError) ->
            every { client.verifyOtp(CODE, OtpType.OOB) } returns failingWith(sdkError, description = "raw_token")

            val failure = failureOf { repository.verifyOtp(CODE) }

            assertThat(failure.error).isEqualTo(loginError)
        }
    }

    @Test
    fun `errors - other server error - keeps the server's description`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returns
            failingWith(EmbeddedAuthError.AccessDenied, description = "Your account is blocked.")

        val failure = failureOf { repository.verifyOtp(CODE) }

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.Server("Your account is blocked."))
    }

    @Test
    fun `errors - no usable server text - fails as unknown`() = runTest {
        listOf<Request<Credentials, EmbeddedAuthException>>(
            failingWith(EmbeddedAuthError.Unknown, code = Auth0Exception.NON_JSON_ERROR, description = "debug text"),
            failingWith(EmbeddedAuthError.Unknown, code = Auth0Exception.EMPTY_BODY_ERROR, description = "debug text"),
            failingWith(EmbeddedAuthError.AccessDenied, description = " "),
        ).forEach { request ->
            every { client.verifyOtp(CODE, OtpType.OOB) } returns request

            val failure = failureOf { repository.verifyOtp(CODE) }

            assertThat(failure.error).isEqualTo(EmbeddedLoginError.Unknown)
        }
    }

    @Test
    fun `errors - SDK error - keeps it as the cause, for the stack trace`() = runTest {
        every { client.authorize(CONNECTION, any(), any(), any()) } returns failingWith(EmbeddedAuthError.Network)

        val failure = failureOf { repository.discover() }

        assertThat(failure.cause).isInstanceOf(EmbeddedAuthException::class.java)
    }

    @Test
    fun `errors - unexpected exception - fails as unknown, keeping it as the cause`() = runTest {
        val unexpected = IllegalStateException("debug output")
        every { client.authorize(CONNECTION, any(), any(), any()) } throws unexpected

        val failure = failureOf { repository.discover() }

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.Unknown)
        assertThat(failure.cause).isSameInstanceAs(unexpected)
    }

    @Test
    fun `errors - cancelled - lets the cancellation through as it is`() = runTest {
        every { client.authorize(CONNECTION, any(), any(), any()) } throws CancellationException()

        val error = runCatching { repository.discover() }.exceptionOrNull()

        assertThat(error).isInstanceOf(CancellationException::class.java)
    }

    // endregion

    // region retrying a step

    @Test
    fun `retrying a step - network error or server outage - the session survives`() = runTest {
        listOf<Request<Credentials, EmbeddedAuthException>>(
            failingWith(EmbeddedAuthError.Network),
            failingWith(EmbeddedAuthError.Unknown, statusCode = 503),
        ).forEach { request ->
            every { client.verifyOtp(CODE, OtpType.OOB) } returns request

            val failure = failureOf { repository.verifyOtp(CODE) }

            assertThat(failure.canRetryStep).isTrue()
        }
    }

    @Test
    fun `retrying a step - any other error - the session is gone`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returns
            failingWith(EmbeddedAuthError.TooManyWrongOtpAttempts, statusCode = 403)

        val failure = failureOf { repository.verifyOtp(CODE) }

        assertThat(failure.canRetryStep).isFalse()
    }

    // endregion

    /** A step that answers "not finished yet, do one of [next]", the way every step before the last does. */
    private fun <T> step(vararg next: NextAction, reason: Reason = Reason.NONE): Request<T, EmbeddedAuthException> =
        failingWith(
            EmbeddedAuthError.InsufficientAuthorization(next.toList(), reason),
            code = "insufficient_authorization",
        )

    private fun <T> failingWith(
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

    private fun challengeEmail(at: Int): NextAction.ChallengeEmail =
        sdkInstance(Int::class.java to at, String::class.java to EMAIL)

    private fun verifyOtp(channel: OtpChannel, destination: String?): NextAction.VerifyOtp =
        sdkInstance(OtpChannel::class.java to channel, String::class.java to destination)

    /** The SDK's constructors are internal to Kotlin but public on the JVM, so tests build real instances. */
    private inline fun <reified T> sdkInstance(vararg args: Pair<Class<*>, Any?>): T =
        T::class.java.getConstructor(*args.map { it.first }.toTypedArray())
            .newInstance(*args.map { it.second }.toTypedArray())

    /** Every failure leaves the repository as an [EmbeddedLoginException]. */
    private suspend fun failureOf(block: suspend () -> Unit): EmbeddedLoginException {
        val error = runCatching { block() }.exceptionOrNull()
        assertThat(error).isInstanceOf(EmbeddedLoginException::class.java)
        return error as EmbeddedLoginException
    }

    private companion object {
        const val CONNECTION = "email"
        const val EMAIL = "jane@example.com"
        const val MASKED_EMAIL = "j***@example.com"
        const val CODE = "123456"
    }
}
