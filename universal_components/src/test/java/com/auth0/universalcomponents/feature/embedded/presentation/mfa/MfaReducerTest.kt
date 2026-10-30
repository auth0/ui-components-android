package com.auth0.universalcomponents.feature.embedded.presentation.mfa

import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.embedded.domain.model.PushPolling
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.junit.Test

class MfaReducerTest {

    private val oneDevice = MfaReducer(listOf(PHONE))
    private val twoDevices = MfaReducer(listOf(PHONE, TABLET))

    // region start

    @Test
    fun `start - one device - sends the push straight away, showing the push screen`() {
        val update = oneDevice.start()

        assertThat(update.step).isEqualTo(MfaStep.PushWaiting(PHONE, PushPhase.Sending, canChooseAnother = false))
        assertThat(update.commands).containsExactly(MfaCommand.SendPush(PHONE))
    }

    @Test
    fun `start - several devices - shows the list`() {
        val update = twoDevices.start()

        assertThat(update.step).isEqualTo(MfaStep.AuthenticatorList(listOf(PHONE, TABLET)))
        assertThat(update.commands).isEmpty()
    }

    @Test
    fun `start - no factors - fails as unsupported`() {
        assertThat(MfaReducer(emptyList()).start().step)
            .isEqualTo(MfaStep.Failed(EmbeddedLoginError.ExtraStepUnsupported))
    }

    // endregion

    // region push

    @Test
    fun `select - a device from the list - sends its push, and Back waits while it's sent`() {
        val selected = twoDevices.handleIntent(LIST, MfaIntent.SelectFactor(TABLET))

        assertThat(selected.step).isEqualTo(MfaStep.PushWaiting(TABLET, PushPhase.Sending, canChooseAnother = true))
        assertThat(selected.commands).containsExactly(MfaCommand.SendPush(TABLET))
        assertThat(twoDevices.handleIntent(selected.step, MfaIntent.Back)).isEqualTo(MfaUpdate(selected.step))
    }

    @Test
    fun `sent, then pending - checks again after the server's wait`() {
        val sending = MfaStep.PushWaiting(PHONE, PushPhase.Sending, canChooseAnother = false)

        val sent = oneDevice.handleOutcome(sending, MfaOutcome.PushSent(POLL))
        val pending = oneDevice.handleOutcome(sent.step, MfaOutcome.PushPending(SLOWER))
        val pendingNoWait = oneDevice.handleOutcome(pending.step, MfaOutcome.PushPending(null))

        assertThat(sent.step).isEqualTo(polling(PHONE, POLL, COOLDOWN, canChooseAnother = false))
        assertThat(sent.commands).containsExactly(MfaCommand.CheckPush(POLL), MfaCommand.CountDownResend).inOrder()
        assertThat(pending.commands).containsExactly(MfaCommand.CheckPush(SLOWER))
        assertThat(pendingNoWait.commands).containsExactly(MfaCommand.CheckPush(SLOWER))
    }

    @Test
    fun `approved - hands over the credentials, and keeps no success step of its own`() {
        val credentials = mockk<Credentials>()
        val waiting = polling(PHONE, canChooseAnother = false)

        val update = oneDevice.handleOutcome(waiting, MfaOutcome.SignedIn(credentials))

        assertThat(update.step).isEqualTo(waiting)
        assertThat(update.commands).isEmpty()
        assertThat(update.effect).isEqualTo(MfaEffect.Authenticated(credentials))
    }

    @Test
    fun `choose another device - stops checking and shows the list`() {
        val update = twoDevices.handleIntent(polling(PHONE, canChooseAnother = true), MfaIntent.ChooseAnotherFactor)

        assertThat(update.step).isEqualTo(LIST)
        assertThat(update.commands).containsExactly(MfaCommand.StopPush)
    }

    @Test
    fun `choose another device - with only one, or while the push is sent - does nothing`() {
        val alone = polling(PHONE, canChooseAnother = false)
        val sending = MfaStep.PushWaiting(PHONE, PushPhase.Sending, canChooseAnother = true)

        assertThat(oneDevice.handleIntent(alone, MfaIntent.ChooseAnotherFactor)).isEqualTo(MfaUpdate(alone))
        assertThat(twoDevices.handleIntent(sending, MfaIntent.ChooseAnotherFactor)).isEqualTo(MfaUpdate(sending))
    }

    @Test
    fun `back with other devices - stops checking and shows the list`() {
        val update = twoDevices.handleIntent(polling(PHONE, canChooseAnother = true), MfaIntent.Back)

        assertThat(update.step).isEqualTo(LIST)
        assertThat(update.commands).containsExactly(MfaCommand.StopPush)
    }

    @Test
    fun `back with only one device - stops checking and starts the sign-in over`() {
        val update = oneDevice.handleIntent(polling(PHONE, canChooseAnother = false), MfaIntent.Back)

        assertThat(update.commands).containsExactly(MfaCommand.StopPush)
        assertThat(update.effect).isEqualTo(MfaEffect.Restart)
    }

    @Test
    fun `pending after the user left - stops`() {
        assertThat(twoDevices.handleOutcome(LIST, MfaOutcome.PushPending(POLL)).commands).isEmpty()
    }

    @Test
    fun `denied - fails for good, and Try again starts the sign-in over`() {
        val denied = MfaOutcome.CheckFailed(LoginFailure(EmbeddedLoginError.PushDenied))

        val failed = oneDevice.handleOutcome(polling(PHONE, canChooseAnother = false), denied)
        val retried = oneDevice.handleIntent(failed.step, MfaIntent.Retry)

        assertThat(failed.step).isEqualTo(MfaStep.Failed(EmbeddedLoginError.PushDenied))
        assertThat(retried.effect).isEqualTo(MfaEffect.Restart)
    }

    @Test
    fun `sending fails on the network - Try again sends the push again`() {
        val sending = MfaStep.PushWaiting(PHONE, PushPhase.Sending, canChooseAnother = false)

        val failed = oneDevice.handleOutcome(sending, MfaOutcome.SendFailed(NETWORK))
        val retried = oneDevice.handleIntent(failed.step, MfaIntent.Retry)

        assertThat(failed.step).isEqualTo(MfaStep.Failed(EmbeddedLoginError.Network, MfaRetry.SendAgain(PHONE)))
        assertThat(retried.step).isEqualTo(sending)
        assertThat(retried.commands).containsExactly(MfaCommand.SendPush(PHONE))
    }

    @Test
    fun `a check fails on the network - checks again quietly, waiting longer each time`() {
        var step: MfaStep = polling(PHONE, everyMs = POLL, canChooseAnother = false)
        val waits = (1..MfaReducer.QUIET_RETRIES).map {
            val update = oneDevice.handleOutcome(step, MfaOutcome.CheckFailed(NETWORK))
            step = update.step
            assertThat(step).isInstanceOf(MfaStep.PushWaiting::class.java)
            (update.commands.single() as MfaCommand.CheckPush).afterMs
        }

        assertThat(waits).containsExactly(POLL * 2, POLL * 4, POLL * 8).inOrder()
    }

    @Test
    fun `a check keeps failing on the network - shows the error, and Try again checks again without a second push`() {
        val waiting = polling(PHONE, resendSecondsLeft = 10, canChooseAnother = false)
        val failing = MfaStep.PushWaiting(
            PHONE,
            PushPhase.Polling(POLL, resendSecondsLeft = 10, failedChecks = MfaReducer.QUIET_RETRIES),
            canChooseAnother = false,
        )

        val failed = oneDevice.handleOutcome(failing, MfaOutcome.CheckFailed(NETWORK))
        val retried = oneDevice.handleIntent(failed.step, MfaIntent.Retry)

        assertThat(failed.step).isEqualTo(MfaStep.Failed(EmbeddedLoginError.Network, MfaRetry.CheckAgain(waiting)))
        assertThat(retried.step).isEqualTo(waiting)
        assertThat(retried.commands).containsExactly(MfaCommand.CheckPush(0), MfaCommand.CountDownResend).inOrder()
    }

    @Test
    fun `an answer after a failed check - starts the quiet retries afresh`() {
        val retried = MfaStep.PushWaiting(PHONE, PushPhase.Polling(POLL, 0, failedChecks = 2), canChooseAnother = false)

        val pending = oneDevice.handleOutcome(retried, MfaOutcome.PushPending(POLL))

        assertThat(pending.step).isEqualTo(polling(PHONE, canChooseAnother = false))
    }

    @Test
    fun `slow down without a new wait - doubles the wait, up to a minute`() {
        val slowDown = MfaOutcome.PushPending(pollInMs = null, isSlowDown = true)

        val doubled = oneDevice.handleOutcome(polling(PHONE, everyMs = POLL, canChooseAnother = false), slowDown)
        val capped = oneDevice.handleOutcome(polling(PHONE, everyMs = 50_000L, canChooseAnother = false), slowDown)

        assertThat(doubled.commands).containsExactly(MfaCommand.CheckPush(POLL * 2))
        assertThat(capped.commands).containsExactly(MfaCommand.CheckPush(PushPolling.MAX_WAIT_MS))
    }

    @Test
    fun `answers that no longer fit the screen - are dropped, except an approval`() {
        val credentials = mockk<Credentials>()
        val sending = MfaStep.PushWaiting(TABLET, PushPhase.Sending, canChooseAnother = true)

        assertThat(twoDevices.handleOutcome(LIST, MfaOutcome.CheckFailed(NETWORK))).isEqualTo(MfaUpdate(LIST))
        assertThat(twoDevices.handleOutcome(sending, MfaOutcome.CheckFailed(NETWORK))).isEqualTo(MfaUpdate(sending))
        assertThat(twoDevices.handleOutcome(LIST, MfaOutcome.SendFailed(NETWORK))).isEqualTo(MfaUpdate(LIST))
        // The user did approve, on the device they moved away from: they're signed in.
        assertThat(twoDevices.handleOutcome(LIST, MfaOutcome.SignedIn(credentials)).effect)
            .isEqualTo(MfaEffect.Authenticated(credentials))
    }

    // endregion

    // region resend

    @Test
    fun `resend - wait not over - does nothing`() {
        val waiting = polling(PHONE, resendSecondsLeft = 1, canChooseAnother = false)

        assertThat(oneDevice.handleIntent(waiting, MfaIntent.Resend)).isEqualTo(MfaUpdate(waiting))
    }

    @Test
    fun `resend - while the push is being sent - does nothing`() {
        val sending = MfaStep.PushWaiting(PHONE, PushPhase.Sending, canChooseAnother = false)

        assertThat(oneDevice.handleIntent(sending, MfaIntent.Resend)).isEqualTo(MfaUpdate(sending))
    }

    @Test
    fun `resend - wait over - sends the push to the same device again`() {
        val update = twoDevices.handleIntent(polling(TABLET, canChooseAnother = true), MfaIntent.Resend)

        assertThat(update.step).isEqualTo(MfaStep.PushWaiting(TABLET, PushPhase.Sending, canChooseAnother = true))
        assertThat(update.commands).containsExactly(MfaCommand.SendPush(TABLET))
    }

    @Test
    fun `second passed - counts down, and stops at zero`() {
        val waiting = polling(PHONE, resendSecondsLeft = 2, canChooseAnother = false)

        val one = oneDevice.handleSecondPassed(waiting)
        val zero = oneDevice.handleSecondPassed(one.step)

        assertThat(one.step).isEqualTo(polling(PHONE, resendSecondsLeft = 1, canChooseAnother = false))
        assertThat(one.commands).containsExactly(MfaCommand.CountDownResend)
        assertThat(zero.step).isEqualTo(polling(PHONE, resendSecondsLeft = 0, canChooseAnother = false))
        assertThat(zero.commands).isEmpty()
    }

    @Test
    fun `second passed - after the push screen was left - stops`() {
        assertThat(twoDevices.handleSecondPassed(LIST)).isEqualTo(MfaUpdate(LIST))
    }

    // endregion

    // region back

    @Test
    fun `back - from the list or an error - starts the sign-in over`() {
        val failed = MfaStep.Failed(EmbeddedLoginError.PushDenied)

        assertThat(twoDevices.handleIntent(LIST, MfaIntent.Back).effect).isEqualTo(MfaEffect.Restart)
        assertThat(oneDevice.handleIntent(failed, MfaIntent.Back).effect).isEqualTo(MfaEffect.Restart)
    }

    // endregion

    private fun polling(
        factor: MfaFactor.Push,
        everyMs: Long = POLL,
        resendSecondsLeft: Int = 0,
        canChooseAnother: Boolean,
    ) = MfaStep.PushWaiting(factor, PushPhase.Polling(everyMs, resendSecondsLeft), canChooseAnother)

    private companion object {
        val PHONE = MfaFactor.Push(0, "Jane's phone")
        val TABLET = MfaFactor.Push(1, "Jane's tablet")
        val LIST = MfaStep.AuthenticatorList(listOf(PHONE, TABLET))
        const val POLL = 3_000L
        const val SLOWER = 5_000L
        const val COOLDOWN = MfaReducer.RESEND_COOLDOWN_SECONDS
        val NETWORK = LoginFailure(EmbeddedLoginError.Network, canRetryStep = true)
    }
}
