package com.auth0.universalcomponents.feature.embedded.presentation.mfa

import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.junit.Test

class MfaIntentHandlerTest {

    // region start

    @Test
    fun `start - one device - sends the push straight away, showing the push screen`() {
        val update = MfaIntentHandler.handleIntent(MfaState(listOf(PHONE)), MfaIntent.Start)

        assertThat(update.state.step).isEqualTo(MfaStep.WaitForPush(PHONE, isSending = true))
        assertThat(update.command).isEqualTo(MfaCommand.SendPush(PHONE))
    }

    @Test
    fun `start - several devices - shows the list`() {
        val update = MfaIntentHandler.handleIntent(MfaState(listOf(PHONE, TABLET)), MfaIntent.Start)

        assertThat(update.state.step).isEqualTo(MfaStep.ChooseFactor)
        assertThat(update.command).isNull()
    }

    @Test
    fun `start - again after a rotation - keeps the user's place`() {
        val started = MfaState(listOf(PHONE, TABLET), MfaStep.WaitForPush(TABLET, POLL), isStarted = true)

        assertThat(MfaIntentHandler.handleIntent(started, MfaIntent.Start)).isEqualTo(MfaUpdate(started))
    }

    @Test
    fun `start - no factors - fails as unsupported`() {
        val update = MfaIntentHandler.handleIntent(MfaState(emptyList()), MfaIntent.Start)

        assertThat(update.state.step).isEqualTo(MfaStep.Failed(EmbeddedLoginError.ExtraStepUnsupported))
    }

    // endregion

    // region push

    @Test
    fun `select - a device from the list - sends its push, and Back waits while it's sent`() {
        val list = MfaState(listOf(PHONE, TABLET), MfaStep.ChooseFactor, isStarted = true)

        val selected = MfaIntentHandler.handleIntent(list, MfaIntent.SelectFactor(TABLET))

        assertThat(selected.state.step).isEqualTo(MfaStep.WaitForPush(TABLET, isSending = true))
        assertThat(selected.command).isEqualTo(MfaCommand.SendPush(TABLET))
        assertThat(MfaIntentHandler.handleIntent(selected.state, MfaIntent.Back)).isEqualTo(MfaUpdate(selected.state))
    }

    @Test
    fun `sent, then pending - checks again after the server's wait`() {
        val sending = MfaState(listOf(PHONE), MfaStep.WaitForPush(PHONE, isSending = true), isStarted = true)

        val sent = MfaIntentHandler.handleResult(sending, MfaResult.PushSent(POLL))
        val pending = MfaIntentHandler.handleResult(sent.state, MfaResult.PushPending(SLOWER))
        val pendingNoWait = MfaIntentHandler.handleResult(pending.state, MfaResult.PushPending(null))

        assertThat(sent.state.step).isEqualTo(MfaStep.WaitForPush(PHONE, POLL))
        assertThat(sent.command).isEqualTo(MfaCommand.CheckPush(POLL))
        assertThat(pending.command).isEqualTo(MfaCommand.CheckPush(SLOWER))
        assertThat(pendingNoWait.command).isEqualTo(MfaCommand.CheckPush(SLOWER))
    }

    @Test
    fun `approved - hands over the credentials`() {
        val credentials = mockk<Credentials>()
        val waiting = MfaState(listOf(PHONE), MfaStep.WaitForPush(PHONE, POLL), isStarted = true)

        val update = MfaIntentHandler.handleResult(waiting, MfaResult.SignedIn(credentials))

        assertThat(update.state.step).isEqualTo(MfaStep.Authenticated)
        assertThat(update.effect).isEqualTo(MfaEffect.Authenticated(credentials))
    }

    @Test
    fun `back with other devices - stops checking and shows the list`() {
        val waiting = MfaState(listOf(PHONE, TABLET), MfaStep.WaitForPush(PHONE, POLL), isStarted = true)

        val update = MfaIntentHandler.handleIntent(waiting, MfaIntent.Back)

        assertThat(update.state.step).isEqualTo(MfaStep.ChooseFactor)
        assertThat(update.command).isEqualTo(MfaCommand.StopPush)
    }

    @Test
    fun `back with only one device - stops checking and starts the sign-in over`() {
        val waiting = MfaState(listOf(PHONE), MfaStep.WaitForPush(PHONE, POLL), isStarted = true)

        val update = MfaIntentHandler.handleIntent(waiting, MfaIntent.Back)

        assertThat(update.command).isEqualTo(MfaCommand.StopPush)
        assertThat(update.effect).isEqualTo(MfaEffect.Restart)
    }

    @Test
    fun `pending after the user left - stops`() {
        val list = MfaState(listOf(PHONE, TABLET), MfaStep.ChooseFactor, isStarted = true)

        assertThat(MfaIntentHandler.handleResult(list, MfaResult.PushPending(POLL)).command).isNull()
    }

    @Test
    fun `denied - fails for good, and Try again starts the sign-in over`() {
        val waiting = MfaState(listOf(PHONE), MfaStep.WaitForPush(PHONE, POLL), isStarted = true)

        val denied = MfaResult.Failed(LoginFailure(EmbeddedLoginError.PushDenied))
        val failed = MfaIntentHandler.handleResult(waiting, denied)
        val retried = MfaIntentHandler.handleIntent(failed.state, MfaIntent.Retry)

        assertThat(failed.state.step).isEqualTo(MfaStep.Failed(EmbeddedLoginError.PushDenied))
        assertThat(retried.effect).isEqualTo(MfaEffect.Restart)
    }

    @Test
    fun `network error - Try again sends the push again`() {
        val waiting = MfaState(listOf(PHONE), MfaStep.WaitForPush(PHONE, POLL), isStarted = true)
        val network = LoginFailure(EmbeddedLoginError.Network, canRetryStep = true)

        val failed = MfaIntentHandler.handleResult(waiting, MfaResult.Failed(network))
        val retried = MfaIntentHandler.handleIntent(failed.state, MfaIntent.Retry)

        assertThat(retried.state.step).isEqualTo(MfaStep.WaitForPush(PHONE, isSending = true))
        assertThat(retried.command).isEqualTo(MfaCommand.SendPush(PHONE))
    }

    // endregion

    // region back

    @Test
    fun `back - from the list or an error - starts the sign-in over`() {
        val list = MfaState(listOf(PHONE, TABLET), MfaStep.ChooseFactor, isStarted = true)
        val failed = MfaState(listOf(PHONE), MfaStep.Failed(EmbeddedLoginError.PushDenied), isStarted = true)

        assertThat(MfaIntentHandler.handleIntent(list, MfaIntent.Back).effect).isEqualTo(MfaEffect.Restart)
        assertThat(MfaIntentHandler.handleIntent(failed, MfaIntent.Back).effect).isEqualTo(MfaEffect.Restart)
    }

    // endregion

    private companion object {
        val PHONE = MfaFactor.Push(0, "Jane's phone")
        val TABLET = MfaFactor.Push(1, "Jane's tablet")
        const val POLL = 3_000L
        const val SLOWER = 5_000L
    }
}
