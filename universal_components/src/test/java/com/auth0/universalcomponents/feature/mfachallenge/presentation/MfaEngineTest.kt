package com.auth0.universalcomponents.feature.mfachallenge.presentation

import com.auth0.android.embedded.authorize.PhoneDeliveryMethod
import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.mfachallenge.MfaError
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.CodeStatus
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
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import kotlin.time.TimeSource

/**
 * The engine runs the reducer's commands with the timing the SDK asks for. It runs in the test's background scope,
 * so a push still pending when a test ends is simply cancelled. Each wait is moved through by hand, so every test
 * can check what happened just before and just after it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MfaEngineTest {

    private lateinit var repository: MfaRepository
    private val steps = mutableListOf<MfaState>()
    private val effects = mutableListOf<MfaEffect>()

    @Before
    fun setup() {
        repository = mockk()
        coEvery { repository.sendPush(any()) } returns Result.Success(PENDING)
        coEvery { repository.checkPushStatus() } returns Result.Success(PENDING)
        coEvery { repository.sendPhoneCode(any()) } returns Result.Success(true)
        coEvery { repository.sendEmailCode(any()) } returns Result.Success(true)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // region checking

    @Test
    fun `one device - the push goes out first, then its screen shows`() = runTest {
        val engine = started(PHONE)

        assertThat(steps.first()).isEqualTo(MfaState.SendingChallenge(ONE, PHONE))
        assertThat(engine.state.value).isEqualTo(waiting(ONE, PHONE))
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
        assertThat((engine.state.value as MfaState.PushWaiting).everyMs).isEqualTo(SLOWER)
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

        assertThat(engine.state.value).isEqualTo(MfaState.Failed(error))
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

        assertThat(engine.state.value).isInstanceOf(MfaState.PushWaiting::class.java)
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
        assertThat(engine.state.value).isEqualTo(MfaState.ChooseFactor(round2))
        coVerify(exactly = 0) { repository.sendPush(TABLET) }

        engine.onIntent(MfaIntent.SelectFactor(TABLET))
        runCurrent()

        assertThat(steps).contains(MfaState.SendingChallenge(round2, TABLET))
        assertThat(engine.state.value).isEqualTo(waiting(round2, TABLET))
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
    fun `resend - the app was frozen meanwhile - the countdown catches up with the time that really passed`() = runTest {
        val time = TestTimeSource()
        val engine = started(PHONE, timeSource = time)

        time += 40.seconds
        waitFor(1_000)

        assertThat(engine.resendSecondsLeft()).isEqualTo(COOLDOWN - 40)
    }

    @Test
    fun `resend - frozen for longer than the wait - opens at zero and stops counting`() = runTest {
        val time = TestTimeSource()
        val engine = started(PHONE, timeSource = time)

        time += (COOLDOWN + 30).seconds
        waitFor(1_000)
        waitFor(5_000)

        assertThat(engine.resendSecondsLeft()).isEqualTo(0)
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

        assertThat(engine.state.value).isEqualTo(MfaState.ChooseFactor(TWO))
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

        assertThat(engine.state.value).isEqualTo(MfaState.ChooseFactor(TWO))
        assertThat(effects).isEmpty()
        coVerify(exactly = 1) { repository.checkPushStatus() }
    }

    @Test
    fun `chose a phone while a check is in flight - the check starts a new round - the code waiting behind it is dropped`() =
        runTest {
            val check = CompletableDeferred<Result<PushStatus, MfaError>>()
            val turnAtSdk = CompletableDeferred<Unit>()
            var codeWentOut = false
            coEvery { repository.checkPushStatus() } coAnswers { check.await() }
            coEvery { repository.sendPhoneCode(any()) } coAnswers {
                turnAtSdk.await()
                codeWentOut = true
                Result.Success(true)
            }
            val engine = started(PHONE, SMS, select = PHONE)
            waitFor(POLL)

            engine.onIntent(MfaIntent.ChooseAnotherFactor)
            engine.onIntent(MfaIntent.SelectFactor(SMS))
            runCurrent()
            check.complete(Result.Success(PushStatus.AnotherFactor(listOf(SMS))))
            runCurrent()
            turnAtSdk.complete(Unit)
            runCurrent()

            assertThat(engine.state.value).isEqualTo(MfaState.ChooseFactor(MfaRound(2, listOf(SMS))))
            assertThat(codeWentOut).isFalse()
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
            val rejected = MfaError.Rejected(null)
            answer.complete(Result.Error(rejected))
            runCurrent()

            assertThat(engine.state.value).isEqualTo(MfaState.Failed(rejected))
            assertThat(effects).containsExactly(MfaEffect.Failed(rejected))
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

            assertThat(engine.state.value).isInstanceOf(MfaState.PushWaiting::class.java)
            assertThat(effects).isEmpty()
            coVerify(exactly = MfaReducer.QUIET_RETRIES + 2) { repository.checkPushStatus() }

            // Back in the app the wait is cut short: the next check runs now, not after the interval.
            engine.onAppForegrounded()
            runCurrent()
            coVerify(exactly = MfaReducer.QUIET_RETRIES + 3) { repository.checkPushStatus() }
        }

    @Test
    fun `back while a resend is going out - is ignored, so the device can't be picked again for a second push`() =
        runTest {
            val resendAnswer = CompletableDeferred<Result<PushStatus.Pending, MfaError>>()
            val engine = started(PHONE, TABLET, select = PHONE)
            waitFor(ONE_SECOND * COOLDOWN)
            coEvery { repository.sendPush(PHONE) } coAnswers { resendAnswer.await() }
            engine.onIntent(MfaIntent.Resend)
            runCurrent()

            engine.back()

            // Still on the push with the resend on its way; the list never showed, so nothing else could be sent.
            assertThat((engine.state.value as MfaState.PushWaiting).phase).isEqualTo(PushPhase.Resending)
            coVerify(exactly = 0) { repository.sendPush(TABLET) }

            resendAnswer.complete(Result.Success(PushStatus.Pending(SLOWER, canResend = true)))
            runCurrent()

            val shown = engine.state.value as MfaState.PushWaiting
            assertThat(shown.phase).isEqualTo(PushPhase.Waiting)
            assertThat(shown.everyMs).isEqualTo(SLOWER)
            coVerify(exactly = 2) { repository.sendPush(PHONE) }
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
        val shown = engine.state.value

        engine.stop()
        engine.onIntent(MfaIntent.SelectFactor(TABLET))
        engine.back()
        runCurrent()

        assertThat(engine.state.value).isEqualTo(shown)
        assertThat(effects).isEmpty()
        coVerify(exactly = 0) { repository.sendPush(any()) }
    }

    // endregion

    // region starting

    @Test
    fun `created but not started - shows its first step and runs nothing`() = runTest {
        val engine = MfaEngine(MfaReducer(listOf(PHONE)), repository, backgroundScope, onEffect = { effects += it })
        waitFor(POLL)

        assertThat(engine.state.value).isEqualTo(MfaState.SendingChallenge(ONE, PHONE))
        coVerify(exactly = 0) { repository.sendPush(any()) }
        assertThat(effects).isEmpty()
    }

    @Test
    fun `nothing usable at start - ends at once on its error screen, with no call to the server`() = runTest {
        val engine = started()

        val failed = engine.state.value as MfaState.Failed
        assertThat(failed.error).isInstanceOf(MfaError.NoEligibleFactors::class.java)
        assertThat(effects).containsExactly(MfaEffect.Failed(failed.error))
        assertThat(engine.isRunning).isFalse()
        coVerify(exactly = 0) { repository.sendPush(any()) }
    }

    @Test
    fun `started twice - the second start is ignored, so one push goes out and progress is kept`() = runTest {
        val engine = started(PHONE)
        waitFor(POLL)
        val shown = engine.state.value

        engine.start()
        runCurrent()

        assertThat(engine.state.value).isEqualTo(shown)
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

    // region phone code

    @Test
    fun `one phone - the code goes out first, then its screen shows with the resend wait running`() = runTest {
        val engine = startedWithCode()

        assertThat(steps.first()).isEqualTo(MfaState.SendingChallenge(BY_SMS, SMS))
        assertThat((engine.state.value as MfaState.CodeEntry).phase).isEqualTo(CodePhase.Ready)
        coVerify(exactly = 1) { repository.sendPhoneCode(SMS) }
        waitFor(ONE_SECOND)
        assertThat((engine.state.value as MfaState.CodeEntry).resendSecondsLeft).isEqualTo(COOLDOWN - 1)
    }

    @Test
    fun `a wrong code - stays on the screen, and the user can type another`() = runTest {
        coEvery { repository.verifyCode("111111") } returns Result.Success(CodeStatus.WrongCode)
        val engine = startedWithCode()

        engine.onIntent(MfaIntent.SubmitCode("111111"))
        runCurrent()

        assertThat((engine.state.value as MfaState.CodeEntry).isWrongCode).isTrue()
        assertThat(effects).isEmpty()
    }

    @Test
    fun `the right code - tells the host the user is signed in`() = runTest {
        val credentials = mockk<Credentials>()
        coEvery { repository.verifyCode("123456") } returns Result.Success(CodeStatus.Verified(credentials))
        val engine = startedWithCode()

        engine.onIntent(MfaIntent.SubmitCode("123456"))
        runCurrent()

        assertThat(effects).containsExactly(MfaEffect.Authenticated(credentials))
    }

    @Test
    fun `an expired code - ends MFA with the error`() = runTest {
        val expired = MfaError.TokenExpired()
        coEvery { repository.verifyCode(any()) } returns Result.Error(expired)
        val engine = startedWithCode()

        engine.onIntent(MfaIntent.SubmitCode("123456"))
        runCurrent()

        assertThat(effects).containsExactly(MfaEffect.Failed(expired))
        assertThat(engine.isRunning).isFalse()
    }

    @Test
    fun `resend - after the wait - sends the code again and starts a new wait`() = runTest {
        val engine = startedWithCode()
        waitFor(COOLDOWN * ONE_SECOND)

        engine.onIntent(MfaIntent.Resend)
        runCurrent()

        coVerify(exactly = 2) { repository.sendPhoneCode(SMS) }
        assertThat((engine.state.value as MfaState.CodeEntry).resendSecondsLeft).isEqualTo(COOLDOWN)
    }

    // endregion

    // region email code

    @Test
    fun `one email - the code goes out first, then its screen shows with the resend wait running`() = runTest {
        val engine = startedWithCode(EMAIL)

        assertThat(steps.first()).isEqualTo(MfaState.SendingChallenge(BY_EMAIL, EMAIL))
        assertThat((engine.state.value as MfaState.CodeEntry).phase).isEqualTo(CodePhase.Ready)
        coVerify(exactly = 1) { repository.sendEmailCode(EMAIL) }
        coVerify(exactly = 0) { repository.sendPhoneCode(any()) }
        waitFor(ONE_SECOND)
        assertThat((engine.state.value as MfaState.CodeEntry).resendSecondsLeft).isEqualTo(COOLDOWN - 1)
    }

    @Test
    fun `an email code - the right one - tells the host the user is signed in`() = runTest {
        val credentials = mockk<Credentials>()
        coEvery { repository.verifyCode("123456") } returns Result.Success(CodeStatus.Verified(credentials))
        val engine = startedWithCode(EMAIL)

        engine.onIntent(MfaIntent.SubmitCode("123456"))
        runCurrent()

        assertThat(effects).containsExactly(MfaEffect.Authenticated(credentials))
    }

    @Test
    fun `an email code - a wrong one - stays on the screen, and the user can type another`() = runTest {
        coEvery { repository.verifyCode("111111") } returns Result.Success(CodeStatus.WrongCode)
        val engine = startedWithCode(EMAIL)

        engine.onIntent(MfaIntent.SubmitCode("111111"))
        runCurrent()

        assertThat((engine.state.value as MfaState.CodeEntry).isWrongCode).isTrue()
        assertThat(effects).isEmpty()
    }

    @Test
    fun `an email code - sending it fails - ends MFA with the error, and checks nothing`() = runTest {
        val error = MfaError.Network()
        coEvery { repository.sendEmailCode(any()) } returns Result.Error(error)

        val engine = startedWithCode(EMAIL)

        assertThat(effects).containsExactly(MfaEffect.Failed(error))
        assertThat(engine.isRunning).isFalse()
        coVerify(exactly = 0) { repository.verifyCode(any()) }
    }

    @Test
    fun `an email code - resend after the wait - sends the code to the same address again`() = runTest {
        val engine = startedWithCode(EMAIL)
        waitFor(COOLDOWN * ONE_SECOND)

        engine.onIntent(MfaIntent.Resend)
        runCurrent()

        coVerify(exactly = 2) { repository.sendEmailCode(EMAIL) }
        coVerify(exactly = 0) { repository.sendPhoneCode(any()) }
        assertThat((engine.state.value as MfaState.CodeEntry).resendSecondsLeft).isEqualTo(COOLDOWN)
    }

    // endregion

    /**
     * Starts MFA, recording every step it shows, and lets the push for [select] go out; with one factor it's picked
     * straight away.
     */
    private fun TestScope.started(
        vararg factors: MfaFactor,
        select: MfaFactor? = null,
        timeSource: TimeSource = TimeSource.Monotonic,
    ): MfaEngine {
        val engine = MfaEngine(
            reducer = MfaReducer(factors.toList()),
            repository = repository,
            hostScope = backgroundScope,
            timeSource = timeSource,
            onEffect = { effects += it },
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { engine.state.collect { steps += it } }
        engine.start()
        if (select != null) engine.onIntent(MfaIntent.SelectFactor(select))
        runCurrent()
        return engine
    }

    /** Starts MFA with a single phone (or [factor]), so its code goes out at once. */
    private fun TestScope.startedWithCode(factor: MfaFactor.Code = SMS): MfaEngine {
        val engine = MfaEngine(
            reducer = MfaReducer(listOf(factor)),
            repository = repository,
            hostScope = backgroundScope,
            onEffect = { effects += it },
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { engine.state.collect { steps += it } }
        engine.start()
        runCurrent()
        return engine
    }

    /** Moves the clock and runs whatever became due, including work due exactly at the end. */
    private fun TestScope.waitFor(millis: Long) {
        advanceTimeBy(millis)
        runCurrent()
    }

    private fun MfaEngine.resendSecondsLeft(): Int = (state.value as MfaState.PushWaiting).resendSecondsLeft

    private fun waiting(round: MfaRound, factor: MfaFactor.Push) = MfaState.PushWaiting(
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
        val SMS = MfaFactor.Phone(3, "***9239", PhoneDeliveryMethod.TEXT)
        val BY_SMS = MfaRound(1, listOf(SMS))
        val EMAIL = MfaFactor.Email(5, "j***@example.com")
        val BY_EMAIL = MfaRound(1, listOf(EMAIL))
        const val POLL = 3_000L
        const val SLOWER = 5_000L
        val PENDING = PushStatus.Pending(POLL, canResend = true)
        const val COOLDOWN = MfaReducer.RESEND_COOLDOWN_SECONDS
        const val ONE_SECOND = 1_000L
    }
}
