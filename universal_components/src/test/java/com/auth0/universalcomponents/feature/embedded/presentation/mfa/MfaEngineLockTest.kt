package com.auth0.universalcomponents.feature.embedded.presentation.mfa

import com.auth0.android.embedded.EmbeddedAuthClient
import com.auth0.android.embedded.EmbeddedAuthError
import com.auth0.android.embedded.EmbeddedAuthError.InsufficientAuthorization.Reason
import com.auth0.android.embedded.authorize.NextAction
import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.feature.embedded.data.repository.EmbeddedAuthSteps
import com.auth0.universalcomponents.feature.embedded.data.repository.InFlightRequest
import com.auth0.universalcomponents.feature.embedded.data.repository.MfaRepositoryImpl
import com.auth0.universalcomponents.feature.embedded.data.repository.sdkException
import com.auth0.universalcomponents.feature.embedded.data.repository.sdkInstance
import com.auth0.universalcomponents.feature.embedded.data.repository.step
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * The engine on the real repository and the real one-call-at-a-time lock, with only the SDK client faked: the lock
 * is what keeps a push check and a push to another device apart, so it can't be tested with a faked repository.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MfaEngineLockTest {

    private lateinit var client: EmbeddedAuthClient

    @Before
    fun setup() {
        client = mockk()
        every { client.challengePush(PHONE.index) } returns step(verifyOob())
        every { client.challengePush(TABLET.index) } returns step(verifyOob())
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `switch device while a check is in flight - the new push waits for the check, then goes out`() = runTest {
        val check = InFlightRequest<Credentials>()
        every { client.verifyOob() } returns check
        val engine = MfaEngine(
            factors = listOf(PHONE, TABLET),
            repository = MfaRepositoryImpl(EmbeddedAuthSteps(client)),
            hostScope = backgroundScope,
            onEffect = {},
        )
        engine.start()
        engine.onIntent(MfaIntent.SelectFactor(PHONE))
        runCurrent()
        advanceTimeBy(POLL_MS)
        runCurrent()
        verify(exactly = 1) { client.verifyOob() }

        engine.onIntent(MfaIntent.ChooseAnotherFactor)
        engine.onIntent(MfaIntent.SelectFactor(TABLET))
        runCurrent()

        // The check holds the SDK session, so the second push hasn't touched it yet.
        verify(exactly = 0) { client.challengePush(TABLET.index) }
        assertThat((engine.step.value as MfaStep.PushWaiting).phase).isEqualTo(PushPhase.Sending)

        check.fail(pending())
        runCurrent()

        verifyOrder {
            client.verifyOob()
            client.challengePush(TABLET.index)
        }
        val shown = engine.step.value as MfaStep.PushWaiting
        assertThat(shown.factor).isEqualTo(TABLET)
        assertThat(shown.phase).isInstanceOf(PushPhase.Polling::class.java)
    }

    private fun verifyOob(): NextAction.VerifyOob = sdkInstance(Int::class.java to POLL_MS.toInt())

    private fun pending() = sdkException(
        EmbeddedAuthError.InsufficientAuthorization(listOf(verifyOob()), Reason.AUTHORIZATION_PENDING),
        code = "insufficient_authorization",
    )

    private companion object {
        val PHONE = MfaFactor.Push(0, "Jane's phone")
        val TABLET = MfaFactor.Push(1, "Jane's tablet")
        const val POLL_MS = 3_000L
    }
}
