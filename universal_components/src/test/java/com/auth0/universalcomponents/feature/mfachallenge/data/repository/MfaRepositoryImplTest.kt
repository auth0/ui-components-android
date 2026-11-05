package com.auth0.universalcomponents.feature.mfachallenge.data.repository

import com.auth0.android.embedded.EmbeddedAuthClient
import com.auth0.android.embedded.EmbeddedAuthError
import com.auth0.android.embedded.EmbeddedAuthError.InsufficientAuthorization.Reason
import com.auth0.android.embedded.EmbeddedAuthException
import com.auth0.android.embedded.authorize.NextAction
import com.auth0.android.embedded.authorize.OtpChannel
import com.auth0.android.embedded.authorize.OtpType
import com.auth0.android.embedded.authorize.PhoneDeliveryMethod
import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.data.FakeRequestImpl
import com.auth0.universalcomponents.data.embeddedauth.EmbeddedAuthSteps
import com.auth0.universalcomponents.data.embeddedauth.failingWith
import com.auth0.universalcomponents.data.embeddedauth.sdkInstance
import com.auth0.universalcomponents.data.embeddedauth.step
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.mfachallenge.MfaError
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.CodeStatus
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.PushStatus
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class MfaRepositoryImplTest {

    private lateinit var client: EmbeddedAuthClient
    private lateinit var repository: MfaRepositoryImpl

    @Before
    fun setup() {
        client = mockk()
        repository = MfaRepositoryImpl(EmbeddedAuthSteps(client))
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // region sendPush

    @Test
    fun `sendPush - sent - pushes to that device and returns the server's wait and resend offer`() = runTest {
        every { client.challengePush(PHONE.index) } returns step(verifyOob(POLL_MS), challengePush())

        assertThat(repository.sendPush(PHONE))
            .isEqualTo(Result.Success(PushStatus.Pending(POLL_MS.toLong(), canResend = true)))

        verify(exactly = 1) { client.challengePush(PHONE.index) }
    }

    @Test
    fun `sendPush - the server doesn't offer another push - says resend isn't allowed`() = runTest {
        every { client.challengePush(PHONE.index) } returns step(verifyOob(POLL_MS))

        assertThat(repository.sendPush(PHONE))
            .isEqualTo(Result.Success(PushStatus.Pending(POLL_MS.toLong(), canResend = false)))
    }

    @Test
    fun `sendPush - no wait to check it with - fails as unexpected`() = runTest {
        every { client.challengePush(PHONE.index) } returns step(NextAction.VerifyRecoveryCode)

        assertThat(repository.sendPush(PHONE).mfaError()).isInstanceOf(MfaError.Unexpected::class.java)
    }

    @Test
    fun `sendPush - signed in on send - fails as unexpected`() = runTest {
        every { client.challengePush(PHONE.index) } returns FakeRequestImpl(response = null)

        assertThat(repository.sendPush(PHONE).mfaError()).isInstanceOf(MfaError.Unexpected::class.java)
    }

    @Test
    fun `sendPush - network error - fails with network`() = runTest {
        every { client.challengePush(PHONE.index) } returns failingWith(EmbeddedAuthError.Network)

        assertThat(repository.sendPush(PHONE).mfaError()).isInstanceOf(MfaError.Network::class.java)
    }

    // endregion

    // region checkPushStatus

    @Test
    fun `checkPushStatus - approved - returns the credentials`() = runTest {
        val credentials = mockk<Credentials>()
        every { client.verifyOob() } returns FakeRequestImpl(response = credentials)

        assertThat(repository.checkPushStatus()).isEqualTo(Result.Success(PushStatus.Approved(credentials)))
    }

    @Test
    fun `checkPushStatus - pending - still waiting, with the server's new wait and resend offer`() = runTest {
        every { client.verifyOob() } returns step(verifyOob(SLOWER_MS), reason = Reason.AUTHORIZATION_PENDING)

        assertThat(repository.checkPushStatus())
            .isEqualTo(Result.Success(PushStatus.Pending(SLOWER_MS.toLong(), canResend = false)))
    }

    @Test
    fun `checkPushStatus - slow down - still waiting, marked as too soon`() = runTest {
        every { client.verifyOob() } returns step(verifyOob(SLOWER_MS), challengePush(), reason = Reason.SLOW_DOWN)

        assertThat(repository.checkPushStatus()).isEqualTo(
            Result.Success(PushStatus.Pending(SLOWER_MS.toLong(), canResend = true, isSlowDown = true)),
        )
    }

    @Test
    fun `checkPushStatus - pending without a wait - leaves the wait to MFA`() = runTest {
        every { client.verifyOob() } returns step(reason = Reason.AUTHORIZATION_PENDING)

        assertThat(repository.checkPushStatus())
            .isEqualTo(Result.Success(PushStatus.Pending(retryInMs = null, canResend = false)))
    }

    @Test
    fun `checkPushStatus - no reason but the push can still be checked - is still pending, not another factor`() =
        runTest {
            every { client.verifyOob() } returns step(verifyOob(SLOWER_MS), reason = Reason.NONE)

            assertThat(repository.checkPushStatus())
                .isEqualTo(Result.Success(PushStatus.Pending(SLOWER_MS.toLong(), canResend = false)))
        }

    @Test
    fun `checkPushStatus - approved, and the server asks for another factor - returns the next menu`() = runTest {
        val tablet = challengePush(index = 1, name = "Jane's tablet")
        every { client.verifyOob() } returns step(tablet, NextAction.VerifyRecoveryCode)

        assertThat(repository.checkPushStatus())
            .isEqualTo(Result.Success(PushStatus.AnotherFactor(listOf(MfaFactor.Push(1, "Jane's tablet")))))
    }

    @Test
    fun `checkPushStatus - another factor with none usable here - returns an empty menu, MFA decides`() = runTest {
        every { client.verifyOob() } returns step(NextAction.VerifyRecoveryCode)

        assertThat(repository.checkPushStatus()).isEqualTo(Result.Success(PushStatus.AnotherFactor(emptyList())))
    }

    @Test
    fun `checkPushStatus - answered as a wrong code - fails as unexpected`() = runTest {
        every { client.verifyOob() } returns step(verifyOob(POLL_MS), reason = Reason.INVALID_CODE)

        assertThat(repository.checkPushStatus().mfaError()).isInstanceOf(MfaError.Unexpected::class.java)
    }

    @Test
    fun `checkPushStatus - user denies the push - is an answer, not an error`() = runTest {
        every { client.verifyOob() } returns failingWith(EmbeddedAuthError.AuthorizationRejected, statusCode = 403)

        val answer = repository.checkPushStatus()

        assertThat(answer).isInstanceOf(Result.Success::class.java)
        val status = (answer as Result.Success).data
        assertThat(status).isInstanceOf(PushStatus.Rejected::class.java)
        // The SDK reply stays behind it, for an app that wants the details.
        assertThat((status as PushStatus.Rejected).cause).isInstanceOf(EmbeddedAuthException::class.java)
    }

    @Test
    fun `checkPushStatus - SDK errors - named as iOS names them`() = runTest {
        mapOf(
            EmbeddedAuthError.ChallengeExpired to MfaError.TokenExpired::class.java,
            EmbeddedAuthError.NoEligibleFactors to MfaError.NoEligibleFactors::class.java,
            // The session is gone, e.g. a late approval already finished it: never the SDK's developer text.
            EmbeddedAuthError.NoActiveSession to MfaError.TokenInvalid::class.java,
            EmbeddedAuthError.SessionExpired to MfaError.TokenInvalid::class.java,
            EmbeddedAuthError.TooManyAttempts to MfaError.TooManyAttempts::class.java,
            EmbeddedAuthError.TooManyLogins to MfaError.TooManyAttempts::class.java,
            EmbeddedAuthError.TooManyWrongOtpAttempts to MfaError.TooManyAttempts::class.java,
            EmbeddedAuthError.IpBlocked to MfaError.Unexpected::class.java,
            EmbeddedAuthError.AccessDenied to MfaError.Unexpected::class.java,
        ).forEach { (sdkError, expected) ->
            every { client.verifyOob() } returns failingWith(sdkError, statusCode = 403)

            val error = repository.checkPushStatus().mfaError()

            assertThat(error).isInstanceOf(expected)
            assertThat(error.cause).isNotNull()
        }
    }

    @Test
    fun `checkPushStatus - IP block, consent and the like - never show the server's machine token`() = runTest {
        listOf(
            EmbeddedAuthError.IpBlocked to "ip_blocked",
            EmbeddedAuthError.ConsentRequired to "consent_required",
            EmbeddedAuthError.InvalidRequest to "Missing parameter: auth_session",
        ).forEach { (sdkError, description) ->
            every { client.verifyOob() } returns failingWith(sdkError, description = description)

            val error = repository.checkPushStatus().mfaError()

            assertThat(error).isInstanceOf(MfaError.Unexpected::class.java)
            assertThat((error as MfaError.Unexpected).serverMessage).isNull()
        }
    }

    @Test
    fun `checkPushStatus - an error the server explained - keeps its words for the screen`() = runTest {
        every { client.verifyOob() } returns failingWith(
            EmbeddedAuthError.AccessDenied,
            code = "access_denied",
            description = "Your account is blocked.",
        )

        val error = repository.checkPushStatus().mfaError()

        assertThat((error as MfaError.Unexpected).serverMessage).isEqualTo("Your account is blocked.")
    }

    // endregion

    // region phone code

    @Test
    fun `sendPhoneCode - sent - texts that phone and returns whether the server offers another`() = runTest {
        every { client.challengePhone(SMS.index, PhoneDeliveryMethod.TEXT) } returns
            step(verifyOtp(OtpChannel.SMS), challengePhone())

        assertThat(repository.sendPhoneCode(SMS)).isEqualTo(Result.Success(true))

        verify(exactly = 1) { client.challengePhone(SMS.index, PhoneDeliveryMethod.TEXT) }
    }

    @Test
    fun `sendPhoneCode - by voice, and no second offer - calls that phone and says resend isn't allowed`() = runTest {
        every { client.challengePhone(SMS.index, PhoneDeliveryMethod.VOICE) } returns
            step(verifyOtp(OtpChannel.VOICE))

        assertThat(repository.sendPhoneCode(CALL)).isEqualTo(Result.Success(false))
    }

    @Test
    fun `sendPhoneCode - nothing to verify it with, or it signed in - fails as unexpected`() = runTest {
        every { client.challengePhone(any(), any()) } returns step(NextAction.VerifyRecoveryCode)
        assertThat(repository.sendPhoneCode(SMS).mfaError()).isInstanceOf(MfaError.Unexpected::class.java)

        every { client.challengePhone(any(), any()) } returns FakeRequestImpl(response = null)
        assertThat(repository.sendPhoneCode(SMS).mfaError()).isInstanceOf(MfaError.Unexpected::class.java)
    }

    @Test
    fun `sendPhoneCode - the server refuses - fails in MFA's terms, with the SDK error as cause`() = runTest {
        every { client.challengePhone(any(), any()) } returns failingWith(EmbeddedAuthError.TooManyAttempts)

        val error = repository.sendPhoneCode(SMS).mfaError()

        assertThat(error).isInstanceOf(MfaError.TooManyAttempts::class.java)
        assertThat(error.cause).isNotNull()
    }

    @Test
    fun `verifyCode - right code - signs in`() = runTest {
        val credentials = mockk<Credentials>()
        every { client.verifyOtp("123456", OtpType.OOB) } returns FakeRequestImpl(response = credentials)

        assertThat(repository.verifyCode("123456")).isEqualTo(Result.Success(CodeStatus.Verified(credentials)))
    }

    @Test
    fun `verifyCode - wrong code - is not an error, the user types another`() = runTest {
        listOf(Reason.INVALID_CODE, Reason.INVALID_IDENTIFIER_OR_CODE).forEach { reason ->
            every { client.verifyOtp(any(), any()) } returns step(verifyOtp(OtpChannel.SMS), reason = reason)

            assertThat(repository.verifyCode("000000")).isEqualTo(Result.Success(CodeStatus.WrongCode))
        }
    }

    @Test
    fun `verifyCode - right code and the policy asks for another factor - returns the next round's menu`() = runTest {
        every { client.verifyOtp(any(), any()) } returns step(challengePush(), challengePhone(), reason = Reason.NONE)

        val status = repository.verifyCode("123456")

        assertThat(status).isEqualTo(
            Result.Success(CodeStatus.AnotherFactor(listOf(PHONE, SMS, CALL))),
        )
    }

    @Test
    fun `verifyCode - any other answer, or a failure - is unexpected, or the mapped error`() = runTest {
        every { client.verifyOtp(any(), any()) } returns step(verifyOtp(OtpChannel.SMS), reason = Reason.SLOW_DOWN)
        assertThat(repository.verifyCode("123456").mfaError()).isInstanceOf(MfaError.Unexpected::class.java)

        every { client.verifyOtp(any(), any()) } returns failingWith(EmbeddedAuthError.ChallengeExpired)
        assertThat(repository.verifyCode("123456").mfaError()).isInstanceOf(MfaError.TokenExpired::class.java)
    }

    @Test
    fun `a phone in the menu - gives one factor per way it can be reached, a text when none is named`() = runTest {
        val noneNamed: NextAction.ChallengePhone = sdkInstance(
            Int::class.java to 7,
            String::class.java to "***0000",
            List::class.java to emptyList<PhoneDeliveryMethod>(),
        )
        every { client.verifyOtp(any(), any()) } returns step(noneNamed, reason = Reason.NONE)

        val status = repository.verifyCode("123456")

        assertThat(status).isEqualTo(
            Result.Success(CodeStatus.AnotherFactor(listOf(MfaFactor.Phone(7, "***0000", PhoneDeliveryMethod.TEXT)))),
        )
    }

    // endregion

    private fun verifyOtp(channel: OtpChannel): NextAction.VerifyOtp =
        sdkInstance(OtpChannel::class.java to channel, String::class.java to SMS.maskedNumber)

    /** The phone [SMS] and [CALL] are made from, which the server offers both ways. */
    private fun challengePhone(): NextAction.ChallengePhone = sdkInstance(
        Int::class.java to SMS.index,
        String::class.java to SMS.maskedNumber,
        List::class.java to listOf(PhoneDeliveryMethod.TEXT, PhoneDeliveryMethod.VOICE),
    )

    private fun verifyOob(pollInMs: Int): NextAction.VerifyOob = sdkInstance(Int::class.java to pollInMs)

    private fun challengePush(index: Int = PHONE.index, name: String = PHONE.deviceName): NextAction.ChallengePush =
        sdkInstance(Int::class.java to index, String::class.java to name)

    private fun Result<*, MfaError>.mfaError(): MfaError {
        assertThat(this).isInstanceOf(Result.Error::class.java)
        return (this as Result.Error).error
    }

    private companion object {
        val PHONE = MfaFactor.Push(2, "Jane's phone")
        val SMS = MfaFactor.Phone(4, "***9239", PhoneDeliveryMethod.TEXT)
        val CALL = SMS.copy(delivery = PhoneDeliveryMethod.VOICE)
        const val POLL_MS = 3_000
        const val SLOWER_MS = 5_000
    }
}
