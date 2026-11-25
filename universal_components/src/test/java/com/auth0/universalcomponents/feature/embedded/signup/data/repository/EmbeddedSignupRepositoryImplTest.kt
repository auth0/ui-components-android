package com.auth0.universalcomponents.feature.embedded.signup.data.repository

import com.auth0.android.embedded.EmbeddedAuthClient
import com.auth0.android.embedded.EmbeddedAuthError
import com.auth0.android.embedded.EmbeddedAuthError.InsufficientAuthorization.Reason
import com.auth0.android.embedded.authorize.EmbeddedCapability
import com.auth0.android.embedded.authorize.IdentifierType
import com.auth0.android.embedded.authorize.NextAction
import com.auth0.android.embedded.authorize.OtpChannel
import com.auth0.android.embedded.authorize.OtpType
import com.auth0.android.embedded.authorize.SignupIdentifier
import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.data.FakeRequestImpl
import com.auth0.universalcomponents.data.embeddedauth.EmbeddedAuthSteps
import com.auth0.universalcomponents.data.embeddedauth.InFlightRequest
import com.auth0.universalcomponents.data.embeddedauth.failingWith
import com.auth0.universalcomponents.data.embeddedauth.sdkException
import com.auth0.universalcomponents.data.embeddedauth.sdkInstance
import com.auth0.universalcomponents.data.embeddedauth.step
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupChallenge
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupError
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupFailure
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupKind
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupNext
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupProgress
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupIdentifier as EmailIdentifier

class EmbeddedSignupRepositoryImplTest {

    private lateinit var client: EmbeddedAuthClient
    private lateinit var repository: EmbeddedSignupRepositoryImpl

    @Before
    fun setup() {
        client = mockk()
        repository = EmbeddedSignupRepositoryImpl(EmbeddedAuthSteps(client), CONNECTION)
        every { client.authorize(CONNECTION, any(), any(), any()) } returns step(signup(EMAIL to true))
        every { client.signup() } returns step(NextAction.IdentifyEmail)
        every { client.identify(ADDRESS, IdentifierType.EMAIL) } returns step(challengeEmail(at = 0))
        every { client.challengeEmail(any()) } returns step(verifyOtp())
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // region open

    @Test
    fun `open - blank connection - fails with connection missing and calls nothing`() = runTest {
        val failure = EmbeddedSignupRepositoryImpl(EmbeddedAuthSteps(client), " ").open().failure()

        assertThat(failure.error).isEqualTo(SignupError.ConnectionMissing)
        verify(exactly = 0) { client.authorize(any(), any(), any(), any()) }
    }

    @Test
    fun `open - tells the server only what these screens can drive`() = runTest {
        repository.open()

        verify {
            client.authorize(
                CONNECTION,
                any(),
                any(),
                setOf(
                    EmbeddedCapability.SIGNUP,
                    EmbeddedCapability.CONFIRM_SIGNUP,
                    EmbeddedCapability.IDENTIFY_EMAIL,
                    EmbeddedCapability.CHALLENGE_EMAIL,
                    EmbeddedCapability.VERIFY_OTP,
                ),
            )
        }
    }

    @Test
    fun `open - a sign-up with an email required - starts it and asks for the email`() = runTest {
        assertThat(repository.open()).isEqualTo(Result.Success(SignupNext.AskFor(SignupKind.Email)))
        verify(exactly = 1) { client.signup() }
    }

    @Test
    fun `open - an optional phone besides the email - still asks for the email`() = runTest {
        every { client.authorize(CONNECTION, any(), any(), any()) } returns
            step(signup(EMAIL to true, PHONE to false))

        assertThat(repository.open()).isEqualTo(Result.Success(SignupNext.AskFor(SignupKind.Email)))
    }

    @Test
    fun `open - no sign-up offered - is unavailable and starts nothing`() = runTest {
        every { client.authorize(CONNECTION, any(), any(), any()) } returns step(NextAction.IdentifyEmail)

        assertThat(repository.open().failure().error).isEqualTo(SignupError.Unavailable)
        verify(exactly = 0) { client.signup() }
    }

    @Test
    fun `open - a phone required - is unavailable, since there is no screen for it`() = runTest {
        every { client.authorize(CONNECTION, any(), any(), any()) } returns step(signup(PHONE to true))

        assertThat(repository.open().failure().error).isEqualTo(SignupError.Unavailable)
        verify(exactly = 0) { client.signup() }
    }

    @Test
    fun `open - an email and a phone both required - is unavailable`() = runTest {
        every { client.authorize(CONNECTION, any(), any(), any()) } returns
            step(signup(EMAIL to true, PHONE to true))

        assertThat(repository.open().failure().error).isEqualTo(SignupError.Unavailable)
    }

    @Test
    fun `open - the server asks for nothing more - there is nothing to do`() = runTest {
        every { client.signup() } returns step()

        assertThat(repository.open()).isEqualTo(Result.Success(SignupNext.NothingToDo))
    }

    @Test
    fun `open - every required identifier already verified - the account is ready to create`() = runTest {
        every { client.signup() } returns step(NextAction.ConfirmSignup)

        assertThat(repository.open()).isEqualTo(Result.Success(SignupNext.ReadyToCreate))
    }

    @Test
    fun `open - the request never arrives - fails with network, and signup is not called`() = runTest {
        every { client.authorize(CONNECTION, any(), any(), any()) } returns failingWith(EmbeddedAuthError.Network)

        assertThat(repository.open().failure().error).isEqualTo(SignupError.Network)
        verify(exactly = 0) { client.signup() }
    }

    @Test
    fun `open - signup fails - names the failure and keeps its cause`() = runTest {
        every { client.signup() } returns failingWith(EmbeddedAuthError.AccessDenied)

        val failure = repository.open().failure()

        assertThat(failure.error).isEqualTo(SignupError.Denied)
        assertThat(failure.cause).isNotNull()
    }

    @Test
    fun `open - the server ends the answer without a menu - fails as unknown, not as a sign-up`() = runTest {
        every { client.authorize(CONNECTION, any(), any(), any()) } returns FakeRequestImpl(response = null)

        assertThat(repository.open().failure().error).isInstanceOf(SignupError.Unknown::class.java)
    }

    @Test
    fun `open again - starts a new session each time`() = runTest {
        repository.open()
        repository.open()

        verify(exactly = 2) { client.authorize(CONNECTION, any(), any(), any()) }
        verify(exactly = 2) { client.signup() }
    }

    // endregion

    // region identify

    @Test
    fun `identify - an email accepted - returns the code the server will send`() = runTest {
        every { client.identify(ADDRESS, IdentifierType.EMAIL) } returns step(challengeEmail(at = 4))

        assertThat(repository.identify(EMAIL_ADDRESS)).isEqualTo(Result.Success(SignupChallenge(index = 4)))
    }

    @Test
    fun `identify - no way to send a code offered - fails with code not sent`() = runTest {
        every { client.identify(ADDRESS, IdentifierType.EMAIL) } returns step(NextAction.IdentifyEmail)

        assertThat(repository.identify(EMAIL_ADDRESS).failure().error).isEqualTo(SignupError.CodeNotSent)
    }

    @Test
    fun `identify - the server calls the email invalid - fails with invalid email`() = runTest {
        every { client.identify(ADDRESS, IdentifierType.EMAIL) } returns failingWith(
            EmbeddedAuthError.InsufficientAuthorization(listOf(NextAction.IdentifyEmail), Reason.NONE),
            code = "insufficient_authorization",
            description = "invalid_email",
        )

        assertThat(repository.identify(EMAIL_ADDRESS).failure().error).isEqualTo(SignupError.InvalidEmail)
    }

    @Test
    fun `identify - sends nothing to the user`() = runTest {
        repository.identify(EMAIL_ADDRESS)

        verify(exactly = 0) { client.challengeEmail(any()) }
    }

    // endregion

    // region sendCode

    @Test
    fun `sendCode - sends the code the challenge stands for`() = runTest {
        assertThat(repository.sendCode(SignupChallenge(index = 2))).isEqualTo(Result.Success(Unit))
        verify(exactly = 1) { client.challengeEmail(2) }
    }

    @Test
    fun `sendCode - again - is the same call`() = runTest {
        repository.sendCode(SignupChallenge(index = 2))
        repository.sendCode(SignupChallenge(index = 2))

        verify(exactly = 2) { client.challengeEmail(2) }
    }

    @Test
    fun `sendCode - too many codes requested - fails with it`() = runTest {
        every { client.challengeEmail(any()) } returns
            failingWith(EmbeddedAuthError.AccessDenied, description = "too_many_resend_attempts")

        assertThat(repository.sendCode(SignupChallenge(index = 0)).failure().error)
            .isEqualTo(SignupError.TooManyCodesRequested)
    }

    @Test
    fun `sendCode - the connection drops - fails with network`() = runTest {
        every { client.challengeEmail(any()) } returns failingWith(EmbeddedAuthError.Network)

        assertThat(repository.sendCode(SignupChallenge(index = 0)).failure().error).isEqualTo(SignupError.Network)
    }

    // endregion

    // region verify

    @Test
    fun `verify - the email already has an account - signs that user in`() = runTest {
        val credentials = mockk<Credentials>()
        every { client.verifyOtp(CODE, OtpType.OOB) } returns FakeRequestImpl(response = credentials)

        assertThat(repository.verify(CODE)).isEqualTo(Result.Success(SignupProgress.SignedIn(credentials)))
    }

    @Test
    fun `verify - every required identifier verified - the account is ready to create`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returns step(NextAction.ConfirmSignup)

        assertThat(repository.verify(CODE))
            .isEqualTo(Result.Success(SignupProgress.Continue(SignupNext.ReadyToCreate)))
    }

    @Test
    fun `verify - the server asks for the email again - asks for it`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returns step(NextAction.IdentifyEmail)

        assertThat(repository.verify(CODE))
            .isEqualTo(Result.Success(SignupProgress.Continue(SignupNext.AskFor(SignupKind.Email))))
    }

    @Test
    fun `verify - the server asks for nothing - there is nothing to do`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returns step()

        assertThat(repository.verify(CODE))
            .isEqualTo(Result.Success(SignupProgress.Continue(SignupNext.NothingToDo)))
    }

    @Test
    fun `verify - a wrong code, by either reason - fails with invalid code`() = runTest {
        listOf(Reason.INVALID_CODE, Reason.INVALID_IDENTIFIER_OR_CODE).forEach { reason ->
            every { client.verifyOtp(CODE, OtpType.OOB) } returns step(NextAction.IdentifyEmail, reason = reason)

            assertThat(repository.verify(CODE).failure().error).isEqualTo(SignupError.InvalidCode)
        }
    }

    @Test
    fun `verify - too many wrong codes - fails with it`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returns failingWith(EmbeddedAuthError.TooManyWrongOtpAttempts)

        assertThat(repository.verify(CODE).failure().error).isEqualTo(SignupError.TooManyWrongCodes)
    }

    @Test
    fun `verify - the code expired - fails with it`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returns failingWith(EmbeddedAuthError.ChallengeExpired)

        assertThat(repository.verify(CODE).failure().error).isEqualTo(SignupError.CodeExpired)
    }

    @Test
    fun `verify - the connection drops - fails with network`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returns failingWith(EmbeddedAuthError.Network)

        assertThat(repository.verify(CODE).failure().error).isEqualTo(SignupError.Network)
    }

    // endregion

    // region createAccount

    @Test
    fun `createAccount - confirmed - returns the credentials`() = runTest {
        val credentials = mockk<Credentials>()
        every { client.confirmSignup() } returns FakeRequestImpl(response = credentials)

        assertThat(repository.createAccount()).isEqualTo(Result.Success(credentials))
    }

    @Test
    fun `createAccount - the tenant wants more, a second factor say - fails with more steps required`() = runTest {
        every { client.confirmSignup() } returns step(NextAction.IdentifyEmail)

        assertThat(repository.createAccount().failure().error).isEqualTo(SignupError.MoreStepsRequired)
    }

    @Test
    fun `createAccount - the connection drops - fails with network`() = runTest {
        every { client.confirmSignup() } returns failingWith(EmbeddedAuthError.Network)

        assertThat(repository.createAccount().failure().error).isEqualTo(SignupError.Network)
    }

    @Test
    fun `createAccount - refused, a pre-registration Action say - fails with denied`() = runTest {
        every { client.confirmSignup() } returns failingWith(EmbeddedAuthError.AccessDenied)

        assertThat(repository.createAccount().failure().error).isEqualTo(SignupError.Denied)
    }

    // endregion

    // region one call at a time

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `two calls at once - the second waits for the whole of the first, not only its current step`() = runTest {
        val signingUp = InFlightRequest<Void?>()
        every { client.signup() } returns signingUp
        val opening = launch { repository.open() }
        runCurrent()

        val identifying = launch { repository.identify(EMAIL_ADDRESS) }
        runCurrent()
        assertThat(identifying.isCompleted).isFalse()
        signingUp.fail(
            sdkException(
                EmbeddedAuthError.InsufficientAuthorization(listOf(NextAction.IdentifyEmail), Reason.NONE),
                code = "insufficient_authorization",
            ),
        )
        advanceUntilIdle()

        assertThat(opening.isCompleted).isTrue()
        assertThat(identifying.isCompleted).isTrue()
        verify(exactly = 1) { client.identify(ADDRESS, IdentifierType.EMAIL) }
    }

    // endregion

    private fun signup(vararg identifiers: Pair<IdentifierType, Boolean>): NextAction.Signup =
        sdkInstance(
            List::class.java to identifiers.map { (type, required) ->
                sdkInstance<SignupIdentifier>(IdentifierType::class.java to type, Boolean::class.java to required)
            },
        )

    private fun challengeEmail(at: Int): NextAction.ChallengeEmail =
        sdkInstance(Int::class.java to at, String::class.java to MASKED_ADDRESS)

    private fun verifyOtp(): NextAction.VerifyOtp =
        sdkInstance(
            OtpChannel::class.java to OtpChannel.EMAIL,
            String::class.java to MASKED_ADDRESS,
        )

    private companion object {
        val EMAIL = IdentifierType.EMAIL
        val PHONE = IdentifierType.PHONE
        const val CONNECTION = "Username-Password-Authentication"
        const val ADDRESS = "jane@example.com"
        const val MASKED_ADDRESS = "j***@example.com"
        const val CODE = "123456"
        val EMAIL_ADDRESS = EmailIdentifier(SignupKind.Email, ADDRESS)
    }
}

/** Every failure comes back from a repository as a [SignupFailure], never thrown. */
private fun Result<*, SignupFailure>.failure(): SignupFailure {
    assertThat(this).isInstanceOf(Result.Error::class.java)
    return (this as Result.Error).error
}
