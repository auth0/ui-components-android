package com.auth0.universalcomponents.feature.mfachallenge.presentation

import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaError
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.PushStatus
import com.auth0.universalcomponents.feature.mfachallenge.domain.repository.MfaRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
        coEvery { repository.sendPush(any()) } returns Result.Success(PENDING)
        coEvery { repository.checkPushStatus() } returns Result.Success(PENDING)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // region checking

    @Test
    fun `one device - the push goes out first, then its screen shows`() = runTest {
        val engine = started(PHONE)

        assertThat(steps.first()).isEqualTo(MfaStep.SendingChallenge(ONE, PHONE))
        assertThat(engine.step.value).isEqualTo(waiting(ONE, PHONE))
        coVerify(exactly = 1) { repository.sendPush(PHONE) }
    }

    @Test
    fun `push sent - first check waits the server's interval`() = runTest {
        started(PHONE)

        waitFor(POLL - 1)
        coVerify(exactly = 0) { repository.checkPushStatus() }
        waitFor(1)
        coVerify(exactly = 1) { repository.checkPushStatus() }
    }

    @Test
    fun `still pending - next check waits the new interval the server gave`() = runTest {
        coEvery { repository.checkPushStatus() } returns Result.Success(PushStatus.Pending(SLOWER, canResend = true))
        val engine = started(PHONE)

        waitFor(POLL)
        assertThat((engine.step.value as MfaStep.PushWaiting).everyMs).isEqualTo(SLOWER)
        waitFor(SLOWER - 1)
        coVerify(exactly = 1) { repository.checkPushStatus() }
        waitFor(1)
        coVerify(exactly = 2) { repository.checkPushStatus() }
    }

    @Test
    fun `approved - tells the host, and nothing more reaches it`() = runTest {
        val credentials = mockk<Credentials>()
        coEvery { repository.checkPushStatus() } returns Result.Success(PushStatus.Approved(credentials))
        started(PHONE)

        waitFor(POLL)
        val stepsSoFar = steps.size
        waitFor(POLL * 2)

        assertThat(effects).containsExactly(MfaEffect.Authenticated(credentials))
        assertThat(steps).hasSize(stepsSoFar)
        coVerify(exactly = 1) { repository.checkPushStatus() }
    }

    @Test
    fun `sending fails - ends MFA on its error screen, and checks nothing`() = runTest {
        val error = MfaError.Network(null)
        coEvery { repository.sendPush(PHONE) } returns Result.Error(error)
        val engine = started(PHONE)

        waitFor(POLL * 2)

        assertThat(engine.step.value).isEqualTo(MfaStep.Failed(error))
        assertThat(effects).containsExactly(MfaEffect.Failed(error))
        assertThat(engine.isRunning).isFalse()
        coVerify(exactly = 0) { repository.checkPushStatus() }
    }

    @Test
    fun `a check fails on the network - checks again quietly, with no second push`() = runTest {
        val network = Result.Error(MfaError.Network(null))
        coEvery { repository.checkPushStatus() } returnsMany listOf(network, Result.Success(PENDING))
        val engine = started(PHONE)

        waitFor(POLL)
        waitFor(POLL)

        assertThat(engine.step.value).isInstanceOf(MfaStep.PushWaiting::class.java)
        coVerify(exactly = 2) { repository.checkPushStatus() }
        coVerify(exactly = 1) { repository.sendPush(PHONE) }
    }

    @Test
    fun `another factor - starts the next round with the server's menu`() = runTest {
        coEvery { repository.checkPushStatus() } returns Result.Success(PushStatus.AnotherFactor(listOf(TABLET)))
        val engine = started(PHONE)

        waitFor(POLL)

        // One factor in round 2: the list still comes back, headed "One more step"; its push goes out once picked.
        val round2 = MfaRound(2, listOf(TABLET))
        assertThat(engine.step.value).isEqualTo(MfaStep.ChooseFactor(round2))
        coVerify(exactly = 0) { repository.sendPush(TABLET) }

        engine.onIntent(MfaIntent.SelectFactor(TABLET))
        runCurrent()

        assertThat(steps).contains(MfaStep.SendingChallenge(round2, TABLET))
        assertThat(engine.step.value).isEqualTo(waiting(round2, TABLET))
        coVerify(exactly = 1) { repository.sendPush(TABLET) }
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

    @Test
    fun `resend - not offered by the server - never sends`() = runTest {
        coEvery { repository.sendPush(any()) } returns Result.Success(PushStatus.Pending(POLL, canResend = false))
        coEvery { repository.checkPushStatus() } returns Result.Success(PushStatus.Pending(POLL, canResend = false))
        val engine = started(PHONE)

        waitFor(ONE_SECOND * COOLDOWN)
        engine.onIntent(MfaIntent.Resend)
        runCurrent()

        coVerify(exactly = 1) { repository.sendPush(PHONE) }
    }

    // endregion

    // region leaving

    @Test
    fun `chose another device during the wait - the check never runs`() = runTest {
        val engine = started(PHONE, TABLET, select = PHONE)

        engine.onIntent(MfaIntent.ChooseAnotherFactor)
        waitFor(POLL * 2)

        assertThat(engine.step.value).isEqualTo(MfaStep.ChooseFactor(TWO))
        coVerify(exactly = 0) { repository.checkPushStatus() }
    }

    // That the next SDK step waits for a check in flight is MfaEngineLockTest's and EmbeddedAuthStepsTest's job.
    @Test
    fun `chose another device while a check is in flight - an approval it brings still signs in`() = runTest {
        val credentials = mockk<Credentials>()
        val answer = CompletableDeferred<Result<PushStatus, MfaError>>()
        coEvery { repository.checkPushStatus() } coAnswers { answer.await() }
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
        val answer = CompletableDeferred<Result<PushStatus, MfaError>>()
        coEvery { repository.checkPushStatus() } coAnswers { answer.await() }
        val engine = started(PHONE, TABLET, select = PHONE)
        waitFor(POLL)

        engine.onIntent(MfaIntent.ChooseAnotherFactor)
        runCurrent()
        answer.complete(Result.Success(PushStatus.Pending(SLOWER, canResend = true)))
        waitFor(SLOWER * 2)

        assertThat(engine.step.value).isEqualTo(MfaStep.ChooseFactor(TWO))
        assertThat(effects).isEmpty()
        coVerify(exactly = 1) { repository.checkPushStatus() }
    }

    @Test
    fun `chose another device while a check is in flight - a denial it brings still ends MFA with its real reason`() =
        runTest {
            val answer = CompletableDeferred<Result<PushStatus, MfaError>>()
            coEvery { repository.checkPushStatus() } coAnswers { answer.await() }
            val engine = started(PHONE, TABLET, select = PHONE)
            waitFor(POLL)

            engine.onIntent(MfaIntent.ChooseAnotherFactor)
            runCurrent()
            answer.complete(Result.Success(PushStatus.Rejected))
            runCurrent()

            assertThat(engine.step.value).isEqualTo(MfaStep.Failed(MfaError.Rejected(null)))
            assertThat(effects).containsExactly(MfaEffect.Failed(MfaError.Rejected(null)))
        }

    @Test
    fun `network blips while the app is in the background - never end the push, and on return it is checked at once`() =
        runTest {
            val network = Result.Error(MfaError.Network(null))
            coEvery { repository.checkPushStatus() } returnsMany List(MfaReducer.QUIET_RETRIES + 2) { network } +
                Result.Success(PENDING)
            val engine = started(PHONE)
            engine.onAppBackgrounded()

            repeat(MfaReducer.QUIET_RETRIES + 2) { waitFor(POLL) }

            assertThat(engine.step.value).isInstanceOf(MfaStep.PushWaiting::class.java)
            assertThat(effects).isEmpty()
            coVerify(exactly = MfaReducer.QUIET_RETRIES + 2) { repository.checkPushStatus() }

            // Back in the app the wait is cut short: the next check runs now, not after the interval.
            engine.onAppForegrounded()
            runCurrent()
            coVerify(exactly = MfaReducer.QUIET_RETRIES + 3) { repository.checkPushStatus() }
        }

    @Test
    fun `left the push while a resend was going out, then picked a device - the old send never answers for the new one`() =
        runTest {
            val oldSend = CompletableDeferred<Result<PushStatus.Pending, MfaError>>()
            val engine = started(PHONE, TABLET, select = PHONE)
            waitFor(ONE_SECOND * COOLDOWN)
            coEvery { repository.sendPush(PHONE) } coAnswers { oldSend.await() }
            engine.onIntent(MfaIntent.Resend)
            runCurrent()

            // Back leaves the push for the list; the user then picks the other device.
            engine.back()
            val newSend = CompletableDeferred<Result<PushStatus.Pending, MfaError>>()
            coEvery { repository.sendPush(TABLET) } coAnswers { newSend.await() }
            engine.onIntent(MfaIntent.SelectFactor(TABLET))
            runCurrent()

            // The old push answers while the new one is still going out: it must not stand in for it.
            oldSend.complete(Result.Success(PushStatus.Pending(POLL, canResend = false)))
            runCurrent()
            assertThat(engine.step.value).isEqualTo(MfaStep.SendingChallenge(TWO, TABLET))

            newSend.complete(Result.Success(PushStatus.Pending(SLOWER, canResend = true)))
            runCurrent()

            val shown = engine.step.value as MfaStep.PushWaiting
            assertThat(shown.factor).isEqualTo(TABLET)
            assertThat(shown.everyMs).isEqualTo(SLOWER)
            assertThat(shown.canResend).isTrue()
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
        assertThat(engine.isRunning).isFalse()
        coVerify(exactly = 0) { repository.checkPushStatus() }
    }

    @Test
    fun `taps after the host stopped it - are ignored, and the last step stays`() = runTest {
        val engine = started(PHONE, TABLET)
        val shown = engine.step.value

        engine.stop()
        engine.onIntent(MfaIntent.SelectFactor(TABLET))
        engine.back()
        runCurrent()

        assertThat(engine.step.value).isEqualTo(shown)
        assertThat(effects).isEmpty()
        coVerify(exactly = 0) { repository.sendPush(any()) }
    }

    // endregion

    // region starting

    @Test
    fun `created but not started - shows its first step and runs nothing`() = runTest {
        val engine = MfaEngine(MfaReducer(listOf(PHONE)), repository, backgroundScope, onEffect = { effects += it })
        waitFor(POLL)

        assertThat(engine.step.value).isEqualTo(MfaStep.SendingChallenge(ONE, PHONE))
        coVerify(exactly = 0) { repository.sendPush(any()) }
        assertThat(effects).isEmpty()
    }

    @Test
    fun `nothing usable at start - ends at once on its error screen, with no call to the server`() = runTest {
        val engine = started()

        assertThat(engine.step.value).isEqualTo(MfaStep.Failed(MfaError.NoEligibleFactors()))
        assertThat(effects).containsExactly(MfaEffect.Failed(MfaError.NoEligibleFactors()))
        assertThat(engine.isRunning).isFalse()
        coVerify(exactly = 0) { repository.sendPush(any()) }
    }

    @Test
    fun `started twice - the second start is ignored, so one push goes out and progress is kept`() = runTest {
        val engine = started(PHONE)
        waitFor(POLL)
        val shown = engine.step.value

        engine.start()
        runCurrent()

        assertThat(engine.step.value).isEqualTo(shown)
        coVerify(exactly = 1) { repository.sendPush(PHONE) }
    }

    @Test
    fun `back - on the list - ends MFA so sign-in starts over`() = runTest {
        val engine = started(PHONE, TABLET)

        engine.back()

        assertThat(effects).containsExactly(MfaEffect.Restart)
        assertThat(engine.isRunning).isFalse()
    }

    // endregion

    /**
     * Starts MFA, recording every step it shows, and lets the push for [select] go out; with one factor it's picked
     * straight away.
     */
    private fun TestScope.started(vararg factors: MfaFactor.Push, select: MfaFactor.Push? = null): MfaEngine {
        val engine = MfaEngine(
            reducer = MfaReducer(factors.toList()),
            repository = repository,
            hostScope = backgroundScope,
            onEffect = { effects += it },
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { engine.step.collect { steps += it } }
        engine.start()
        if (select != null) engine.onIntent(MfaIntent.SelectFactor(select))
        runCurrent()
        return engine
    }

    /** Moves the clock and runs whatever became due, including work due exactly at the end. */
    private fun TestScope.waitFor(millis: Long) {
        advanceTimeBy(millis)
        runCurrent()
    }

    private fun MfaEngine.resendSecondsLeft(): Int = (step.value as MfaStep.PushWaiting).resendSecondsLeft

    private fun waiting(round: MfaRound, factor: MfaFactor.Push) = MfaStep.PushWaiting(
        round = round,
        factor = factor,
        phase = PushPhase.Waiting,
        everyMs = POLL,
        canResend = true,
        resendSecondsLeft = COOLDOWN,
    )

    private companion object {
        val PHONE = MfaFactor.Push(0, "Jane's phone")
        val TABLET = MfaFactor.Push(1, "Jane's tablet")
        val ONE = MfaRound(1, listOf(PHONE))
        val TWO = MfaRound(1, listOf(PHONE, TABLET))
        const val POLL = 3_000L
        const val SLOWER = 5_000L
        val PENDING = PushStatus.Pending(POLL, canResend = true)
        const val COOLDOWN = MfaReducer.RESEND_COOLDOWN_SECONDS
        const val ONE_SECOND = 1_000L
    }
}
