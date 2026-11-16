package com.auth0.universalcomponents.feature.embedded.data.repository

import com.auth0.android.Auth0Exception
import com.auth0.android.embedded.EmbeddedAuthClient
import com.auth0.android.embedded.EmbeddedAuthError
import com.auth0.android.embedded.EmbeddedAuthError.InsufficientAuthorization.Reason
import com.auth0.android.embedded.EmbeddedAuthException
import com.auth0.android.embedded.authorize.EmbeddedCapability
import com.auth0.android.embedded.authorize.IdentifierType
import com.auth0.android.embedded.authorize.NextAction
import com.auth0.android.embedded.authorize.OtpChannel
import com.auth0.android.embedded.authorize.OtpType
import com.auth0.android.embedded.authorize.PhoneDeliveryMethod
import com.auth0.android.request.Request
import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.data.FakeRequestImpl
import com.auth0.universalcomponents.data.embeddedauth.EmbeddedAuthSteps
import com.auth0.universalcomponents.data.embeddedauth.failingWith
import com.auth0.universalcomponents.data.embeddedauth.sdkInstance
import com.auth0.universalcomponents.data.embeddedauth.step
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.MfaFactor
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
        repository = EmbeddedLoginRepositoryImpl(EmbeddedAuthSteps(client), CONNECTION)
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
        val failure = EmbeddedLoginRepositoryImpl(EmbeddedAuthSteps(client), " ").discover().failure()

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.ConnectionMissing)
        verify(exactly = 0) { client.authorize(any(), any(), any(), any()) }
    }

    @Test
    fun `discover - tells the server only what these screens can drive`() = runTest {
        repository.discover()

        verify {
            client.authorize(
                CONNECTION,
                any(),
                any(),
                setOf(
                    EmbeddedCapability.IDENTIFY_EMAIL,
                    EmbeddedCapability.CHALLENGE_EMAIL,
                    EmbeddedCapability.IDENTIFY_PHONE,
                    EmbeddedCapability.IDENTIFY_USERNAME,
                    EmbeddedCapability.VERIFY_OTP,
                    EmbeddedCapability.VERIFY_PASSWORD,
                    EmbeddedCapability.CHALLENGE_PUSH,
                    EmbeddedCapability.CHALLENGE_PHONE,
                    EmbeddedCapability.VERIFY_OOB,
                    EmbeddedCapability.VERIFY_RECOVERY_CODE,
                    EmbeddedCapability.CONFIRM_RECOVERY_CODE,
                ),
            )
        }
    }

    @Test
    fun `discover - email offered - returns an email code option`() = runTest {
        assertThat(repository.discover()).isEqualTo(Result.Success(listOf(LoginOption.EmailOtp)))
    }

    @Test
    fun `discover - code already being sent - returns an email code option`() = runTest {
        every { client.authorize(CONNECTION, any(), any(), any()) } returns
            step(verifyOtp(OtpChannel.EMAIL, null))

        assertThat(repository.discover()).isEqualTo(Result.Success(listOf(LoginOption.EmailOtp)))
    }

    @Test
    fun `discover - phone offered - returns a phone code option`() = runTest {
        every { client.authorize(CONNECTION, any(), any(), any()) } returns
            step(NextAction.IdentifyEmail, NextAction.IdentifyPhone)

        assertThat(repository.discover())
            .isEqualTo(Result.Success(listOf(LoginOption.EmailOtp, LoginOption.PhoneOtp)))
    }

    @Test
    fun `discover - code already being sent by text or call - returns a phone code option`() = runTest {
        listOf(OtpChannel.SMS, OtpChannel.VOICE).forEach { channel ->
            every { client.authorize(CONNECTION, any(), any(), any()) } returns step(verifyOtp(channel, null))

            assertThat(repository.discover()).isEqualTo(Result.Success(listOf(LoginOption.PhoneOtp)))
        }
    }

    @Test
    fun `discover - unsupported method offered - returns it as unsupported`() = runTest {
        every { client.authorize(CONNECTION, any(), any(), any()) } returns step(UNSUPPORTED)

        assertThat(repository.discover())
            .isEqualTo(Result.Success(listOf(LoginOption.Unsupported(UNSUPPORTED.capability.value))))
    }

    @Test
    fun `discover - network error - fails with network`() = runTest {
        every { client.authorize(CONNECTION, any(), any(), any()) } returns failingWith(EmbeddedAuthError.Network)

        val failure = repository.discover().failure()

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.Network)
    }

    // endregion

    // region startOtp

    @Test
    fun `startOtp - email - right after discover - reuses that session`() = runTest {
        repository.discover()
        repository.startOtp(LoginOption.EmailOtp, EMAIL)

        verify(exactly = 1) { client.authorize(CONNECTION, any(), any(), any()) }
    }

    @Test
    fun `startOtp - email - resend - opens a new session first`() = runTest {
        repository.discover()
        repository.startOtp(LoginOption.EmailOtp, EMAIL)
        repository.startOtp(LoginOption.EmailOtp, EMAIL)

        verify(exactly = 2) { client.authorize(CONNECTION, any(), any(), any()) }
    }

    @Test
    fun `startOtp - email - email challenge offered - asks the server to send the code`() = runTest {
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returns step(challengeEmail(at = 2))

        assertThat(repository.startOtp(LoginOption.EmailOtp, EMAIL)).isEqualTo(Result.Success(Unit))

        verify(exactly = 1) { client.challengeEmail(2) }
    }

    @Test
    fun `startOtp - email - code already sent - doesn't send another`() = runTest {
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returns step(verifyOtp(OtpChannel.EMAIL, EMAIL))

        assertThat(repository.startOtp(LoginOption.EmailOtp, EMAIL)).isEqualTo(Result.Success(Unit))

        verify(exactly = 0) { client.challengeEmail(any()) }
    }

    @Test
    fun `startOtp - email - sending the code fails - returns that failure`() = runTest {
        every { client.challengeEmail(any()) } returns failingWith(EmbeddedAuthError.TooManyAttempts)

        val failure = repository.startOtp(LoginOption.EmailOtp, EMAIL).failure()

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.TooManyAttempts)
    }

    @Test
    fun `startOtp - email - no way to send a code - fails with code not sent`() = runTest {
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returns step(NextAction.IdentifyPhone)

        val failure = repository.startOtp(LoginOption.EmailOtp, EMAIL).failure()

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.CodeNotSent(LoginOption.EmailOtp))
        verify(exactly = 0) { client.challengeEmail(any()) }
    }

    @Test
    fun `startOtp - phone - identifies by phone and texts the code`() = runTest {
        every { client.identify(PHONE, IdentifierType.PHONE) } returns step(challengePhone(at = 3, TEXT, VOICE))
        every { client.challengePhone(3, PhoneDeliveryMethod.TEXT) } returns step(verifyOtp(OtpChannel.SMS, MASKED_PHONE))

        assertThat(repository.startOtp(LoginOption.PhoneOtp, PHONE)).isEqualTo(Result.Success(Unit))

        verify(exactly = 1) { client.challengePhone(3, PhoneDeliveryMethod.TEXT) }
        verify(exactly = 0) { client.challengeEmail(any()) }
    }

    @Test
    fun `startOtp - phone that only takes calls - calls it`() = runTest {
        every { client.identify(PHONE, IdentifierType.PHONE) } returns step(challengePhone(at = 3, VOICE))
        every { client.challengePhone(3, PhoneDeliveryMethod.VOICE) } returns step(verifyOtp(OtpChannel.VOICE, null))

        assertThat(repository.startOtp(LoginOption.PhoneOtp, PHONE)).isEqualTo(Result.Success(Unit))

        verify(exactly = 1) { client.challengePhone(3, PhoneDeliveryMethod.VOICE) }
    }

    @Test
    fun `startOtp - phone with no way named - texts it, as the SDK does`() = runTest {
        every { client.identify(PHONE, IdentifierType.PHONE) } returns step(challengePhone(at = 3))
        every { client.challengePhone(3, PhoneDeliveryMethod.TEXT) } returns step(verifyOtp(OtpChannel.SMS, null))

        assertThat(repository.startOtp(LoginOption.PhoneOtp, PHONE)).isEqualTo(Result.Success(Unit))

        verify(exactly = 1) { client.challengePhone(3, PhoneDeliveryMethod.TEXT) }
    }

    @Test
    fun `startOtp - phone code already sent - doesn't send another`() = runTest {
        every { client.identify(PHONE, IdentifierType.PHONE) } returns step(verifyOtp(OtpChannel.SMS, MASKED_PHONE))

        assertThat(repository.startOtp(LoginOption.PhoneOtp, PHONE)).isEqualTo(Result.Success(Unit))

        verify(exactly = 0) { client.challengePhone(any(), any()) }
    }

    @Test
    fun `startOtp - phone, sending the code fails - returns that failure`() = runTest {
        every { client.identify(PHONE, IdentifierType.PHONE) } returns step(challengePhone(at = 3, TEXT))
        every { client.challengePhone(any(), any()) } returns failingWith(EmbeddedAuthError.TooManyAttempts)

        val failure = repository.startOtp(LoginOption.PhoneOtp, PHONE).failure()

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.TooManyAttempts)
    }

    @Test
    fun `startOtp - phone, no way to send a code - fails with code not sent for the phone`() = runTest {
        // Only an email code is on offer, so a phone code would never come.
        every { client.identify(PHONE, IdentifierType.PHONE) } returns step(challengeEmail(at = 0))

        val failure = repository.startOtp(LoginOption.PhoneOtp, PHONE).failure()

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.CodeNotSent(LoginOption.PhoneOtp))
        verify(exactly = 0) { client.challengePhone(any(), any()) }
    }

    @Test
    fun `startOtp - server refuses the phone number - fails as an invalid phone, not the generic error`() = runTest {
        every { client.identify(PHONE, IdentifierType.PHONE) } returns
            failingWith(EmbeddedAuthError.InvalidRequest, description = "Invalid phone format.", statusCode = 400)

        val failure = repository.startOtp(LoginOption.PhoneOtp, PHONE).failure()

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.InvalidIdentifier(LoginOption.PhoneOtp))
        // Kept, so the log shows what the server said.
        assertThat(failure.cause).isInstanceOf(EmbeddedAuthException::class.java)
        assertThat((failure.cause as EmbeddedAuthException).description).isEqualTo("Invalid phone format.")
    }

    @Test
    fun `startOtp - server refuses the email - fails as an invalid email`() = runTest {
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returns
            failingWith(EmbeddedAuthError.InvalidRequest, statusCode = 400)

        val failure = repository.startOtp(LoginOption.EmailOtp, EMAIL).failure()

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.InvalidIdentifier(LoginOption.EmailOtp))
    }

    @Test
    fun `startOtp - a method these screens can't drive - fails with code not sent and calls nothing`() = runTest {
        val unsupported = LoginOption.Unsupported("verify-recovery-code")

        val failure = repository.startOtp(unsupported, PHONE).failure()

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.CodeNotSent(unsupported))
        verify(exactly = 0) { client.authorize(any(), any(), any(), any()) }
    }

    // endregion

    // region verify

    @Test
    fun `verifyOtp - code accepted - returns the credentials`() = runTest {
        val credentials = mockk<Credentials>()
        every { client.verifyOtp(CODE, OtpType.OOB) } returns FakeRequestImpl(response = credentials)

        assertThat(repository.verifyOtp(CODE)).isEqualTo(Result.Success(credentials))
    }

    @Test
    fun `verifyOtp - invalid code - fails with invalid code`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returns
            step(verifyOtp(OtpChannel.EMAIL, EMAIL), reason = Reason.INVALID_CODE)

        val failure = repository.verifyOtp(CODE).failure()

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.InvalidCode)
    }

    @Test
    fun `verifyOtp - invalid identifier or code - fails with invalid code, not as another step`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returns
            step(verifyOtp(OtpChannel.EMAIL, EMAIL), reason = Reason.INVALID_IDENTIFIER_OR_CODE)

        val failure = repository.verifyOtp(CODE).failure()

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.InvalidCode)
    }

    @Test
    fun `verifyOtp - code expired - fails with code expired`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returns failingWith(EmbeddedAuthError.ChallengeExpired)

        val failure = repository.verifyOtp(CODE).failure()

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.CodeExpired)
    }

    @Test
    fun `verifyOtp - second factors offered - fails as MFA required, with only the factors these screens drive, listed push, app, text, voice, email, recovery code`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returns step(
            challengeEmail(at = 0),
            sdkInstance<NextAction.ChallengePhone>(
                Int::class.java to 1,
                String::class.java to MASKED_PHONE,
                List::class.java to listOf(PhoneDeliveryMethod.TEXT, PhoneDeliveryMethod.VOICE),
            ),
            sdkInstance<NextAction.ChallengePush>(Int::class.java to 2, String::class.java to DEVICE),
            verifyOtp(OtpChannel.TOTP, null),
            NextAction.VerifyRecoveryCode,
            UNSUPPORTED,
            sdkInstance<NextAction.ChallengePush>(Int::class.java to 3, String::class.java to OTHER_DEVICE),
        )

        val failure = repository.verifyOtp(CODE).failure()

        assertThat(failure.error).isEqualTo(
            EmbeddedLoginError.MfaRequired(
                listOf(
                    MfaFactor.Push(2, DEVICE),
                    MfaFactor.Push(3, OTHER_DEVICE),
                    MfaFactor.AuthenticatorApp,
                    MfaFactor.Phone(1, MASKED_PHONE, PhoneDeliveryMethod.TEXT),
                    MfaFactor.Phone(1, MASKED_PHONE, PhoneDeliveryMethod.VOICE),
                    MfaFactor.Email(0, EMAIL),
                    MfaFactor.RecoveryCode,
                ),
            ),
        )
    }

    @Test
    fun `verifyOtp - only an authenticator app is offered - fails as MFA required, so the user can type its code`() =
        runTest {
            every { client.verifyOtp(CODE, OtpType.OOB) } returns step(verifyOtp(OtpChannel.TOTP, null))

            val failure = repository.verifyOtp(CODE).failure()

            assertThat(failure.error).isEqualTo(EmbeddedLoginError.MfaRequired(listOf(MfaFactor.AuthenticatorApp)))
        }

    @Test
    fun `verifyOtp - only a recovery code is offered - fails as MFA required, so the user can type it`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returns step(NextAction.VerifyRecoveryCode)

        val failure = repository.verifyOtp(CODE).failure()

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.MfaRequired(listOf(MfaFactor.RecoveryCode)))
    }

    @Test
    fun `verifyOtp - only steps these screens can't drive - hands MFA no factors, so it says so`() = runTest {
        listOf(
            verifyOtp(OtpChannel.EMAIL, MASKED_EMAIL),
            verifyOtp(OtpChannel.SMS, null),
            // A step no screen here can drive.
            UNSUPPORTED,
        ).forEach { nextAction ->
            every { client.verifyOtp(CODE, OtpType.OOB) } returns step(nextAction)

            val failure = repository.verifyOtp(CODE).failure()

            assertThat(failure.error).isEqualTo(EmbeddedLoginError.MfaRequired.NoUsableFactor)
        }
    }

    @Test
    fun `verifyOtp - server says no factor is usable - hands MFA no factors, so it says so`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returns failingWith(EmbeddedAuthError.NoEligibleFactors)

        val failure = repository.verifyOtp(CODE).failure()

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.MfaRequired.NoUsableFactor)
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

            val failure = repository.verifyOtp(CODE).failure()

            assertThat(failure.error).isEqualTo(loginError)
        }
    }

    @Test
    fun `errors - other server error - keeps the server's description`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returns
            failingWith(EmbeddedAuthError.AccessDenied, description = "Your account is blocked.")

        val failure = repository.verifyOtp(CODE).failure()

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.Server("Your account is blocked."))
    }

    @Test
    fun `errors - no usable server text - fails as unknown, keeping the SDK's code and text for the logs`() = runTest {
        mapOf<Request<Credentials, EmbeddedAuthException>, String>(
            failingWith<Credentials>(EmbeddedAuthError.Unknown, code = Auth0Exception.NON_JSON_ERROR, description = "debug text")
                to "${Auth0Exception.NON_JSON_ERROR}: debug text",
            failingWith<Credentials>(EmbeddedAuthError.Unknown, code = Auth0Exception.EMPTY_BODY_ERROR, description = "debug text")
                to "${Auth0Exception.EMPTY_BODY_ERROR}: debug text",
            failingWith<Credentials>(EmbeddedAuthError.AccessDenied, description = " ") to "access_denied:  ",
        ).forEach { (request, message) ->
            every { client.verifyOtp(CODE, OtpType.OOB) } returns request

            val failure = repository.verifyOtp(CODE).failure()

            assertThat(failure.error).isEqualTo(EmbeddedLoginError.Unknown(message))
        }
    }

    @Test
    fun `errors - an error the SDK recognised - never shows the server's machine token`() = runTest {
        listOf(
            EmbeddedAuthError.NoEligibleFactors to "no_eligible_factors",
            EmbeddedAuthError.AuthorizationRejected to "authorization_rejected",
            EmbeddedAuthError.InvalidRequest to "Missing parameter: connection",
        ).forEach { (sdkError, description) ->
            every { client.authorize(CONNECTION, any(), any(), any()) } returns
                failingWith(sdkError, description = description)

            val failure = repository.discover().failure()

            assertThat(failure.error).isInstanceOf(EmbeddedLoginError.Unknown::class.java)
        }
    }

    @Test
    fun `errors - the address is blocked, or the sign-in needs a consent - each has its own words`() = runTest {
        listOf(
            Triple(EmbeddedAuthError.IpBlocked, "ip_blocked", EmbeddedLoginError.IpBlocked),
            Triple(EmbeddedAuthError.ConsentRequired, "consent_required", EmbeddedLoginError.ConsentRequired),
        ).forEach { (sdkError, description, expected) ->
            every { client.authorize(CONNECTION, any(), any(), any()) } returns
                failingWith(sdkError, description = description)

            val failure = repository.discover().failure()

            assertThat(failure.error).isEqualTo(expected)
        }
    }

    @Test
    fun `errors - SDK error - keeps it as the cause, for the stack trace`() = runTest {
        every { client.authorize(CONNECTION, any(), any(), any()) } returns failingWith(EmbeddedAuthError.Network)

        val failure = repository.discover().failure()

        assertThat(failure.cause).isInstanceOf(EmbeddedAuthException::class.java)
    }

    @Test
    fun `errors - unexpected exception - fails as unknown, keeping it as the cause`() = runTest {
        val unexpected = IllegalStateException("debug output")
        every { client.authorize(CONNECTION, any(), any(), any()) } throws unexpected

        val failure = repository.discover().failure()

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.Unknown("debug output"))
        assertThat(failure.cause).isSameInstanceAs(unexpected)
    }

    @Test
    fun `errors - cancelled - lets the cancellation through as it is`() = runTest {
        every { client.authorize(CONNECTION, any(), any(), any()) } throws CancellationException()

        val error = runCatching { repository.discover() }.exceptionOrNull()

        assertThat(error).isInstanceOf(CancellationException::class.java)
    }

    // endregion

    // region identify

    @Test
    fun `identify - username offered at discovery - returns the username method`() = runTest {
        every { client.authorize(CONNECTION, any(), any(), any()) } returns
            step(NextAction.IdentifyEmail, NextAction.IdentifyUsername)

        val options = (repository.discover() as Result.Success).data

        assertThat(options).containsExactly(LoginOption.EmailOtp, LoginOption.Username).inOrder()
    }

    @Test
    fun `identify - code listed first - the code leads, and nothing is sent`() = runTest {
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returns
            step(challengeEmail(at = 0), NextAction.VerifyPassword)
        repository.discover()

        val offer = (repository.identify(LoginOption.EmailOtp, EMAIL) as Result.Success).data

        assertThat(offer).containsExactly(LoginOption.EmailOtp, LoginOption.Password).inOrder()
        verify(exactly = 0) { client.challengeEmail(any()) }
    }

    @Test
    fun `identify - password listed first - the password leads`() = runTest {
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returns
            step(NextAction.VerifyPassword, challengeEmail(at = 0))
        repository.discover()

        val offer = (repository.identify(LoginOption.EmailOtp, EMAIL) as Result.Success).data

        assertThat(offer).containsExactly(LoginOption.Password, LoginOption.EmailOtp).inOrder()
    }

    @Test
    fun `identify - only a password offered - that is all`() = runTest {
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returns step(NextAction.VerifyPassword)
        repository.discover()

        val offer = (repository.identify(LoginOption.EmailOtp, EMAIL) as Result.Success).data

        assertThat(offer).containsExactly(LoginOption.Password)
    }

    @Test
    fun `identify - a code already sent by the server - counts as a code offered`() = runTest {
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returns
            step(verifyOtp(OtpChannel.EMAIL, EMAIL), NextAction.VerifyPassword)
        repository.discover()

        val offer = (repository.identify(LoginOption.EmailOtp, EMAIL) as Result.Success).data

        assertThat(offer).containsExactly(LoginOption.EmailOtp, LoginOption.Password).inOrder()
    }

    @Test
    fun `identify - other steps in the menu - don't change which method leads`() = runTest {
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returns
            step(UNSUPPORTED, NextAction.VerifyPassword, challengeEmail(at = 0))
        repository.discover()

        val offer = (repository.identify(LoginOption.EmailOtp, EMAIL) as Result.Success).data

        assertThat(offer).containsExactly(LoginOption.Password, LoginOption.EmailOtp).inOrder()
    }

    @Test
    fun `identify - phone that takes texts, with a password listed - offers the code only`() = runTest {
        every { client.identify(PHONE, IdentifierType.PHONE) } returns
            step(challengePhone(at = 0, TEXT, VOICE), NextAction.VerifyPassword)
        repository.discover()

        val offer = (repository.identify(LoginOption.PhoneOtp, PHONE) as Result.Success).data

        // A phone is not offered a password yet, whatever the server lists.
        assertThat(offer).containsExactly(LoginOption.PhoneOtp)
    }

    @Test
    fun `identify - phone that only takes calls, with a password listed - the call is the way left`() = runTest {
        every { client.identify(PHONE, IdentifierType.PHONE) } returns
            step(challengePhone(at = 0, VOICE), NextAction.VerifyPassword)
        repository.discover()

        val offer = (repository.identify(LoginOption.PhoneOtp, PHONE) as Result.Success).data

        assertThat(offer).containsExactly(LoginOption.PhoneOtp)
    }

    @Test
    fun `identify - phone with only a password listed - offers nothing`() = runTest {
        every { client.identify(PHONE, IdentifierType.PHONE) } returns step(NextAction.VerifyPassword)
        repository.discover()

        val offer = (repository.identify(LoginOption.PhoneOtp, PHONE) as Result.Success).data

        assertThat(offer).isEmpty()
    }

    @Test
    fun `identify - phone that only takes calls, with no password - the call is the way left`() = runTest {
        every { client.identify(PHONE, IdentifierType.PHONE) } returns step(challengePhone(at = 0, VOICE))
        repository.discover()

        val offer = (repository.identify(LoginOption.PhoneOtp, PHONE) as Result.Success).data

        assertThat(offer).containsExactly(LoginOption.PhoneOtp)
    }

    @Test
    fun `identify - username with a password step - identifies by username and offers the password`() = runTest {
        every { client.identify(USERNAME, IdentifierType.USERNAME) } returns step(NextAction.VerifyPassword)
        repository.discover()

        val offer = (repository.identify(LoginOption.Username, USERNAME) as Result.Success).data

        assertThat(offer).containsExactly(LoginOption.Password)
    }

    @Test
    fun `identify - username with no password step - fails like a wrong password, so nobody can tell if it exists`() =
        runTest {
            every { client.identify(USERNAME, IdentifierType.USERNAME) } returns step(UNSUPPORTED)
            repository.discover()

            val failure = repository.identify(LoginOption.Username, USERNAME).failure()

            assertThat(failure.error).isEqualTo(EmbeddedLoginError.InvalidPassword(LoginOption.Username))
        }

    @Test
    fun `identify - server refuses the email - fails as an invalid email`() = runTest {
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returns failingWith(EmbeddedAuthError.InvalidRequest)
        repository.discover()

        val failure = repository.identify(LoginOption.EmailOtp, EMAIL).failure()

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.InvalidIdentifier(LoginOption.EmailOtp))
    }

    @Test
    fun `identify - a method that can't be identified - fails and calls nothing`() = runTest {
        val failure = repository.identify(LoginOption.Password, "x").failure()

        assertThat(failure.error).isInstanceOf(EmbeddedLoginError.Unknown::class.java)
        verify(exactly = 0) { client.identify(any(), any()) }
    }

    // endregion

    // region sessions

    @Test
    fun `sessions - code right after identify - goes on the same session, with no second identify`() = runTest {
        repository.discover()
        repository.identify(LoginOption.EmailOtp, EMAIL)
        repository.startOtp(LoginOption.EmailOtp, EMAIL)

        verify(exactly = 1) { client.authorize(CONNECTION, any(), any(), any()) }
        verify(exactly = 1) { client.identify(EMAIL, IdentifierType.EMAIL) }
        verify(exactly = 1) { client.challengeEmail(0) }
    }

    @Test
    fun `sessions - a code the server already sent - isn't sent again`() = runTest {
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returns step(verifyOtp(OtpChannel.EMAIL, EMAIL))
        repository.discover()
        repository.identify(LoginOption.EmailOtp, EMAIL)

        assertThat(repository.startOtp(LoginOption.EmailOtp, EMAIL)).isEqualTo(Result.Success(Unit))
        verify(exactly = 0) { client.challengeEmail(any()) }
    }

    @Test
    fun `sessions - identify twice - the second opens a new session`() = runTest {
        repository.discover()
        repository.identify(LoginOption.EmailOtp, EMAIL)
        repository.identify(LoginOption.EmailOtp, EMAIL)

        verify(exactly = 2) { client.authorize(CONNECTION, any(), any(), any()) }
    }

    @Test
    fun `sessions - a code for someone else - opens a new session`() = runTest {
        every { client.identify(OTHER_EMAIL, IdentifierType.EMAIL) } returns step(challengeEmail(at = 0))
        repository.discover()
        repository.identify(LoginOption.EmailOtp, EMAIL)
        repository.startOtp(LoginOption.EmailOtp, OTHER_EMAIL)

        verify(exactly = 2) { client.authorize(CONNECTION, any(), any(), any()) }
        verify(exactly = 1) { client.identify(OTHER_EMAIL, IdentifierType.EMAIL) }
    }

    @Test
    fun `sessions - code after a wrong password - opens a new session`() = runTest {
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returns
            step(NextAction.VerifyPassword, challengeEmail(at = 0))
        every { client.verifyPassword(PASSWORD) } returns
            step(NextAction.VerifyPassword, reason = Reason.INVALID_IDENTIFIER_OR_PASSWORD)
        repository.discover()
        repository.identify(LoginOption.EmailOtp, EMAIL)
        repository.verifyPassword(LoginOption.EmailOtp, EMAIL, PASSWORD)
        repository.startOtp(LoginOption.EmailOtp, EMAIL)

        verify(exactly = 2) { client.authorize(CONNECTION, any(), any(), any()) }
        verify(exactly = 2) { client.identify(EMAIL, IdentifierType.EMAIL) }
    }

    @Test
    fun `sessions - code after a rate limit - opens a new session`() = runTest {
        every { client.verifyPassword(PASSWORD) } returns
            failingWith(EmbeddedAuthError.TooManyAttempts, statusCode = 429)
        repository.discover()
        repository.identify(LoginOption.EmailOtp, EMAIL)
        repository.verifyPassword(LoginOption.EmailOtp, EMAIL, PASSWORD)
        repository.startOtp(LoginOption.EmailOtp, EMAIL)

        verify(exactly = 2) { client.authorize(CONNECTION, any(), any(), any()) }
    }

    @Test
    fun `sessions - a refused reopen - is reopened on the next try`() = runTest {
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returnsMany listOf(
            failingWith(EmbeddedAuthError.Network),
            step(challengeEmail(at = 0)),
        )
        repository.discover()

        assertThat(repository.startOtp(LoginOption.EmailOtp, EMAIL).failure().error)
            .isEqualTo(EmbeddedLoginError.Network)
        assertThat(repository.startOtp(LoginOption.EmailOtp, EMAIL)).isEqualTo(Result.Success(Unit))

        verify(exactly = 1) { client.authorize(CONNECTION, any(), any(), any()) }
        verify(exactly = 2) { client.identify(EMAIL, IdentifierType.EMAIL) }
    }

    @Test
    fun `sessions - the server ended it - the next code is refused as no longer valid, without asking the server`() =
        runTest {
            every { client.verifyOtp(CODE, OtpType.OOB) } returns
                failingWith(EmbeddedAuthError.TooManyWrongOtpAttempts, statusCode = 403)
            repository.discover()
            repository.startOtp(LoginOption.EmailOtp, EMAIL)
            assertThat(repository.verifyOtp(CODE).failure().error).isEqualTo(EmbeddedLoginError.TooManyWrongCodes)

            val failure = repository.verifyOtp(CODE).failure()

            assertThat(failure.error).isEqualTo(EmbeddedLoginError.CodeNoLongerValid)
            verify(exactly = 1) { client.verifyOtp(CODE, OtpType.OOB) }
        }

    @Test
    fun `sessions - a resend after the server ended it - checks the new code again`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returnsMany listOf(
            failingWith(EmbeddedAuthError.TooManyWrongOtpAttempts, statusCode = 403),
            step(verifyOtp(OtpChannel.EMAIL, EMAIL), reason = Reason.INVALID_CODE),
        )
        repository.discover()
        repository.startOtp(LoginOption.EmailOtp, EMAIL)
        repository.verifyOtp(CODE)
        repository.startOtp(LoginOption.EmailOtp, EMAIL)

        assertThat(repository.verifyOtp(CODE).failure().error).isEqualTo(EmbeddedLoginError.InvalidCode)
    }

    @Test
    fun `sessions - a network drop while checking a code - keeps the session, so the same code is tried again`() =
        runTest {
            every { client.verifyOtp(CODE, OtpType.OOB) } returns failingWith(EmbeddedAuthError.Network)
            repository.discover()
            repository.startOtp(LoginOption.EmailOtp, EMAIL)
            repository.verifyOtp(CODE)

            assertThat(repository.verifyOtp(CODE).failure().error).isEqualTo(EmbeddedLoginError.Network)
            verify(exactly = 2) { client.verifyOtp(CODE, OtpType.OOB) }
        }

    // endregion

    // region verifyPassword

    @Test
    fun `verifyPassword - password accepted - returns the credentials on the session identify opened`() = runTest {
        val credentials = mockk<Credentials>()
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returns step(NextAction.VerifyPassword)
        every { client.verifyPassword(PASSWORD) } returns FakeRequestImpl(response = credentials)
        repository.discover()
        repository.identify(LoginOption.EmailOtp, EMAIL)

        val result = repository.verifyPassword(LoginOption.EmailOtp, EMAIL, PASSWORD)

        assertThat(result).isEqualTo(Result.Success(credentials))
        verify(exactly = 1) { client.authorize(CONNECTION, any(), any(), any()) }
        verify(exactly = 1) { client.identify(EMAIL, IdentifierType.EMAIL) }
    }

    @Test
    fun `verifyPassword - wrong password - fails as invalid for what was typed, and the next try reuses the session`() =
        runTest {
            every { client.identify(USERNAME, IdentifierType.USERNAME) } returns step(NextAction.VerifyPassword)
            every { client.verifyPassword(PASSWORD) } returns
                step(NextAction.VerifyPassword, reason = Reason.INVALID_IDENTIFIER_OR_PASSWORD)
            repository.discover()
            repository.identify(LoginOption.Username, USERNAME)

            val first = repository.verifyPassword(LoginOption.Username, USERNAME, PASSWORD).failure()
            repository.verifyPassword(LoginOption.Username, USERNAME, PASSWORD)

            assertThat(first.error).isEqualTo(EmbeddedLoginError.InvalidPassword(LoginOption.Username))
            verify(exactly = 1) { client.authorize(CONNECTION, any(), any(), any()) }
            verify(exactly = 1) { client.identify(USERNAME, IdentifierType.USERNAME) }
            verify(exactly = 2) { client.verifyPassword(PASSWORD) }
        }

    @Test
    fun `verifyPassword - network drop - keeps the session`() = runTest {
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returns step(NextAction.VerifyPassword)
        every { client.verifyPassword(PASSWORD) } returns failingWith(EmbeddedAuthError.Network)
        repository.discover()
        repository.identify(LoginOption.EmailOtp, EMAIL)

        repository.verifyPassword(LoginOption.EmailOtp, EMAIL, PASSWORD)
        repository.verifyPassword(LoginOption.EmailOtp, EMAIL, PASSWORD)

        verify(exactly = 1) { client.authorize(CONNECTION, any(), any(), any()) }
        verify(exactly = 2) { client.verifyPassword(PASSWORD) }
    }

    @Test
    fun `verifyPassword - after a rate limit - the next try opens a new session for the same person`() = runTest {
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returns step(NextAction.VerifyPassword)
        every { client.verifyPassword(PASSWORD) } returnsMany listOf(
            failingWith(EmbeddedAuthError.TooManyAttempts, statusCode = 429),
            step(NextAction.VerifyPassword, reason = Reason.INVALID_IDENTIFIER_OR_PASSWORD),
        )
        repository.discover()
        repository.identify(LoginOption.EmailOtp, EMAIL)

        assertThat(repository.verifyPassword(LoginOption.EmailOtp, EMAIL, PASSWORD).failure().error)
            .isEqualTo(EmbeddedLoginError.TooManyAttempts)
        assertThat(repository.verifyPassword(LoginOption.EmailOtp, EMAIL, PASSWORD).failure().error)
            .isEqualTo(EmbeddedLoginError.InvalidPassword(LoginOption.EmailOtp))

        verify(exactly = 2) { client.authorize(CONNECTION, any(), any(), any()) }
        verify(exactly = 2) { client.identify(EMAIL, IdentifierType.EMAIL) }
    }

    @Test
    fun `verifyPassword - breached password - fails as leaked, and the next try opens a new session`() = runTest {
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returns step(NextAction.VerifyPassword)
        every { client.verifyPassword(PASSWORD) } returns failingWith(EmbeddedAuthError.PasswordLeaked, statusCode = 401)
        repository.discover()
        repository.identify(LoginOption.EmailOtp, EMAIL)

        val failure = repository.verifyPassword(LoginOption.EmailOtp, EMAIL, PASSWORD).failure()
        repository.verifyPassword(LoginOption.EmailOtp, EMAIL, PASSWORD)

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.PasswordLeaked)
        verify(exactly = 2) { client.authorize(CONNECTION, any(), any(), any()) }
    }

    @Test
    fun `verifyPassword - nothing left to ask for - fails with its own error`() = runTest {
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returns step(NextAction.VerifyPassword)
        every { client.verifyPassword(PASSWORD) } returns failingWith(EmbeddedAuthError.NoNextSteps)
        repository.discover()
        repository.identify(LoginOption.EmailOtp, EMAIL)

        val failure = repository.verifyPassword(LoginOption.EmailOtp, EMAIL, PASSWORD).failure()

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.NoNextSteps)
    }

    @Test
    fun `verifyPassword - a session that was never identified - identifies the same person first`() = runTest {
        every { client.verifyPassword(PASSWORD) } returns FakeRequestImpl(response = mockk<Credentials>())
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returns step(NextAction.VerifyPassword)
        repository.discover()

        repository.verifyPassword(LoginOption.EmailOtp, EMAIL, PASSWORD)

        verify(exactly = 1) { client.authorize(CONNECTION, any(), any(), any()) }
        verify(exactly = 1) { client.identify(EMAIL, IdentifierType.EMAIL) }
        verify(exactly = 1) { client.verifyPassword(PASSWORD) }
    }

    @Test
    fun `verifyPassword - second factor needed - fails as MFA required with the factors`() = runTest {
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returns step(NextAction.VerifyPassword)
        every { client.verifyPassword(PASSWORD) } returns step(NextAction.VerifyRecoveryCode)
        repository.discover()
        repository.identify(LoginOption.EmailOtp, EMAIL)

        val failure = repository.verifyPassword(LoginOption.EmailOtp, EMAIL, PASSWORD).failure()

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.MfaRequired(listOf(MfaFactor.RecoveryCode)))
    }

    @Test
    fun `verifyPassword - second factor needed but none usable - hands MFA no factors`() = runTest {
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returns step(NextAction.VerifyPassword)
        every { client.verifyPassword(PASSWORD) } returns failingWith(EmbeddedAuthError.NoEligibleFactors)
        repository.discover()
        repository.identify(LoginOption.EmailOtp, EMAIL)

        val failure = repository.verifyPassword(LoginOption.EmailOtp, EMAIL, PASSWORD).failure()

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.MfaRequired.NoUsableFactor)
    }

    @Test
    fun `verifyPassword - an answer for some other reason - fails as unexpected`() = runTest {
        every { client.identify(EMAIL, IdentifierType.EMAIL) } returns step(NextAction.VerifyPassword)
        every { client.verifyPassword(PASSWORD) } returns
            step(NextAction.VerifyPassword, reason = Reason.SLOW_DOWN)
        repository.discover()
        repository.identify(LoginOption.EmailOtp, EMAIL)

        val failure = repository.verifyPassword(LoginOption.EmailOtp, EMAIL, PASSWORD).failure()

        assertThat(failure.error).isInstanceOf(EmbeddedLoginError.Unknown::class.java)
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

            val failure = repository.verifyOtp(CODE).failure()

            assertThat(failure.canRetryStep).isTrue()
        }
    }

    @Test
    fun `retrying a step - any other error - the session is gone`() = runTest {
        every { client.verifyOtp(CODE, OtpType.OOB) } returns
            failingWith(EmbeddedAuthError.TooManyWrongOtpAttempts, statusCode = 403)

        val failure = repository.verifyOtp(CODE).failure()

        assertThat(failure.canRetryStep).isFalse()
    }

    // endregion

    private fun challengeEmail(at: Int): NextAction.ChallengeEmail =
        sdkInstance(Int::class.java to at, String::class.java to EMAIL)

    private fun challengePhone(at: Int, vararg ways: PhoneDeliveryMethod): NextAction.ChallengePhone =
        sdkInstance(Int::class.java to at, String::class.java to MASKED_PHONE, List::class.java to ways.toList())

    private fun verifyOtp(channel: OtpChannel, destination: String?): NextAction.VerifyOtp =
        sdkInstance(OtpChannel::class.java to channel, String::class.java to destination)

    private companion object {
        val TEXT = PhoneDeliveryMethod.TEXT
        val VOICE = PhoneDeliveryMethod.VOICE
        const val CONNECTION = "email"
        const val EMAIL = "jane@example.com"
        const val OTHER_EMAIL = "john@example.com"
        const val USERNAME = "jane"
        const val PASSWORD = "s3cret!"

        /** A step these screens have no way to drive: whatever the server may add after this version. */
        val UNSUPPORTED = sdkInstance<NextAction.Unknown>(String::class.java to "future_step")
        const val PHONE = "+14233530900"
        const val MASKED_EMAIL = "j***@example.com"
        const val MASKED_PHONE = "+1***1234"
        const val DEVICE = "Jane's phone"
        const val OTHER_DEVICE = "Jane's tablet"
        const val CODE = "123456"
    }
}

/** Every failure comes back from a repository as a [LoginFailure], never thrown. */
private fun Result<*, LoginFailure>.failure(): LoginFailure {
    assertThat(this).isInstanceOf(Result.Error::class.java)
    return (this as Result.Error).error
}
