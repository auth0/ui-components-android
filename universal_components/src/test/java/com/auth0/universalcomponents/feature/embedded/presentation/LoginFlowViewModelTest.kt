package com.auth0.universalcomponents.feature.embedded.presentation

import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.embedded.domain.model.PushStatus
import com.auth0.universalcomponents.feature.embedded.domain.repository.EmbeddedLoginRepository
import com.auth0.universalcomponents.feature.embedded.domain.repository.MfaRepository
import com.auth0.universalcomponents.feature.embedded.presentation.mfa.MfaIntent
import com.auth0.universalcomponents.feature.embedded.presentation.mfa.MfaStep
import com.auth0.universalcomponents.feature.embedded.presentation.mfa.PushPhase
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
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
        coEvery { mfaRepository.sendPush(any()) } returns Result.Success(PUSH_POLL_MS)
        // A push nobody answers, unless a test says otherwise, so MFA stays on screen.
        coEvery { mfaRepository.checkPush() } coAnswers { awaitCancellation() }
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
        assertThat(viewModel.loginStep.value).isEqualTo(LoginStep.Loading)
    }

    @Test
    fun `start - email code offered - asks for the email`() = runTest {
        viewModel.send(LoginEvent.Start)
        advanceUntilIdle()

        assertThat(viewModel.loginStep.value).isEqualTo(LoginStep.CollectOtpIdentifier())
    }

    @Test
    fun `start - nothing usable offered - shows no methods`() = runTest {
        coEvery { repository.discover() } returns Result.Success(listOf(LoginOption.Unsupported("identify_phone")))

        viewModel.send(LoginEvent.Start)
        advanceUntilIdle()

        assertThat(viewModel.loginStep.value).isEqualTo(LoginStep.NoMethodsAvailable)
    }

    @Test
    fun `start - sent twice - discovers once`() = runTest {
        viewModel.send(LoginEvent.Start)
        advanceUntilIdle()
        viewModel.send(LoginEvent.Start)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.discover() }
    }

    @Test
    fun `start - fails - shows that error full-screen`() = runTest {
        coEvery { repository.discover() } returns failure(EmbeddedLoginError.Network)

        viewModel.send(LoginEvent.Start)
        advanceUntilIdle()

        assertThat(viewModel.loginStep.value).isEqualTo(LoginStep.Failed(EmbeddedLoginError.Network))
    }

    @Test
    fun `retry - after discovery failed - discovers again`() = runTest {
        coEvery { repository.discover() } returns failure(EmbeddedLoginError.Unknown("debug output"))
        viewModel.send(LoginEvent.Start)
        advanceUntilIdle()

        coEvery { repository.discover() } returns Result.Success(listOf(LoginOption.EmailOtp))
        viewModel.send(LoginEvent.Retry)
        advanceUntilIdle()

        assertThat(viewModel.loginStep.value).isEqualTo(LoginStep.CollectOtpIdentifier())
        coVerify(exactly = 2) { repository.discover() }
    }

    // endregion

    // region email

    @Test
    fun `submit email - while sending - stays on email with Continue spinning`() = runTest {
        startAtEmail()
        val sent = CompletableDeferred<Unit>()
        coEvery { repository.startEmailOtp(EMAIL) } coAnswers { Result.Success(sent.await()) }

        viewModel.send(LoginEvent.SubmitIdentifier(EMAIL))
        runCurrent()

        assertThat(viewModel.loginStep.value).isEqualTo(LoginStep.CollectOtpIdentifier(EMAIL, isSubmitting = true))
        sent.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `submit email - code sent - asks for the code, with the resend countdown started`() = runTest {
        startAtEmail()

        viewModel.send(LoginEvent.SubmitIdentifier(EMAIL))
        runCurrent()

        assertThat(viewModel.loginStep.value)
            .isEqualTo(LoginStep.CollectOtpCode(EMAIL, resendSecondsLeft = COOLDOWN_SECONDS))
        coVerify(exactly = 1) { repository.startEmailOtp(EMAIL) }
    }

    @Test
    fun `submit email - fails for good - retry returns to email with the typed email`() = runTest {
        startAtEmail()
        coEvery { repository.startEmailOtp(EMAIL) } returns failure(EmbeddedLoginError.Unknown("debug output"))
        viewModel.send(LoginEvent.SubmitIdentifier(EMAIL))
        advanceUntilIdle()
        assertThat(viewModel.loginStep.value).isInstanceOf(LoginStep.Failed::class.java)

        viewModel.send(LoginEvent.Retry)
        advanceUntilIdle()

        assertThat(viewModel.loginStep.value).isEqualTo(LoginStep.CollectOtpIdentifier(EMAIL))
    }

    @Test
    fun `submit email - not on the email step - is ignored`() = runTest {
        startAtCode()

        viewModel.send(LoginEvent.SubmitIdentifier(OTHER_EMAIL))
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
        val job = launch { viewModel.authenticated.toList(received) }

        viewModel.send(LoginEvent.SubmitOtp(CODE))
        advanceUntilIdle()

        assertThat(viewModel.loginStep.value).isEqualTo(LoginStep.Authenticated)
        assertThat(received).containsExactly(credentials)
        job.cancel()
    }

    @Test
    fun `submit code - second factor needed - hands over to MFA with the offered factors`() = runTest {
        startAtCode()
        coEvery { repository.verifyOtp(CODE) } returns failure(EmbeddedLoginError.MfaRequired(listOf(PHONE, TABLET)))

        viewModel.send(LoginEvent.SubmitOtp(CODE))
        advanceUntilIdle()

        assertThat(viewModel.loginStep.value)
            .isEqualTo(LoginStep.Mfa(EMAIL, MfaStep.AuthenticatorList(listOf(PHONE, TABLET))))
    }

    @Test
    fun `mfa - taps - reach MFA, which shows its next step`() = runTest {
        startAtMfa(PHONE, TABLET)

        viewModel.send(LoginEvent.Mfa(MfaIntent.SelectFactor(TABLET)))
        advanceUntilIdle()

        val step = (viewModel.loginStep.value as LoginStep.Mfa).step
        assertThat(step).isInstanceOf(MfaStep.PushWaiting::class.java)
        assertThat((step as MfaStep.PushWaiting).factor).isEqualTo(TABLET)
        coVerify(exactly = 1) { mfaRepository.sendPush(TABLET) }
    }

    @Test
    fun `mfa - taps outside MFA - are ignored`() = runTest {
        startAtCode()

        viewModel.send(LoginEvent.Mfa(MfaIntent.SelectFactor(PHONE)))
        advanceUntilIdle()

        assertThat(viewModel.loginStep.value).isInstanceOf(LoginStep.CollectOtpCode::class.java)
        coVerify(exactly = 0) { mfaRepository.sendPush(any()) }
    }

    @Test
    fun `mfa - approved - signs in once and hands over the credentials`() = runTest {
        val credentials = mockk<Credentials>()
        coEvery { mfaRepository.checkPush() } returns Result.Success(PushStatus.Approved(credentials))
        val received = mutableListOf<Credentials>()
        val job = launch { viewModel.authenticated.toList(received) }

        startAtMfa(PHONE)

        assertThat(viewModel.loginStep.value).isEqualTo(LoginStep.Authenticated)
        assertThat(received).containsExactly(credentials)
        job.cancel()
    }

    @Test
    fun `mfa - Back with one device - goes back to email entry with the email kept, and the next visit is fresh`() =
        runTest {
            startAtMfa(PHONE)

            assertThat(viewModel.goBack()).isTrue()
            advanceUntilIdle()
            assertThat(viewModel.loginStep.value).isEqualTo(LoginStep.CollectOtpIdentifier(EMAIL))

            viewModel.send(LoginEvent.SubmitIdentifier(EMAIL))
            advanceUntilIdle()
            viewModel.send(LoginEvent.SubmitOtp(CODE))
            advanceUntilIdle()

            // A fresh push, whose resend wait the test clock has already run out.
            val step = (viewModel.loginStep.value as LoginStep.Mfa).step
            val polling = PushPhase.Polling(PUSH_POLL_MS, resendSecondsLeft = 0)
            assertThat(step).isEqualTo(MfaStep.PushWaiting(PHONE, polling, canChooseAnother = false))
            coVerify(exactly = 2) { mfaRepository.sendPush(PHONE) }
        }

    @Test
    fun `submit code - wrong code - stays on the code step with the error, the countdown going on`() = runTest {
        startAtCodeCountingDown()
        advanceTimeBy(10 * ONE_SECOND_MILLIS)
        runCurrent()
        coEvery { repository.verifyOtp(CODE) } returns failure(EmbeddedLoginError.InvalidCode)

        viewModel.send(LoginEvent.SubmitOtp(CODE))
        runCurrent()

        assertThat(viewModel.loginStep.value).isEqualTo(
            LoginStep.CollectOtpCode(EMAIL, error = EmbeddedLoginError.InvalidCode, resendSecondsLeft = 50),
        )
    }

    @Test
    fun `submit code - too many wrong codes - shows it full-screen, not under the field`() = runTest {
        startAtCode()
        coEvery { repository.verifyOtp(CODE) } returns failure(EmbeddedLoginError.TooManyWrongCodes)

        viewModel.send(LoginEvent.SubmitOtp(CODE))
        advanceUntilIdle()

        val step = viewModel.loginStep.value
        assertThat(step).isInstanceOf(LoginStep.Failed::class.java)
        assertThat((step as LoginStep.Failed).error).isEqualTo(EmbeddedLoginError.TooManyWrongCodes)
    }

    @Test
    fun `submit code - fails but the session survives - retry returns to the code step`() = runTest {
        startAtCode()
        coEvery { repository.verifyOtp(CODE) } returns
            failure(EmbeddedLoginError.Network, canRetryStep = true)
        viewModel.send(LoginEvent.SubmitOtp(CODE))
        advanceUntilIdle()

        viewModel.send(LoginEvent.Retry)
        advanceUntilIdle()

        assertThat(viewModel.loginStep.value).isEqualTo(LoginStep.CollectOtpCode(EMAIL))
    }

    @Test
    fun `submit code - fails and the session is gone - retry returns to email with the email kept`() = runTest {
        startAtCode()
        coEvery { repository.verifyOtp(CODE) } returns failure(EmbeddedLoginError.Unknown("debug output"))
        viewModel.send(LoginEvent.SubmitOtp(CODE))
        advanceUntilIdle()

        viewModel.send(LoginEvent.Retry)
        advanceUntilIdle()

        assertThat(viewModel.loginStep.value).isEqualTo(LoginStep.CollectOtpIdentifier(EMAIL))
    }

    @Test
    fun `submit code - code expired - shows the expired error and opens resend at once`() = runTest {
        startAtCodeCountingDown()
        coEvery { repository.verifyOtp(CODE) } returns failure(EmbeddedLoginError.CodeExpired)

        viewModel.send(LoginEvent.SubmitOtp(CODE))
        runCurrent()

        assertThat(viewModel.loginStep.value)
            .isEqualTo(LoginStep.CollectOtpCode(EMAIL, error = EmbeddedLoginError.CodeExpired))
    }

    @Test
    fun `submit code - while verifying - stays on the code step with Verify spinning`() = runTest {
        startAtCode()
        val answer = CompletableDeferred<Credentials>()
        coEvery { repository.verifyOtp(CODE) } coAnswers { Result.Success(answer.await()) }

        viewModel.send(LoginEvent.SubmitOtp(CODE))
        runCurrent()

        assertThat(viewModel.loginStep.value).isEqualTo(LoginStep.CollectOtpCode(EMAIL, isSubmitting = true))
        answer.complete(mockk())
        advanceUntilIdle()
    }

    @Test
    fun `submit code - tapped twice while verifying - verifies once`() = runTest {
        startAtCode()
        val answer = CompletableDeferred<Credentials>()
        coEvery { repository.verifyOtp(CODE) } coAnswers { Result.Success(answer.await()) }

        viewModel.send(LoginEvent.SubmitOtp(CODE))
        runCurrent()
        viewModel.send(LoginEvent.SubmitOtp(CODE))
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

        viewModel.send(LoginEvent.Resend)
        runCurrent()

        assertThat(viewModel.loginStep.value).isEqualTo(LoginStep.CollectOtpCode(EMAIL, isResending = true))

        sent.complete(Unit)
        advanceUntilIdle()

        assertThat(viewModel.loginStep.value).isEqualTo(LoginStep.CollectOtpCode(EMAIL))
        coVerify(exactly = 2) { repository.startEmailOtp(EMAIL) }
    }

    @Test
    fun `resend - fails for good - shows it full-screen, retry returns to email`() = runTest {
        startAtCode()
        val error = EmbeddedLoginError.Unknown("debug output")
        coEvery { repository.startEmailOtp(EMAIL) } returns failure(error)

        viewModel.send(LoginEvent.Resend)
        advanceUntilIdle()

        assertThat(viewModel.loginStep.value)
            .isEqualTo(LoginStep.Failed(error, retryTo = LoginStep.CollectOtpIdentifier(EMAIL)))
    }

    @Test
    fun `resend - code sent - restarts the countdown`() = runTest {
        startAtCode()

        viewModel.send(LoginEvent.Resend)
        runCurrent()

        assertThat(viewModel.loginStep.value)
            .isEqualTo(LoginStep.CollectOtpCode(EMAIL, resendSecondsLeft = COOLDOWN_SECONDS))
    }

    @Test
    fun `resend - during the countdown - is ignored`() = runTest {
        startAtCodeCountingDown()

        viewModel.send(LoginEvent.Resend)
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

        assertThat(viewModel.loginStep.value)
            .isEqualTo(LoginStep.CollectOtpCode(EMAIL, resendSecondsLeft = COOLDOWN_SECONDS - 1))
    }

    @Test
    fun `countdown - the full wait passes - opens resend`() = runTest {
        startAtCodeCountingDown()

        advanceTimeBy(COOLDOWN_SECONDS * ONE_SECOND_MILLIS)
        runCurrent()

        assertThat(viewModel.loginStep.value).isEqualTo(LoginStep.CollectOtpCode(EMAIL))
    }

    @Test
    fun `countdown - back to email while it runs - leaves email as it is`() = runTest {
        startAtCodeCountingDown()

        viewModel.goBack()
        advanceUntilIdle()

        assertThat(viewModel.loginStep.value).isEqualTo(LoginStep.CollectOtpIdentifier(EMAIL))
    }

    // endregion

    // region starting over

    @Test
    fun `start - after signing in - begins a new flow`() = runTest {
        startAtCode()
        coEvery { repository.verifyOtp(CODE) } returns Result.Success(mockk())
        viewModel.send(LoginEvent.SubmitOtp(CODE))
        advanceUntilIdle()

        viewModel.send(LoginEvent.Start)
        advanceUntilIdle()

        assertThat(viewModel.loginStep.value).isEqualTo(LoginStep.CollectOtpIdentifier())
        coVerify(exactly = 2) { repository.discover() }
    }

    @Test
    fun `start - after leaving from email - begins a new flow`() = runTest {
        startAtEmail()
        viewModel.goBack()

        viewModel.send(LoginEvent.Start)
        advanceUntilIdle()

        coVerify(exactly = 2) { repository.discover() }
    }

    // endregion

    // region back

    @Test
    fun `back - on the code step - returns to email with the email kept`() = runTest {
        startAtCode()

        assertThat(viewModel.goBack()).isTrue()
        assertThat(viewModel.loginStep.value).isEqualTo(LoginStep.CollectOtpIdentifier(EMAIL))
    }

    @Test
    fun `back - on the code step - drops the inline error`() = runTest {
        startAtCode()
        coEvery { repository.verifyOtp(CODE) } returns failure(EmbeddedLoginError.InvalidCode)
        viewModel.send(LoginEvent.SubmitOtp(CODE))
        advanceUntilIdle()

        viewModel.goBack()

        assertThat(viewModel.loginStep.value).isEqualTo(LoginStep.CollectOtpIdentifier(EMAIL))
    }

    @Test
    fun `back - while verifying - stays in the flow`() = runTest {
        startAtCode()
        val answer = CompletableDeferred<Credentials>()
        coEvery { repository.verifyOtp(CODE) } coAnswers { Result.Success(answer.await()) }
        viewModel.send(LoginEvent.SubmitOtp(CODE))
        runCurrent()

        assertThat(viewModel.goBack()).isTrue()
        assertThat(viewModel.loginStep.value).isEqualTo(LoginStep.CollectOtpCode(EMAIL, isSubmitting = true))

        answer.complete(mockk())
        advanceUntilIdle()

        assertThat(viewModel.loginStep.value).isEqualTo(LoginStep.Authenticated)
    }

    @Test
    fun `back - on email - leaves the flow`() = runTest {
        startAtEmail()

        assertThat(viewModel.goBack()).isFalse()
    }

    // endregion

    private fun TestScope.startAtEmail() {
        viewModel.send(LoginEvent.Start)
        advanceUntilIdle()
    }

    /** On MFA with [factors] on offer, after a correct code. */
    private fun TestScope.startAtMfa(vararg factors: MfaFactor) {
        startAtCode()
        coEvery { repository.verifyOtp(CODE) } returns failure(EmbeddedLoginError.MfaRequired(factors.toList()))
        viewModel.send(LoginEvent.SubmitOtp(CODE))
        advanceUntilIdle()
    }

    /** On the code step with the resend countdown already run out. */
    private fun TestScope.startAtCode() {
        startAtEmail()
        viewModel.send(LoginEvent.SubmitIdentifier(EMAIL))
        advanceUntilIdle()
    }

    /** On the code step with the resend countdown just started. */
    private fun TestScope.startAtCodeCountingDown() {
        startAtEmail()
        viewModel.send(LoginEvent.SubmitIdentifier(EMAIL))
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
        val TABLET = MfaFactor.Push(1, "Jane's tablet")
    }
}
