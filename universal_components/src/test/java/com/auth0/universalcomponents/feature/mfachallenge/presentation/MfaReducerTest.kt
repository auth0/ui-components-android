package com.auth0.universalcomponents.feature.mfachallenge.presentation

import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.feature.mfachallenge.MfaError
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.PushStatus
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.junit.Test

class MfaReducerTest {

    private val oneDevice = MfaReducer(listOf(PHONE))
    private val twoDevices = MfaReducer(listOf(PHONE, TABLET))

    // region start

    @Test
    fun `start - one device - sends its push first, with no screen of its own until it's out`() {
        val update = oneDevice.start()

        assertThat(update.step).isEqualTo(MfaState.SendingChallenge(ONE, PHONE))
        assertThat(update.commands).containsExactly(MfaCommand.SendPush(PHONE))
    }

    @Test
    fun `start - several devices - shows the list`() {
        val update = twoDevices.start()

        assertThat(update.step).isEqualTo(MfaState.ChooseFactor(TWO))
        assertThat(update.commands).isEmpty()
    }

    @Test
    fun `start - nothing usable - ends MFA with no usable factor`() {
        val update = MfaReducer(emptyList()).start()

        assertEndedWithNoEligibleFactors(update)
        assertThat(update.commands).isEmpty()
    }

    // endregion

    // region choosing and sending

    @Test
    fun `select - a device from the list - sends its push, with the list kept`() {
        val update = twoDevices.handleIntent(MfaState.ChooseFactor(TWO), MfaIntent.SelectFactor(TABLET))

        assertThat(update.step).isEqualTo(MfaState.SendingChallenge(TWO, TABLET))
        assertThat(update.commands).containsExactly(MfaCommand.SendPush(TABLET))
    }

    @Test
    fun `select - while a push is going out, or a device not offered - does nothing`() {
        val sending = MfaState.SendingChallenge(TWO, TABLET)
        val stranger = MfaFactor.Push(9, "Someone else's phone")

        assertThat(twoDevices.handleIntent(sending, MfaIntent.SelectFactor(PHONE))).isEqualTo(MfaUpdate(sending))
        assertThat(twoDevices.handleIntent(MfaState.ChooseFactor(TWO), MfaIntent.SelectFactor(stranger)))
            .isEqualTo(MfaUpdate(MfaState.ChooseFactor(TWO)))
    }

    @Test
    fun `sent - shows the push screen, then waits the server's interval and the resend wait`() {
        val update = oneDevice.handleOutcome(MfaState.SendingChallenge(ONE, PHONE), sent(POLL))

        assertThat(update.step).isEqualTo(waiting(ONE, PHONE))
        assertThat(update.commands)
            .containsExactly(MfaCommand.SchedulePushCheck(POLL), MfaCommand.CountDownResend)
            .inOrder()
    }

    @Test
    fun `sent - the server's interval - is kept between 1 and 60 seconds`() {
        val sending = MfaState.SendingChallenge(ONE, PHONE)

        val tooFast = oneDevice.handleOutcome(sending, sent(0)).step as MfaState.PushWaiting
        val tooSlow = oneDevice.handleOutcome(sending, sent(120_000)).step as MfaState.PushWaiting

        assertThat(tooFast.everyMs).isEqualTo(1_000)
        assertThat(tooSlow.everyMs).isEqualTo(60_000)
    }

    @Test
    fun `sent - after the user left that screen - is dropped`() {
        val list = MfaState.ChooseFactor(TWO)

        assertThat(twoDevices.handleOutcome(list, sent(POLL))).isEqualTo(MfaUpdate(list))
    }

    @Test
    fun `send fails - even on the network - ends MFA`() {
        val error = MfaError.Network(null)

        val update = oneDevice.handleOutcome(MfaState.SendingChallenge(ONE, PHONE), MfaOutcome.SendFailed(error))

        assertThat(update.step).isEqualTo(MfaState.Failed(error))
        assertThat(update.effect).isEqualTo(MfaEffect.Failed(error))
    }

    @Test
    fun `send fails - after the user left that screen - is dropped`() {
        val list = MfaState.ChooseFactor(TWO)

        assertThat(twoDevices.handleOutcome(list, MfaOutcome.SendFailed(MfaError.Network(null))))
            .isEqualTo(MfaUpdate(list))
    }

    // endregion

    // region checking

    @Test
    fun `check due - while waiting - checks once`() {
        val update = oneDevice.handlePushCheckDue(waiting(ONE, PHONE))

        assertThat(update.step).isEqualTo(waiting(ONE, PHONE).copy(phase = PushPhase.Checking))
        assertThat(update.commands).containsExactly(MfaCommand.CheckPush)
    }

    @Test
    fun `check due - with a check or a resend on its way, or off the push screen - does nothing`() {
        listOf(
            waiting(ONE, PHONE).copy(phase = PushPhase.Checking),
            waiting(ONE, PHONE).copy(phase = PushPhase.Resending),
            MfaState.ChooseFactor(TWO),
        ).forEach { step -> assertThat(twoDevices.handlePushCheckDue(step)).isEqualTo(MfaUpdate(step)) }
    }

    @Test
    fun `pending - waits the server's new interval and says again whether resend is allowed`() {
        val checking = checking(ONE, PHONE).copy(failedChecks = 2)

        val update = oneDevice.handleOutcome(checking, pending(SLOWER, canResend = false))

        assertThat(update.step).isEqualTo(
            waiting(ONE, PHONE).copy(everyMs = SLOWER, canResend = false, failedChecks = 0),
        )
        assertThat(update.commands).containsExactly(MfaCommand.SchedulePushCheck(SLOWER))
    }

    @Test
    fun `pending - with no interval - keeps the last one`() {
        val update = oneDevice.handleOutcome(checking(ONE, PHONE), pending(null))

        assertThat((update.step as MfaState.PushWaiting).everyMs).isEqualTo(POLL)
    }

    @Test
    fun `slow down - waits 2 seconds longer, or the server's wait if that's longer`() {
        val shorter = oneDevice.handleOutcome(checking(ONE, PHONE), pending(1_000, isSlowDown = true))
        val longer = oneDevice.handleOutcome(checking(ONE, PHONE), pending(9_000, isSlowDown = true))

        assertThat((shorter.step as MfaState.PushWaiting).everyMs).isEqualTo(POLL + 2_000)
        assertThat((longer.step as MfaState.PushWaiting).everyMs).isEqualTo(9_000)
    }

    @Test
    fun `pending - after the user left, or while a resend is going out - is dropped`() {
        listOf(MfaState.ChooseFactor(TWO), waiting(TWO, PHONE).copy(phase = PushPhase.Resending)).forEach { step ->
            assertThat(twoDevices.handleOutcome(step, pending(POLL))).isEqualTo(MfaUpdate(step))
        }
    }

    @Test
    fun `approved - from any screen, even after the user switched device - signs in`() {
        val credentials = mockk<Credentials>()
        listOf(
            checking(ONE, PHONE),
            MfaState.ChooseFactor(TWO),
            MfaState.SendingChallenge(TWO, TABLET),
        ).forEach { step ->
            val update = twoDevices.handleOutcome(step, MfaOutcome.SignedIn(credentials))

            assertThat(update.step).isEqualTo(step)
            assertThat(update.effect).isEqualTo(MfaEffect.Authenticated(credentials))
        }
    }

    @Test
    fun `check fails on the network - checks again quietly at the same pace, up to three times`() {
        var step: MfaState = checking(ONE, PHONE)
        repeat(MfaReducer.QUIET_RETRIES) { retry ->
            val update = oneDevice.handleOutcome(step, MfaOutcome.CheckFailed(NETWORK))

            assertThat(update.step).isEqualTo(waiting(ONE, PHONE).copy(failedChecks = retry + 1))
            assertThat(update.commands).containsExactly(MfaCommand.SchedulePushCheck(POLL))
            assertThat(update.effect).isNull()
            step = oneDevice.handlePushCheckDue(update.step).step
        }

        val last = oneDevice.handleOutcome(step, MfaOutcome.CheckFailed(NETWORK))

        assertThat(last.step).isEqualTo(MfaState.Failed(NETWORK))
        assertThat(last.effect).isEqualTo(MfaEffect.Failed(NETWORK))
    }

    @Test
    fun `check fails for good - ends MFA at once`() {
        val expired = MfaError.TokenExpired(null)

        val update = oneDevice.handleOutcome(checking(ONE, PHONE), MfaOutcome.CheckFailed(expired))

        assertThat(update.step).isEqualTo(MfaState.Failed(expired))
        assertThat(update.effect).isEqualTo(MfaEffect.Failed(expired))
    }

    @Test
    fun `network blip from a check the user left, or while a resend is going out - is dropped`() {
        listOf(
            MfaState.ChooseFactor(TWO),
            MfaState.SendingChallenge(TWO, TABLET),
            waiting(TWO, PHONE).copy(phase = PushPhase.Resending),
        ).forEach { step ->
            assertThat(twoDevices.handleOutcome(step, MfaOutcome.CheckFailed(NETWORK))).isEqualTo(MfaUpdate(step))
        }
    }

    @Test
    fun `denial from a check the user left - still ends MFA with the real reason, not a later session ended`() {
        val rejected = MfaError.Rejected(null)
        listOf(
            MfaState.ChooseFactor(TWO),
            MfaState.SendingChallenge(TWO, TABLET),
            waiting(TWO, PHONE).copy(phase = PushPhase.Resending),
        ).forEach { step ->
            val update = twoDevices.handleOutcome(step, MfaOutcome.PushRejected)

            assertThat(update.step).isEqualTo(MfaState.Failed(rejected))
            assertThat(update.effect).isEqualTo(MfaEffect.Failed(rejected))
        }
    }

    @Test
    fun `network blips while the app is in the background - are not counted, so the push isn't lost`() {
        var step: MfaState = checking(ONE, PHONE)
        repeat(MfaReducer.QUIET_RETRIES + 2) {
            val update = oneDevice.handleOutcome(step, MfaOutcome.CheckFailed(NETWORK, whileInBackground = true))

            assertThat(update.effect).isNull()
            assertThat((update.step as MfaState.PushWaiting).failedChecks).isEqualTo(0)
            step = oneDevice.handlePushCheckDue(update.step).step
        }
    }

    @Test
    fun `back in the foreground - a waiting push is checked at once`() {
        val update = oneDevice.handleAppForegrounded(waiting(ONE, PHONE))

        assertThat(update.step).isEqualTo(checking(ONE, PHONE))
        assertThat(update.commands).containsExactly(MfaCommand.CancelPushWork, MfaCommand.CheckPush).inOrder()
    }

    @Test
    fun `back in the foreground - with a check already on its way, or off the push screen - does nothing`() {
        val steps = listOf(checking(ONE, PHONE), MfaState.ChooseFactor(TWO), MfaState.SendingChallenge(ONE, PHONE))
        steps.forEach { step ->
            assertThat(oneDevice.handleAppForegrounded(step)).isEqualTo(MfaUpdate(step))
        }
    }

    // endregion

    // region one more step

    @Test
    fun `another factor - with a choice - shows the next round's list`() {
        val update = twoDevices.handleOutcome(checking(TWO, PHONE), MfaOutcome.AnotherFactor(listOf(PHONE, TABLET)))

        assertThat(update.step).isEqualTo(MfaState.ChooseFactor(MfaRound(2, listOf(PHONE, TABLET))))
        assertThat(update.commands).containsExactly(MfaCommand.CancelPushWork)
    }

    @Test
    fun `another factor - just one - still shows the list headed One more step, as the PRD says`() {
        val update = oneDevice.handleOutcome(checking(ONE, PHONE), MfaOutcome.AnotherFactor(listOf(TABLET)))
        val round2 = MfaRound(2, listOf(TABLET))

        assertThat(update.step).isEqualTo(MfaState.ChooseFactor(round2))
        assertThat(update.commands).containsExactly(MfaCommand.CancelPushWork)
        assertThat(round2.isAdditional).isTrue()

        val picked = oneDevice.handleIntent(update.step, MfaIntent.SelectFactor(TABLET))
        assertThat(picked.step).isEqualTo(MfaState.SendingChallenge(round2, TABLET))
        assertThat(picked.commands).containsExactly(MfaCommand.SendPush(TABLET))
    }

    @Test
    fun `first round - just one factor - still skips the list`() {
        assertThat(oneDevice.start().step).isEqualTo(MfaState.SendingChallenge(ONE, PHONE))
    }

    @Test
    fun `back - on the push of a later round with one factor - goes back to that round's list`() {
        val round2 = MfaRound(2, listOf(TABLET))

        val update = oneDevice.handleIntent(waiting(round2, TABLET), MfaIntent.Back)

        assertThat(update.step).isEqualTo(MfaState.ChooseFactor(round2))
        assertThat(update.effect).isNull()
    }

    @Test
    fun `another factor - none usable - ends MFA with no usable factor`() {
        val update = oneDevice.handleOutcome(checking(ONE, PHONE), MfaOutcome.AnotherFactor(emptyList()))

        assertEndedWithNoEligibleFactors(update)
    }

    @Test
    fun `another factor - after the user switched device - still counts`() {
        val update = twoDevices.handleOutcome(MfaState.ChooseFactor(TWO), MfaOutcome.AnotherFactor(listOf(TABLET)))

        assertThat(update.step).isEqualTo(MfaState.ChooseFactor(MfaRound(2, listOf(TABLET))))
    }

    @Test
    fun `another factor - again and again - has no limit, the server decides`() {
        var round = ONE
        repeat(5) {
            val update = oneDevice.handleOutcome(checking(round, PHONE), MfaOutcome.AnotherFactor(listOf(PHONE)))
            round = (update.step as MfaState.ChooseFactor).round
        }

        assertThat(round.number).isEqualTo(6)
    }

    // endregion

    // region resend

    @Test
    fun `resend - wait not over, server doesn't offer it, or already going out - does nothing`() {
        listOf(
            waiting(ONE, PHONE).copy(resendSecondsLeft = 5),
            waiting(ONE, PHONE).copy(resendSecondsLeft = 0, canResend = false),
            waiting(ONE, PHONE).copy(resendSecondsLeft = 0, phase = PushPhase.Resending),
        ).forEach { step -> assertThat(oneDevice.handleIntent(step, MfaIntent.Resend)).isEqualTo(MfaUpdate(step)) }
    }

    @Test
    fun `resend - wait over - stops the checks and sends the push to the same device again`() {
        val update = oneDevice.handleIntent(waiting(ONE, PHONE).copy(resendSecondsLeft = 0), MfaIntent.Resend)

        assertThat(update.step).isEqualTo(
            waiting(ONE, PHONE).copy(phase = PushPhase.Resending, canResend = false, resendSecondsLeft = 0),
        )
        assertThat(update.commands).containsExactly(MfaCommand.CancelPushWork, MfaCommand.SendPush(PHONE)).inOrder()
    }

    @Test
    fun `resend sent - checks again and starts a new resend wait`() {
        val resending = waiting(ONE, PHONE).copy(phase = PushPhase.Resending, canResend = false, failedChecks = 1)

        val update = oneDevice.handleOutcome(resending, sent(SLOWER))

        assertThat(update.step).isEqualTo(waiting(ONE, PHONE).copy(everyMs = SLOWER))
        assertThat(update.commands)
            .containsExactly(MfaCommand.SchedulePushCheck(SLOWER), MfaCommand.CountDownResend)
            .inOrder()
    }

    @Test
    fun `resend fails - ends MFA`() {
        val resending = waiting(ONE, PHONE).copy(phase = PushPhase.Resending)

        val update = oneDevice.handleOutcome(resending, MfaOutcome.SendFailed(NETWORK))

        assertThat(update.effect).isEqualTo(MfaEffect.Failed(NETWORK))
    }

    @Test
    fun `second passed - counts down, and stops at zero`() {
        val counted = oneDevice.handleSecondPassed(waiting(ONE, PHONE).copy(resendSecondsLeft = 2))
        val last = oneDevice.handleSecondPassed(counted.step)
        val atZero = oneDevice.handleSecondPassed(last.step)

        assertThat((counted.step as MfaState.PushWaiting).resendSecondsLeft).isEqualTo(1)
        assertThat(counted.commands).containsExactly(MfaCommand.CountDownResend)
        assertThat((last.step as MfaState.PushWaiting).resendSecondsLeft).isEqualTo(0)
        assertThat(last.commands).isEmpty()
        assertThat(atZero).isEqualTo(MfaUpdate(last.step))
    }

    @Test
    fun `second passed - several at once after the app was frozen - catches up, and stops at zero`() {
        val caughtUp = oneDevice.handleSecondPassed(waiting(ONE, PHONE).copy(resendSecondsLeft = 50), seconds = 40)
        val past = oneDevice.handleSecondPassed(caughtUp.step, seconds = 40)

        assertThat((caughtUp.step as MfaState.PushWaiting).resendSecondsLeft).isEqualTo(10)
        assertThat(caughtUp.commands).containsExactly(MfaCommand.CountDownResend)
        assertThat((past.step as MfaState.PushWaiting).resendSecondsLeft).isEqualTo(0)
        assertThat(past.commands).isEmpty()
    }

    @Test
    fun `second passed - after the push screen was left - stops`() {
        val list = MfaState.ChooseFactor(TWO)

        assertThat(twoDevices.handleSecondPassed(list)).isEqualTo(MfaUpdate(list))
    }

    // endregion

    // region leaving

    @Test
    fun `verify another way - with a choice - stops checking and shows the list`() {
        val update = twoDevices.handleIntent(checking(TWO, PHONE), MfaIntent.ChooseAnotherFactor)

        assertThat(update.step).isEqualTo(MfaState.ChooseFactor(TWO))
        assertThat(update.commands).containsExactly(MfaCommand.CancelPushWork)
    }

    @Test
    fun `verify another way - while a resend is going out - does nothing, so it can't send another push`() {
        val resending = waiting(TWO, PHONE).copy(phase = PushPhase.Resending, canResend = false)

        assertThat(twoDevices.handleIntent(resending, MfaIntent.ChooseAnotherFactor)).isEqualTo(MfaUpdate(resending))
    }

    @Test
    fun `verify another way - with only one device - does nothing`() {
        val step = waiting(ONE, PHONE)

        assertThat(oneDevice.handleIntent(step, MfaIntent.ChooseAnotherFactor)).isEqualTo(MfaUpdate(step))
    }

    @Test
    fun `back - on the push with a choice - goes back to the list`() {
        val update = twoDevices.handleIntent(waiting(TWO, PHONE), MfaIntent.Back)

        assertThat(update.step).isEqualTo(MfaState.ChooseFactor(TWO))
        assertThat(update.commands).containsExactly(MfaCommand.CancelPushWork)
        assertThat(update.effect).isNull()
    }

    @Test
    fun `back - on the push with one device, the list, or while a push goes out - starts the sign-in over`() {
        listOf(
            waiting(ONE, PHONE),
            MfaState.ChooseFactor(TWO),
            MfaState.SendingChallenge(TWO, TABLET),
            MfaState.SendingChallenge(ONE, PHONE),
        ).forEach { step ->
            val update = twoDevices.handleIntent(step, MfaIntent.Back)

            assertThat(update.effect).isEqualTo(MfaEffect.Restart)
        }
    }

    @Test
    fun `back - once MFA has ended - is left to the host`() {
        val failed = MfaState.Failed(NETWORK)

        assertThat(oneDevice.handleIntent(failed, MfaIntent.Back)).isEqualTo(MfaUpdate(failed))
    }

    @Test
    fun `cancel - from any screen, even the push with a choice - leaves MFA, where Back would only show the list`() {
        listOf(
            waiting(TWO, PHONE),
            waiting(ONE, PHONE),
            MfaState.ChooseFactor(TWO),
            MfaState.SendingChallenge(TWO, TABLET),
            MfaState.SendingChallenge(ONE, PHONE),
        ).forEach { step ->
            val update = twoDevices.handleIntent(step, MfaIntent.Cancel)

            assertThat(update.effect).isEqualTo(MfaEffect.Restart)
        }
    }

    @Test
    fun `cancel - once MFA has ended - is left to the host`() {
        val failed = MfaState.Failed(NETWORK)

        assertThat(oneDevice.handleIntent(failed, MfaIntent.Cancel)).isEqualTo(MfaUpdate(failed))
    }

    // endregion

    private fun sent(pollMs: Long, canResend: Boolean = true) =
        MfaOutcome.PushSent(PushStatus.Pending(pollMs, canResend))

    private fun pending(pollMs: Long?, canResend: Boolean = true, isSlowDown: Boolean = false) =
        MfaOutcome.PushPending(PushStatus.Pending(pollMs, canResend, isSlowDown))

    /** On the push screen right after it went out: waiting for the first check, the resend wait just started. */
    private fun waiting(round: MfaRound, factor: MfaFactor.Push) = MfaState.PushWaiting(
        round = round,
        factor = factor,
        phase = PushPhase.Waiting,
        everyMs = POLL,
        canResend = true,
        resendSecondsLeft = MfaReducer.RESEND_COOLDOWN_SECONDS,
    )

    /** The error screen and the effect carry the one error, which MFA itself raised: no cause. */
    private fun assertEndedWithNoEligibleFactors(update: MfaUpdate) {
        val failed = update.step as MfaState.Failed
        assertThat(failed.error).isInstanceOf(MfaError.NoEligibleFactors::class.java)
        assertThat(failed.error.cause).isNull()
        assertThat((update.effect as MfaEffect.Failed).error).isSameInstanceAs(failed.error)
    }

    private fun checking(round: MfaRound, factor: MfaFactor.Push) =
        waiting(round, factor).copy(phase = PushPhase.Checking)

    private companion object {
        val PHONE = MfaFactor.Push(0, "Jane's phone")
        val TABLET = MfaFactor.Push(1, "Jane's tablet")
        val ONE = MfaRound(1, listOf(PHONE))
        val TWO = MfaRound(1, listOf(PHONE, TABLET))
        val NETWORK = MfaError.Network(null)
        const val POLL = 3_000L
        const val SLOWER = 5_000L
    }
}
