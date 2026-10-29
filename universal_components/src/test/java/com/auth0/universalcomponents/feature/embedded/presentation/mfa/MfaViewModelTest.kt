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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * The push-check timing the SDK asks for: one step at a time, a check in flight is never cut off,
 * and its answer is dropped once the user has left. Each wait is moved through by hand, so every
 * test can check what happened just before and just after it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MfaViewModelTest {

    private lateinit var repository: MfaRepository

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        coEvery { repository.sendPush(any()) } returns Result.Success(POLL)
        // Approve by default: runTest runs what is left at the end, and a push that stays pending is checked forever.
        coEvery { repository.checkPush() } returns Result.Success(PushStatus.Approved(mockk()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    // region checking

    @Test
    fun `push sent - first check waits the server's interval`() = runTest {
        val viewModel = started(PHONE)

        assertThat(viewModel.state.value.step).isEqualTo(MfaStep.WaitForPush(PHONE, POLL, resendSecondsLeft = COOLDOWN))
        waitFor(POLL - 1)
        coVerify(exactly = 0) { repository.checkPush() }
        waitFor(1)
        coVerify(exactly = 1) { repository.checkPush() }
    }

    @Test
    fun `still pending - next check waits the new interval the server gave`() = runTest {
        coEvery { repository.checkPush() } returnsMany listOf(
            Result.Success(PushStatus.Pending(SLOWER)),
            Result.Success(PushStatus.Approved(mockk())),
        )
        val viewModel = started(PHONE)

        waitFor(POLL)
        val secondsWaited = (POLL / ONE_SECOND).toInt()
        assertThat(viewModel.state.value.step)
            .isEqualTo(MfaStep.WaitForPush(PHONE, SLOWER, resendSecondsLeft = COOLDOWN - secondsWaited))
        waitFor(SLOWER - 1)
        coVerify(exactly = 1) { repository.checkPush() }
        waitFor(1)
        coVerify(exactly = 2) { repository.checkPush() }
    }

    @Test
    fun `approved - signs in and hands over the credentials`() = runTest {
        val credentials = mockk<Credentials>()
        coEvery { repository.checkPush() } returns Result.Success(PushStatus.Approved(credentials))
        val viewModel = started(PHONE)
        val effects = collectEffects(viewModel)

        waitFor(POLL)

        assertThat(viewModel.state.value.step).isEqualTo(MfaStep.Authenticated)
        assertThat(effects).containsExactly(MfaEffect.Authenticated(credentials))
    }

    @Test
    fun `sending fails - shows the error and checks nothing`() = runTest {
        coEvery { repository.sendPush(PHONE) } returns Result.Error(LoginFailure(EmbeddedLoginError.Network))
        val viewModel = started(PHONE)

        waitFor(POLL * 2)

        assertThat(viewModel.state.value.step).isEqualTo(MfaStep.Failed(EmbeddedLoginError.Network))
        coVerify(exactly = 0) { repository.checkPush() }
    }

    // endregion

    // region resend

    @Test
    fun `resend - counts down a second at a time, then sends the push to the same device again`() = runTest {
        var pushesSent = 0
        coEvery { repository.sendPush(PHONE) } coAnswers {
            pushesSent++
            Result.Success(POLL)
        }
        // Still waiting until the resent push is checked, so the countdown has time to run out.
        coEvery { repository.checkPush() } coAnswers {
            Result.Success(if (pushesSent < 2) PushStatus.Pending(null) else PushStatus.Approved(mockk()))
        }
        val viewModel = started(PHONE)

        waitFor(ONE_SECOND)
        assertThat(viewModel.resendSecondsLeft()).isEqualTo(COOLDOWN - 1)
        viewModel.onIntent(MfaIntent.Resend)
        runCurrent()
        assertThat(pushesSent).isEqualTo(1)

        waitFor(ONE_SECOND * (COOLDOWN - 1))
        assertThat(viewModel.resendSecondsLeft()).isEqualTo(0)
        viewModel.onIntent(MfaIntent.Resend)
        runCurrent()

        assertThat(pushesSent).isEqualTo(2)
        assertThat(viewModel.resendSecondsLeft()).isEqualTo(COOLDOWN)
    }

    // endregion

    // region leaving

    @Test
    fun `left during the wait - the check never runs`() = runTest {
        val viewModel = started(PHONE, TABLET, select = PHONE)

        viewModel.onIntent(MfaIntent.Back)
        waitFor(POLL * 2)

        assertThat(viewModel.state.value.step).isEqualTo(MfaStep.ChooseFactor)
        coVerify(exactly = 0) { repository.checkPush() }
    }

    // That a check in flight still finishes, and that the next step waits for it, is EmbeddedAuthStepsTest's job.
    @Test
    fun `left while a check is in flight - its answer never reaches the screen`() = runTest {
        val answer = CompletableDeferred<Result<PushStatus, LoginFailure>>()
        coEvery { repository.checkPush() } coAnswers { answer.await() }
        val viewModel = started(PHONE, TABLET, select = PHONE)
        val effects = collectEffects(viewModel)
        waitFor(POLL)

        viewModel.onIntent(MfaIntent.Back)
        runCurrent()
        answer.complete(Result.Success(PushStatus.Approved(mockk())))
        runCurrent()

        assertThat(viewModel.state.value.step).isEqualTo(MfaStep.ChooseFactor)
        assertThat(effects).isEmpty()
    }

    // endregion

    /** Starts MFA and lets the push for [select] go out; with one factor it's picked straight away. */
    private fun TestScope.started(vararg factors: MfaFactor.Push, select: MfaFactor.Push? = null): MfaViewModel {
        val viewModel = MfaViewModel(factors.toList(), repository)
        viewModel.onIntent(MfaIntent.Start)
        if (select != null) viewModel.onIntent(MfaIntent.SelectFactor(select))
        runCurrent()
        return viewModel
    }

    private fun TestScope.collectEffects(viewModel: MfaViewModel): List<MfaEffect> {
        val effects = mutableListOf<MfaEffect>()
        backgroundScope.launch(testDispatcher) { viewModel.effects.toList(effects) }
        runCurrent()
        return effects
    }

    /** Moves the clock and runs whatever became due, including work due exactly at the end. */
    private fun TestScope.waitFor(millis: Long) {
        advanceTimeBy(millis)
        runCurrent()
    }

    private fun MfaViewModel.resendSecondsLeft(): Int =
        (state.value.step as MfaStep.WaitForPush).resendSecondsLeft

    private companion object {
        val PHONE = MfaFactor.Push(0, "Jane's phone")
        val TABLET = MfaFactor.Push(1, "Jane's tablet")
        const val POLL = 3_000L
        const val SLOWER = 5_000L
        const val COOLDOWN = MfaIntentHandler.RESEND_COOLDOWN_SECONDS
        const val ONE_SECOND = 1_000L
    }
}
