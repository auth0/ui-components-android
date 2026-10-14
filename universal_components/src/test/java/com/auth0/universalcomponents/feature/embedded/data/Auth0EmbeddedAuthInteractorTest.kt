package com.auth0.universalcomponents.feature.embedded.data

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
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.data.FakeRequestImpl
import com.auth0.universalcomponents.feature.embedded.domain.model.AuthFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.AuthFailureException
import com.auth0.universalcomponents.feature.embedded.domain.model.AuthOutcome
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedAuthRequestFailed
import com.auth0.universalcomponents.feature.embedded.domain.model.FailureMessage
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaChallenge
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class Auth0EmbeddedAuthInteractorTest {

    private lateinit var client: EmbeddedAuthClient
    private lateinit var interactor: Auth0EmbeddedAuthInteractor

    @Before
    fun setup() {
        client = mockk()
        interactor = Auth0EmbeddedAuthInteractor(client, CONNECTION)
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
    fun `discover - blank connection - fails with the missing-connection text and calls nothing`() = runTest {
        val failure = failureOf<EmbeddedAuthRequestFailed> {
            Auth0EmbeddedAuthInteractor(client, " ").discover()
        }

        assertThat(failure.displayMessage)
            .isEqualTo(FailureMessage.Local(R.string.embedded_login_error_connection_missing))
        verify(exactly = 0) { client.authorize(any(), any(), any(), any()) }
    }

    @Test
    fun `discover - email offered - returns an email code option`() = runTest {
        assertThat(interactor.discover()).containsExactly(LoginOption.EmailOtp)
    }

    @Test
    fun `discover - code already being sent - returns an email code option`() = runTest {
        every { client.authorize(CONNECTION, any(), any(), any()) } returns
            step(verifyOtp(OtpChannel.EMAIL, null))

        assertThat(interactor.discover()).containsExactly(LoginOption.EmailOtp)
    }

    @Test
    fun `discover - unsupported method offered - returns it as unsupported`() = runTest {
        every { client.authorize(CONNECTION, any(), any(), any()) } returns step(NextAction.IdentifyPhone)

        assertThat(interactor.discover())
            .containsExactly(LoginOption.Unsupported(NextAction.IdentifyPhone.capability.value))
    }

    @Test
    fun `discover - network error - fails with the network text`() = runTest {
        every { client.authorize(CONNECTION, any(), any(), any()) } returns failingWith(EmbeddedAuthError.Network)

        val failure = failureOf<EmbeddedAuthRequestFailed> { interactor.discover() }

        assertThat(failure.displayMessage).isEqualTo(FailureMessage.Local(R.string.embedded_login_error_network))
    }

    // endregion

    // region startOtp

    @Test
    fun `startOtp - right after discover - reuses that session`() = runTest {
        interactor.discover()
        interactor.startOtp(EMAIL)

        verify(exactly = 1) { client.authorize(CONNECTION, any(), any(), any()) }
    }

    @Test
    fun `startOtp - resend - opens a new session first`() = runTest {
        interactor.discover()
        interactor.startOtp(EMAIL)
        interactor.startOtp(EMAIL)

        verify(exactly = 2) { client.authorize(CONNECTION, any(), any(), any()) }
    }

    @Test
    fun `startOtp - email challenge offered - asks the server to send the code`() = runTest {
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returns step(challengeEmail(at = 2))

        interactor.startOtp(EMAIL)

        verify(exactly = 1) { client.challengeEmail(2) }
    }

    @Test
    fun `startOtp - code already sent - doesn't send another`() = runTest {
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returns step(verifyOtp(OtpChannel.EMAIL, EMAIL))

        interactor.startOtp(EMAIL)

        verify(exactly = 0) { client.challengeEmail(any()) }
    }

    @Test
    fun `startOtp - no way to send a code - fails with code not sent`() = runTest {
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returns step(NextAction.IdentifyPhone)

        val failure = failureOf<AuthFailureException> { interactor.startOtp(EMAIL) }

        assertThat(failure.failure).isEqualTo(AuthFailure.CODE_NOT_SENT)
    }

    // endregion

    // region verify

    @Test
    fun `verifyOtp - code accepted - returns the credentials`() = runTest {
        val credentials = mockk<Credentials>()
        every { client.verifyOtp(CODE, OtpType.OOB) } returns FakeRequestImpl(response = credentials)

        assertThat(interactor.verifyOtp(CODE)).isEqualTo(AuthOutcome.Authenticated(credentials))
    }

    @Test
    fun `verifyOtp - invalid code - fails with invalid code`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returns
            step(verifyOtp(OtpChannel.EMAIL, EMAIL), reason = Reason.INVALID_CODE)

        val failure = failureOf<AuthFailureException> { interactor.verifyOtp(CODE) }

        assertThat(failure.failure).isEqualTo(AuthFailure.INVALID_CODE)
    }

    @Test
    fun `verifyOtp - invalid identifier or code - fails with invalid code, not a step-up`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returns
            step(verifyOtp(OtpChannel.EMAIL, EMAIL), reason = Reason.INVALID_IDENTIFIER_OR_CODE)

        val failure = failureOf<AuthFailureException> { interactor.verifyOtp(CODE) }

        assertThat(failure.failure).isEqualTo(AuthFailure.INVALID_CODE)
    }

    @Test
    fun `verifyOtp - code expired - fails inline with code expired`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returns failingWith(EmbeddedAuthError.ChallengeExpired)

        val failure = failureOf<AuthFailureException> { interactor.verifyOtp(CODE) }

        assertThat(failure.failure).isEqualTo(AuthFailure.CODE_EXPIRED)
    }

    @Test
    fun `verifyOtp - authenticator step-up - returns an authenticator challenge`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returns step(verifyOtp(OtpChannel.TOTP, null))

        assertThat(interactor.verifyOtp(CODE))
            .isEqualTo(AuthOutcome.MfaRequired(MfaChallenge(MfaFactor.OTP, null)))
    }

    @Test
    fun `verifyOtp - email step-up - returns an email challenge with its destination`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returns step(verifyOtp(OtpChannel.EMAIL, MASKED_EMAIL))

        assertThat(interactor.verifyOtp(CODE))
            .isEqualTo(AuthOutcome.MfaRequired(MfaChallenge(MfaFactor.EMAIL, MASKED_EMAIL)))
    }

    @Test
    fun `verifyOtp - only an unsupported step-up - fails for good`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returns step(verifyOtp(OtpChannel.SMS, null))

        failureOf<EmbeddedAuthRequestFailed> { interactor.verifyOtp(CODE) }
    }

    @Test
    fun `verifyMfa - authenticator code - verifies as TOTP`() = runTest {
        every { client.verifyOtp(CODE, OtpType.TOTP) } returns FakeRequestImpl(response = mockk<Credentials>())

        interactor.verifyMfa(CODE, MfaFactor.OTP)

        verify(exactly = 1) { client.verifyOtp(CODE, OtpType.TOTP) }
    }

    @Test
    fun `verifyMfa - email code - verifies as OOB`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returns FakeRequestImpl(response = mockk<Credentials>())

        interactor.verifyMfa(CODE, MfaFactor.EMAIL)

        verify(exactly = 1) { client.verifyOtp(CODE, OtpType.OOB) }
    }

    // endregion

    // region error text

    @Test
    fun `error text - known errors - use our own words`() = runTest {
        val expected = mapOf(
            EmbeddedAuthError.TooManyWrongOtpAttempts to R.string.embedded_login_error_too_many_wrong_codes,
            EmbeddedAuthError.TooManyAttempts to R.string.embedded_login_error_too_many_attempts,
            EmbeddedAuthError.TooManyLogins to R.string.embedded_login_error_too_many_logins,
        )
        expected.forEach { (error, textRes) ->
            every { client.verifyOtp(CODE, OtpType.OOB) } returns failingWith(error, description = "raw_token")

            val failure = failureOf<EmbeddedAuthRequestFailed> { interactor.verifyOtp(CODE) }

            assertThat(failure.displayMessage).isEqualTo(FailureMessage.Local(textRes))
        }
    }

    @Test
    fun `error text - other server error - uses the server's description`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returns
            failingWith(EmbeddedAuthError.AccessDenied, description = "Your account is blocked.")

        val failure = failureOf<EmbeddedAuthRequestFailed> { interactor.verifyOtp(CODE) }

        assertThat(failure.displayMessage).isEqualTo(FailureMessage.Server("Your account is blocked."))
    }

    @Test
    fun `error text - no usable server text - leaves it to the generic message`() = runTest {
        listOf<Request<Credentials, EmbeddedAuthException>>(
            failingWith(EmbeddedAuthError.Unknown, code = Auth0Exception.NON_JSON_ERROR, description = "debug text"),
            failingWith(EmbeddedAuthError.Unknown, code = Auth0Exception.EMPTY_BODY_ERROR, description = "debug text"),
            failingWith(EmbeddedAuthError.AccessDenied, description = " "),
        ).forEach { request ->
            every { client.verifyOtp(CODE, OtpType.OOB) } returns request

            val failure = failureOf<EmbeddedAuthRequestFailed> { interactor.verifyOtp(CODE) }

            assertThat(failure.displayMessage).isNull()
        }
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

            val failure = failureOf<EmbeddedAuthRequestFailed> { interactor.verifyOtp(CODE) }

            assertThat(failure.canRetryStep).isTrue()
        }
    }

    @Test
    fun `retrying a step - any other error - the session is gone`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returns
            failingWith(EmbeddedAuthError.TooManyWrongOtpAttempts, statusCode = 403)

        val failure = failureOf<EmbeddedAuthRequestFailed> { interactor.verifyOtp(CODE) }

        assertThat(failure.canRetryStep).isFalse()
    }

    // endregion

    /** A step that answers "not finished yet, do one of [next]", the way every step before the last does. */
    private fun <T> step(vararg next: NextAction, reason: Reason = Reason.NONE): Request<T, EmbeddedAuthException> =
        failingWith(EmbeddedAuthError.InsufficientAuthorization(next.toList(), reason), code = "insufficient_authorization")

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

    private suspend inline fun <reified T : Throwable> failureOf(block: suspend () -> Unit): T {
        val error = runCatching { block() }.exceptionOrNull()
        assertThat(error).isInstanceOf(T::class.java)
        return error as T
    }

    private companion object {
        const val CONNECTION = "email"
        const val EMAIL = "jane@example.com"
        const val MASKED_EMAIL = "j***@example.com"
        const val CODE = "123456"
    }
}
