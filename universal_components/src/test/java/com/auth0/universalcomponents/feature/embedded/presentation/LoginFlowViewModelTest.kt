package com.auth0.universalcomponents.feature.embedded.presentation

import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.domain.model.AuthFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.AuthFailureException
import com.auth0.universalcomponents.feature.embedded.domain.model.AuthOutcome
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedAuthRequestFailed
import com.auth0.universalcomponents.feature.embedded.domain.model.FailureMessage
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginEvent
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginStep
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaChallenge
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.embedded.domain.repository.AuthInteracting
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

    private lateinit var interactor: AuthInteracting
    private lateinit var viewModel: LoginFlowViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        interactor = mockk()
        coEvery { interactor.discover() } returns listOf(LoginOption.EmailOtp)
        coEvery { interactor.startOtp(any()) } returns Unit
        viewModel = LoginFlowViewModel(interactor)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    // region start

    @Test
    fun `initialization - step starts as Loading`() = runTest {
        assertThat(viewModel.step.value).isEqualTo(LoginStep.Loading)
    }

    @Test
    fun `start - email code offered - asks for the email`() = runTest {
        viewModel.send(LoginEvent.Start)
        advanceUntilIdle()

        assertThat(viewModel.step.value).isEqualTo(LoginStep.CollectOtpIdentifier())
    }

    @Test
    fun `start - nothing usable offered - shows no methods`() = runTest {
        coEvery { interactor.discover() } returns listOf(LoginOption.Unsupported("identify_phone"))

        viewModel.send(LoginEvent.Start)
        advanceUntilIdle()

        assertThat(viewModel.step.value).isEqualTo(LoginStep.NoMethodsAvailable)
    }

    @Test
    fun `start - sent twice - discovers once`() = runTest {
        viewModel.send(LoginEvent.Start)
        advanceUntilIdle()
        viewModel.send(LoginEvent.Start)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.discover() }
    }

    @Test
    fun `start - fails with a message - shows that message`() = runTest {
        val message = FailureMessage.Local(R.string.embedded_login_error_network)
        coEvery { interactor.discover() } throws EmbeddedAuthRequestFailed(message)

        viewModel.send(LoginEvent.Start)
        advanceUntilIdle()

        assertThat(viewModel.step.value).isEqualTo(LoginStep.Failed(message))
    }

    @Test
    fun `start - unexpected error - shows the generic text, never the exception's`() = runTest {
        coEvery { interactor.discover() } throws IllegalStateException("debug output")

        viewModel.send(LoginEvent.Start)
        advanceUntilIdle()

        assertThat(viewModel.step.value)
            .isEqualTo(LoginStep.Failed(FailureMessage.Local(R.string.embedded_login_error_generic)))
    }

    @Test
    fun `retry - after discovery failed - discovers again`() = runTest {
        coEvery { interactor.discover() } throws EmbeddedAuthRequestFailed(null)
        viewModel.send(LoginEvent.Start)
        advanceUntilIdle()

        coEvery { interactor.discover() } returns listOf(LoginOption.EmailOtp)
        viewModel.send(LoginEvent.Retry)
        advanceUntilIdle()

        assertThat(viewModel.step.value).isEqualTo(LoginStep.CollectOtpIdentifier())
        coVerify(exactly = 2) { interactor.discover() }
    }

    // endregion

    // region email

    @Test
    fun `submit email - code sent - asks for the code sent to that email`() = runTest {
        startAtEmail()

        viewModel.send(LoginEvent.SubmitIdentifier(EMAIL))
        advanceUntilIdle()

        assertThat(viewModel.step.value).isEqualTo(LoginStep.CollectOtpCode(EMAIL))
        coVerify(exactly = 1) { interactor.startOtp(EMAIL) }
    }

    @Test
    fun `submit email - code not sent - stays on email with the error and the typed email`() = runTest {
        startAtEmail()
        coEvery { interactor.startOtp(EMAIL) } throws AuthFailureException(AuthFailure.CODE_NOT_SENT)

        viewModel.send(LoginEvent.SubmitIdentifier(EMAIL))
        advanceUntilIdle()

        assertThat(viewModel.step.value).isEqualTo(LoginStep.CollectOtpIdentifier(EMAIL))
        assertThat(viewModel.inputError.value).isEqualTo(AuthFailure.CODE_NOT_SENT)
    }

    @Test
    fun `submit email - fails for good - retry returns to email with the typed email`() = runTest {
        startAtEmail()
        coEvery { interactor.startOtp(EMAIL) } throws EmbeddedAuthRequestFailed(null)
        viewModel.send(LoginEvent.SubmitIdentifier(EMAIL))
        advanceUntilIdle()
        assertThat(viewModel.step.value).isInstanceOf(LoginStep.Failed::class.java)

        viewModel.send(LoginEvent.Retry)
        advanceUntilIdle()

        assertThat(viewModel.step.value).isEqualTo(LoginStep.CollectOtpIdentifier(EMAIL))
    }

    // endregion

    // region code

    @Test
    fun `submit code - accepted - signs in and hands over the credentials`() = runTest {
        startAtCode()
        val credentials = mockk<Credentials>()
        coEvery { interactor.verifyOtp(CODE) } returns AuthOutcome.Authenticated(credentials)
        val received = mutableListOf<Credentials>()
        val job = launch { viewModel.authenticated.toList(received) }

        viewModel.send(LoginEvent.SubmitOtp(CODE))
        advanceUntilIdle()

        assertThat(viewModel.step.value).isEqualTo(LoginStep.Authenticated)
        assertThat(received).containsExactly(credentials)
        job.cancel()
    }

    @Test
    fun `submit code - wrong code - stays on the code step with the error`() = runTest {
        startAtCode()
        coEvery { interactor.verifyOtp(CODE) } throws AuthFailureException(AuthFailure.INVALID_CODE)

        viewModel.send(LoginEvent.SubmitOtp(CODE))
        advanceUntilIdle()

        assertThat(viewModel.step.value).isEqualTo(LoginStep.CollectOtpCode(EMAIL))
        assertThat(viewModel.inputError.value).isEqualTo(AuthFailure.INVALID_CODE)
    }

    @Test
    fun `submit code - fails but the session survives - retry returns to the code step`() = runTest {
        startAtCode()
        coEvery { interactor.verifyOtp(CODE) } throws EmbeddedAuthRequestFailed(null, canRetryStep = true)
        viewModel.send(LoginEvent.SubmitOtp(CODE))
        advanceUntilIdle()

        viewModel.send(LoginEvent.Retry)
        advanceUntilIdle()

        assertThat(viewModel.step.value).isEqualTo(LoginStep.CollectOtpCode(EMAIL))
    }

    @Test
    fun `submit code - fails and the session is gone - retry returns to email with the email kept`() = runTest {
        startAtCode()
        coEvery { interactor.verifyOtp(CODE) } throws EmbeddedAuthRequestFailed(null, canRetryStep = false)
        viewModel.send(LoginEvent.SubmitOtp(CODE))
        advanceUntilIdle()

        viewModel.send(LoginEvent.Retry)
        advanceUntilIdle()

        assertThat(viewModel.step.value).isEqualTo(LoginStep.CollectOtpIdentifier(EMAIL))
    }

    @Test
    fun `submit code - code expired - stays on the code step with the expired error`() = runTest {
        startAtCode()
        coEvery { interactor.verifyOtp(CODE) } throws AuthFailureException(AuthFailure.CODE_EXPIRED)

        viewModel.send(LoginEvent.SubmitOtp(CODE))
        advanceUntilIdle()

        assertThat(viewModel.step.value).isEqualTo(LoginStep.CollectOtpCode(EMAIL))
        assertThat(viewModel.inputError.value).isEqualTo(AuthFailure.CODE_EXPIRED)
    }

    @Test
    fun `submit code - tapped twice while verifying - verifies once`() = runTest {
        startAtCode()
        val answer = CompletableDeferred<AuthOutcome>()
        coEvery { interactor.verifyOtp(CODE) } coAnswers { answer.await() }

        viewModel.send(LoginEvent.SubmitOtp(CODE))
        runCurrent()
        viewModel.send(LoginEvent.SubmitOtp(CODE))
        answer.complete(AuthOutcome.Authenticated(mockk()))
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.verifyOtp(CODE) }
    }

    @Test
    fun `submit code - step-up needed - asks for the step-up code, which is verified with its factor`() = runTest {
        startAtCode()
        val challenge = MfaChallenge(MfaFactor.OTP)
        coEvery { interactor.verifyOtp(CODE) } returns AuthOutcome.MfaRequired(challenge)
        coEvery { interactor.verifyMfa(MFA_CODE, MfaFactor.OTP) } returns AuthOutcome.Authenticated(mockk())

        viewModel.send(LoginEvent.SubmitOtp(CODE))
        advanceUntilIdle()
        assertThat(viewModel.step.value).isEqualTo(LoginStep.StepUpMfa(challenge))

        viewModel.send(LoginEvent.SubmitMfa(MFA_CODE))
        advanceUntilIdle()

        assertThat(viewModel.step.value).isEqualTo(LoginStep.Authenticated)
        coVerify(exactly = 1) { interactor.verifyMfa(MFA_CODE, MfaFactor.OTP) }
    }

    @Test
    fun `submit step-up code - wrong code - stays on the step-up with the error`() = runTest {
        startAtCode()
        val challenge = MfaChallenge(MfaFactor.EMAIL, "j***@example.com")
        coEvery { interactor.verifyOtp(CODE) } returns AuthOutcome.MfaRequired(challenge)
        coEvery { interactor.verifyMfa(MFA_CODE, MfaFactor.EMAIL) } throws
            AuthFailureException(AuthFailure.INVALID_CODE)
        viewModel.send(LoginEvent.SubmitOtp(CODE))
        advanceUntilIdle()

        viewModel.send(LoginEvent.SubmitMfa(MFA_CODE))
        advanceUntilIdle()

        assertThat(viewModel.step.value).isEqualTo(LoginStep.StepUpMfa(challenge))
        assertThat(viewModel.inputError.value).isEqualTo(AuthFailure.INVALID_CODE)
    }

    // endregion

    // region resend

    @Test
    fun `resend - while sending - keeps the code step instead of loading`() = runTest {
        startAtCode()
        val sent = CompletableDeferred<Unit>()
        coEvery { interactor.startOtp(EMAIL) } coAnswers { sent.await() }

        viewModel.resend()
        runCurrent()

        assertThat(viewModel.step.value).isEqualTo(LoginStep.CollectOtpCode(EMAIL))
        assertThat(viewModel.isResending.value).isTrue()

        sent.complete(Unit)
        advanceUntilIdle()

        assertThat(viewModel.isResending.value).isFalse()
        coVerify(exactly = 2) { interactor.startOtp(EMAIL) }
    }

    @Test
    fun `resend - fails - isResending is cleared`() = runTest {
        startAtCode()
        coEvery { interactor.startOtp(EMAIL) } throws EmbeddedAuthRequestFailed(null)

        viewModel.resend()
        advanceUntilIdle()

        assertThat(viewModel.isResending.value).isFalse()
    }

    @Test
    fun `resend - code not sent - stays on the code step with the error`() = runTest {
        startAtCode()
        coEvery { interactor.startOtp(EMAIL) } throws AuthFailureException(AuthFailure.CODE_NOT_SENT)

        viewModel.resend()
        advanceUntilIdle()

        assertThat(viewModel.step.value).isEqualTo(LoginStep.CollectOtpCode(EMAIL))
        assertThat(viewModel.inputError.value).isEqualTo(AuthFailure.CODE_NOT_SENT)
    }

    // endregion

    // region starting over

    @Test
    fun `start - after signing in - begins a new flow`() = runTest {
        startAtCode()
        coEvery { interactor.verifyOtp(CODE) } returns AuthOutcome.Authenticated(mockk())
        viewModel.send(LoginEvent.SubmitOtp(CODE))
        advanceUntilIdle()

        viewModel.send(LoginEvent.Start)
        advanceUntilIdle()

        assertThat(viewModel.step.value).isEqualTo(LoginStep.CollectOtpIdentifier())
        coVerify(exactly = 2) { interactor.discover() }
    }

    @Test
    fun `start - after leaving from email - begins a new flow`() = runTest {
        startAtEmail()
        viewModel.goBack()

        viewModel.send(LoginEvent.Start)
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.discover() }
    }

    // endregion

    // region back

    @Test
    fun `back - on the code step - returns to email with the email kept`() = runTest {
        startAtCode()

        assertThat(viewModel.goBack()).isTrue()
        assertThat(viewModel.step.value).isEqualTo(LoginStep.CollectOtpIdentifier(EMAIL))
    }

    @Test
    fun `back - on the code step - clears the inline error`() = runTest {
        startAtCode()
        coEvery { interactor.verifyOtp(CODE) } throws AuthFailureException(AuthFailure.INVALID_CODE)
        viewModel.send(LoginEvent.SubmitOtp(CODE))
        advanceUntilIdle()

        viewModel.goBack()

        assertThat(viewModel.inputError.value).isNull()
    }

    @Test
    fun `back - while verifying - stays in the flow`() = runTest {
        startAtCode()
        val answer = CompletableDeferred<AuthOutcome>()
        coEvery { interactor.verifyOtp(CODE) } coAnswers { answer.await() }
        viewModel.send(LoginEvent.SubmitOtp(CODE))
        runCurrent()

        assertThat(viewModel.goBack()).isTrue()
        assertThat(viewModel.step.value).isEqualTo(LoginStep.Loading)

        answer.complete(AuthOutcome.Authenticated(mockk()))
        advanceUntilIdle()

        assertThat(viewModel.step.value).isEqualTo(LoginStep.Authenticated)
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

    private fun TestScope.startAtCode() {
        startAtEmail()
        viewModel.send(LoginEvent.SubmitIdentifier(EMAIL))
        advanceUntilIdle()
    }

    private companion object {
        const val EMAIL = "jane@example.com"
        const val CODE = "123456"
        const val MFA_CODE = "654321"
    }
}
