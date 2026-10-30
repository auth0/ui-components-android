package com.auth0.universalcomponents.feature.embedded.presentation.mfa

import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.embedded.domain.model.PushStatus
import com.auth0.universalcomponents.feature.embedded.domain.repository.MfaRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * The engine runs the reducer's commands with the timing the SDK asks for. It runs in the test's background scope,
 * so a push still pending when a test ends is simply cancelled. Each wait is moved through by hand, so every test
 * can check what happened just before and just after it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MfaEngineTest {

    private lateinit var repository: MfaRepository
    private val steps = mutableListOf<MfaStep>()
    private val effects = mutableListOf<MfaEffect>()

    @Before
    fun setup() {
        repository = mockk()
        coEvery { repository.sendPush(any()) } returns Result.Success(POLL)
        coEvery { repository.checkPush() } returns Result.Success(PushStatus.Pending(null))
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // region checking

    @Test
    fun `one device - the first screen is the push, sent straight away, with no Start needed`() = runTest {
        val engine = started(PHONE)

        assertThat(steps.first()).isEqualTo(MfaStep.PushWaiting(PHONE, PushPhase.Sending, canChooseAnother = false))
        assertThat(engine.step).isEqualTo(polling(PHONE, POLL, COOLDOWN))
        coVerify(exactly = 1) { repository.sendPush(PHONE) }
    }

    @Test
    fun `push sent - first check waits the server's interval`() = runTest {
        started(PHONE)

        waitFor(POLL - 1)
        coVerify(exactly = 0) { repository.checkPush() }
        waitFor(1)
        coVerify(exactly = 1) { repository.checkPush() }
    }

    @Test
    fun `still pending - next check waits the new interval the server gave`() = runTest {
        coEvery { repository.checkPush() } returns Result.Success(PushStatus.Pending(SLOWER))
        val engine = started(PHONE)

        waitFor(POLL)
        val secondsWaited = (POLL / ONE_SECOND).toInt()
        assertThat(engine.step).isEqualTo(polling(PHONE, SLOWER, COOLDOWN - secondsWaited))
        waitFor(SLOWER - 1)
        coVerify(exactly = 1) { repository.checkPush() }
        waitFor(1)
        coVerify(exactly = 2) { repository.checkPush() }
    }

    @Test
    fun `approved - tells the host, and nothing more reaches it`() = runTest {
        val credentials = mockk<Credentials>()
        coEvery { repository.checkPush() } returns Result.Success(PushStatus.Approved(credentials))
        started(PHONE)

        waitFor(POLL)
        val stepsSoFar = steps.size
        waitFor(POLL * 2)

        assertThat(effects).containsExactly(MfaEffect.Authenticated(credentials))
        assertThat(steps).hasSize(stepsSoFar)
        coVerify(exactly = 1) { repository.checkPush() }
    }

    @Test
    fun `sending fails - shows the error and checks nothing`() = runTest {
        coEvery { repository.sendPush(PHONE) } returns Result.Error(LoginFailure(EmbeddedLoginError.Network))
        val engine = started(PHONE)

        waitFor(POLL * 2)

        assertThat(engine.step).isEqualTo(MfaStep.Failed(EmbeddedLoginError.Network))
        coVerify(exactly = 0) { repository.checkPush() }
    }

    // endregion

    // region resend

    @Test
    fun `resend - counts down a second at a time, then sends the push to the same device again`() = runTest {
        val engine = started(PHONE)

        waitFor(ONE_SECOND)
        assertThat(engine.resendSecondsLeft()).isEqualTo(COOLDOWN - 1)
        engine.onIntent(MfaIntent.Resend)
        runCurrent()
        coVerify(exactly = 1) { repository.sendPush(PHONE) }

        waitFor(ONE_SECOND * (COOLDOWN - 1))
        assertThat(engine.resendSecondsLeft()).isEqualTo(0)
        engine.onIntent(MfaIntent.Resend)
        runCurrent()

        coVerify(exactly = 2) { repository.sendPush(PHONE) }
        assertThat(engine.resendSecondsLeft()).isEqualTo(COOLDOWN)
    }

    // endregion

    // region leaving

    @Test
    fun `chose another device during the wait - the check never runs`() = runTest {
        val engine = started(PHONE, TABLET, select = PHONE)

        engine.onIntent(MfaIntent.ChooseAnotherFactor)
        waitFor(POLL * 2)

        assertThat(engine.step).isEqualTo(MfaStep.AuthenticatorList(listOf(PHONE, TABLET)))
        coVerify(exactly = 0) { repository.checkPush() }
    }

    // That the next SDK step waits for a check in flight is EmbeddedAuthStepsTest's job.
    @Test
    fun `chose another device while a check is in flight - an approval it brings still signs in`() = runTest {
        val credentials = mockk<Credentials>()
        val answer = CompletableDeferred<Result<PushStatus, LoginFailure>>()
        coEvery { repository.checkPush() } coAnswers { answer.await() }
        val engine = started(PHONE, TABLET, select = PHONE)
        waitFor(POLL)

        engine.onIntent(MfaIntent.ChooseAnotherFactor)
        runCurrent()
        answer.complete(Result.Success(PushStatus.Approved(credentials)))
        runCurrent()

        assertThat(effects).containsExactly(MfaEffect.Authenticated(credentials))
    }

    @Test
    fun `chose another device while a check is in flight - a pending answer it brings changes nothing`() = runTest {
        val answer = CompletableDeferred<Result<PushStatus, LoginFailure>>()
        coEvery { repository.checkPush() } coAnswers { answer.await() }
        val engine = started(PHONE, TABLET, select = PHONE)
        waitFor(POLL)

        engine.onIntent(MfaIntent.ChooseAnotherFactor)
        runCurrent()
        answer.complete(Result.Success(PushStatus.Pending(SLOWER)))
        waitFor(SLOWER * 2)

        assertThat(engine.step).isEqualTo(MfaStep.AuthenticatorList(listOf(PHONE, TABLET)))
        assertThat(effects).isEmpty()
        coVerify(exactly = 1) { repository.checkPush() }
    }

    @Test
    fun `a check fails on the network - checks again quietly, with no second push`() = runTest {
        val network = Result.Error(LoginFailure(EmbeddedLoginError.Network, canRetryStep = true))
        coEvery { repository.checkPush() } returnsMany listOf(network, Result.Success(PushStatus.Pending(null)))
        val engine = started(PHONE)

        waitFor(POLL)
        waitFor(POLL * 2)

        assertThat(engine.step).isInstanceOf(MfaStep.PushWaiting::class.java)
        coVerify(exactly = 2) { repository.checkPush() }
        coVerify(exactly = 1) { repository.sendPush(PHONE) }
    }

    @Test
    fun `stopped by the host - nothing it started reaches the host any more`() = runTest {
        val engine = started(PHONE)
        val stepsSoFar = steps.size

        engine.stop()
        waitFor(POLL * 2)
        engine.onIntent(MfaIntent.Back)

        assertThat(steps).hasSize(stepsSoFar)
        assertThat(effects).isEmpty()
        coVerify(exactly = 0) { repository.checkPush() }
    }

    // endregion

    /** Starts MFA and lets the push for [select] go out; with one factor it's picked straight away. */
    private fun TestScope.started(vararg factors: MfaFactor.Push, select: MfaFactor.Push? = null): MfaEngine {
        val engine = MfaEngine(
            factors = factors.toList(),
            repository = repository,
            hostScope = backgroundScope,
            onStep = { steps += it },
            onEffect = { effects += it },
        )
        if (select != null) engine.onIntent(MfaIntent.SelectFactor(select))
        runCurrent()
        return engine
    }

    /** Moves the clock and runs whatever became due, including work due exactly at the end. */
    private fun TestScope.waitFor(millis: Long) {
        advanceTimeBy(millis)
        runCurrent()
    }

    private fun MfaEngine.resendSecondsLeft(): Int =
        ((step as MfaStep.PushWaiting).phase as PushPhase.Polling).resendSecondsLeft

    private fun polling(factor: MfaFactor.Push, everyMs: Long, resendSecondsLeft: Int) =
        MfaStep.PushWaiting(factor, PushPhase.Polling(everyMs, resendSecondsLeft), canChooseAnother = false)

    private companion object {
        val PHONE = MfaFactor.Push(0, "Jane's phone")
        val TABLET = MfaFactor.Push(1, "Jane's tablet")
        const val POLL = 3_000L
        const val SLOWER = 5_000L
        const val COOLDOWN = MfaReducer.RESEND_COOLDOWN_SECONDS
        const val ONE_SECOND = 1_000L
    }
}
