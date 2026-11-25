package com.auth0.universalcomponents.feature.embedded.signup.presentation

import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupChallenge
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupError
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupFailure
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupIdentifier
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupKind
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupNext
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupProgress
import com.auth0.universalcomponents.feature.embedded.signup.domain.repository.EmbeddedSignupRepository
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
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

@OptIn(ExperimentalCoroutinesApi::class)
class SignupViewModelTest {

    private lateinit var repository: EmbeddedSignupRepository
    private lateinit var viewModel: SignupViewModel

    private val testDispatcher = StandardTestDispatcher()
    private val credentials = mockk<Credentials>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        coEvery { repository.open() } returns Result.Success(SignupNext.AskFor(SignupKind.Email))
        coEvery { repository.identify(IDENTIFIER) } returns Result.Success(CHALLENGE)
        coEvery { repository.sendCode(CHALLENGE) } returns Result.Success(Unit)
        coEvery { repository.verify(CODE) } returns
            Result.Success(SignupProgress.Continue(SignupNext.ReadyToCreate))
        coEvery { repository.createAccount() } returns Result.Success(credentials)
        viewModel = SignupViewModel(repository, timeSource = VirtualTimeSource(testDispatcher.scheduler))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `initialization - nothing has run`() = runTest {
        assertThat(viewModel.state.value).isEqualTo(SignupState.Idle)
        coVerify(exactly = 0) { repository.open() }
    }

    @Test
    fun `start - opens the sign-up and asks for the email`() = runTest {
        viewModel.send(SignupIntent.Start)
        advanceUntilIdle()

        assertThat(viewModel.state.value).isEqualTo(SignupState.Entry(SignupKind.Email))
    }

    @Test
    fun `start - twice - opens once`() = runTest {
        viewModel.send(SignupIntent.Start)
        viewModel.send(SignupIntent.Start)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.open() }
    }

    @Test
    fun `a new account - email, code, then the account - signs in once, with the credentials`() = runTest {
        val effects = collectEffects()

        startAtEntry()
        viewModel.send(SignupIntent.SubmitIdentifier(IDENTIFIER))
        advanceUntilIdle()
        assertThat(viewModel.state.value).isInstanceOf(SignupState.Code::class.java)
        viewModel.send(SignupIntent.SubmitCode(CODE))
        advanceUntilIdle()

        assertThat(viewModel.state.value).isEqualTo(SignupState.Authenticated)
        assertThat(effects).containsExactly(SignupEffect.Authenticated(credentials))
        coVerify(exactly = 1) { repository.createAccount() }
    }

    @Test
    fun `an email that already has an account - the code signs that user in, with no account created`() = runTest {
        coEvery { repository.verify(CODE) } returns Result.Success(SignupProgress.SignedIn(credentials))
        val effects = collectEffects()

        startAtCode()
        viewModel.send(SignupIntent.SubmitCode(CODE))
        advanceUntilIdle()

        assertThat(effects).containsExactly(SignupEffect.Authenticated(credentials))
        coVerify(exactly = 0) { repository.createAccount() }
    }

    @Test
    fun `an email the server refuses - stays on the entry with the message, and sends no code`() = runTest {
        coEvery { repository.identify(IDENTIFIER) } returns failure(SignupError.InvalidEmail)

        startAtEntry()
        viewModel.send(SignupIntent.SubmitIdentifier(IDENTIFIER))
        advanceUntilIdle()

        assertThat(viewModel.state.value)
            .isEqualTo(SignupState.Entry(SignupKind.Email, ADDRESS, error = SignupError.InvalidEmail))
        coVerify(exactly = 0) { repository.sendCode(any()) }
    }

    @Test
    fun `a wrong code - stays on the code screen with the message`() = runTest {
        coEvery { repository.verify(CODE) } returns failure(SignupError.InvalidCode)

        startAtCode()
        viewModel.send(SignupIntent.SubmitCode(CODE))
        advanceUntilIdle()

        val code = viewModel.state.value as SignupState.Code
        assertThat(code.error).isEqualTo(SignupError.InvalidCode)
        assertThat(code.phase).isEqualTo(CodePhase.Ready)
    }

    @Test
    fun `the connection drops while the account is created - try again creates it without the code`() = runTest {
        coEvery { repository.createAccount() } returnsMany listOf(
            failure(SignupError.Network),
            Result.Success(credentials),
        )
        val effects = collectEffects()

        startAtCode()
        viewModel.send(SignupIntent.SubmitCode(CODE))
        advanceUntilIdle()
        assertThat(viewModel.state.value)
            .isEqualTo(SignupState.Failed(SignupError.Network, SignupRetry.CreateAccount))
        viewModel.send(SignupIntent.Retry)
        advanceUntilIdle()

        assertThat(viewModel.state.value).isEqualTo(SignupState.Authenticated)
        assertThat(effects).containsExactly(SignupEffect.Authenticated(credentials))
        coVerify(exactly = 1) { repository.verify(CODE) }
        coVerify(exactly = 2) { repository.createAccount() }
    }

    @Test
    fun `no sign-up offered - says it is unavailable`() = runTest {
        coEvery { repository.open() } returns failure(SignupError.Unavailable)

        viewModel.send(SignupIntent.Start)
        advanceUntilIdle()

        assertThat(viewModel.state.value).isEqualTo(SignupState.Unavailable)
    }

    @Test
    fun `try again after the sign-up opens badly - opens a new one`() = runTest {
        coEvery { repository.open() } returnsMany listOf(
            failure(SignupError.Network),
            Result.Success(SignupNext.AskFor(SignupKind.Email)),
        )

        viewModel.send(SignupIntent.Start)
        advanceUntilIdle()
        viewModel.send(SignupIntent.Retry)
        advanceUntilIdle()

        assertThat(viewModel.state.value).isEqualTo(SignupState.Entry(SignupKind.Email))
        coVerify(exactly = 2) { repository.open() }
    }

    @Test
    fun `back from the code screen - returns to the entry with the email, sending nothing`() = runTest {
        startAtCode()

        viewModel.send(SignupIntent.Back)
        advanceUntilIdle()

        assertThat(viewModel.state.value).isEqualTo(SignupState.Entry(SignupKind.Email, ADDRESS))
        coVerify(exactly = 1) { repository.sendCode(CHALLENGE) }
    }

    @Test
    fun `back from the entry - tells the host to leave, once`() = runTest {
        val effects = collectEffects()
        startAtEntry()

        viewModel.send(SignupIntent.Back)
        advanceUntilIdle()

        assertThat(effects).containsExactly(SignupEffect.Exit)
    }

    @Test
    fun `leave then start - opens a new sign-up`() = runTest {
        startAtCode()

        viewModel.send(SignupIntent.Leave)
        assertThat(viewModel.state.value).isEqualTo(SignupState.Idle)
        viewModel.send(SignupIntent.Start)
        advanceUntilIdle()

        assertThat(viewModel.state.value).isEqualTo(SignupState.Entry(SignupKind.Email))
        coVerify(exactly = 2) { repository.open() }
    }

    @Test
    fun `leave while a call is on its way - its answer is not heard`() = runTest {
        val answer = CompletableDeferred<Result<SignupNext, SignupFailure>>()
        coEvery { repository.open() } coAnswers { answer.await() }
        viewModel.send(SignupIntent.Start)
        runCurrent()

        viewModel.send(SignupIntent.Leave)
        answer.complete(Result.Success(SignupNext.AskFor(SignupKind.Email)))
        advanceUntilIdle()

        assertThat(viewModel.state.value).isEqualTo(SignupState.Idle)
    }

    @Test
    fun `the wait before another code - counts down, then opens resend, which sends the code again`() = runTest {
        startAtCode(untilIdle = false)

        viewModel.send(SignupIntent.Resend)
        runCurrent()
        assertThat((viewModel.state.value as SignupState.Code).phase).isEqualTo(CodePhase.Ready)
        advanceTimeBy(RESEND_WAIT_SECONDS * 1_000L)
        runCurrent()
        assertThat((viewModel.state.value as SignupState.Code).resendSecondsLeft).isEqualTo(0)
        viewModel.send(SignupIntent.Resend)
        runCurrent()

        coVerify(exactly = 2) { repository.sendCode(CHALLENGE) }
        assertThat((viewModel.state.value as SignupState.Code).resendSecondsLeft).isEqualTo(RESEND_WAIT_SECONDS)
    }

    @Test
    fun `the wait - the device sleeps through it - is made up for on the next tick`() = runTest {
        val clock = VirtualTimeSource(testDispatcher.scheduler)
        viewModel = SignupViewModel(repository, timeSource = clock)
        startAtCode()

        clock.sleep((RESEND_WAIT_SECONDS + 5) * 1_000L)
        advanceTimeBy(1_000L)
        runCurrent()

        assertThat((viewModel.state.value as SignupState.Code).resendSecondsLeft).isEqualTo(0)
    }

    private suspend fun kotlinx.coroutines.test.TestScope.startAtEntry() {
        viewModel.send(SignupIntent.Start)
        advanceUntilIdle()
    }

    /** [untilIdle] false stops right after the code is out, with the wait still counting. */
    private suspend fun kotlinx.coroutines.test.TestScope.startAtCode(untilIdle: Boolean = true) {
        startAtEntry()
        viewModel.send(SignupIntent.SubmitIdentifier(IDENTIFIER))
        if (untilIdle) advanceUntilIdle() else runCurrent()
    }

    /** Collects what the host would be told, from now on. */
    private fun kotlinx.coroutines.test.TestScope.collectEffects(): MutableList<SignupEffect> {
        val effects = mutableListOf<SignupEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.toList(effects) }
        return effects
    }

    private fun failure(error: SignupError) = Result.Error(SignupFailure(error))

    private companion object {
        const val ADDRESS = "jane@example.com"
        const val CODE = "123456"
        val IDENTIFIER = SignupIdentifier(SignupKind.Email, ADDRESS)
        val CHALLENGE = SignupChallenge(index = 1)
    }
}

/** A clock that follows the test's virtual time, and can be pushed ahead to stand for a device that slept. */
@OptIn(ExperimentalCoroutinesApi::class)
private class VirtualTimeSource(private val scheduler: TestCoroutineScheduler) : TimeSource {

    private var sleptMillis = 0L

    fun sleep(millis: Long) {
        sleptMillis += millis
    }

    private fun nowMillis() = scheduler.currentTime + sleptMillis

    override fun markNow(): TimeMark = VirtualMark(nowMillis())

    private inner class VirtualMark(private val startedAtMillis: Long) : TimeMark {
        override fun elapsedNow(): Duration = (nowMillis() - startedAtMillis).milliseconds
    }
}
