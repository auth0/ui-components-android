package com.auth0.universalcomponents.feature.embedded.data.repository

import com.auth0.android.embedded.EmbeddedAuthClient
import com.auth0.android.embedded.EmbeddedAuthError
import com.auth0.android.embedded.EmbeddedAuthError.InsufficientAuthorization.Reason
import com.auth0.android.embedded.authorize.NextAction
import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.data.FakeRequestImpl
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaError
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.embedded.domain.model.PushStatus
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

        assertThat(repository.sendPush(PHONE).mfaError()).isEqualTo(MfaError.Unexpected())
    }

    @Test
    fun `sendPush - signed in on send - fails as unexpected`() = runTest {
        every { client.challengePush(PHONE.index) } returns FakeRequestImpl(response = null)

        assertThat(repository.sendPush(PHONE).mfaError()).isEqualTo(MfaError.Unexpected())
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

        assertThat(repository.checkPushStatus().mfaError()).isEqualTo(MfaError.Unexpected())
    }

    @Test
    fun `checkPushStatus - user denies the push - is an answer, not an error`() = runTest {
        every { client.verifyOob() } returns failingWith(EmbeddedAuthError.AuthorizationRejected, statusCode = 403)

        assertThat(repository.checkPushStatus()).isEqualTo(Result.Success(PushStatus.Rejected))
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

    private fun verifyOob(pollInMs: Int): NextAction.VerifyOob = sdkInstance(Int::class.java to pollInMs)

    private fun challengePush(index: Int = PHONE.index, name: String = PHONE.deviceName): NextAction.ChallengePush =
        sdkInstance(Int::class.java to index, String::class.java to name)

    private fun Result<*, MfaError>.mfaError(): MfaError {
        assertThat(this).isInstanceOf(Result.Error::class.java)
        return (this as Result.Error).error
    }

    private companion object {
        val PHONE = MfaFactor.Push(2, "Jane's phone")
        const val POLL_MS = 3_000
        const val SLOWER_MS = 5_000
    }
}
