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
import com.auth0.universalcomponents.presentation.ui.mfa.phone.Country
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
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
import kotlinx.coroutines.test.TestCoroutineScheduler
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

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
        coEvery { repository.startOtp(any(), any()) } returns Result.Success(null)
        // Who is identified decides what the server offers: a code to an email or a phone, a password to a username.
        coEvery { repository.identify(LoginOption.EmailOtp, any()) } returns Result.Success(listOf(LoginOption.EmailOtp))
        coEvery { repository.identify(LoginOption.PhoneOtp, any()) } returns Result.Success(listOf(LoginOption.PhoneOtp))
        coEvery { repository.identify(LoginOption.Username, any()) } returns
            Result.Success(listOf(LoginOption.Password))
        mfaRepository = mockk()
        coEvery { mfaRepository.sendPush(any()) } returns Result.Success(PushStatus.Pending(PUSH_POLL_MS, canResend = true))
        // A push nobody answers, unless a test says otherwise, so MFA stays on screen.
        coEvery { mfaRepository.checkPushStatus() } coAnswers { awaitCancellation() }
        viewModel = LoginFlowViewModel(repository, mfaRepository, VirtualTimeSource(testDispatcher.scheduler))
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
        val sent = CompletableDeferred<String?>()
        coEvery { repository.startOtp(LoginOption.EmailOtp, EMAIL) } coAnswers { Result.Success(sent.await()) }

        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.EmailOtp, EMAIL))
        runCurrent()

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpIdentifier(value = EMAIL, isSubmitting = true))
        sent.complete(null)
        advanceUntilIdle()
    }

    @Test
    fun `submit email - code sent - asks for the code, with the resend countdown started`() = runTest {
        startAtEmail()

        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.EmailOtp, EMAIL))
        runCurrent()

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpCode(EMAIL, resendSecondsLeft = COOLDOWN_SECONDS))
        coVerify(exactly = 1) { repository.startOtp(LoginOption.EmailOtp, EMAIL) }
    }

    @Test
    fun `submit email - fails for good - retry returns to email with the typed email`() = runTest {
        startAtEmail()
        coEvery { repository.startOtp(LoginOption.EmailOtp, EMAIL) } returns failure(EmbeddedLoginError.Unknown("debug output"))
        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.EmailOtp, EMAIL))
        advanceUntilIdle()
        assertThat(viewModel.loginState.value).isInstanceOf(LoginState.Failed::class.java)

        viewModel.send(LoginIntent.Retry)
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(value = EMAIL))
    }

    @Test
    fun `submit email - not on the email step - is ignored`() = runTest {
        startAtCode()

        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.EmailOtp, OTHER_EMAIL))
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.startOtp(LoginOption.EmailOtp, OTHER_EMAIL) }
    }

    // endregion

    // region phone

    @Test
    fun `start - email and phone offered - starts on email, with the way to the phone`() = runTest {
        startWithMethods(LoginOption.EmailOtp, LoginOption.PhoneOtp)

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpIdentifier(otherMethods = listOf(LoginOption.PhoneOtp)))
    }

    @Test
    fun `start - phone listed before email - phone is the first screen, with the way to email`() = runTest {
        startWithMethods(LoginOption.PhoneOtp, LoginOption.EmailOtp)

        assertThat(viewModel.loginState.value).isEqualTo(
            LoginState.CollectOtpIdentifier(LoginOption.PhoneOtp, otherMethods = listOf(LoginOption.EmailOtp)),
        )
    }

    @Test
    fun `start - only phone offered - starts on phone, with nothing to switch to`() = runTest {
        startWithMethods(LoginOption.PhoneOtp)

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(LoginOption.PhoneOtp))
    }

    @Test
    fun `switch - to phone - shows phone entry, which has no way to switch back`() = runTest {
        startWithMethods(LoginOption.EmailOtp, LoginOption.PhoneOtp)

        viewModel.send(LoginIntent.SwitchMethod(LoginOption.PhoneOtp))

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpIdentifier(LoginOption.PhoneOtp, isFirst = false))
    }

    @Test
    fun `switch - while the email code is being sent - is ignored`() = runTest {
        startWithMethods(LoginOption.EmailOtp, LoginOption.PhoneOtp)
        val sent = CompletableDeferred<String?>()
        coEvery { repository.startOtp(LoginOption.EmailOtp, EMAIL) } coAnswers { Result.Success(sent.await()) }
        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.EmailOtp, EMAIL))
        runCurrent()

        viewModel.send(LoginIntent.SwitchMethod(LoginOption.PhoneOtp))
        sent.complete(null)
        runCurrent()

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpCode(EMAIL, resendSecondsLeft = COOLDOWN_SECONDS))
    }

    @Test
    fun `switch - phone not offered, or not on the first screen - is ignored`() = runTest {
        startAtEmail()
        viewModel.send(LoginIntent.SwitchMethod(LoginOption.PhoneOtp))
        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier())

        startWithMethods(LoginOption.EmailOtp, LoginOption.PhoneOtp, restart = true)
        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.EmailOtp, EMAIL))
        advanceUntilIdle()
        viewModel.send(LoginIntent.SwitchMethod(LoginOption.PhoneOtp))

        assertThat(viewModel.loginState.value).isInstanceOf(LoginState.CollectOtpCode::class.java)
    }

    @Test
    fun `submit phone - code sent - asks for the code, sent to that number`() = runTest {
        startAtPhone()

        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.PhoneOtp, NUMBER))
        runCurrent()

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpCode(NUMBER, LoginOption.PhoneOtp, resendSecondsLeft = COOLDOWN_SECONDS))
        coVerify(exactly = 1) { repository.startOtp(LoginOption.PhoneOtp, NUMBER) }
    }

    @Test
    fun `submit - typed on the email screen after the switch to phone - is dropped`() = runTest {
        startAtPhone()

        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.EmailOtp, EMAIL))
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.startOtp(any(), any()) }
        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpIdentifier(LoginOption.PhoneOtp, isFirst = false))
    }

    @Test
    fun `submit phone - fails for good - retry returns to phone with the number kept`() = runTest {
        startAtPhone()
        val error = EmbeddedLoginError.Unknown("debug output")
        coEvery { repository.startOtp(LoginOption.PhoneOtp, NUMBER) } returns failure(error)
        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.PhoneOtp, NUMBER))
        advanceUntilIdle()
        assertThat(viewModel.loginState.value).isInstanceOf(LoginState.Failed::class.java)

        viewModel.send(LoginIntent.Retry)
        advanceUntilIdle()

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpIdentifier(LoginOption.PhoneOtp, NUMBER, isFirst = false))
    }

    @Test
    fun `submit phone - server refuses the number - shows it full-screen, retry returns to phone with the number kept`() = runTest {
        startAtPhone()
        val error = EmbeddedLoginError.InvalidIdentifier(LoginOption.PhoneOtp)
        coEvery { repository.startOtp(LoginOption.PhoneOtp, NUMBER) } returns failure(error)
        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.PhoneOtp, NUMBER))
        advanceUntilIdle()
        assertThat(viewModel.loginState.value).isInstanceOf(LoginState.Failed::class.java)

        viewModel.send(LoginIntent.Retry)
        advanceUntilIdle()

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpIdentifier(LoginOption.PhoneOtp, NUMBER, isFirst = false))
    }

    @Test
    fun `submit phone - with a country - going back to edit shows that country again`() = runTest {
        startAtPhone()
        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.PhoneOtp, NUMBER, CANADA))
        runCurrent()

        viewModel.goBack()

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpIdentifier(LoginOption.PhoneOtp, NUMBER, country = CANADA, isFirst = false))
    }

    @Test
    fun `submit phone - with a country, fails for good - retry shows that country again`() = runTest {
        startAtPhone()
        val error = EmbeddedLoginError.Unknown("debug output")
        coEvery { repository.startOtp(LoginOption.PhoneOtp, NUMBER) } returns failure(error)
        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.PhoneOtp, NUMBER, CANADA))
        advanceUntilIdle()

        viewModel.send(LoginIntent.Retry)
        advanceUntilIdle()

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpIdentifier(LoginOption.PhoneOtp, NUMBER, country = CANADA, isFirst = false))
    }

    @Test
    fun `submit phone - code sent - the code step carries where the server says it went`() = runTest {
        startAtPhone()
        coEvery { repository.startOtp(LoginOption.PhoneOtp, NUMBER) } returns Result.Success("XXXXXXXXX0128")

        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.PhoneOtp, NUMBER))
        runCurrent()

        assertThat(viewModel.loginState.value).isEqualTo(
            LoginState.CollectOtpCode(
                NUMBER,
                LoginOption.PhoneOtp,
                resendSecondsLeft = COOLDOWN_SECONDS,
                sentTo = "XXXXXXXXX0128",
            ),
        )
    }

    @Test
    fun `submit phone - code step - keeps where it went through a failed check and Try again`() = runTest {
        startAtPhone()
        coEvery { repository.startOtp(LoginOption.PhoneOtp, NUMBER) } returns Result.Success("XXXXXXXXX0128")
        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.PhoneOtp, NUMBER))
        advanceUntilIdle()
        coEvery { repository.verifyOtp(CODE) } returns failure(EmbeddedLoginError.Network, canRetryStep = true)
        viewModel.send(LoginIntent.SubmitOtp(CODE))
        advanceUntilIdle()

        viewModel.send(LoginIntent.Retry)
        advanceUntilIdle()

        assertThat((viewModel.loginState.value as LoginState.CollectOtpCode).sentTo).isEqualTo("XXXXXXXXX0128")
    }

    @Test
    fun `resend - phone code - keeps where it went`() = runTest {
        startAtPhone()
        coEvery { repository.startOtp(LoginOption.PhoneOtp, NUMBER) } returns Result.Success("XXXXXXXXX0128")
        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.PhoneOtp, NUMBER))
        advanceUntilIdle()

        viewModel.send(LoginIntent.Resend)
        advanceUntilIdle()

        assertThat((viewModel.loginState.value as LoginState.CollectOtpCode).sentTo).isEqualTo("XXXXXXXXX0128")
    }

    @Test
    fun `resend - phone code - sends to the same number again`() = runTest {
        startAtPhoneCode()

        viewModel.send(LoginIntent.Resend)
        runCurrent()

        coVerify(exactly = 2) { repository.startOtp(LoginOption.PhoneOtp, NUMBER) }
        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpCode(NUMBER, LoginOption.PhoneOtp, resendSecondsLeft = COOLDOWN_SECONDS))
    }

    @Test
    fun `back - phone code, then phone entry, then email - steps back one screen at a time and then leaves`() = runTest {
        startAtPhoneCode()

        assertThat(viewModel.goBack()).isTrue()
        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpIdentifier(LoginOption.PhoneOtp, NUMBER, isFirst = false))

        assertThat(viewModel.goBack()).isTrue()
        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpIdentifier(otherMethods = listOf(LoginOption.PhoneOtp)))

        assertThat(viewModel.goBack()).isFalse()
    }

    @Test
    fun `back - phone entry as the only method - leaves the flow`() = runTest {
        startWithMethods(LoginOption.PhoneOtp)

        assertThat(viewModel.goBack()).isFalse()
    }

    @Test
    fun `back - while the phone code is being sent - stays in the flow`() = runTest {
        startAtPhone()
        val sent = CompletableDeferred<String?>()
        coEvery { repository.startOtp(LoginOption.PhoneOtp, NUMBER) } coAnswers { Result.Success(sent.await()) }
        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.PhoneOtp, NUMBER))
        runCurrent()

        assertThat(viewModel.goBack()).isTrue()
        assertThat(viewModel.loginState.value)
            .isEqualTo(
                LoginState.CollectOtpIdentifier(LoginOption.PhoneOtp, NUMBER, isSubmitting = true, isFirst = false),
            )

        sent.complete(null)
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isInstanceOf(LoginState.CollectOtpCode::class.java)
    }

    @Test
    fun `second factor after a phone code - start over - returns to the first screen, which is email`() = runTest {
        startAtPhoneCode()
        coEvery { repository.verifyOtp(CODE) } returns failure(EmbeddedLoginError.MfaRequired(listOf(TABLET)))
        viewModel.send(LoginIntent.SubmitOtp(CODE))
        advanceUntilIdle()

        viewModel.send(LoginIntent.LeaveMfa)

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpIdentifier(otherMethods = listOf(LoginOption.PhoneOtp)))
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

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(value = EMAIL))
        // Back from here would exit the login (false); had the second tap been Back, the screen would be gone.
        assertThat(viewModel.goBack()).isFalse()
    }

    @Test
    fun `mfa - Back - is passed to MFA, which decides what it means`() = runTest {
        startAtMfa(PHONE, TABLET)

        assertThat(viewModel.goBack()).isTrue()
        advanceUntilIdle()

        // Back on the list leaves MFA; had login handled it, the list would still be on show.
        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(value = EMAIL))
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
            assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(value = EMAIL))

            viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.EmailOtp, EMAIL))
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
        startAtMfa()

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
        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(value = EMAIL))
    }

    // region app told about MFA errors

    @Test
    fun `mfa error - no usable factor - waits for the app behind a spinner and tells it once`() = runTest {
        val reported = mutableListOf<MfaError>()
        val job = launch { mfaFailedEvents.toList(reported) }

        startAtMfa()
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isInstanceOf(LoginState.AwaitingApp::class.java)
        assertThat(reported).hasSize(1)
        assertThat(reported.single()).isInstanceOf(MfaError.NoEligibleFactors::class.java)
        job.cancel()
    }

    @Test
    fun `mfa error - no usable factor and the app handles it - stays on the spinner`() = runTest {
        startAtMfa()

        viewModel.send(LoginIntent.MfaErrorAnswered(MfaErrorHandling.Handled))
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isInstanceOf(LoginState.AwaitingApp::class.java)
    }

    @Test
    fun `mfa error - no usable factor and the app does not handle it - shows the not available screen`() = runTest {
        startAtMfa()

        viewModel.send(LoginIntent.MfaErrorAnswered(MfaErrorHandling.NotHandled))
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isInstanceOf(LoginState.Mfa::class.java)
        assertThat((mfaStep() as MfaState.Failed).error).isInstanceOf(MfaError.NoEligibleFactors::class.java)

        // From there, "Start over" still returns to email entry with the email kept.
        viewModel.send(LoginIntent.LeaveMfa)
        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(value = EMAIL))
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

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(value = EMAIL))
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
        startAtMfa()

        assertThat(viewModel.goBack()).isTrue()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(value = EMAIL))
    }

    @Test
    fun `mfa error - the app answers after the user went back - the answer is dropped`() = runTest {
        startAtMfa()
        viewModel.goBack()

        viewModel.send(LoginIntent.MfaErrorAnswered(MfaErrorHandling.NotHandled))

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(value = EMAIL))
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

        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.EmailOtp, EMAIL))
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
    fun `submit code - too many wrong codes - stays on the code step with the error under the field`() = runTest {
        startAtCode()
        coEvery { repository.verifyOtp(CODE) } returns failure(EmbeddedLoginError.TooManyWrongCodes)

        viewModel.send(LoginIntent.SubmitOtp(CODE))
        advanceUntilIdle()

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpCode(EMAIL, error = EmbeddedLoginError.TooManyWrongCodes))
    }

    @Test
    fun `submit code - too many wrong codes, then another - says the code is no longer valid, with resend open`() = runTest {
        startAtCodeCountingDown()
        coEvery { repository.verifyOtp(CODE) } returnsMany listOf(
            failure(EmbeddedLoginError.TooManyWrongCodes),
            failure(EmbeddedLoginError.CodeNoLongerValid),
        )
        viewModel.send(LoginIntent.SubmitOtp(CODE))
        runCurrent()
        // The wait is still running after the rate limit: only a code that can't be used opens Resend.
        assertThat((viewModel.loginState.value as LoginState.CollectOtpCode).resendSecondsLeft)
            .isEqualTo(COOLDOWN_SECONDS)

        viewModel.send(LoginIntent.SubmitOtp(CODE))
        runCurrent()

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpCode(EMAIL, error = EmbeddedLoginError.CodeNoLongerValid))
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
    fun `submit code - fails and the session is gone - retry returns to the code step all the same`() = runTest {
        startAtCode()
        coEvery { repository.verifyOtp(CODE) } returns failure(EmbeddedLoginError.Unknown("debug output"))
        viewModel.send(LoginIntent.SubmitOtp(CODE))
        advanceUntilIdle()

        viewModel.send(LoginIntent.Retry)
        advanceUntilIdle()

        // The next Verify says the code is no longer valid, if the server ended it.
        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpCode(EMAIL))
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
        val sent = CompletableDeferred<String?>()
        coEvery { repository.startOtp(LoginOption.EmailOtp, EMAIL) } coAnswers { Result.Success(sent.await()) }

        viewModel.send(LoginIntent.Resend)
        runCurrent()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpCode(EMAIL, isResending = true))

        sent.complete(null)
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpCode(EMAIL))
        coVerify(exactly = 2) { repository.startOtp(LoginOption.EmailOtp, EMAIL) }
    }

    @Test
    fun `resend - fails for good - shows it full-screen, retry returns to email`() = runTest {
        startAtCode()
        val error = EmbeddedLoginError.Unknown("debug output")
        coEvery { repository.startOtp(LoginOption.EmailOtp, EMAIL) } returns failure(error)

        viewModel.send(LoginIntent.Resend)
        advanceUntilIdle()

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.Failed(error, retryTo = LoginState.CollectOtpIdentifier(value = EMAIL)))
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

        coVerify(exactly = 1) { repository.startOtp(LoginOption.EmailOtp, EMAIL) }
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
    fun `countdown - ticks held back while the device sleeps - the next one shows the time that really passed`() =
        runTest {
            val clock = VirtualTimeSource(testDispatcher.scheduler)
            viewModel = LoginFlowViewModel(repository, mfaRepository, clock)
            startAtCodeCountingDown()

            // The next tick comes a second on, but the device slept 45 seconds meanwhile.
            clock.sleep(45.seconds)
            advanceTimeBy(ONE_SECOND_MILLIS)
            runCurrent()

            assertThat(viewModel.loginState.value)
                .isEqualTo(LoginState.CollectOtpCode(EMAIL, resendSecondsLeft = COOLDOWN_SECONDS - 46))
        }

    @Test
    fun `countdown - the whole wait passes while the device sleeps - opens resend`() = runTest {
        val clock = VirtualTimeSource(testDispatcher.scheduler)
        viewModel = LoginFlowViewModel(repository, mfaRepository, clock)
        startAtCodeCountingDown()

        clock.sleep((COOLDOWN_SECONDS + 5).seconds)
        advanceTimeBy(ONE_SECOND_MILLIS)
        runCurrent()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpCode(EMAIL))
    }

    @Test
    fun `countdown - back to email while it runs - leaves email as it is`() = runTest {
        startAtCodeCountingDown()

        viewModel.goBack()
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(value = EMAIL))
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
        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(value = EMAIL))
    }

    @Test
    fun `back - on the code step - drops the inline error`() = runTest {
        startAtCode()
        coEvery { repository.verifyOtp(CODE) } returns failure(EmbeddedLoginError.InvalidCode)
        viewModel.send(LoginIntent.SubmitOtp(CODE))
        advanceUntilIdle()

        viewModel.goBack()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(value = EMAIL))
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

    // region tenant's order

    @Test
    fun `start - username only - starts on username, with nothing to switch to`() = runTest {
        startWithMethods(LoginOption.Username)

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(LoginOption.Username))
    }

    @Test
    fun `start - three methods - the first listed leads and the others follow in order`() = runTest {
        startWithMethods(LoginOption.PhoneOtp, LoginOption.EmailOtp, LoginOption.Username)

        assertThat(viewModel.loginState.value).isEqualTo(
            LoginState.CollectOtpIdentifier(
                LoginOption.PhoneOtp,
                otherMethods = listOf(LoginOption.EmailOtp, LoginOption.Username),
            ),
        )
    }

    @Test
    fun `switch - to username - opens its own screen, with no others, and Back returns to the first`() = runTest {
        startWithMethods(LoginOption.EmailOtp, LoginOption.Username)

        viewModel.send(LoginIntent.SwitchMethod(LoginOption.Username))

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpIdentifier(LoginOption.Username, isFirst = false))
        assertThat(viewModel.goBack()).isTrue()
        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpIdentifier(otherMethods = listOf(LoginOption.Username)))
    }

    @Test
    fun `start - a method the screens can't draw is skipped`() = runTest {
        startWithMethods(LoginOption.Unsupported("identify_other"), LoginOption.PhoneOtp)

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(LoginOption.PhoneOtp))
    }

    // endregion

    // region after identify

    @Test
    fun `submit email - code leads, password offered - sends the code on the same session and offers the password`() =
        runTest {
            startAtEmail()
            offerToEmail(LoginOption.EmailOtp, LoginOption.Password)

            viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.EmailOtp, EMAIL))
            runCurrent()

            assertThat(viewModel.loginState.value).isEqualTo(
                LoginState.CollectOtpCode(EMAIL, resendSecondsLeft = COOLDOWN_SECONDS, canUsePassword = true),
            )
            coVerifyOrder {
                repository.identify(LoginOption.EmailOtp, EMAIL)
                repository.startOtp(LoginOption.EmailOtp, EMAIL)
            }
        }

    @Test
    fun `submit email - password leads, code offered - asks for the password and sends nothing`() = runTest {
        startAtEmail()
        offerToEmail(LoginOption.Password, LoginOption.EmailOtp)

        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.EmailOtp, EMAIL))
        advanceUntilIdle()

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectPassword(EMAIL, LoginOption.EmailOtp, canSendCode = true))
        coVerify(exactly = 0) { repository.startOtp(any(), any()) }
    }

    @Test
    fun `submit email - password only - asks for the password with no way to a code`() = runTest {
        startAtEmail()
        offerToEmail(LoginOption.Password)

        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.EmailOtp, EMAIL))
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectPassword(EMAIL, LoginOption.EmailOtp))
    }

    @Test
    fun `submit phone - nothing offered, as when the server lists only a password - says the code was not sent`() =
        runTest {
            startAtPhone()
            // A phone is not offered a password yet, so the repository leaves one out: nothing is left to send.
            coEvery { repository.identify(LoginOption.PhoneOtp, NUMBER) } returns Result.Success(emptyList())

            viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.PhoneOtp, NUMBER))
            advanceUntilIdle()

            assertThat(viewModel.loginState.value).isEqualTo(
                LoginState.CollectOtpIdentifier(
                    LoginOption.PhoneOtp,
                    NUMBER,
                    error = EmbeddedLoginError.CodeNotSent(LoginOption.PhoneOtp),
                    isFirst = false,
                ),
            )
        }

    @Test
    fun `submit email - nothing offered - says the code was not sent, under the email field`() = runTest {
        startAtEmail()
        offerToEmail()

        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.EmailOtp, EMAIL))
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(
            LoginState.CollectOtpIdentifier(
                value = EMAIL,
                error = EmbeddedLoginError.CodeNotSent(LoginOption.EmailOtp),
            ),
        )
    }

    @Test
    fun `submit username - asks for the password with no way to a code`() = runTest {
        startWithMethods(LoginOption.Username)

        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.Username, USERNAME))
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectPassword(USERNAME, LoginOption.Username))
        coVerify(exactly = 0) { repository.startOtp(any(), any()) }
    }

    @Test
    fun `submit username - no password step - says so under the username field, as a wrong password`() = runTest {
        startWithMethods(LoginOption.Username)
        val error = EmbeddedLoginError.InvalidPassword(LoginOption.Username)
        coEvery { repository.identify(LoginOption.Username, USERNAME) } returns failure(error)

        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.Username, USERNAME))
        advanceUntilIdle()

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpIdentifier(LoginOption.Username, USERNAME, error = error))
    }

    @Test
    fun `submit email - while the server is asked who it is - Continue spins and a second tap is dropped`() = runTest {
        startAtEmail()
        val answer = CompletableDeferred<List<LoginOption>>()
        coEvery { repository.identify(LoginOption.EmailOtp, EMAIL) } coAnswers { Result.Success(answer.await()) }

        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.EmailOtp, EMAIL))
        runCurrent()
        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.EmailOtp, EMAIL))

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpIdentifier(value = EMAIL, isSubmitting = true))
        answer.complete(listOf(LoginOption.EmailOtp))
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.identify(LoginOption.EmailOtp, EMAIL) }
    }

    // endregion

    // region where each failure shows

    @Test
    fun `submit email - identify fails - shows it full-screen, retry returns to email with the email kept`() = runTest {
        startAtEmail()
        coEvery { repository.identify(LoginOption.EmailOtp, EMAIL) } returns failure(EmbeddedLoginError.Network)
        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.EmailOtp, EMAIL))
        advanceUntilIdle()
        assertThat((viewModel.loginState.value as LoginState.Failed).error).isEqualTo(EmbeddedLoginError.Network)

        viewModel.send(LoginIntent.Retry)
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(value = EMAIL))
    }

    @Test
    fun `submit email - too many attempts - stays on email with the error under the field`() = runTest {
        startAtEmail()
        coEvery { repository.identify(LoginOption.EmailOtp, EMAIL) } returns failure(EmbeddedLoginError.TooManyAttempts)

        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.EmailOtp, EMAIL))
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(
            LoginState.CollectOtpIdentifier(value = EMAIL, error = EmbeddedLoginError.TooManyAttempts),
        )
    }

    @Test
    fun `submit email - code not sent - says so under the email field`() = runTest {
        startAtEmail()
        val error = EmbeddedLoginError.CodeNotSent(LoginOption.EmailOtp)
        coEvery { repository.startOtp(LoginOption.EmailOtp, EMAIL) } returns failure(error)

        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.EmailOtp, EMAIL))
        advanceUntilIdle()

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpIdentifier(value = EMAIL, error = error))
    }

    @Test
    fun `submit phone - code not sent - says so under the phone field, with the number and country kept`() = runTest {
        startAtPhone()
        val error = EmbeddedLoginError.CodeNotSent(LoginOption.PhoneOtp)
        coEvery { repository.startOtp(LoginOption.PhoneOtp, NUMBER) } returns failure(error)

        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.PhoneOtp, NUMBER, CANADA))
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(
            LoginState.CollectOtpIdentifier(
                LoginOption.PhoneOtp,
                NUMBER,
                country = CANADA,
                error = error,
                isFirst = false,
            ),
        )
    }

    @Test
    fun `resend - code not sent - says so on the identifier screen`() = runTest {
        startAtCode()
        val error = EmbeddedLoginError.CodeNotSent(LoginOption.EmailOtp)
        coEvery { repository.startOtp(LoginOption.EmailOtp, EMAIL) } returns failure(error)

        viewModel.send(LoginIntent.Resend)
        advanceUntilIdle()

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpIdentifier(value = EMAIL, error = error))
    }

    @Test
    fun `resend - too many attempts - stays on the code step with the error`() = runTest {
        startAtCode()
        coEvery { repository.startOtp(LoginOption.EmailOtp, EMAIL) } returns failure(EmbeddedLoginError.TooManyAttempts)

        viewModel.send(LoginIntent.Resend)
        advanceUntilIdle()

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpCode(EMAIL, error = EmbeddedLoginError.TooManyAttempts))
    }

    @Test
    fun `resend - after a code that needs replacing - is open at once`() = runTest {
        startAtCodeCountingDown()
        coEvery { repository.verifyOtp(CODE) } returns failure(EmbeddedLoginError.CodeNoLongerValid)
        viewModel.send(LoginIntent.SubmitOtp(CODE))
        runCurrent()

        viewModel.send(LoginIntent.Resend)
        runCurrent()

        coVerify(exactly = 2) { repository.startOtp(LoginOption.EmailOtp, EMAIL) }
    }

    // endregion

    // region "Continue with a password"

    @Test
    fun `use password - on the code step - asks the server again, then shows the password, the wait carried`() = runTest {
        startAtCodeWithPassword()
        viewModel.send(LoginIntent.UsePassword)
        runCurrent()

        assertThat(viewModel.loginState.value).isEqualTo(
            LoginState.CollectPassword(
                EMAIL,
                LoginOption.EmailOtp,
                canSendCode = true,
                resendSecondsLeft = COOLDOWN_SECONDS,
            ),
        )
        coVerify(exactly = 2) { repository.identify(LoginOption.EmailOtp, EMAIL) }
    }

    @Test
    fun `use password - while the server is asked - only that button is busy`() = runTest {
        startAtCodeWithPassword()
        val answer = CompletableDeferred<List<LoginOption>>()
        coEvery { repository.identify(LoginOption.EmailOtp, EMAIL) } coAnswers { Result.Success(answer.await()) }

        viewModel.send(LoginIntent.UsePassword)
        runCurrent()

        assertThat(viewModel.loginState.value).isEqualTo(
            LoginState.CollectOtpCode(
                EMAIL,
                resendSecondsLeft = COOLDOWN_SECONDS,
                canUsePassword = true,
                isOpeningPassword = true,
            ),
        )
        // While it is on its way nothing else on the screen answers.
        viewModel.send(LoginIntent.UsePassword)
        viewModel.send(LoginIntent.Resend)
        assertThat(viewModel.goBack()).isTrue()
        answer.complete(listOf(LoginOption.Password))
        advanceUntilIdle()
        assertThat(viewModel.loginState.value).isInstanceOf(LoginState.CollectPassword::class.java)
    }

    @Test
    fun `use password - the server no longer offers one - says so on the identifier screen`() = runTest {
        startAtCodeWithPassword()
        offerToEmail(LoginOption.EmailOtp)

        viewModel.send(LoginIntent.UsePassword)
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(
            LoginState.CollectOtpIdentifier(value = EMAIL, error = EmbeddedLoginError.PasswordNotAvailable),
        )
    }

    @Test
    fun `use password - too many attempts - stays on the code step with the error`() = runTest {
        startAtCodeWithPassword()
        coEvery { repository.identify(LoginOption.EmailOtp, EMAIL) } returns failure(EmbeddedLoginError.TooManyAttempts)

        viewModel.send(LoginIntent.UsePassword)
        runCurrent()

        assertThat(viewModel.loginState.value).isEqualTo(
            LoginState.CollectOtpCode(
                EMAIL,
                error = EmbeddedLoginError.TooManyAttempts,
                resendSecondsLeft = COOLDOWN_SECONDS,
                canUsePassword = true,
            ),
        )
    }

    @Test
    fun `use password - any other failure - shows it full-screen, retry returns to email`() = runTest {
        startAtCodeWithPassword()
        coEvery { repository.identify(LoginOption.EmailOtp, EMAIL) } returns failure(EmbeddedLoginError.Network)

        viewModel.send(LoginIntent.UsePassword)
        advanceUntilIdle()
        viewModel.send(LoginIntent.Retry)
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(value = EMAIL))
    }

    @Test
    fun `use password - no password on offer - is ignored`() = runTest {
        startAtCode()

        viewModel.send(LoginIntent.UsePassword)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.identify(LoginOption.EmailOtp, EMAIL) }
    }

    @Test
    fun `resend - with a password on offer - keeps the way to it`() = runTest {
        startAtCodeWithPassword()
        advanceTimeBy(COOLDOWN_SECONDS * ONE_SECOND_MILLIS)
        runCurrent()

        viewModel.send(LoginIntent.Resend)
        runCurrent()

        assertThat(viewModel.loginState.value).isEqualTo(
            LoginState.CollectOtpCode(EMAIL, resendSecondsLeft = COOLDOWN_SECONDS, canUsePassword = true),
        )
    }

    // endregion

    // region "Email me a code"

    @Test
    fun `send code - on the password step - sends it, and the code step offers the password again`() = runTest {
        startAtPassword(canSendCode = true)

        viewModel.send(LoginIntent.SendCode)
        runCurrent()

        assertThat(viewModel.loginState.value).isEqualTo(
            LoginState.CollectOtpCode(EMAIL, resendSecondsLeft = COOLDOWN_SECONDS, canUsePassword = true),
        )
        coVerify(exactly = 1) { repository.startOtp(LoginOption.EmailOtp, EMAIL) }
    }

    @Test
    fun `send code - while it is on its way - only that button is busy`() = runTest {
        startAtPassword(canSendCode = true)
        val sent = CompletableDeferred<String?>()
        coEvery { repository.startOtp(LoginOption.EmailOtp, EMAIL) } coAnswers { Result.Success(sent.await()) }

        viewModel.send(LoginIntent.SendCode)
        runCurrent()

        assertThat(viewModel.loginState.value).isEqualTo(
            LoginState.CollectPassword(EMAIL, LoginOption.EmailOtp, canSendCode = true, isSendingCode = true),
        )
        viewModel.send(LoginIntent.SubmitPassword(PASSWORD))
        viewModel.send(LoginIntent.SendCode)
        assertThat(viewModel.goBack()).isTrue()
        sent.complete(null)
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.startOtp(LoginOption.EmailOtp, EMAIL) }
        coVerify(exactly = 0) { repository.verifyPassword(any(), any(), any()) }
    }

    @Test
    fun `send code - during the wait after a code was sent - is ignored until it is over`() = runTest {
        startAtCodeWithPassword()
        viewModel.send(LoginIntent.UsePassword)
        runCurrent()

        viewModel.send(LoginIntent.SendCode)
        runCurrent()
        coVerify(exactly = 1) { repository.startOtp(LoginOption.EmailOtp, EMAIL) }

        advanceTimeBy(COOLDOWN_SECONDS * ONE_SECOND_MILLIS)
        runCurrent()
        assertThat((viewModel.loginState.value as LoginState.CollectPassword).resendSecondsLeft).isEqualTo(0)
        viewModel.send(LoginIntent.SendCode)
        runCurrent()

        coVerify(exactly = 2) { repository.startOtp(LoginOption.EmailOtp, EMAIL) }
    }

    @Test
    fun `send code - the wait runs on across Back and Continue - and shows on the password step`() = runTest {
        startAtCodeWithPassword()
        advanceTimeBy(10 * ONE_SECOND_MILLIS)
        runCurrent()
        viewModel.goBack()
        offerToEmail(LoginOption.Password, LoginOption.EmailOtp)

        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.EmailOtp, EMAIL))
        runCurrent()

        assertThat(viewModel.loginState.value).isEqualTo(
            LoginState.CollectPassword(
                EMAIL,
                LoginOption.EmailOtp,
                canSendCode = true,
                resendSecondsLeft = COOLDOWN_SECONDS - 10,
            ),
        )
        advanceTimeBy(ONE_SECOND_MILLIS)
        runCurrent()
        assertThat((viewModel.loginState.value as LoginState.CollectPassword).resendSecondsLeft)
            .isEqualTo(COOLDOWN_SECONDS - 11)
    }

    @Test
    fun `send code - the wait of someone else - does not hold it back`() = runTest {
        startAtCodeWithPassword()
        viewModel.goBack()
        offerToEmail(LoginOption.Password, LoginOption.EmailOtp)

        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.EmailOtp, OTHER_EMAIL))
        runCurrent()

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectPassword(OTHER_EMAIL, LoginOption.EmailOtp, canSendCode = true))
    }

    @Test
    fun `send code - code not sent - says so on the identifier screen`() = runTest {
        startAtPassword(canSendCode = true)
        val error = EmbeddedLoginError.CodeNotSent(LoginOption.EmailOtp)
        coEvery { repository.startOtp(LoginOption.EmailOtp, EMAIL) } returns failure(error)

        viewModel.send(LoginIntent.SendCode)
        advanceUntilIdle()

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpIdentifier(value = EMAIL, error = error))
    }

    @Test
    fun `send code - too many attempts - stays on the password step with the error`() = runTest {
        startAtPassword(canSendCode = true)
        coEvery { repository.startOtp(LoginOption.EmailOtp, EMAIL) } returns failure(EmbeddedLoginError.TooManyAttempts)

        viewModel.send(LoginIntent.SendCode)
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(
            LoginState.CollectPassword(
                EMAIL,
                LoginOption.EmailOtp,
                canSendCode = true,
                error = EmbeddedLoginError.TooManyAttempts,
            ),
        )
    }

    @Test
    fun `send code - no code on offer - is ignored`() = runTest {
        startAtPassword(canSendCode = false)

        viewModel.send(LoginIntent.SendCode)
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.startOtp(any(), any()) }
    }

    // endregion

    // region password

    @Test
    fun `submit password - accepted - signs in and hands over the credentials`() = runTest {
        startAtPassword()
        val credentials = mockk<Credentials>()
        coEvery { repository.verifyPassword(LoginOption.EmailOtp, EMAIL, PASSWORD) } returns
            Result.Success(credentials)
        val received = mutableListOf<Credentials>()
        val job = launch { authenticatedEvents.toList(received) }

        viewModel.send(LoginIntent.SubmitPassword(PASSWORD))
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.Authenticated)
        assertThat(received).containsExactly(credentials)
        job.cancel()
    }

    @Test
    fun `submit password - while checking - Continue spins, and a second tap is dropped`() = runTest {
        startAtPassword()
        val answer = CompletableDeferred<Credentials>()
        coEvery { repository.verifyPassword(LoginOption.EmailOtp, EMAIL, PASSWORD) } coAnswers {
            Result.Success(answer.await())
        }

        viewModel.send(LoginIntent.SubmitPassword(PASSWORD))
        runCurrent()
        viewModel.send(LoginIntent.SubmitPassword(PASSWORD))

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectPassword(EMAIL, LoginOption.EmailOtp, isSubmitting = true))
        answer.complete(mockk())
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.verifyPassword(LoginOption.EmailOtp, EMAIL, PASSWORD) }
    }

    @Test
    fun `submit password - wrong - stays on the password step with the error`() = runTest {
        startAtPassword(canSendCode = true)
        val error = EmbeddedLoginError.InvalidPassword(LoginOption.EmailOtp)
        coEvery { repository.verifyPassword(LoginOption.EmailOtp, EMAIL, PASSWORD) } returns failure(error)

        viewModel.send(LoginIntent.SubmitPassword(PASSWORD))
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(
            LoginState.CollectPassword(EMAIL, LoginOption.EmailOtp, canSendCode = true, error = error),
        )
    }

    @Test
    fun `submit password - wrong, then another - checks again on the same step`() = runTest {
        startAtPassword()
        coEvery { repository.verifyPassword(LoginOption.EmailOtp, EMAIL, PASSWORD) } returnsMany listOf(
            failure(EmbeddedLoginError.InvalidPassword(LoginOption.EmailOtp)),
            Result.Success(mockk<Credentials>()),
        )

        viewModel.send(LoginIntent.SubmitPassword(PASSWORD))
        advanceUntilIdle()
        viewModel.send(LoginIntent.SubmitPassword(PASSWORD))
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.Authenticated)
    }

    @Test
    fun `submit password - too many attempts - stays on the password step with the error`() = runTest {
        startAtPassword()
        coEvery { repository.verifyPassword(LoginOption.EmailOtp, EMAIL, PASSWORD) } returns
            failure(EmbeddedLoginError.TooManyLogins)

        viewModel.send(LoginIntent.SubmitPassword(PASSWORD))
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(
            LoginState.CollectPassword(EMAIL, LoginOption.EmailOtp, error = EmbeddedLoginError.TooManyLogins),
        )
    }

    @Test
    fun `submit password - in a data breach - shows it full-screen, retry returns to the password step`() = runTest {
        startAtPassword(canSendCode = true)
        coEvery { repository.verifyPassword(LoginOption.EmailOtp, EMAIL, PASSWORD) } returns
            failure(EmbeddedLoginError.PasswordLeaked)

        viewModel.send(LoginIntent.SubmitPassword(PASSWORD))
        advanceUntilIdle()
        assertThat((viewModel.loginState.value as LoginState.Failed).error).isEqualTo(EmbeddedLoginError.PasswordLeaked)
        viewModel.send(LoginIntent.Retry)
        advanceUntilIdle()

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectPassword(EMAIL, LoginOption.EmailOtp, canSendCode = true))
    }

    @Test
    fun `submit password - no connection - shows it full-screen, retry returns to the password step`() = runTest {
        startAtPassword()
        coEvery { repository.verifyPassword(LoginOption.EmailOtp, EMAIL, PASSWORD) } returns
            failure(EmbeddedLoginError.Network, canRetryStep = true)

        viewModel.send(LoginIntent.SubmitPassword(PASSWORD))
        advanceUntilIdle()
        viewModel.send(LoginIntent.Retry)
        advanceUntilIdle()

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectPassword(EMAIL, LoginOption.EmailOtp))
    }

    @Test
    fun `submit password - nothing left to ask - shows it full-screen`() = runTest {
        startAtPassword()
        coEvery { repository.verifyPassword(LoginOption.EmailOtp, EMAIL, PASSWORD) } returns
            failure(EmbeddedLoginError.NoNextSteps)

        viewModel.send(LoginIntent.SubmitPassword(PASSWORD))
        advanceUntilIdle()

        assertThat((viewModel.loginState.value as LoginState.Failed).error).isEqualTo(EmbeddedLoginError.NoNextSteps)
    }

    @Test
    fun `submit password - the sign-in opened again has no password - says so on the identifier screen`() = runTest {
        startAtPassword(canSendCode = true)
        coEvery { repository.verifyPassword(LoginOption.EmailOtp, EMAIL, PASSWORD) } returns
            failure(EmbeddedLoginError.PasswordNotAvailable)

        viewModel.send(LoginIntent.SubmitPassword(PASSWORD))
        advanceUntilIdle()

        assertThat(viewModel.loginState.value).isEqualTo(
            LoginState.CollectOtpIdentifier(value = EMAIL, error = EmbeddedLoginError.PasswordNotAvailable),
        )
    }

    @Test
    fun `submit password - second factor needed - hands over to MFA, and Start over returns to the first screen`() =
        runTest {
            startAtPassword()
            coEvery { repository.verifyPassword(LoginOption.EmailOtp, EMAIL, PASSWORD) } returns
                failure(EmbeddedLoginError.MfaRequired(listOf(PHONE, TABLET)))

            viewModel.send(LoginIntent.SubmitPassword(PASSWORD))
            advanceUntilIdle()

            assertThat(mfaStep()).isEqualTo(MfaState.ChooseFactor(MfaRound(1, listOf(PHONE, TABLET))))
            viewModel.send(LoginIntent.LeaveMfa)
            assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(value = EMAIL))
        }

    @Test
    fun `submit password - username, second factor, Start over - returns to the first screen, empty`() = runTest {
        startWithMethods(LoginOption.EmailOtp, LoginOption.Username)
        viewModel.send(LoginIntent.SwitchMethod(LoginOption.Username))
        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.Username, USERNAME))
        advanceUntilIdle()
        coEvery { repository.verifyPassword(LoginOption.Username, USERNAME, PASSWORD) } returns
            failure(EmbeddedLoginError.MfaRequired(listOf(PHONE)))
        viewModel.send(LoginIntent.SubmitPassword(PASSWORD))
        advanceUntilIdle()

        viewModel.send(LoginIntent.LeaveMfa)

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpIdentifier(otherMethods = listOf(LoginOption.Username)))
    }

    @Test
    fun `submit password - not on the password step - is ignored`() = runTest {
        startAtCode()

        viewModel.send(LoginIntent.SubmitPassword(PASSWORD))
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.verifyPassword(any(), any(), any()) }
    }

    @Test
    fun `submit password - the intent - never prints the password`() {
        assertThat(LoginIntent.SubmitPassword(PASSWORD).toString()).doesNotContain(PASSWORD)
    }

    @Test
    fun `back - on the password step - returns to the identifier with it kept`() = runTest {
        startAtPassword(canSendCode = true)

        assertThat(viewModel.goBack()).isTrue()

        assertThat(viewModel.loginState.value).isEqualTo(LoginState.CollectOtpIdentifier(value = EMAIL))
    }

    @Test
    fun `back - on the password step of a username - returns to the username with it kept`() = runTest {
        startWithMethods(LoginOption.Username)
        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.Username, USERNAME))
        advanceUntilIdle()

        assertThat(viewModel.goBack()).isTrue()

        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectOtpIdentifier(LoginOption.Username, USERNAME))
    }

    @Test
    fun `back - while the password is being checked - stays in the flow`() = runTest {
        startAtPassword()
        val answer = CompletableDeferred<Credentials>()
        coEvery { repository.verifyPassword(LoginOption.EmailOtp, EMAIL, PASSWORD) } coAnswers {
            Result.Success(answer.await())
        }
        viewModel.send(LoginIntent.SubmitPassword(PASSWORD))
        runCurrent()

        assertThat(viewModel.goBack()).isTrue()
        assertThat(viewModel.loginState.value)
            .isEqualTo(LoginState.CollectPassword(EMAIL, LoginOption.EmailOtp, isSubmitting = true))
        answer.complete(mockk())
        advanceUntilIdle()
        assertThat(viewModel.loginState.value).isEqualTo(LoginState.Authenticated)
    }

    // endregion

    /** What the server offers an email once it knows who it is, in its order. */
    private fun offerToEmail(vararg options: LoginOption) {
        coEvery { repository.identify(LoginOption.EmailOtp, any()) } returns Result.Success(options.toList())
    }

    /** On the password step of an email, with a code also on offer when [canSendCode], and no code sent yet. */
    private fun TestScope.startAtPassword(canSendCode: Boolean = false) {
        startAtEmail()
        if (canSendCode) {
            offerToEmail(LoginOption.Password, LoginOption.EmailOtp)
        } else {
            offerToEmail(LoginOption.Password)
        }
        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.EmailOtp, EMAIL))
        advanceUntilIdle()
    }

    /** On the code step of an email the server also offers a password, with the resend countdown just started. */
    private fun TestScope.startAtCodeWithPassword() {
        startAtEmail()
        offerToEmail(LoginOption.EmailOtp, LoginOption.Password)
        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.EmailOtp, EMAIL))
        runCurrent()
    }

    private fun TestScope.startAtEmail() {
        viewModel.send(LoginIntent.Start)
        advanceUntilIdle()
    }

    /** Starts with the server offering [methods] in that order; [restart] begins again after an earlier start. */
    private fun TestScope.startWithMethods(vararg methods: LoginOption, restart: Boolean = false) {
        coEvery { repository.discover() } returns Result.Success(methods.toList())
        if (restart) {
            viewModel.goBack()
            viewModel.goBack()
        }
        startAtEmail()
    }

    /** On the phone entry, reached from the email screen. */
    private fun TestScope.startAtPhone() {
        startWithMethods(LoginOption.EmailOtp, LoginOption.PhoneOtp)
        viewModel.send(LoginIntent.SwitchMethod(LoginOption.PhoneOtp))
    }

    /** On the code step of a phone number, with the resend countdown run out. */
    private fun TestScope.startAtPhoneCode() {
        startAtPhone()
        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.PhoneOtp, NUMBER))
        advanceUntilIdle()
    }

    /** On MFA with [factors] on offer, after a correct code. */
    private fun TestScope.startAtMfa(vararg factors: MfaFactor) {
        startAtCode()
        coEvery { repository.verifyOtp(CODE) } returns failure(EmbeddedLoginError.MfaRequired(factors.toList()))
        viewModel.send(LoginIntent.SubmitOtp(CODE))
        advanceUntilIdle()
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
        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.EmailOtp, EMAIL))
        advanceUntilIdle()
    }

    /** On the code step with the resend countdown just started. */
    private fun TestScope.startAtCodeCountingDown() {
        startAtEmail()
        viewModel.send(LoginIntent.SubmitIdentifier(LoginOption.EmailOtp, EMAIL))
        runCurrent()
    }

    private fun failure(error: EmbeddedLoginError, canRetryStep: Boolean = false): Result.Error<LoginFailure> =
        Result.Error(LoginFailure(error, canRetryStep))

    private companion object {
        const val EMAIL = "jane@example.com"
        const val OTHER_EMAIL = "john@example.com"
        const val USERNAME = "jane.doe"
        const val PASSWORD = "s3cret pass "
        const val NUMBER = "+14233530900"
        const val CODE = "123456"
        const val COOLDOWN_SECONDS = 60
        const val ONE_SECOND_MILLIS = 1_000L
        const val PUSH_POLL_MS = 3_000L

        // Shares +1 with the United States, the case a guess from the dial code gets wrong.
        val CANADA = Country("Canada", "+1", "🇨🇦")
        val PHONE = MfaFactor.Push(0, "Jane's phone")
        val REJECTED = MfaError.Rejected()
        val TABLET = MfaFactor.Push(1, "Jane's tablet")
    }
}

/**
 * A clock that moves with the test's virtual time, so a wait measured on it follows `delay`. [sleep] moves it on
 * without any tick running, as a device that slept does.
 */
@OptIn(ExperimentalCoroutinesApi::class)
private class VirtualTimeSource(private val scheduler: TestCoroutineScheduler) : TimeSource {

    private var sleptMillis = 0L

    fun sleep(duration: Duration) {
        sleptMillis += duration.inWholeMilliseconds
    }

    private fun nowMillis() = scheduler.currentTime + sleptMillis

    override fun markNow(): TimeMark = VirtualMark(nowMillis())

    private inner class VirtualMark(private val startedAtMillis: Long) : TimeMark {
        override fun elapsedNow(): Duration = (nowMillis() - startedAtMillis).milliseconds
    }
}
