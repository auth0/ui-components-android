package com.auth0.universalcomponents.feature.embedded.data.repository

import com.auth0.android.embedded.EmbeddedAuthClient
import com.auth0.android.embedded.EmbeddedAuthError
import com.auth0.android.embedded.EmbeddedAuthError.InsufficientAuthorization.Reason
import com.auth0.android.embedded.authorize.IdentifierType
import com.auth0.android.embedded.authorize.NextAction
import com.auth0.android.result.Credentials
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * The SDK rule both flows share: one step at a time on the client, and a step that has started always finishes,
 * because every answer rewrites the session the client keeps.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EmbeddedAuthStepsTest {

    private lateinit var client: EmbeddedAuthClient
    private lateinit var steps: EmbeddedAuthSteps

    @Before
    fun setup() {
        client = mockk()
        steps = EmbeddedAuthSteps(client)
        every { client.authorize(CONNECTION, any(), any(), any()) } returns step(NextAction.IdentifyEmail)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `a new sign-in while a push check is in flight - waits until the check has answered`() = runTest {
        val check = InFlightRequest<Credentials>()
        every { client.verifyOob() } returns check
        val mfa = MfaRepositoryImpl(steps)
        val login = EmbeddedLoginRepositoryImpl(steps, CONNECTION)

        var authorized = 0
        every { client.authorize(CONNECTION, any(), any(), any()) } answers {
            authorized++
            step(NextAction.IdentifyEmail)
        }

        launch { mfa.checkPush() }
        runCurrent()
        // The user went back to email entry and tapped Continue straight away.
        launch { login.discover() }
        runCurrent()
        val authorizedDuringCheck = authorized
        // Answer before asserting: a step in flight can't be cancelled, so a failed assert would leave it hanging.
        check.fail(sdkException(stillPending(), code = "insufficient_authorization"))
        runCurrent()

        assertThat(authorizedDuringCheck).isEqualTo(0)
        assertThat(authorized).isEqualTo(1)
    }

    @Test
    fun `the caller left - its step still finishes, but the answer is dropped`() = runTest {
        val check = InFlightRequest<Credentials>()
        every { client.verifyOob() } returns check
        var answered = false

        val caller = launch {
            steps.run { verifyOob() }
            answered = true
        }
        runCurrent()
        caller.cancel()
        runCurrent()
        check.answer(mockk())
        runCurrent()

        assertThat(check.finished).isTrue()
        assertThat(answered).isFalse()
    }

    @Test
    fun `left while waiting its turn - its step never starts`() = runTest {
        val check = InFlightRequest<Credentials>()
        every { client.verifyOob() } returns check
        every { client.identify(any(), any()) } returns step(NextAction.IdentifyEmail)

        launch { steps.run { verifyOob() } }
        runCurrent()
        val waiting = launch { steps.run { identify(EMAIL, IdentifierType.EMAIL) } }
        runCurrent()
        waiting.cancel()
        check.answer(mockk())
        runCurrent()

        verify(exactly = 0) { client.identify(any(), any()) }
    }

    private fun stillPending(): EmbeddedAuthError =
        EmbeddedAuthError.InsufficientAuthorization(
            listOf(sdkInstance<NextAction.VerifyOob>(Int::class.java to POLL_MS)),
            Reason.AUTHORIZATION_PENDING,
        )

    private companion object {
        const val CONNECTION = "email"
        const val EMAIL = "jane@example.com"
        const val POLL_MS = 3_000
    }
}
