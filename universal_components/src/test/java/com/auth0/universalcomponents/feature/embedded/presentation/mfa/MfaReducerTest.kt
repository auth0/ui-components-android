package com.auth0.universalcomponents.feature.embedded.presentation.mfa

import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor
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
        val denied = MfaOutcome.Failed(LoginFailure(EmbeddedLoginError.PushDenied))

        val failed = oneDevice.handleOutcome(polling(PHONE, canChooseAnother = false), denied)
        val retried = oneDevice.handleIntent(failed.step, MfaIntent.Retry)

        assertThat(failed.step).isEqualTo(MfaStep.Failed(EmbeddedLoginError.PushDenied))
        assertThat(retried.effect).isEqualTo(MfaEffect.Restart)
    }

    @Test
    fun `network error - Try again sends the push again`() {
        val network = MfaOutcome.Failed(LoginFailure(EmbeddedLoginError.Network, canRetryStep = true))

        val failed = oneDevice.handleOutcome(polling(PHONE, canChooseAnother = false), network)
        val retried = oneDevice.handleIntent(failed.step, MfaIntent.Retry)

        assertThat(retried.step).isEqualTo(MfaStep.PushWaiting(PHONE, PushPhase.Sending, canChooseAnother = false))
        assertThat(retried.commands).containsExactly(MfaCommand.SendPush(PHONE))
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
    }
}
