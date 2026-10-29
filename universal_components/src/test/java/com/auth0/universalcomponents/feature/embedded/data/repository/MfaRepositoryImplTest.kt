package com.auth0.universalcomponents.feature.embedded.data.repository

import com.auth0.android.embedded.EmbeddedAuthClient
import com.auth0.android.embedded.EmbeddedAuthError
import com.auth0.android.embedded.EmbeddedAuthError.InsufficientAuthorization.Reason
import com.auth0.android.embedded.authorize.NextAction
import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.data.FakeRequestImpl
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
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
    fun `sendPush - sent - pushes to that device and returns the server's wait`() = runTest {
        every { client.challengePush(PHONE.index) } returns step(verifyOob(POLL_MS))

        assertThat(repository.sendPush(PHONE)).isEqualTo(Result.Success(POLL_MS.toLong()))

        verify(exactly = 1) { client.challengePush(PHONE.index) }
    }

    @Test
    fun `sendPush - no wait to check it with - fails as unknown`() = runTest {
        every { client.challengePush(PHONE.index) } returns step(NextAction.VerifyRecoveryCode)

        val failure = repository.sendPush(PHONE).failure()

        assertThat(failure.error).isInstanceOf(EmbeddedLoginError.Unknown::class.java)
    }

    @Test
    fun `sendPush - signed in on send - fails as unknown`() = runTest {
        every { client.challengePush(PHONE.index) } returns FakeRequestImpl(response = null)

        val failure = repository.sendPush(PHONE).failure()

        assertThat(failure.error).isInstanceOf(EmbeddedLoginError.Unknown::class.java)
    }

    @Test
    fun `sendPush - network error - fails with network, and the session survives`() = runTest {
        every { client.challengePush(PHONE.index) } returns failingWith(EmbeddedAuthError.Network)

        val failure = repository.sendPush(PHONE).failure()

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.Network)
        assertThat(failure.canRetryStep).isTrue()
    }

    // endregion

    // region checkPush

    @Test
    fun `checkPush - approved - returns the credentials`() = runTest {
        val credentials = mockk<Credentials>()
        every { client.verifyOob() } returns FakeRequestImpl(response = credentials)

        assertThat(repository.checkPush()).isEqualTo(Result.Success(PushStatus.Approved(credentials)))
    }

    @Test
    fun `checkPush - pending or slow down - still waiting, with the server's new wait`() = runTest {
        listOf(Reason.AUTHORIZATION_PENDING, Reason.SLOW_DOWN).forEach { reason ->
            every { client.verifyOob() } returns step(verifyOob(SLOWER_MS), reason = reason)

            assertThat(repository.checkPush())
                .isEqualTo(Result.Success(PushStatus.Pending(SLOWER_MS.toLong())))
        }
    }

    @Test
    fun `checkPush - pending without a wait - still waiting, keeping the last wait`() = runTest {
        every { client.verifyOob() } returns step(reason = Reason.AUTHORIZATION_PENDING)

        assertThat(repository.checkPush()).isEqualTo(Result.Success(PushStatus.Pending(null)))
    }

    @Test
    fun `checkPush - server asks for another step - fails for good as unsupported`() = runTest {
        every { client.verifyOob() } returns step(NextAction.VerifyRecoveryCode)

        val failure = repository.checkPush().failure()

        assertThat(failure.error).isEqualTo(EmbeddedLoginError.ExtraStepUnsupported)
        assertThat(failure.canRetryStep).isFalse()
    }

    @Test
    fun `checkPush - declined or expired - named in our terms`() = runTest {
        mapOf(
            EmbeddedAuthError.AuthorizationRejected to EmbeddedLoginError.PushDenied,
            EmbeddedAuthError.ChallengeExpired to EmbeddedLoginError.CodeExpired,
            EmbeddedAuthError.NoEligibleFactors to EmbeddedLoginError.NoEligibleFactors,
        ).forEach { (sdkError, expected) ->
            every { client.verifyOob() } returns failingWith(sdkError, statusCode = 403)

            val failure = repository.checkPush().failure()

            assertThat(failure.error).isEqualTo(expected)
            assertThat(failure.canRetryStep).isFalse()
        }
    }

    // endregion

    private fun verifyOob(pollInMs: Int): NextAction.VerifyOob = sdkInstance(Int::class.java to pollInMs)

    private companion object {
        val PHONE = MfaFactor.Push(2, "Jane's phone")
        const val POLL_MS = 3_000
        const val SLOWER_MS = 5_000
    }
}
