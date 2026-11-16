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
    private val appOnly = MfaReducer(listOf(APP))
    private val appOrPush = MfaReducer(listOf(APP, PHONE))
    private val recoveryOnly = MfaReducer(listOf(RECOVERY))
    private val recoveryOrPush = MfaReducer(listOf(RECOVERY, PHONE))

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
    fun `phone code send fails - the first one, or a resend for any other reason - ends MFA, and a stale one is dropped`() {
        val other = MfaError.Unexpected()
        val sending = smsOnly.handleOutcome(MfaState.SendingChallenge(BY_SMS, SMS), MfaOutcome.SendFailed(NETWORK))
        val resending = smsOnly.handleOutcome(
            codeReady(BY_SMS).copy(phase = CodePhase.Resending),
            MfaOutcome.SendFailed(other),
        )
        val stale = smsOnly.handleOutcome(codeReady(BY_SMS), MfaOutcome.SendFailed(NETWORK))

        assertThat(sending.state).isEqualTo(MfaState.Failed(NETWORK))
        assertThat(resending.state).isEqualTo(MfaState.Failed(other))
        assertThat(stale).isEqualTo(MfaUpdate(codeReady(BY_SMS)))
    }

    @Test
    fun `submit - a typed code - is checked, and the screen waits`() {
        val update = smsOnly.handleIntent(codeReady(BY_SMS), MfaIntent.SubmitCode("123456"))

        assertThat(update.state).isEqualTo(codeReady(BY_SMS).copy(phase = CodePhase.Verifying))
        assertThat(update.commands).containsExactly(MfaCommand.VerifyCode(SMS, "123456"))
    }

    @Test
    fun `submit - a code that is not whole - is never sent, for a phone or an email`() {
        val phone = codeReady(BY_SMS)
        val email = emailReady(BY_EMAIL)

        NOT_WHOLE_CODES.forEach { code ->
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
    fun `code check fails - too many tries, anything else, or an expiry with no other code on offer - ends MFA`() {
        val verifying = codeReady(BY_SMS).copy(phase = CodePhase.Verifying)
        val expiredWithNoOffer = codeReady(BY_SMS, canResend = false).copy(phase = CodePhase.Verifying)

        assertThat(smsOnly.handleOutcome(expiredWithNoOffer, MfaOutcome.CodeFailed(MfaError.TokenExpired())).state)
            .isInstanceOf(MfaState.Failed::class.java)
        listOf(MfaError.TooManyAttempts(), MfaError.Unexpected()).forEach { error ->
            val update = smsOnly.handleOutcome(verifying, MfaOutcome.CodeFailed(error))

            assertThat(update.state).isEqualTo(MfaState.Failed(error))
            assertThat(update.effect).isEqualTo(MfaEffect.Failed(error))
        }
    }

    @Test
    fun `code check fails on the network - stays on the screen, and the same code can be sent again`() {
        // The same rule for a texted or called code, an emailed one and an authenticator app's.
        listOf(
            smsOnly to codeReady(BY_SMS),
            emailOnly to emailReady(BY_EMAIL),
            appOnly to appReady(BY_APP),
        ).forEach { (reducer, ready) ->
            val verifying = ready.copy(phase = CodePhase.Verifying)

            val dropped = reducer.handleOutcome(verifying, MfaOutcome.CodeFailed(NETWORK))
            val again = reducer.handleIntent(dropped.state, MfaIntent.SubmitCode("123456"))

            assertThat(dropped.state).isEqualTo(ready.copy(requestError = NETWORK))
            assertThat(dropped.commands).isEmpty()
            assertThat(dropped.effect).isNull()
            assertThat(again.state).isEqualTo(verifying)
            assertThat(again.commands).containsExactly(MfaCommand.VerifyCode(ready.factor, "123456"))
        }
    }

    @Test
    fun `resend - after a connection error - clears it, as the code on show is replaced`() {
        val offline = codeReady(BY_SMS, secondsLeft = 0).copy(requestError = NETWORK)

        val update = smsOnly.handleIntent(offline, MfaIntent.Resend)

        assertThat((update.state as MfaState.CodeEntry).requestError).isNull()
        assertThat(update.commands).containsExactly(MfaCommand.SendPhoneCode(SMS))
    }

    @Test
    fun `wrong code or failed check - from a screen the user has left - is dropped`() {
        val list = MfaState.ChooseFactor(SMS_OR_PUSH)

        assertThat(smsOrPush.handleOutcome(list, MfaOutcome.CodeRejected)).isEqualTo(MfaUpdate(list))
        assertThat(smsOrPush.handleOutcome(list, MfaOutcome.CodeFailed(NETWORK))).isEqualTo(MfaUpdate(list))
        assertThat(smsOnly.handleOutcome(codeReady(BY_SMS), MfaOutcome.CodeRejected))
            .isEqualTo(MfaUpdate(codeReady(BY_SMS)))
        // A connection error for a check nobody is waiting on is not shown either.
        assertThat(smsOnly.handleOutcome(codeReady(BY_SMS), MfaOutcome.CodeFailed(NETWORK)))
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
            codeReady(BY_SMS, secondsLeft = 0).copy(phase = CodePhase.Resending, resend = RESEND_SENT),
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

        assertThat((one.state as MfaState.CodeEntry).resend?.secondsLeft).isEqualTo(1)
        assertThat(one.commands).containsExactly(MfaCommand.CountDownResend)
        assertThat((last.state as MfaState.CodeEntry).resend?.secondsLeft).isEqualTo(0)
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
        assertThat(submitted.commands).containsExactly(MfaCommand.VerifyCode(EMAIL, "123456"))
        assertThat(rejected.state).isEqualTo(emailReady(BY_EMAIL).copy(isWrongCode = true))
        assertThat(signedIn.effect).isEqualTo(MfaEffect.Authenticated(credentials))
    }

    @Test
    fun `email code - resend - sends the code to the same address again`() {
        val update = emailOnly.handleIntent(emailReady(BY_EMAIL, secondsLeft = 0), MfaIntent.Resend)

        assertThat(update.state).isEqualTo(
            emailReady(BY_EMAIL, secondsLeft = 0).copy(phase = CodePhase.Resending, resend = RESEND_SENT),
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
    fun `email code send fails - the first one, or a resend for any other reason - ends MFA, and a stale one is dropped`() {
        val other = MfaError.Unexpected()
        val sending = emailOnly.handleOutcome(
            MfaState.SendingChallenge(BY_EMAIL, EMAIL),
            MfaOutcome.SendFailed(NETWORK),
        )
        val resending = emailOnly.handleOutcome(
            emailReady(BY_EMAIL).copy(phase = CodePhase.Resending),
            MfaOutcome.SendFailed(other),
        )
        val stale = emailOnly.handleOutcome(emailReady(BY_EMAIL), MfaOutcome.SendFailed(NETWORK))

        assertThat(sending.state).isEqualTo(MfaState.Failed(NETWORK))
        assertThat(resending.state).isEqualTo(MfaState.Failed(other))
        assertThat(stale).isEqualTo(MfaUpdate(emailReady(BY_EMAIL)))
    }

    // endregion

    // region failures that keep the code screen

    @Test
    fun `resend fails on the network or is refused as too many - the code on show stays, with the reason`() {
        listOf(smsOnly to codeReady(BY_SMS), emailOnly to emailReady(BY_EMAIL)).forEach { (reducer, ready) ->
            listOf(NETWORK, MfaError.TooManyAttempts()).forEach { error ->
                val resending = ready.copy(
                    phase = CodePhase.Resending,
                    resend = RESEND_SENT,
                )

                val update = reducer.handleOutcome(resending, MfaOutcome.SendFailed(error))

                // Ready again, with the usual wait before the next try and the reason on show.
                assertThat(update.state).isEqualTo(ready.copy(requestError = error))
                assertThat(update.commands).containsExactly(MfaCommand.CountDownResend)
                assertThat(update.effect).isNull()
            }
        }
    }

    @Test
    fun `resend fails after the code expired - Resend opens at once, and the code stays expired`() {
        val expired = codeReady(BY_SMS, secondsLeft = 0).copy(isCodeExpired = true)
        val resending = expired.copy(phase = CodePhase.Resending, resend = RESEND_SENT)

        val update = smsOnly.handleOutcome(resending, MfaOutcome.SendFailed(NETWORK))

        assertThat(update.state).isEqualTo(expired.copy(requestError = NETWORK))
        assertThat(update.commands).isEmpty()
    }

    @Test
    fun `code expired - when the server offers another - stays, with Verify held back and Resend open at once`() {
        listOf(smsOnly to codeReady(BY_SMS), emailOnly to emailReady(BY_EMAIL)).forEach { (reducer, ready) ->
            val verifying = ready.copy(phase = CodePhase.Verifying)

            val expired = reducer.handleOutcome(verifying, MfaOutcome.CodeFailed(MfaError.TokenExpired()))

            assertThat(expired.state).isEqualTo(
                ready.copy(resend = RESEND_OPEN, isCodeExpired = true),
            )
            assertThat(expired.effect).isNull()
            // Verify waits for the new code.
            val typed = reducer.handleIntent(expired.state, MfaIntent.SubmitCode("123456"))
            assertThat(typed).isEqualTo(MfaUpdate(expired.state))
        }
    }

    @Test
    fun `code expired - Resend brings a new code, which clears the expiry`() {
        val expired = codeReady(BY_SMS, secondsLeft = 0).copy(isCodeExpired = true)

        val resending = smsOnly.handleIntent(expired, MfaIntent.Resend)
        val sent = smsOnly.handleOutcome(resending.state, MfaOutcome.CodeSent(canResend = true))

        assertThat(resending.commands).containsExactly(MfaCommand.SendPhoneCode(SMS))
        assertThat((resending.state as MfaState.CodeEntry).isCodeExpired).isTrue()
        assertThat(sent.state).isEqualTo(codeReady(BY_SMS))
    }

    // endregion

    // region authenticator app

    @Test
    fun `app - the only factor - shows its code screen at once, with nothing to send or wait for`() {
        val update = appOnly.start()

        assertThat(update.state).isEqualTo(appReady(BY_APP))
        assertThat(update.commands).isEmpty()
    }

    @Test
    fun `app - picked from the list - shows its code screen at once, with nothing to send or wait for`() {
        val update = appOrPush.handleIntent(MfaState.ChooseFactor(APP_OR_PUSH), MfaIntent.SelectFactor(APP))

        assertThat(update.state).isEqualTo(appReady(APP_OR_PUSH))
        assertThat(update.commands).isEmpty()
    }

    @Test
    fun `app code - typed, wrong, then right - follows the same rules as a phone code`() {
        val submitted = appOnly.handleIntent(appReady(BY_APP), MfaIntent.SubmitCode("123456"))
        val rejected = appOnly.handleOutcome(submitted.state, MfaOutcome.CodeRejected)
        val credentials = mockk<Credentials>()
        val signedIn = appOnly.handleOutcome(submitted.state, MfaOutcome.SignedIn(credentials))

        assertThat(submitted.state).isEqualTo(appReady(BY_APP).copy(phase = CodePhase.Verifying))
        assertThat(submitted.commands).containsExactly(MfaCommand.VerifyCode(APP, "123456"))
        assertThat(rejected.state).isEqualTo(appReady(BY_APP).copy(isWrongCode = true))
        assertThat(signedIn.effect).isEqualTo(MfaEffect.Authenticated(credentials))
    }

    @Test
    fun `app code - one that is not whole - is never sent`() {
        val ready = appReady(BY_APP)

        NOT_WHOLE_CODES.forEach { code ->
            assertThat(appOnly.handleIntent(ready, MfaIntent.SubmitCode(code))).isEqualTo(MfaUpdate(ready))
        }
    }

    @Test
    fun `app code - check fails, or one more factor is asked for - ends MFA, or starts the next round`() {
        val verifying = appReady(BY_APP).copy(phase = CodePhase.Verifying)

        val expired = MfaError.TokenExpired()
        val failed = appOnly.handleOutcome(verifying, MfaOutcome.CodeFailed(expired))
        val another = appOnly.handleOutcome(verifying, MfaOutcome.AnotherFactor(listOf(PHONE)))

        assertThat(failed.state).isEqualTo(MfaState.Failed(expired))
        assertThat(another.state).isEqualTo(MfaState.ChooseFactor(MfaRound(2, listOf(PHONE))))
    }

    @Test
    fun `app code - an expiry has nothing to send again, so it ends MFA`() {
        val verifying = appReady(BY_APP).copy(phase = CodePhase.Verifying)
        val expired = MfaError.TokenExpired()

        val update = appOnly.handleOutcome(verifying, MfaOutcome.CodeFailed(expired))

        assertThat(update.state).isEqualTo(MfaState.Failed(expired))
    }

    @Test
    fun `app code - has no Resend, and no wait to count down`() {
        val ready = appReady(BY_APP)

        assertThat(appOnly.handleIntent(ready, MfaIntent.Resend)).isEqualTo(MfaUpdate(ready))
        assertThat(appOnly.handleSecondPassed(ready)).isEqualTo(MfaUpdate(ready))
    }

    @Test
    fun `app code - a send that was never asked for - is dropped`() {
        val ready = appReady(BY_APP)

        assertThat(appOnly.handleOutcome(ready, MfaOutcome.CodeSent(true))).isEqualTo(MfaUpdate(ready))
        assertThat(appOnly.handleOutcome(ready, MfaOutcome.SendFailed(NETWORK))).isEqualTo(MfaUpdate(ready))
    }

    @Test
    fun `app code - verify another way and back - go to the list with a choice, else start the sign-in over`() {
        val switched = appOrPush.handleIntent(appReady(APP_OR_PUSH), MfaIntent.ChooseAnotherFactor)
        val back = appOrPush.handleIntent(appReady(APP_OR_PUSH), MfaIntent.Back)
        val alone = appOnly.handleIntent(appReady(BY_APP), MfaIntent.Back)
        val checking = appReady(APP_OR_PUSH).copy(phase = CodePhase.Verifying)

        assertThat(switched.state).isEqualTo(MfaState.ChooseFactor(APP_OR_PUSH))
        assertThat(back.state).isEqualTo(MfaState.ChooseFactor(APP_OR_PUSH))
        assertThat(alone.effect).isEqualTo(MfaEffect.Restart)
        assertThat(appOrPush.handleIntent(checking, MfaIntent.ChooseAnotherFactor)).isEqualTo(MfaUpdate(checking))
        assertThat(appOrPush.handleIntent(checking, MfaIntent.Back)).isEqualTo(MfaUpdate(checking))
    }

    @Test
    fun `app code - a late push answer asking for one more factor - still starts the next round, dropping the check`() {
        val typing = appReady(APP_OR_PUSH).copy(phase = CodePhase.Verifying)

        val update = appOrPush.handleOutcome(typing, MfaOutcome.AnotherFactor(listOf(TABLET)))

        assertThat(update.state).isEqualTo(MfaState.ChooseFactor(MfaRound(2, listOf(TABLET))))
        assertThat(update.commands).containsExactly(MfaCommand.CancelChallengeWork)
    }

    // endregion

    // region recovery code

    @Test
    fun `recovery - the only factor, or picked from the list - shows its code screen at once, with nothing to send`() {
        val alone = recoveryOnly.start()
        val list = MfaState.ChooseFactor(RECOVERY_OR_PUSH)
        val picked = recoveryOrPush.handleIntent(list, MfaIntent.SelectFactor(RECOVERY))

        assertThat(alone.state).isEqualTo(recoveryReady(BY_RECOVERY))
        assertThat(alone.commands).isEmpty()
        assertThat(picked.state).isEqualTo(recoveryReady(RECOVERY_OR_PUSH))
        assertThat(picked.commands).isEmpty()
    }

    @Test
    fun `recovery code - typed - is sent as it is, of any length, with what a paste brings along removed`() {
        val ready = recoveryReady(BY_RECOVERY)

        mapOf(
            // Case, hyphens and spaces inside the code are the user's.
            "Abcd-1234 efgh" to "Abcd-1234 efgh",
            "  YHARATFUJJKZNV5N3X1L2ZEC\n" to "YHARATFUJJKZNV5N3X1L2ZEC",
            // A zero-width space, a left-to-right mark and a byte-order mark come along with pasted text.
            "\u200BYHARAT\u200EFUJJ\uFEFF" to "YHARATFUJJ",
            "7" to "7",
        ).forEach { (typed, sent) ->
            val update = recoveryOnly.handleIntent(ready, MfaIntent.SubmitCode(typed))

            assertThat(update.state).isEqualTo(ready.copy(phase = CodePhase.Verifying))
            assertThat(update.commands).containsExactly(MfaCommand.VerifyCode(RECOVERY, sent))
        }
    }

    @Test
    fun `recovery code - blank, or only what a paste brings along - is never sent`() {
        val ready = recoveryReady(BY_RECOVERY)

        listOf("", "   ", "\n", "\u200B", "\u200E \uFEFF").forEach { typed ->
            assertThat(recoveryOnly.handleIntent(ready, MfaIntent.SubmitCode(typed))).isEqualTo(MfaUpdate(ready))
        }
    }

    @Test
    fun `recovery code - wrong - stays for another try, and the next submit clears the line`() {
        val submitted = recoveryOnly.handleIntent(recoveryReady(BY_RECOVERY), MfaIntent.SubmitCode("nope"))
        val rejected = recoveryOnly.handleOutcome(submitted.state, MfaOutcome.CodeRejected)
        val again = recoveryOnly.handleIntent(rejected.state, MfaIntent.SubmitCode("again"))

        assertThat(rejected.state).isEqualTo(recoveryReady(BY_RECOVERY).copy(isWrongCode = true))
        assertThat(again.state).isEqualTo(recoveryReady(BY_RECOVERY).copy(phase = CodePhase.Verifying))
    }

    @Test
    fun `recovery code - right - is used up, and the new code is shown to save, with nothing sent yet`() {
        val verifying = recoveryReady(BY_RECOVERY).copy(phase = CodePhase.Verifying)

        val update = recoveryOnly.handleOutcome(verifying, MfaOutcome.RecoveryCodeRotated(NEW_CODE))

        assertThat(update.state).isEqualTo(saving(BY_RECOVERY))
        assertThat(update.commands).isEmpty()
        assertThat(update.effect).isNull()
    }

    @Test
    fun `recovery code - right, but for a screen the user left - is dropped`() {
        val list = MfaState.ChooseFactor(RECOVERY_OR_PUSH)
        val typing = recoveryReady(RECOVERY_OR_PUSH)
        val otherFactor = MfaState.CodeEntry(RECOVERY_OR_PUSH, APP, CodePhase.Verifying, resend = null)

        listOf(list, typing, otherFactor).forEach { state ->
            val update = recoveryOrPush.handleOutcome(state, MfaOutcome.RecoveryCodeRotated(NEW_CODE))

            assertThat(update).isEqualTo(MfaUpdate(state))
        }
    }

    @Test
    fun `recovery code - checked, then the connection drops - stays on the screen, so the same code can be sent again`() {
        val verifying = recoveryReady(BY_RECOVERY).copy(phase = CodePhase.Verifying)

        val dropped = recoveryOnly.handleOutcome(verifying, MfaOutcome.CodeFailed(NETWORK))
        val again = recoveryOnly.handleIntent(dropped.state, MfaIntent.SubmitCode("same"))

        assertThat(dropped.state).isEqualTo(recoveryReady(BY_RECOVERY).copy(requestError = NETWORK))
        assertThat(again.state).isEqualTo(recoveryReady(BY_RECOVERY).copy(phase = CodePhase.Verifying))
    }

    @Test
    fun `recovery code - checked, then any other failure - ends MFA, since the session is gone`() {
        val verifying = recoveryReady(BY_RECOVERY).copy(phase = CodePhase.Verifying)
        val tooMany = MfaError.TooManyAttempts()

        val update = recoveryOnly.handleOutcome(verifying, MfaOutcome.CodeFailed(tooMany))

        assertThat(update.state).isEqualTo(MfaState.Failed(tooMany))
        assertThat(update.effect).isEqualTo(MfaEffect.Failed(tooMany))
    }

    @Test
    fun `recovery code - has no Resend, and no wait to count down`() {
        val ready = recoveryReady(BY_RECOVERY)

        assertThat(recoveryOnly.handleIntent(ready, MfaIntent.Resend)).isEqualTo(MfaUpdate(ready))
        assertThat(recoveryOnly.handleSecondPassed(ready)).isEqualTo(MfaUpdate(ready))
    }

    @Test
    fun `recovery code - verify another way and back - go to the list with a choice, else start the sign-in over`() {
        val switched = recoveryOrPush.handleIntent(recoveryReady(RECOVERY_OR_PUSH), MfaIntent.ChooseAnotherFactor)
        val alone = recoveryOnly.handleIntent(recoveryReady(BY_RECOVERY), MfaIntent.Back)
        val checking = recoveryReady(RECOVERY_OR_PUSH).copy(phase = CodePhase.Verifying)

        assertThat(switched.state).isEqualTo(MfaState.ChooseFactor(RECOVERY_OR_PUSH))
        assertThat(alone.effect).isEqualTo(MfaEffect.Restart)
        // The check can't be recalled and uses the code up, so there is no leaving while it is on its way.
        assertThat(recoveryOrPush.handleIntent(checking, MfaIntent.ChooseAnotherFactor)).isEqualTo(MfaUpdate(checking))
        assertThat(recoveryOrPush.handleIntent(checking, MfaIntent.Back)).isEqualTo(MfaUpdate(checking))
    }

    @Test
    fun `recovery code - Cancel - leaves while it is being typed, and while the check is on its way`() {
        val typing = recoveryReady(BY_RECOVERY)
        val checking = typing.copy(phase = CodePhase.Verifying)

        // The user may leave a screen that looks stuck; the check, if right, has used the code up and its answer is dropped.
        assertThat(recoveryOnly.handleIntent(typing, MfaIntent.Cancel).effect).isEqualTo(MfaEffect.Restart)
        assertThat(recoveryOnly.handleIntent(checking, MfaIntent.Cancel).effect).isEqualTo(MfaEffect.Restart)
    }

    @Test
    fun `other codes - Cancel - still leaves while they are being checked`() {
        val checking = appReady(BY_APP).copy(phase = CodePhase.Verifying)

        assertThat(appOnly.handleIntent(checking, MfaIntent.Cancel).effect).isEqualTo(MfaEffect.Restart)
    }

    @Test
    fun `new recovery code - Continue - tells the server once, and shows it is on its way`() {
        val update = recoveryOnly.handleIntent(saving(BY_RECOVERY), MfaIntent.ConfirmRecoveryCode)
        val twice = recoveryOnly.handleIntent(update.state, MfaIntent.ConfirmRecoveryCode)

        assertThat(update.state).isEqualTo(saving(BY_RECOVERY, isConfirming = true))
        assertThat(update.commands).containsExactly(MfaCommand.ConfirmRecoveryCode)
        assertThat(twice).isEqualTo(MfaUpdate(update.state))
    }

    @Test
    fun `new recovery code - confirmed by something other than its screen - is ignored`() {
        listOf(recoveryReady(BY_RECOVERY), MfaState.ChooseFactor(RECOVERY_OR_PUSH)).forEach { state ->
            assertThat(recoveryOnly.handleIntent(state, MfaIntent.ConfirmRecoveryCode)).isEqualTo(MfaUpdate(state))
        }
    }

    @Test
    fun `new recovery code - confirmed - signs in, or starts the next round when one more factor is asked for`() {
        val confirming = saving(BY_RECOVERY, isConfirming = true)
        val credentials = mockk<Credentials>()

        val signedIn = recoveryOnly.handleOutcome(confirming, MfaOutcome.SignedIn(credentials))
        val another = recoveryOnly.handleOutcome(confirming, MfaOutcome.AnotherFactor(listOf(PHONE)))

        assertThat(signedIn.effect).isEqualTo(MfaEffect.Authenticated(credentials))
        assertThat(another.state).isEqualTo(MfaState.ChooseFactor(MfaRound(2, listOf(PHONE))))
    }

    @Test
    fun `new recovery code - confirmed, then the connection drops - stays on the code, so Continue can be tapped again`() {
        val confirming = saving(BY_RECOVERY, isConfirming = true)

        val dropped = recoveryOnly.handleOutcome(confirming, MfaOutcome.CodeFailed(NETWORK))
        val again = recoveryOnly.handleIntent(dropped.state, MfaIntent.ConfirmRecoveryCode)

        assertThat(dropped.state).isEqualTo(MfaState.SaveRecoveryCode(BY_RECOVERY, NEW_CODE, requestError = NETWORK))
        assertThat(dropped.effect).isNull()
        assertThat(again.state).isEqualTo(saving(BY_RECOVERY, isConfirming = true))
        assertThat(again.commands).containsExactly(MfaCommand.ConfirmRecoveryCode)
    }

    @Test
    fun `new recovery code - confirmed, then any other failure - ends MFA`() {
        val confirming = saving(BY_RECOVERY, isConfirming = true)
        val ended = MfaError.TokenInvalid()

        val update = recoveryOnly.handleOutcome(confirming, MfaOutcome.CodeFailed(ended))

        assertThat(update.state).isEqualTo(MfaState.Failed(ended))
        assertThat(update.effect).isEqualTo(MfaEffect.Failed(ended))
    }

    @Test
    fun `new recovery code - a failure nobody waits for - is dropped`() {
        val update = recoveryOnly.handleOutcome(saving(BY_RECOVERY), MfaOutcome.CodeFailed(NETWORK))

        assertThat(update).isEqualTo(MfaUpdate(saving(BY_RECOVERY)))
    }

    @Test
    fun `new recovery code - Back and Cancel - leave, and the code is lost with them`() {
        val state = saving(RECOVERY_OR_PUSH)

        assertThat(recoveryOrPush.handleIntent(state, MfaIntent.Back).effect).isEqualTo(MfaEffect.Restart)
        assertThat(recoveryOrPush.handleIntent(state, MfaIntent.Cancel).effect).isEqualTo(MfaEffect.Restart)
    }

    @Test
    fun `new recovery code - being confirmed - Back waits for the answer, which would sign the user in, but Cancel leaves`() {
        val confirming = saving(RECOVERY_OR_PUSH, isConfirming = true)

        assertThat(recoveryOrPush.handleIntent(confirming, MfaIntent.Back)).isEqualTo(MfaUpdate(confirming))
        assertThat(recoveryOrPush.handleIntent(confirming, MfaIntent.Cancel).effect).isEqualTo(MfaEffect.Restart)
    }

    @Test
    fun `new recovery code - verify another way and everything else - do nothing, since the code is used up`() {
        listOf(saving(RECOVERY_OR_PUSH), saving(RECOVERY_OR_PUSH, isConfirming = true)).forEach { state ->
            listOf(
                MfaIntent.ChooseAnotherFactor,
                MfaIntent.Resend,
                MfaIntent.SubmitCode("123456"),
                MfaIntent.SelectFactor(PHONE),
            ).forEach { intent ->
                assertThat(recoveryOrPush.handleIntent(state, intent)).isEqualTo(MfaUpdate(state))
            }
        }
    }

    @Test
    fun `new recovery code - a late push answer asking for one more factor - still starts the next round`() {
        val update = recoveryOrPush.handleOutcome(saving(RECOVERY_OR_PUSH), MfaOutcome.AnotherFactor(listOf(TABLET)))

        assertThat(update.state).isEqualTo(MfaState.ChooseFactor(MfaRound(2, listOf(TABLET))))
        assertThat(update.commands).containsExactly(MfaCommand.CancelChallengeWork)
    }

    @Test
    fun `new recovery code - is never written out by the state or the outcome`() {
        assertThat(saving(BY_RECOVERY).toString()).doesNotContain(NEW_CODE)
        assertThat(MfaOutcome.RecoveryCodeRotated(NEW_CODE).toString()).doesNotContain(NEW_CODE)
    }

    // endregion

    // region cleaned recovery code

    @Test
    fun `cleaned - a code as typed - is left as it is, in its case, with its hyphens and inner spaces`() {
        assertThat("Abcd-1234 efgh".cleanedRecoveryCode()).isEqualTo("Abcd-1234 efgh")
        assertThat(NEW_CODE.cleanedRecoveryCode()).isEqualTo(NEW_CODE)
    }

    @Test
    fun `cleaned - spaces and line breaks around it - are removed, including a no-break space`() {
        assertThat("  ABCD-1234\n".cleanedRecoveryCode()).isEqualTo("ABCD-1234")
        assertThat(" ABCD\t".cleanedRecoveryCode()).isEqualTo("ABCD")
        assertThat("\u00A0ABCD\u00A0".cleanedRecoveryCode()).isEqualTo("ABCD")
    }

    @Test
    fun `cleaned - the invisible characters a paste brings along - are removed wherever they are`() {
        // A zero-width space, a left-to-right mark, a byte-order mark, and a language tag outside the basic plane.
        val pasted = "\u200BABCD\u200E-12\uFEFF34\uDB40\uDC01"

        assertThat(pasted.cleanedRecoveryCode()).isEqualTo("ABCD-1234")
    }

    @Test
    fun `cleaned - nothing but spaces and invisible characters - is empty`() {
        assertThat("".cleanedRecoveryCode()).isEmpty()
        assertThat(" \n\u200B\u200E \uFEFF".cleanedRecoveryCode()).isEmpty()
    }

    @Test
    fun `cleaned - letters of other scripts and emoji - are kept, since only the server can call them wrong`() {
        assertThat("абв-😀".cleanedRecoveryCode()).isEqualTo("абв-😀")
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
        resend = CodeResend(isOffered = canResend, secondsLeft = secondsLeft),
    )

    /** On the code screen for an email address right after the code went out. */
    private fun emailReady(round: MfaRound, secondsLeft: Int = MfaReducer.RESEND_COOLDOWN_SECONDS) =
        codeReady(round, secondsLeft).copy(factor = EMAIL)

    /** On the code screen of the authenticator app: nothing was sent, so there is no resend wait. */
    private fun appReady(round: MfaRound) = MfaState.CodeEntry(
        round = round,
        factor = APP,
        phase = CodePhase.Ready,
        resend = null,
    )

    /** On the screen for typing a recovery code: nothing was sent, so there is no resend wait. */
    private fun recoveryReady(round: MfaRound) = MfaState.CodeEntry(
        round = round,
        factor = RECOVERY,
        phase = CodePhase.Ready,
        resend = null,
    )

    /** On the screen that shows the new recovery code. */
    private fun saving(round: MfaRound, isConfirming: Boolean = false) =
        MfaState.SaveRecoveryCode(round, NEW_CODE, isConfirming)

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
        val APP = MfaFactor.AuthenticatorApp
        val BY_APP = MfaRound(1, listOf(APP))
        val APP_OR_PUSH = MfaRound(1, listOf(APP, PHONE))
        val RECOVERY = MfaFactor.RecoveryCode
        val BY_RECOVERY = MfaRound(1, listOf(RECOVERY))
        val RECOVERY_OR_PUSH = MfaRound(1, listOf(RECOVERY, PHONE))
        const val NEW_CODE = "YHARATFUJJKZNV5N3X1L2ZEC"
        val NETWORK = MfaError.Network(null)

        /** A resend that is going out, or was just used: not on offer. */
        val RESEND_SENT = CodeResend(isOffered = false, secondsLeft = 0)

        /** A resend the server offers, with no wait left. */
        val RESEND_OPEN = CodeResend(isOffered = true, secondsLeft = 0)

        val DENIED_CAUSE = IllegalStateException("denied")
        const val POLL = 3_000L
        const val SLOWER = 5_000L

        /** Never sent: too short or long, not digits, or six digits of another script (Arabic-Indic, fullwidth). */
        val NOT_WHOLE_CODES = listOf(
            "",
            " ",
            "12345",
            "1234567",
            "12345a",
            "12 456",
            "\u0661\u0662\u0663\u0664\u0665\u0666",
            "\uFF11\uFF12\uFF13\uFF14\uFF15\uFF16",
        )
    }
}
