package com.auth0.universalcomponents.feature.embedded.presentation

import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption
import com.auth0.universalcomponents.feature.embedded.domain.repository.EmbeddedLoginRepository
import com.auth0.universalcomponents.feature.mfachallenge.MfaError
import com.auth0.universalcomponents.feature.mfachallenge.MfaErrorHandling
import com.auth0.universalcomponents.feature.mfachallenge.MfaResult
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.PushStatus
import com.auth0.universalcomponents.feature.mfachallenge.domain.repository.MfaRepository
import com.auth0.universalcomponents.feature.mfachallenge.presentation.MfaIntent
import com.auth0.universalcomponents.feature.mfachallenge.presentation.MfaRound
import com.auth0.universalcomponents.feature.mfachallenge.presentation.MfaState
import com.auth0.universalcomponents.feature.mfachallenge.presentation.PushPhase
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginFlowViewModelTest {

    private lateinit var repository: EmbeddedLoginRepository
    private lateinit var mfaRepository: MfaRepository
    private lateinit var viewModel: LoginFlowViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        coEvery { repository.discover() } returns Result.Success(listOf(LoginOption.EmailOtp))
        coEvery { repository.startEmailOtp(any()) } returns Result.Success(Unit)
        mfaRepository = mockk()
        coEvery { mfaRepository.sendPush(any()) } returns Result.Success(PushStatus.Pending(PUSH_POLL_MS, canResend = true))
        // A push nobody answers, unless a test says otherwise, so MFA stays on screen.
        coEvery { mfaRepository.checkPushStatus() } coAnswers { awaitCancellation() }
        viewModel = LoginFlowViewModel(repository, mfaRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    // region start

    @Test
    fun `initialization - step starts as Loading`() = runTest {
        assertThat(viewModel.loginState.value).isEqualTo(LoginState.Loading)
    }

    @Test
    fun `start - email code offered - asks for the email`() = runTest {
        viewModel.send(LoginIntent.Start)
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier())
    }

    @Test
    fun `start - nothing usable offered - shows no methods`() = runTest {
        coEvery { repository.discover() } returns Result.Success(listOf(LoginOption.Unsupported("identify_phone")))

        viewModel.send(LoginIntent.Start)
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.NoMethodsAvailable)
    }

    @Test
    fun `start - sent twice - discovers once`() = runTest {
        viewModel.send(LoginIntent.Start)
        advanceUntilIdle()
        viewModel.send(LoginIntent.Start)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.discover() }
    }

    @Test
    fun `start - fails - shows that error full-screen`() = runTest {
        coEvery { repository.discover() } returns failure(EmbeddedLoginError.Network)

        viewModel.send(LoginIntent.Start)
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.Failed(EmbeddedLoginError.Network))
    }

    @Test
    fun `retry - after discovery failed - discovers again`() = runTest {
        coEvery { repository.discover() } returns failure(EmbeddedLoginError.Unknown("debug output"))
        viewModel.send(LoginIntent.Start)
        advanceUntilIdle()

        coEvery { repository.discover() } returns Result.Success(listOf(LoginOption.EmailOtp))
        viewModel.send(LoginIntent.Retry)
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier())
        coVerify(exactly = 2) { repository.discover() }
    }

    // endregion

    // region email

    @Test
    fun `submit email - while sending - stays on email with Continue spinning`() = runTest {
        startAtEmail()
        val sent = CompletableDeferred<Unit>()
        coEvery { repository.startEmailOtp(EMAIL) } coAnswers { Result.Success(sent.await()) }

        viewModel.send(LoginIntent.SubmitIdentifier(EMAIL))
        runCurrent()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(EMAIL, isSubmitting = true))
        sent.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `submit email - code sent - asks for the code, with the resend countdown started`() = runTest {
        startAtEmail()

        viewModel.send(LoginIntent.SubmitIdentifier(EMAIL))
        runCurrent()

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpCode(EMAIL, resendSecondsLeft = COOLDOWN_SECONDS))
        coVerify(exactly = 1) { repository.startEmailOtp(EMAIL) }
    }

    @Test
    fun `submit email - fails for good - retry returns to email with the typed email`() = runTest {
        startAtEmail()
        coEvery { repository.startEmailOtp(EMAIL) } returns failure(EmbeddedLoginError.Unknown("debug output"))
        viewModel.send(LoginIntent.SubmitIdentifier(EMAIL))
        advanceUntilIdle()
        assertThat(viewModel.loginState.value).isInstanceOf(LoginState.Failed::class.java)

        viewModel.send(LoginIntent.Retry)
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(EMAIL))
    }

    @Test
    fun `submit email - not on the email step - is ignored`() = runTest {
        startAtCode()

        viewModel.send(LoginIntent.SubmitIdentifier(OTHER_EMAIL))
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.startEmailOtp(OTHER_EMAIL) }
    }

    // endregion

    // region code

    @Test
    fun `submit code - accepted - signs in and hands over the credentials`() = runTest {
        startAtCode()
        val credentials = mockk<Credentials>()
        coEvery { repository.verifyOtp(CODE) } returns Result.Success(credentials)
        val received = mutableListOf<Credentials>()
        val job = launch { authenticatedEvents.toList(received) }

        viewModel.send(LoginIntent.SubmitOtp(CODE))
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.Authenticated)
        assertThat(received).containsExactly(credentials)
        job.cancel()
    }

    @Test
    fun `submit code - second factor needed - hands over to MFA with the offered factors`() = runTest {
        startAtCode()
        coEvery { repository.verifyOtp(CODE) } returns failure(EmbeddedLoginError.MfaRequired(listOf(PHONE, TABLET)))

        viewModel.send(LoginIntent.SubmitOtp(CODE))
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isInstanceOf(LoginState.Mfa::class.java)
        assertThat(mfaStep()).isEqualTo(MfaState.ChooseFactor(MfaRound(1, listOf(PHONE, TABLET))))
    }

    @Test
    fun `mfa - taps on its own screens - go to MFA alone, which shows its next step`() = runTest {
        startAtMfa(PHONE, TABLET)
        val engine = (viewModel.loginState.value as LoginState.Mfa).engine

        engine.onIntent(MfaIntent.SelectFactor(TABLET))
        advanceUntilIdle()

        val step = mfaStep()
        assertThat(step).isInstanceOf(MfaState.PushWaiting::class.java)
        assertThat((step as MfaState.PushWaiting).factor).isEqualTo(TABLET)
        coVerify(exactly = 1) { mfaRepository.sendPush(TABLET) }
    }

    @Test
    fun `mfa - Start over tapped twice - leaves MFA once, and the second tap does not leave the login`() = runTest {
        coEvery { mfaRepository.checkPushStatus() } returns Result.Error(REJECTED)
        startAtMfa(PHONE)
        viewModel.send(LoginIntent.LeaveMfa)
        viewModel.send(LoginIntent.LeaveMfa)

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(EMAIL))
        // Back from here would exit the login (false); had the second tap been Back, the screen would be gone.
        assertThat(viewModel.goBack()).isFalse()
    }

    @Test
    fun `mfa - Back - is passed to MFA, which decides what it means`() = runTest {
        startAtMfa(PHONE, TABLET)

        assertThat(viewModel.goBack()).isTrue()
        advanceUntilIdle()

        // Back on the list leaves MFA; had login handled it, the list would still be on show.
        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(EMAIL))
    }

    @Test
    fun `mfa - approved - signs in once and hands over the credentials`() = runTest {
        val credentials = mockk<Credentials>()
        coEvery { mfaRepository.checkPushStatus() } returns Result.Success(PushStatus.Approved(credentials))
        val received = mutableListOf<Credentials>()
        val job = launch { authenticatedEvents.toList(received) }

        startAtMfa(PHONE)

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.Authenticated)
        assertThat(received).containsExactly(credentials)
        job.cancel()
    }

    @Test
    fun `mfa - Back with one device - goes back to email entry with the email kept, and the next visit is fresh`() =
        runTest {
            startAtMfa(PHONE)

            assertThat(viewModel.goBack()).isTrue()
            advanceUntilIdle()
            assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(EMAIL))

            viewModel.send(LoginIntent.SubmitIdentifier(EMAIL))
            advanceUntilIdle()
            viewModel.send(LoginIntent.SubmitOtp(CODE))
            advanceUntilIdle()

            // A fresh push, whose resend wait the test clock has already run out, with its first check on its way.
            assertThat(mfaStep()).isEqualTo(
                MfaState.PushWaiting(
                    round = MfaRound(1, listOf(PHONE)),
                    factor = PHONE,
                    phase = PushPhase.Checking,
                    everyMs = PUSH_POLL_MS,
                    canResend = true,
                    resendSecondsLeft = 0,
                ),
            )
            coVerify(exactly = 2) { mfaRepository.sendPush(PHONE) }
        }

    @Test
    fun `mfa - nothing usable - sends no push, and hands the decision to the app`() = runTest {
        startAtMfaWithHandler()

        assertThat(viewModel.loginState.value).isInstanceOf(LoginState.AwaitingApp::class.java)
        coVerify(exactly = 0) { mfaRepository.sendPush(any()) }
    }

    @Test
    fun `mfa - ends with an error - its screen stays until Back, which starts over with the email kept`() = runTest {
        coEvery { mfaRepository.checkPushStatus() } returns Result.Error(REJECTED)
        startAtMfa(PHONE)

        assertThat(mfaStep()).isEqualTo(MfaState.Failed(REJECTED))
        assertThat((viewModel.loginState.value as LoginState.Mfa).engine.isRunning).isFalse()

        assertThat(viewModel.goBack()).isTrue()
        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(EMAIL))
    }

    // region app told about MFA errors

    @Test
    fun `mfa error - no usable factor - waits for the app behind a spinner and tells it once`() = runTest {
        val reported = mutableListOf<MfaError>()
        val job = launch { mfaFailedEvents.toList(reported) }

        startAtMfaWithHandler()
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isInstanceOf(LoginState.AwaitingApp::class.java)
        assertThat(reported).hasSize(1)
        assertThat(reported.single()).isInstanceOf(MfaError.NoEligibleFactors::class.java)
        job.cancel()
    }

    @Test
    fun `mfa error - no usable factor and the app set no handler - shows the not available screen at once`() = runTest {
        val reported = mutableListOf<MfaError>()
        val job = launch { mfaFailedEvents.toList(reported) }

        startAtMfa()
        advanceUntilIdle()

        // Nobody can answer, so there is no spinner to wait behind.
        assertThat(viewModel.loginState.value).isInstanceOf(LoginState.Mfa::class.java)
        assertThat((mfaStep() as MfaState.Failed).error).isInstanceOf(MfaError.NoEligibleFactors::class.java)
        assertThat(reported).hasSize(1)
        job.cancel()
    }

    @Test
    fun `mfa error - the app removed its handler - nothing waits for it`() = runTest {
        viewModel.send(LoginIntent.MfaErrorHandlerChanged(isSet = true))
        viewModel.send(LoginIntent.MfaErrorHandlerChanged(isSet = false))

        startAtMfa()

        assertThat(viewModel.loginState.value).isInstanceOf(LoginState.Mfa::class.java)
    }

    @Test
    fun `mfa error - no usable factor and the app handles it - stays on the spinner`() = runTest {
        startAtMfaWithHandler()

        viewModel.send(LoginIntent.MfaErrorAnswered(MfaErrorHandling.Handled))
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isInstanceOf(LoginState.AwaitingApp::class.java)
    }

    @Test
    fun `mfa error - no usable factor and the app does not handle it - shows the not available screen`() = runTest {
        startAtMfaWithHandler()

        viewModel.send(LoginIntent.MfaErrorAnswered(MfaErrorHandling.NotHandled))
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isInstanceOf(LoginState.Mfa::class.java)
        assertThat((mfaStep() as MfaState.Failed).error).isInstanceOf(MfaError.NoEligibleFactors::class.java)

        // From there, "Start over" still returns to email entry with the email kept.
        viewModel.send(LoginIntent.LeaveMfa)
        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(EMAIL))
    }

    @Test
    fun `mfa error - any other - tells the app once, and its screen shows whatever the app answers`() = runTest {
        coEvery { mfaRepository.checkPushStatus() } returns Result.Error(REJECTED)
        val reported = mutableListOf<MfaError>()
        val job = launch { mfaFailedEvents.toList(reported) }

        startAtMfa(PHONE)
        viewModel.send(LoginIntent.MfaErrorAnswered(MfaErrorHandling.Handled))
        advanceUntilIdle()

        assertThat(reported).containsExactly(REJECTED)
        assertThat(viewModel.loginState.value).isInstanceOf(LoginState.Mfa::class.java)
        assertThat(mfaStep()).isEqualTo(MfaState.Failed(REJECTED))
        job.cancel()
    }

    @Test
    fun `mfa error - the event and the kept result are the same ending`() = runTest {
        coEvery { mfaRepository.checkPushStatus() } returns Result.Error(REJECTED)
        val reported = mutableListOf<MfaError>()
        val job = launch { mfaFailedEvents.toList(reported) }

        startAtMfa(PHONE)
        advanceUntilIdle()

        val ended = viewModel.mfaResult.value as MfaResult.Ended
        assertThat(reported).hasSize(1)
        assertThat(reported.single()).isSameInstanceAs(ended.error)
        job.cancel()
    }

    @Test
    fun `mfa error - the user leaves MFA - the app is not told`() = runTest {
        val reported = mutableListOf<MfaError>()
        val job = launch { mfaFailedEvents.toList(reported) }
        startAtMfa(PHONE, TABLET)

        viewModel.goBack()
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(EMAIL))
        assertThat(reported).isEmpty()
        job.cancel()
    }

    @Test
    fun `mfa error - signed in - the app is not told`() = runTest {
        coEvery { mfaRepository.checkPushStatus() } returns Result.Success(PushStatus.Approved(mockk()))
        val reported = mutableListOf<MfaError>()
        val job = launch { mfaFailedEvents.toList(reported) }

        startAtMfa(PHONE)
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.Authenticated)
        assertThat(reported).isEmpty()
        job.cancel()
    }

    @Test
    fun `mfa error - Back while waiting for the app - returns to email entry with the email kept`() = runTest {
        startAtMfaWithHandler()

        assertThat(viewModel.goBack()).isTrue()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(EMAIL))
    }

    @Test
    fun `mfa error - the app answers after the user went back - the answer is dropped`() = runTest {
        startAtMfaWithHandler()
        viewModel.goBack()

        viewModel.send(LoginIntent.MfaErrorAnswered(MfaErrorHandling.NotHandled))

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(EMAIL))
    }

    // endregion

    // region how MFA ended

    @Test
    fun `result - before and during MFA - is none`() = runTest {
        assertThat(viewModel.mfaResult.value).isNull()

        startAtMfa(PHONE, TABLET)

        assertThat(viewModel.mfaResult.value).isNull()
    }

    @Test
    fun `result - signed in - is SignedIn`() = runTest {
        coEvery { mfaRepository.checkPushStatus() } returns Result.Success(PushStatus.Approved(mockk()))

        startAtMfa(PHONE)
        advanceUntilIdle()

        assertThat(viewModel.mfaResult.value).isEqualTo(MfaResult.SignedIn)
    }

    @Test
    fun `result - ended with an error - is Ended with that error`() = runTest {
        coEvery { mfaRepository.checkPushStatus() } returns Result.Error(REJECTED)

        startAtMfa(PHONE)

        assertThat((viewModel.mfaResult.value as MfaResult.Ended).error).isSameInstanceAs(REJECTED)
    }

    @Test
    fun `result - the user leaves - is Ended as cancelled`() = runTest {
        startAtMfa(PHONE, TABLET)

        viewModel.goBack()
        advanceUntilIdle()

        assertThat((viewModel.mfaResult.value as MfaResult.Ended).error).isInstanceOf(MfaError.Cancelled::class.java)
    }

    @Test
    fun `result - Start over after an error - keeps the error it ended with`() = runTest {
        coEvery { mfaRepository.checkPushStatus() } returns Result.Error(REJECTED)
        startAtMfa(PHONE)

        viewModel.send(LoginIntent.LeaveMfa)

        assertThat((viewModel.mfaResult.value as MfaResult.Ended).error).isSameInstanceAs(REJECTED)
    }

    @Test
    fun `result - the next MFA starts - clears the last one`() = runTest {
        startAtMfa(PHONE, TABLET)
        viewModel.goBack()
        advanceUntilIdle()
        assertThat(viewModel.mfaResult.value).isNotNull()

        viewModel.send(LoginIntent.SubmitIdentifier(EMAIL))
        advanceUntilIdle()
        viewModel.send(LoginIntent.SubmitOtp(CODE))
        advanceUntilIdle()

        assertThat(viewModel.mfaResult.value).isNull()
    }

    // endregion

    @Test
    fun `submit code - wrong code - stays on the code step with the error, the countdown going on`() = runTest {
        startAtCodeCountingDown()
        advanceTimeBy(10 * ONE_SECOND_MILLIS)
        runCurrent()
        coEvery { repository.verifyOtp(CODE) } returns failure(EmbeddedLoginError.InvalidCode)

        viewModel.send(LoginIntent.SubmitOtp(CODE))
        runCurrent()

        assertThat(viewModel.loginState.value).isEqualTo(
            LoginState.CollectOtpCode(EMAIL, error = EmbeddedLoginError.InvalidCode, resendSecondsLeft = 50),
        )
    }

    @Test
    fun `submit code - too many wrong codes - shows it full-screen, not under the field`() = runTest {
        startAtCode()
        coEvery { repository.verifyOtp(CODE) } returns failure(EmbeddedLoginError.TooManyWrongCodes)

        viewModel.send(LoginIntent.SubmitOtp(CODE))
        advanceUntilIdle()

        val step = viewModel.loginState.value
        assertThat(step).isInstanceOf(LoginState.Failed::class.java)
        assertThat((step as LoginState.Failed).error).isEqualTo(EmbeddedLoginError.TooManyWrongCodes)
    }

    @Test
    fun `submit code - fails but the session survives - retry returns to the code step`() = runTest {
        startAtCode()
        coEvery { repository.verifyOtp(CODE) } returns
            failure(EmbeddedLoginError.Network, canRetryStep = true)
        viewModel.send(LoginIntent.SubmitOtp(CODE))
        advanceUntilIdle()

        viewModel.send(LoginIntent.Retry)
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpCode(EMAIL))
    }

    @Test
    fun `submit code - fails and the session is gone - retry returns to email with the email kept`() = runTest {
        startAtCode()
        coEvery { repository.verifyOtp(CODE) } returns failure(EmbeddedLoginError.Unknown("debug output"))
        viewModel.send(LoginIntent.SubmitOtp(CODE))
        advanceUntilIdle()

        viewModel.send(LoginIntent.Retry)
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(EMAIL))
    }

    @Test
    fun `submit code - code expired - shows the expired error and opens resend at once`() = runTest {
        startAtCodeCountingDown()
        coEvery { repository.verifyOtp(CODE) } returns failure(EmbeddedLoginError.CodeExpired)

        viewModel.send(LoginIntent.SubmitOtp(CODE))
        runCurrent()

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpCode(EMAIL, error = EmbeddedLoginError.CodeExpired))
    }

    @Test
    fun `submit code - while verifying - stays on the code step with Verify spinning`() = runTest {
        startAtCode()
        val answer = CompletableDeferred<Credentials>()
        coEvery { repository.verifyOtp(CODE) } coAnswers { Result.Success(answer.await()) }

        viewModel.send(LoginIntent.SubmitOtp(CODE))
        runCurrent()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpCode(EMAIL, isSubmitting = true))
        answer.complete(mockk())
        advanceUntilIdle()
    }

    @Test
    fun `submit code - tapped twice while verifying - verifies once`() = runTest {
        startAtCode()
        val answer = CompletableDeferred<Credentials>()
        coEvery { repository.verifyOtp(CODE) } coAnswers { Result.Success(answer.await()) }

        viewModel.send(LoginIntent.SubmitOtp(CODE))
        runCurrent()
        viewModel.send(LoginIntent.SubmitOtp(CODE))
        answer.complete(mockk())
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.verifyOtp(CODE) }
    }

    // endregion

    // region resend

    @Test
    fun `resend - while sending - keeps the code step with only the resend row busy`() = runTest {
        startAtCode()
        val sent = CompletableDeferred<Unit>()
        coEvery { repository.startEmailOtp(EMAIL) } coAnswers { Result.Success(sent.await()) }

        viewModel.send(LoginIntent.Resend)
        runCurrent()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpCode(EMAIL, isResending = true))

        sent.complete(Unit)
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpCode(EMAIL))
        coVerify(exactly = 2) { repository.startEmailOtp(EMAIL) }
    }

    @Test
    fun `resend - fails for good - shows it full-screen, retry returns to email`() = runTest {
        startAtCode()
        val error = EmbeddedLoginError.Unknown("debug output")
        coEvery { repository.startEmailOtp(EMAIL) } returns failure(error)

        viewModel.send(LoginIntent.Resend)
        advanceUntilIdle()

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.Failed(error, retryTo = LoginState.CollectOtpIdentifier(EMAIL)))
    }

    @Test
    fun `resend - code sent - restarts the countdown`() = runTest {
        startAtCode()

        viewModel.send(LoginIntent.Resend)
        runCurrent()

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpCode(EMAIL, resendSecondsLeft = COOLDOWN_SECONDS))
    }

    @Test
    fun `resend - during the countdown - is ignored`() = runTest {
        startAtCodeCountingDown()

        viewModel.send(LoginIntent.Resend)
        runCurrent()

        coVerify(exactly = 1) { repository.startEmailOtp(EMAIL) }
    }

    // endregion

    // region countdown

    @Test
    fun `countdown - a second passes - takes one off`() = runTest {
        startAtCodeCountingDown()

        advanceTimeBy(ONE_SECOND_MILLIS)
        runCurrent()

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpCode(EMAIL, resendSecondsLeft = COOLDOWN_SECONDS - 1))
    }

    @Test
    fun `countdown - the full wait passes - opens resend`() = runTest {
        startAtCodeCountingDown()

        advanceTimeBy(COOLDOWN_SECONDS * ONE_SECOND_MILLIS)
        runCurrent()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpCode(EMAIL))
    }

    @Test
    fun `countdown - back to email while it runs - leaves email as it is`() = runTest {
        startAtCodeCountingDown()

        viewModel.goBack()
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(EMAIL))
    }

    // endregion

    // region starting over

    @Test
    fun `start - after signing in - begins a new flow`() = runTest {
        startAtCode()
        coEvery { repository.verifyOtp(CODE) } returns Result.Success(mockk())
        viewModel.send(LoginIntent.SubmitOtp(CODE))
        advanceUntilIdle()

        viewModel.send(LoginIntent.Start)
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier())
        coVerify(exactly = 2) { repository.discover() }
    }

    @Test
    fun `start - after leaving from email - begins a new flow`() = runTest {
        startAtEmail()
        viewModel.goBack()

        viewModel.send(LoginIntent.Start)
        advanceUntilIdle()

        coVerify(exactly = 2) { repository.discover() }
    }

    // endregion

    // region back

    @Test
    fun `back - on the code step - returns to email with the email kept`() = runTest {
        startAtCode()

        assertThat(viewModel.goBack()).isTrue()
        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(EMAIL))
    }

    @Test
    fun `back - on the code step - drops the inline error`() = runTest {
        startAtCode()
        coEvery { repository.verifyOtp(CODE) } returns failure(EmbeddedLoginError.InvalidCode)
        viewModel.send(LoginIntent.SubmitOtp(CODE))
        advanceUntilIdle()

        viewModel.goBack()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(EMAIL))
    }

    @Test
    fun `back - while verifying - stays in the flow`() = runTest {
        startAtCode()
        val answer = CompletableDeferred<Credentials>()
        coEvery { repository.verifyOtp(CODE) } coAnswers { Result.Success(answer.await()) }
        viewModel.send(LoginIntent.SubmitOtp(CODE))
        runCurrent()

        assertThat(viewModel.goBack()).isTrue()
        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpCode(EMAIL, isSubmitting = true))

        answer.complete(mockk())
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.Authenticated)
    }

    @Test
    fun `back - on email - leaves the flow`() = runTest {
        startAtEmail()

        assertThat(viewModel.goBack()).isFalse()
    }

    // endregion

    private fun TestScope.startAtEmail() {
        viewModel.send(LoginIntent.Start)
        advanceUntilIdle()
    }

    /** On MFA with [factors] on offer, after a correct code. */
    private fun TestScope.startAtMfa(vararg factors: MfaFactor) {
        startAtCode()
        coEvery { repository.verifyOtp(CODE) } returns failure(EmbeddedLoginError.MfaRequired(factors.toList()))
        viewModel.send(LoginIntent.SubmitOtp(CODE))
        advanceUntilIdle()
    }

    /** Like [startAtMfa], for an app that set `onMfaError`: it is waited for when no factor can be used. */
    private fun TestScope.startAtMfaWithHandler(vararg factors: MfaFactor) {
        viewModel.send(LoginIntent.MfaErrorHandlerChanged(isSet = true))
        startAtMfa(*factors)
    }

    /** What MFA is showing, read from the engine the login step carries. */
    private fun mfaStep(): MfaState = (viewModel.loginState.value as LoginState.Mfa).engine.state.value

    /** The credentials of each sign-in event. */
    private val authenticatedEvents
        get() = viewModel.events.filterIsInstance<LoginEvent.Authenticated>().map { it.credentials }

    /** The error of each MFA-failed event. */
    private val mfaFailedEvents
        get() = viewModel.events.filterIsInstance<LoginEvent.MfaFailed>().map { it.error }

    /** On the code step with the resend countdown already run out. */
    private fun TestScope.startAtCode() {
        startAtEmail()
        viewModel.send(LoginIntent.SubmitIdentifier(EMAIL))
        advanceUntilIdle()
    }

    /** On the code step with the resend countdown just started. */
    private fun TestScope.startAtCodeCountingDown() {
        startAtEmail()
        viewModel.send(LoginIntent.SubmitIdentifier(EMAIL))
        runCurrent()
    }

    private fun failure(error: EmbeddedLoginError, canRetryStep: Boolean = false): Result.Error<LoginFailure> =
        Result.Error(LoginFailure(error, canRetryStep))

    private companion object {
        const val EMAIL = "jane@example.com"
        const val OTHER_EMAIL = "john@example.com"
        const val CODE = "123456"
        const val COOLDOWN_SECONDS = 60
        const val ONE_SECOND_MILLIS = 1_000L
        const val PUSH_POLL_MS = 3_000L
        val PHONE = MfaFactor.Push(0, "Jane's phone")
        val REJECTED = MfaError.Rejected()
        val TABLET = MfaFactor.Push(1, "Jane's tablet")
    }
}
