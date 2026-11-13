package com.auth0.universalcomponents.feature.mfachallenge.presentation

import com.auth0.android.embedded.authorize.PhoneDeliveryMethod
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
    private val smsOnly = MfaReducer(listOf(SMS))
    private val smsOrPush = MfaReducer(listOf(SMS, PHONE))
    private val emailOnly = MfaReducer(listOf(EMAIL))
    private val emailOrPush = MfaReducer(listOf(EMAIL, PHONE))

    // region start

    @Test
    fun `start - one device - sends its push first, with no screen of its own until it's out`() {
        val update = oneDevice.start()

        assertThat(update.state).isEqualTo(MfaState.SendingChallenge(ONE, PHONE))
        assertThat(update.commands).containsExactly(MfaCommand.SendPush(PHONE))
    }

    @Test
    fun `start - several devices - shows the list`() {
        val update = twoDevices.start()

        assertThat(update.state).isEqualTo(MfaState.ChooseFactor(TWO))
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

        assertThat(update.state).isEqualTo(MfaState.SendingChallenge(TWO, TABLET))
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

        assertThat(update.state).isEqualTo(waiting(ONE, PHONE))
        assertThat(update.commands)
            .containsExactly(MfaCommand.SchedulePushCheck(POLL), MfaCommand.CountDownResend)
            .inOrder()
    }

    @Test
    fun `sent - the server's interval - is kept between 1 and 60 seconds`() {
        val sending = MfaState.SendingChallenge(ONE, PHONE)

        val tooFast = oneDevice.handleOutcome(sending, sent(0)).state as MfaState.PushWaiting
        val tooSlow = oneDevice.handleOutcome(sending, sent(120_000)).state as MfaState.PushWaiting

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

        assertThat(update.state).isEqualTo(MfaState.Failed(error))
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

        assertThat(update.state).isEqualTo(waiting(ONE, PHONE).copy(phase = PushPhase.Checking))
        assertThat(update.commands).containsExactly(MfaCommand.CheckPush)
    }

    @Test
    fun `check due - with a check or a resend on its way, or off the push screen - does nothing`() {
        listOf(
            waiting(ONE, PHONE).copy(phase = PushPhase.Checking),
            waiting(ONE, PHONE).copy(phase = PushPhase.Resending),
            MfaState.ChooseFactor(TWO),
        ).forEach { state -> assertThat(twoDevices.handlePushCheckDue(state)).isEqualTo(MfaUpdate(state)) }
    }

    @Test
    fun `pending - waits the server's new interval and says again whether resend is allowed`() {
        val checking = checking(ONE, PHONE).copy(failedChecks = 2)

        val update = oneDevice.handleOutcome(checking, pending(SLOWER, canResend = false))

        assertThat(update.state).isEqualTo(
            waiting(ONE, PHONE).copy(everyMs = SLOWER, canResend = false, failedChecks = 0),
        )
        assertThat(update.commands).containsExactly(MfaCommand.SchedulePushCheck(SLOWER))
    }

    @Test
    fun `pending - with no interval - keeps the last one`() {
        val update = oneDevice.handleOutcome(checking(ONE, PHONE), pending(null))

        assertThat((update.state as MfaState.PushWaiting).everyMs).isEqualTo(POLL)
    }

    @Test
    fun `slow down - waits 2 seconds longer, or the server's wait if that's longer`() {
        val shorter = oneDevice.handleOutcome(checking(ONE, PHONE), pending(1_000, isSlowDown = true))
        val longer = oneDevice.handleOutcome(checking(ONE, PHONE), pending(9_000, isSlowDown = true))

        assertThat((shorter.state as MfaState.PushWaiting).everyMs).isEqualTo(POLL + 2_000)
        assertThat((longer.state as MfaState.PushWaiting).everyMs).isEqualTo(9_000)
    }

    @Test
    fun `pending - after the user left, or while a resend is going out - is dropped`() {
        listOf(MfaState.ChooseFactor(TWO), waiting(TWO, PHONE).copy(phase = PushPhase.Resending)).forEach { state ->
            assertThat(twoDevices.handleOutcome(state, pending(POLL))).isEqualTo(MfaUpdate(state))
        }
    }

    @Test
    fun `approved - from any screen, even after the user switched device - signs in`() {
        val credentials = mockk<Credentials>()
        listOf(
            checking(ONE, PHONE),
            MfaState.ChooseFactor(TWO),
            MfaState.SendingChallenge(TWO, TABLET),
        ).forEach { state ->
            val update = twoDevices.handleOutcome(state, MfaOutcome.SignedIn(credentials))

            assertThat(update.state).isEqualTo(state)
            assertThat(update.effect).isEqualTo(MfaEffect.Authenticated(credentials))
        }
    }

    @Test
    fun `check fails on the network - checks again quietly at the same pace, up to three times`() {
        var state: MfaState = checking(ONE, PHONE)
        repeat(MfaReducer.QUIET_RETRIES) { retry ->
            val update = oneDevice.handleOutcome(state, MfaOutcome.CheckFailed(NETWORK))

            assertThat(update.state).isEqualTo(waiting(ONE, PHONE).copy(failedChecks = retry + 1))
            assertThat(update.commands).containsExactly(MfaCommand.SchedulePushCheck(POLL))
            assertThat(update.effect).isNull()
            state = oneDevice.handlePushCheckDue(update.state).state
        }

        val last = oneDevice.handleOutcome(state, MfaOutcome.CheckFailed(NETWORK))

        assertThat(last.state).isEqualTo(MfaState.Failed(NETWORK))
        assertThat(last.effect).isEqualTo(MfaEffect.Failed(NETWORK))
    }

    @Test
    fun `check fails for good - ends MFA at once`() {
        val expired = MfaError.TokenExpired(null)

        val update = oneDevice.handleOutcome(checking(ONE, PHONE), MfaOutcome.CheckFailed(expired))

        assertThat(update.state).isEqualTo(MfaState.Failed(expired))
        assertThat(update.effect).isEqualTo(MfaEffect.Failed(expired))
    }

    @Test
    fun `network blip from a check the user left, or while a resend is going out - is dropped`() {
        listOf(
            MfaState.ChooseFactor(TWO),
            MfaState.SendingChallenge(TWO, TABLET),
            waiting(TWO, PHONE).copy(phase = PushPhase.Resending),
        ).forEach { state ->
            assertThat(twoDevices.handleOutcome(state, MfaOutcome.CheckFailed(NETWORK))).isEqualTo(MfaUpdate(state))
        }
    }

    @Test
    fun `denial from a check the user left - still ends MFA with the real reason, not a later session ended`() {
        listOf(
            MfaState.ChooseFactor(TWO),
            MfaState.SendingChallenge(TWO, TABLET),
            waiting(TWO, PHONE).copy(phase = PushPhase.Resending),
        ).forEach { state ->
            val update = twoDevices.handleOutcome(state, MfaOutcome.PushRejected(DENIED_CAUSE))

            assertRejected(update)
        }
    }

    @Test
    fun `push denied - ends MFA at once, keeping the SDK reply as the cause`() {
        val update = oneDevice.handleOutcome(checking(ONE, PHONE), MfaOutcome.PushRejected(DENIED_CAUSE))

        assertRejected(update)
    }

    private fun assertRejected(update: MfaUpdate) {
        val failed = update.state as MfaState.Failed
        assertThat(failed.error).isInstanceOf(MfaError.Rejected::class.java)
        assertThat(failed.error.cause).isSameInstanceAs(DENIED_CAUSE)
        assertThat(update.effect).isEqualTo(MfaEffect.Failed(failed.error))
    }

    @Test
    fun `network blips while the app is in the background - are not counted, so the push isn't lost`() {
        var state: MfaState = checking(ONE, PHONE)
        repeat(MfaReducer.QUIET_RETRIES + 2) {
            val update = oneDevice.handleOutcome(state, MfaOutcome.CheckFailed(NETWORK, whileInBackground = true))

            assertThat(update.effect).isNull()
            assertThat((update.state as MfaState.PushWaiting).failedChecks).isEqualTo(0)
            state = oneDevice.handlePushCheckDue(update.state).state
        }
    }

    @Test
    fun `back in the foreground - a waiting push is checked at once`() {
        val update = oneDevice.handleAppForegrounded(waiting(ONE, PHONE))

        assertThat(update.state).isEqualTo(checking(ONE, PHONE))
        assertThat(update.commands).containsExactly(MfaCommand.CancelChallengeWork, MfaCommand.CheckPush).inOrder()
    }

    @Test
    fun `back in the foreground - with a check already on its way, or off the push screen - does nothing`() {
        val steps = listOf(checking(ONE, PHONE), MfaState.ChooseFactor(TWO), MfaState.SendingChallenge(ONE, PHONE))
        steps.forEach { state ->
            assertThat(oneDevice.handleAppForegrounded(state)).isEqualTo(MfaUpdate(state))
        }
    }

    // endregion

    // region one more step

    @Test
    fun `another factor - with a choice - shows the next round's list`() {
        val update = twoDevices.handleOutcome(checking(TWO, PHONE), MfaOutcome.AnotherFactor(listOf(PHONE, TABLET)))

        assertThat(update.state).isEqualTo(MfaState.ChooseFactor(MfaRound(2, listOf(PHONE, TABLET))))
        assertThat(update.commands).containsExactly(MfaCommand.CancelChallengeWork)
    }

    @Test
    fun `another factor - just one - still shows the list headed One more step, as the PRD says`() {
        val update = oneDevice.handleOutcome(checking(ONE, PHONE), MfaOutcome.AnotherFactor(listOf(TABLET)))
        val round2 = MfaRound(2, listOf(TABLET))

        assertThat(update.state).isEqualTo(MfaState.ChooseFactor(round2))
        assertThat(update.commands).containsExactly(MfaCommand.CancelChallengeWork)
        assertThat(round2.isAdditional).isTrue()

        val picked = oneDevice.handleIntent(update.state, MfaIntent.SelectFactor(TABLET))
        assertThat(picked.state).isEqualTo(MfaState.SendingChallenge(round2, TABLET))
        assertThat(picked.commands).containsExactly(MfaCommand.SendPush(TABLET))
    }

    @Test
    fun `first round - just one factor - still skips the list`() {
        assertThat(oneDevice.start().state).isEqualTo(MfaState.SendingChallenge(ONE, PHONE))
    }

    @Test
    fun `back - on the push of a later round with one factor - goes back to that round's list`() {
        val round2 = MfaRound(2, listOf(TABLET))

        val update = oneDevice.handleIntent(waiting(round2, TABLET), MfaIntent.Back)

        assertThat(update.state).isEqualTo(MfaState.ChooseFactor(round2))
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

        assertThat(update.state).isEqualTo(MfaState.ChooseFactor(MfaRound(2, listOf(TABLET))))
    }

    @Test
    fun `another factor - again and again - has no limit, the server decides`() {
        var round = ONE
        repeat(5) {
            val update = oneDevice.handleOutcome(checking(round, PHONE), MfaOutcome.AnotherFactor(listOf(PHONE)))
            round = (update.state as MfaState.ChooseFactor).round
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
        ).forEach { state -> assertThat(oneDevice.handleIntent(state, MfaIntent.Resend)).isEqualTo(MfaUpdate(state)) }
    }

    @Test
    fun `resend - wait over - stops the checks and sends the push to the same device again`() {
        val update = oneDevice.handleIntent(waiting(ONE, PHONE).copy(resendSecondsLeft = 0), MfaIntent.Resend)

        assertThat(update.state).isEqualTo(
            waiting(ONE, PHONE).copy(phase = PushPhase.Resending, canResend = false, resendSecondsLeft = 0),
        )
        assertThat(update.commands)
            .containsExactly(MfaCommand.CancelChallengeWork, MfaCommand.SendPush(PHONE))
            .inOrder()
    }

    @Test
    fun `resend sent - checks again and starts a new resend wait`() {
        val resending = waiting(ONE, PHONE).copy(phase = PushPhase.Resending, canResend = false, failedChecks = 1)

        val update = oneDevice.handleOutcome(resending, sent(SLOWER))

        assertThat(update.state).isEqualTo(waiting(ONE, PHONE).copy(everyMs = SLOWER))
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
        val last = oneDevice.handleSecondPassed(counted.state)
        val atZero = oneDevice.handleSecondPassed(last.state)

        assertThat((counted.state as MfaState.PushWaiting).resendSecondsLeft).isEqualTo(1)
        assertThat(counted.commands).containsExactly(MfaCommand.CountDownResend)
        assertThat((last.state as MfaState.PushWaiting).resendSecondsLeft).isEqualTo(0)
        assertThat(last.commands).isEmpty()
        assertThat(atZero).isEqualTo(MfaUpdate(last.state))
    }

    @Test
    fun `second passed - several at once after the app was frozen - catches up, and stops at zero`() {
        val caughtUp = oneDevice.handleSecondPassed(waiting(ONE, PHONE).copy(resendSecondsLeft = 50), seconds = 40)
        val past = oneDevice.handleSecondPassed(caughtUp.state, seconds = 40)

        assertThat((caughtUp.state as MfaState.PushWaiting).resendSecondsLeft).isEqualTo(10)
        assertThat(caughtUp.commands).containsExactly(MfaCommand.CountDownResend)
        assertThat((past.state as MfaState.PushWaiting).resendSecondsLeft).isEqualTo(0)
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

        assertThat(update.state).isEqualTo(MfaState.ChooseFactor(TWO))
        assertThat(update.commands).containsExactly(MfaCommand.CancelChallengeWork)
    }

    @Test
    fun `verify another way - while a resend is going out - does nothing, so it can't send another push`() {
        val resending = waiting(TWO, PHONE).copy(phase = PushPhase.Resending, canResend = false)

        assertThat(twoDevices.handleIntent(resending, MfaIntent.ChooseAnotherFactor)).isEqualTo(MfaUpdate(resending))
    }

    @Test
    fun `verify another way - with only one device - does nothing`() {
        val state = waiting(ONE, PHONE)

        assertThat(oneDevice.handleIntent(state, MfaIntent.ChooseAnotherFactor)).isEqualTo(MfaUpdate(state))
    }

    @Test
    fun `back - while a resend is going out - is ignored, so a second push can't follow`() {
        val resendingWithChoice = waiting(TWO, PHONE).copy(phase = PushPhase.Resending)
        val resendingAlone = waiting(ONE, PHONE).copy(phase = PushPhase.Resending)

        val ignoredWithChoice = twoDevices.handleIntent(resendingWithChoice, MfaIntent.Back)
        val ignoredAlone = oneDevice.handleIntent(resendingAlone, MfaIntent.Back)

        assertThat(ignoredWithChoice).isEqualTo(MfaUpdate(resendingWithChoice))
        assertThat(ignoredAlone).isEqualTo(MfaUpdate(resendingAlone))
    }

    @Test
    fun `cancel - while a resend is going out - still leaves MFA`() {
        val resending = waiting(TWO, PHONE).copy(phase = PushPhase.Resending)

        assertThat(twoDevices.handleIntent(resending, MfaIntent.Cancel).effect).isEqualTo(MfaEffect.Restart)
    }

    @Test
    fun `back - on the push with a choice - goes back to the list`() {
        val update = twoDevices.handleIntent(waiting(TWO, PHONE), MfaIntent.Back)

        assertThat(update.state).isEqualTo(MfaState.ChooseFactor(TWO))
        assertThat(update.commands).containsExactly(MfaCommand.CancelChallengeWork)
        assertThat(update.effect).isNull()
    }

    @Test
    fun `back - on the push with one device, the list, or while a push goes out - starts the sign-in over`() {
        listOf(
            waiting(ONE, PHONE),
            MfaState.ChooseFactor(TWO),
            MfaState.SendingChallenge(TWO, TABLET),
            MfaState.SendingChallenge(ONE, PHONE),
        ).forEach { state ->
            val update = twoDevices.handleIntent(state, MfaIntent.Back)

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
        ).forEach { state ->
            val update = twoDevices.handleIntent(state, MfaIntent.Cancel)

            assertThat(update.effect).isEqualTo(MfaEffect.Restart)
        }
    }

    @Test
    fun `cancel - once MFA has ended - is left to the host`() {
        val failed = MfaState.Failed(NETWORK)

        assertThat(oneDevice.handleIntent(failed, MfaIntent.Cancel)).isEqualTo(MfaUpdate(failed))
    }

    // endregion

    // region phone code

    @Test
    fun `phone - the only factor - sends its code first, with no screen of its own until it's out`() {
        val update = MfaReducer(listOf(SMS)).start()

        assertThat(update.state).isEqualTo(MfaState.SendingChallenge(BY_SMS, SMS))
        assertThat(update.commands).containsExactly(MfaCommand.SendPhoneCode(SMS))
    }

    @Test
    fun `phone - picked from the list - sends its code, with the list kept`() {
        val update = smsOrPush.handleIntent(MfaState.ChooseFactor(SMS_OR_PUSH), MfaIntent.SelectFactor(SMS))

        assertThat(update.state).isEqualTo(MfaState.SendingChallenge(SMS_OR_PUSH, SMS))
        assertThat(update.commands).containsExactly(MfaCommand.SendPhoneCode(SMS))
    }

    @Test
    fun `phone code sent - shows the code screen and starts the resend wait`() {
        val update = smsOnly.handleOutcome(MfaState.SendingChallenge(BY_SMS, SMS), MfaOutcome.CodeSent(true))

        assertThat(update.state).isEqualTo(codeReady(BY_SMS))
        assertThat(update.commands).containsExactly(MfaCommand.CountDownResend)
    }

    @Test
    fun `phone code sent - after the user left that screen - is dropped`() {
        val list = MfaState.ChooseFactor(SMS_OR_PUSH)

        assertThat(smsOrPush.handleOutcome(list, MfaOutcome.CodeSent(true))).isEqualTo(MfaUpdate(list))
    }

    @Test
    fun `phone code send fails - the first one, or a resend - ends MFA, and a stale one is dropped`() {
        val sending = smsOnly.handleOutcome(MfaState.SendingChallenge(BY_SMS, SMS), MfaOutcome.SendFailed(NETWORK))
        val resending = smsOnly.handleOutcome(
            codeReady(BY_SMS).copy(phase = CodePhase.Resending),
            MfaOutcome.SendFailed(NETWORK),
        )
        val stale = smsOnly.handleOutcome(codeReady(BY_SMS), MfaOutcome.SendFailed(NETWORK))

        assertThat(sending.state).isEqualTo(MfaState.Failed(NETWORK))
        assertThat(resending.state).isEqualTo(MfaState.Failed(NETWORK))
        assertThat(stale).isEqualTo(MfaUpdate(codeReady(BY_SMS)))
    }

    @Test
    fun `submit - a typed code - is checked, and the screen waits`() {
        val update = smsOnly.handleIntent(codeReady(BY_SMS), MfaIntent.SubmitCode("123456"))

        assertThat(update.state).isEqualTo(codeReady(BY_SMS).copy(phase = CodePhase.Verifying))
        assertThat(update.commands).containsExactly(MfaCommand.VerifyCode("123456"))
    }

    @Test
    fun `submit - a code that is not whole - is never sent, for a phone or an email`() {
        val phone = codeReady(BY_SMS)
        val email = emailReady(BY_EMAIL)

        listOf("", " ", "12345", "1234567", "12345a", "12 456").forEach { code ->
            assertThat(smsOnly.handleIntent(phone, MfaIntent.SubmitCode(code))).isEqualTo(MfaUpdate(phone))
            assertThat(emailOnly.handleIntent(email, MfaIntent.SubmitCode(code))).isEqualTo(MfaUpdate(email))
        }
    }

    @Test
    fun `submit - nothing typed, a check already on its way, or off the code screen - does nothing`() {
        val verifying = codeReady(BY_SMS).copy(phase = CodePhase.Verifying)
        val resending = codeReady(BY_SMS).copy(phase = CodePhase.Resending)
        val list = MfaState.ChooseFactor(SMS_OR_PUSH)

        val ready = codeReady(BY_SMS)

        assertThat(smsOnly.handleIntent(ready, MfaIntent.SubmitCode(" "))).isEqualTo(MfaUpdate(ready))
        assertThat(smsOnly.handleIntent(verifying, MfaIntent.SubmitCode("123456"))).isEqualTo(MfaUpdate(verifying))
        assertThat(smsOnly.handleIntent(resending, MfaIntent.SubmitCode("123456"))).isEqualTo(MfaUpdate(resending))
        assertThat(smsOrPush.handleIntent(list, MfaIntent.SubmitCode("123456"))).isEqualTo(MfaUpdate(list))
    }

    @Test
    fun `wrong code - stays on the screen with the line shown, and typing again clears it`() {
        val verifying = codeReady(BY_SMS).copy(phase = CodePhase.Verifying)

        val rejected = smsOnly.handleOutcome(verifying, MfaOutcome.CodeRejected)
        val again = smsOnly.handleIntent(rejected.state, MfaIntent.SubmitCode("654321"))

        assertThat(rejected.state).isEqualTo(codeReady(BY_SMS).copy(isWrongCode = true))
        assertThat(rejected.effect).isNull()
        assertThat((again.state as MfaState.CodeEntry).isWrongCode).isFalse()
    }

    @Test
    fun `code check fails - expired, too many tries, network - ends MFA with that error`() {
        val verifying = codeReady(BY_SMS).copy(phase = CodePhase.Verifying)
        val expired = MfaError.TokenExpired()

        val update = smsOnly.handleOutcome(verifying, MfaOutcome.CodeFailed(expired))

        assertThat(update.state).isEqualTo(MfaState.Failed(expired))
        assertThat(update.effect).isEqualTo(MfaEffect.Failed(expired))
    }

    @Test
    fun `wrong code or failed check - from a screen the user has left - is dropped`() {
        val list = MfaState.ChooseFactor(SMS_OR_PUSH)

        assertThat(smsOrPush.handleOutcome(list, MfaOutcome.CodeRejected)).isEqualTo(MfaUpdate(list))
        assertThat(smsOrPush.handleOutcome(list, MfaOutcome.CodeFailed(NETWORK))).isEqualTo(MfaUpdate(list))
        assertThat(smsOnly.handleOutcome(codeReady(BY_SMS), MfaOutcome.CodeRejected))
            .isEqualTo(MfaUpdate(codeReady(BY_SMS)))
    }

    @Test
    fun `right code - signs in, from the code screen`() {
        val credentials = mockk<Credentials>()
        val verifying = codeReady(BY_SMS).copy(phase = CodePhase.Verifying)

        val update = smsOnly.handleOutcome(verifying, MfaOutcome.SignedIn(credentials))

        assertThat(update.effect).isEqualTo(MfaEffect.Authenticated(credentials))
    }

    @Test
    fun `right code - and the server asks for one more factor - starts the next round`() {
        val verifying = codeReady(BY_SMS).copy(phase = CodePhase.Verifying)

        val update = smsOnly.handleOutcome(verifying, MfaOutcome.AnotherFactor(listOf(PHONE)))

        assertThat(update.state).isEqualTo(MfaState.ChooseFactor(MfaRound(2, listOf(PHONE))))
    }

    @Test
    fun `resend code - wait over - sends the code to the same phone again`() {
        val update = smsOnly.handleIntent(codeReady(BY_SMS, secondsLeft = 0), MfaIntent.Resend)

        assertThat(update.state).isEqualTo(
            codeReady(BY_SMS, secondsLeft = 0).copy(phase = CodePhase.Resending, canResend = false),
        )
        assertThat(update.commands).containsExactly(MfaCommand.SendPhoneCode(SMS))
    }

    @Test
    fun `resend code - wait not over, not offered, or a check on its way - does nothing`() {
        val waitingOut = codeReady(BY_SMS)
        val notOffered = codeReady(BY_SMS, secondsLeft = 0, canResend = false)
        val verifying = codeReady(BY_SMS, secondsLeft = 0).copy(phase = CodePhase.Verifying)

        listOf(waitingOut, notOffered, verifying).forEach { state ->
            assertThat(smsOnly.handleIntent(state, MfaIntent.Resend)).isEqualTo(MfaUpdate(state))
        }
    }

    @Test
    fun `resend code sent - ready again with a new wait, and the wrong-code line gone`() {
        val resending = codeReady(BY_SMS, secondsLeft = 0).copy(phase = CodePhase.Resending, isWrongCode = true)

        val update = smsOnly.handleOutcome(resending, MfaOutcome.CodeSent(canResend = false))

        assertThat(update.state).isEqualTo(codeReady(BY_SMS, canResend = false))
        assertThat(update.commands).containsExactly(MfaCommand.CountDownResend)
    }

    @Test
    fun `second passed - on the code screen - counts down, and stops at zero`() {
        val one = smsOnly.handleSecondPassed(codeReady(BY_SMS, secondsLeft = 2))
        val last = smsOnly.handleSecondPassed(one.state)
        val done = smsOnly.handleSecondPassed(last.state)

        assertThat((one.state as MfaState.CodeEntry).resendSecondsLeft).isEqualTo(1)
        assertThat(one.commands).containsExactly(MfaCommand.CountDownResend)
        assertThat((last.state as MfaState.CodeEntry).resendSecondsLeft).isEqualTo(0)
        assertThat(last.commands).isEmpty()
        assertThat(done).isEqualTo(MfaUpdate(last.state))
    }

    @Test
    fun `verify another way - from the code screen with a choice - shows the list`() {
        val update = smsOrPush.handleIntent(codeReady(SMS_OR_PUSH), MfaIntent.ChooseAnotherFactor)

        assertThat(update.state).isEqualTo(MfaState.ChooseFactor(SMS_OR_PUSH))
    }

    @Test
    fun `verify another way - with one factor, or while a code is being checked or sent - does nothing`() {
        val alone = codeReady(BY_SMS)
        val verifying = codeReady(SMS_OR_PUSH).copy(phase = CodePhase.Verifying)
        val resending = codeReady(SMS_OR_PUSH).copy(phase = CodePhase.Resending)

        listOf(alone, verifying, resending).forEach { state ->
            assertThat(smsOrPush.handleIntent(state, MfaIntent.ChooseAnotherFactor)).isEqualTo(MfaUpdate(state))
        }
    }

    @Test
    fun `back - on the code screen - goes to the list with a choice, else starts the sign-in over`() {
        val withChoice = smsOrPush.handleIntent(codeReady(SMS_OR_PUSH), MfaIntent.Back)
        val alone = smsOnly.handleIntent(codeReady(BY_SMS), MfaIntent.Back)

        assertThat(withChoice.state).isEqualTo(MfaState.ChooseFactor(SMS_OR_PUSH))
        assertThat(alone.effect).isEqualTo(MfaEffect.Restart)
    }

    @Test
    fun `back - while a code is being checked or sent - is ignored, but Cancel still leaves`() {
        val verifying = codeReady(BY_SMS).copy(phase = CodePhase.Verifying)
        val resending = codeReady(BY_SMS).copy(phase = CodePhase.Resending)

        listOf(verifying, resending).forEach { state ->
            assertThat(smsOnly.handleIntent(state, MfaIntent.Back)).isEqualTo(MfaUpdate(state))
            assertThat(smsOnly.handleIntent(state, MfaIntent.Cancel).effect).isEqualTo(MfaEffect.Restart)
        }
    }

    // endregion

    // region email code

    @Test
    fun `email - the only factor - sends its code first, with no screen of its own until it's out`() {
        val update = emailOnly.start()

        assertThat(update.state).isEqualTo(MfaState.SendingChallenge(BY_EMAIL, EMAIL))
        assertThat(update.commands).containsExactly(MfaCommand.SendEmailCode(EMAIL))
    }

    @Test
    fun `email - picked from the list - sends its code, with the list kept`() {
        val update = emailOrPush.handleIntent(MfaState.ChooseFactor(EMAIL_OR_PUSH), MfaIntent.SelectFactor(EMAIL))

        assertThat(update.state).isEqualTo(MfaState.SendingChallenge(EMAIL_OR_PUSH, EMAIL))
        assertThat(update.commands).containsExactly(MfaCommand.SendEmailCode(EMAIL))
    }

    @Test
    fun `email code sent - shows the code screen for that address and starts the resend wait`() {
        val update = emailOnly.handleOutcome(MfaState.SendingChallenge(BY_EMAIL, EMAIL), MfaOutcome.CodeSent(true))

        assertThat(update.state).isEqualTo(emailReady(BY_EMAIL))
        assertThat(update.commands).containsExactly(MfaCommand.CountDownResend)
    }

    @Test
    fun `email code - typed, wrong, then right - follows the same rules as a phone code`() {
        val submitted = emailOnly.handleIntent(emailReady(BY_EMAIL), MfaIntent.SubmitCode("123456"))
        val rejected = emailOnly.handleOutcome(submitted.state, MfaOutcome.CodeRejected)
        val credentials = mockk<Credentials>()
        val signedIn = emailOnly.handleOutcome(submitted.state, MfaOutcome.SignedIn(credentials))

        assertThat(submitted.state).isEqualTo(emailReady(BY_EMAIL).copy(phase = CodePhase.Verifying))
        assertThat(submitted.commands).containsExactly(MfaCommand.VerifyCode("123456"))
        assertThat(rejected.state).isEqualTo(emailReady(BY_EMAIL).copy(isWrongCode = true))
        assertThat(signedIn.effect).isEqualTo(MfaEffect.Authenticated(credentials))
    }

    @Test
    fun `email code - resend - sends the code to the same address again`() {
        val update = emailOnly.handleIntent(emailReady(BY_EMAIL, secondsLeft = 0), MfaIntent.Resend)

        assertThat(update.state).isEqualTo(
            emailReady(BY_EMAIL, secondsLeft = 0).copy(phase = CodePhase.Resending, canResend = false),
        )
        assertThat(update.commands).containsExactly(MfaCommand.SendEmailCode(EMAIL))
    }

    @Test
    fun `email code - verify another way and back - go to the list with a choice, else start the sign-in over`() {
        val switched = emailOrPush.handleIntent(emailReady(EMAIL_OR_PUSH), MfaIntent.ChooseAnotherFactor)
        val back = emailOrPush.handleIntent(emailReady(EMAIL_OR_PUSH), MfaIntent.Back)
        val alone = emailOnly.handleIntent(emailReady(BY_EMAIL), MfaIntent.Back)

        assertThat(switched.state).isEqualTo(MfaState.ChooseFactor(EMAIL_OR_PUSH))
        assertThat(back.state).isEqualTo(MfaState.ChooseFactor(EMAIL_OR_PUSH))
        assertThat(alone.effect).isEqualTo(MfaEffect.Restart)
    }

    @Test
    fun `email code send fails - the first one, or a resend - ends MFA, and a stale one is dropped`() {
        val sending = emailOnly.handleOutcome(
            MfaState.SendingChallenge(BY_EMAIL, EMAIL),
            MfaOutcome.SendFailed(NETWORK),
        )
        val resending = emailOnly.handleOutcome(
            emailReady(BY_EMAIL).copy(phase = CodePhase.Resending),
            MfaOutcome.SendFailed(NETWORK),
        )
        val stale = emailOnly.handleOutcome(emailReady(BY_EMAIL), MfaOutcome.SendFailed(NETWORK))

        assertThat(sending.state).isEqualTo(MfaState.Failed(NETWORK))
        assertThat(resending.state).isEqualTo(MfaState.Failed(NETWORK))
        assertThat(stale).isEqualTo(MfaUpdate(emailReady(BY_EMAIL)))
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
        val failed = update.state as MfaState.Failed
        assertThat(failed.error).isInstanceOf(MfaError.NoEligibleFactors::class.java)
        assertThat(failed.error.cause).isNull()
        assertThat((update.effect as MfaEffect.Failed).error).isSameInstanceAs(failed.error)
    }

    /** On the code screen right after the code went out. */
    private fun codeReady(
        round: MfaRound,
        secondsLeft: Int = MfaReducer.RESEND_COOLDOWN_SECONDS,
        canResend: Boolean = true,
    ) = MfaState.CodeEntry(
        round = round,
        factor = SMS,
        phase = CodePhase.Ready,
        canResend = canResend,
        resendSecondsLeft = secondsLeft,
    )

    /** On the code screen for an email address right after the code went out. */
    private fun emailReady(round: MfaRound, secondsLeft: Int = MfaReducer.RESEND_COOLDOWN_SECONDS) =
        codeReady(round, secondsLeft).copy(factor = EMAIL)

    private fun checking(round: MfaRound, factor: MfaFactor.Push) =
        waiting(round, factor).copy(phase = PushPhase.Checking)

    private companion object {
        val PHONE = MfaFactor.Push(0, "Jane's phone")
        val TABLET = MfaFactor.Push(1, "Jane's tablet")
        val ONE = MfaRound(1, listOf(PHONE))
        val TWO = MfaRound(1, listOf(PHONE, TABLET))
        val SMS = MfaFactor.Phone(3, "***9239", PhoneDeliveryMethod.TEXT)
        val BY_SMS = MfaRound(1, listOf(SMS))
        val SMS_OR_PUSH = MfaRound(1, listOf(SMS, PHONE))
        val EMAIL = MfaFactor.Email(5, "j***@example.com")
        val BY_EMAIL = MfaRound(1, listOf(EMAIL))
        val EMAIL_OR_PUSH = MfaRound(1, listOf(EMAIL, PHONE))
        val NETWORK = MfaError.Network(null)
        val DENIED_CAUSE = IllegalStateException("denied")
        const val POLL = 3_000L
        const val SLOWER = 5_000L
    }
}
