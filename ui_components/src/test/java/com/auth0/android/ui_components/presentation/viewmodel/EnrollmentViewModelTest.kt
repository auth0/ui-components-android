package com.auth0.android.ui_components.presentation.viewmodel

import com.auth0.android.ui_components.TestData
import com.auth0.android.ui_components.domain.error.Auth0Error
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.model.EnrollmentInput
import com.auth0.android.ui_components.domain.network.Result
import com.auth0.android.ui_components.domain.usecase.EnrollAuthenticatorUseCase
import com.auth0.android.ui_components.domain.usecase.VerifyAuthenticatorUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EnrollmentViewModelTest {

    private lateinit var enrollAuthenticatorUseCase: EnrollAuthenticatorUseCase
    private lateinit var verifyAuthenticatorUseCase: VerifyAuthenticatorUseCase

    private lateinit var viewModel: EnrollmentViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        enrollAuthenticatorUseCase = mockk(relaxed = true)
        verifyAuthenticatorUseCase = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `initialization - TOTP authenticator with auto-start enabled - automatically starts enrollment on uiState collection`() =
        runTest {
            coEvery {
                enrollAuthenticatorUseCase(
                    AuthenticatorType.TOTP,
                    EnrollmentInput.None
                )
            } returns Result.Success(TestData.totpEnrollmentResult)

            viewModel = EnrollmentViewModel(
                enrollAuthenticatorUseCase = enrollAuthenticatorUseCase,
                verifyAuthenticatorUseCase = verifyAuthenticatorUseCase,
                authenticatorType = AuthenticatorType.TOTP,
                startDefaultEnrollment = true
            )

            val job = launch {
                viewModel.uiState.collect { }
            }
            val eventJob = launch {
                viewModel.events.collect { }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) {
                enrollAuthenticatorUseCase(AuthenticatorType.TOTP, EnrollmentInput.None)
            }

            val finalState = viewModel.uiState.value
            assertThat(finalState.enrollingAuthenticator).isFalse()
            assertThat(finalState.verifyingAuthenticator).isFalse()
            assertThat(finalState.uiError).isNull()

            job.cancel()
            eventJob.cancel()
        }

    @Test
    fun `initialization - PHONE authenticator - does NOT auto-start enrollment`() = runTest {
        viewModel = EnrollmentViewModel(
            enrollAuthenticatorUseCase = enrollAuthenticatorUseCase,
            verifyAuthenticatorUseCase = verifyAuthenticatorUseCase,
            authenticatorType = AuthenticatorType.PHONE,
            startDefaultEnrollment = true
        )

        val job = launch {
            viewModel.uiState.collect { }
        }
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) {
            enrollAuthenticatorUseCase(any(), any())
        }

        val state = viewModel.uiState.value
        assertThat(state.enrollingAuthenticator).isFalse()

        job.cancel()
    }

    @Test
    fun `initialization - TOTP with startDefaultEnrollment false - does NOT auto-start enrollment`() =
        runTest {
            viewModel = EnrollmentViewModel(
                enrollAuthenticatorUseCase = enrollAuthenticatorUseCase,
                verifyAuthenticatorUseCase = verifyAuthenticatorUseCase,
                authenticatorType = AuthenticatorType.TOTP,
                startDefaultEnrollment = false
            )

            val job = launch {
                viewModel.uiState.collect { }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) {
                enrollAuthenticatorUseCase(any(), any())
            }

            val state = viewModel.uiState.value
            assertThat(state.enrollingAuthenticator).isFalse()

            job.cancel()
        }


    @Test
    fun `startEnrollment - TOTP with None input - emits EnrollmentChallengeSuccess event and updates state`() =
        runTest {
            viewModel = EnrollmentViewModel(
                enrollAuthenticatorUseCase = enrollAuthenticatorUseCase,
                verifyAuthenticatorUseCase = verifyAuthenticatorUseCase,
                authenticatorType = AuthenticatorType.TOTP,
                startDefaultEnrollment = true
            )

            val job = launch {
                viewModel.uiState.collect {}
            }

            coEvery {
                enrollAuthenticatorUseCase(AuthenticatorType.TOTP, EnrollmentInput.None)
            } returns Result.Success(TestData.totpEnrollmentResult)

            val events = mutableListOf<EnrollmentEvent>()
            val eventJob = launch {
                viewModel.events.collect { event ->
                    events.add(event)
                }
            }

            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(events).hasSize(1)
            val event = events[0]
            assertThat(event).isInstanceOf(EnrollmentEvent.EnrollmentChallengeSuccess::class.java)

            val successEvent = event as EnrollmentEvent.EnrollmentChallengeSuccess
            assertThat(successEvent.enrollmentResult).isEqualTo(TestData.totpEnrollmentResult)
            assertThat(successEvent.authenticationMethodId).isEqualTo("auth_totp_new_001")
            assertThat(successEvent.authSession).isEqualTo("session_totp_001")

            val state = viewModel.uiState.value
            assertThat(state.enrollingAuthenticator).isFalse()
            assertThat(state.uiError).isNull()

            coVerify(exactly = 1) {
                enrollAuthenticatorUseCase(AuthenticatorType.TOTP, EnrollmentInput.None)
            }

            eventJob.cancel()
            job.cancel()
        }

    @Test
    fun `startEnrollment - PHONE with Phone input - emits DefaultEnrollment event and updates state`() =
        runTest {
            viewModel = EnrollmentViewModel(
                enrollAuthenticatorUseCase = enrollAuthenticatorUseCase,
                verifyAuthenticatorUseCase = verifyAuthenticatorUseCase,
                authenticatorType = AuthenticatorType.PHONE,
                startDefaultEnrollment = false
            )

            val phoneInput = EnrollmentInput.Phone("+15551234567")
            coEvery {
                enrollAuthenticatorUseCase(AuthenticatorType.PHONE, phoneInput)
            } returns Result.Success(TestData.defaultPhoneEnrollmentResult)

            val events = mutableListOf<EnrollmentEvent>()
            val eventJob = launch {
                viewModel.events.collect { event ->
                    events.add(event)
                }
            }

            viewModel.startEnrollment(AuthenticatorType.PHONE, phoneInput)
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(events).hasSize(1)
            val event = events[0]
            assertThat(event).isInstanceOf(EnrollmentEvent.EnrollmentChallengeSuccess::class.java)

            val successEvent = event as EnrollmentEvent.EnrollmentChallengeSuccess
            assertThat(successEvent.enrollmentResult).isEqualTo(TestData.defaultPhoneEnrollmentResult)
            assertThat(successEvent.authenticationMethodId).isEqualTo("auth_phone_new_001")
            assertThat(successEvent.authSession).isEqualTo("session_phone_001")

            val state = viewModel.uiState.value
            assertThat(state.enrollingAuthenticator).isFalse()
            assertThat(state.uiError).isNull()

            coVerify(exactly = 1) {
                enrollAuthenticatorUseCase(AuthenticatorType.PHONE, phoneInput)
            }

            eventJob.cancel()
        }

    @Test
    fun `startEnrollment - EMAIL with Email input - emits DefaultEnrollment event and updates state`() =
        runTest {
            viewModel = EnrollmentViewModel(
                enrollAuthenticatorUseCase = enrollAuthenticatorUseCase,
                verifyAuthenticatorUseCase = verifyAuthenticatorUseCase,
                authenticatorType = AuthenticatorType.EMAIL,
                startDefaultEnrollment = false
            )

            val emailInput = EnrollmentInput.Email("user@example.com")
            coEvery {
                enrollAuthenticatorUseCase(AuthenticatorType.EMAIL, emailInput)
            } returns Result.Success(TestData.defaultEmailEnrollmentResult)

            val events = mutableListOf<EnrollmentEvent>()
            val eventJob = launch {
                viewModel.events.collect { event ->
                    events.add(event)
                }
            }

            viewModel.startEnrollment(AuthenticatorType.EMAIL, emailInput)
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(events).hasSize(1)
            val event = events[0]
            assertThat(event).isInstanceOf(EnrollmentEvent.EnrollmentChallengeSuccess::class.java)

            val successEvent = event as EnrollmentEvent.EnrollmentChallengeSuccess
            assertThat(successEvent.enrollmentResult).isEqualTo(TestData.defaultEmailEnrollmentResult)
            assertThat(successEvent.authenticationMethodId).isEqualTo("auth_email_new_001")
            assertThat(successEvent.authSession).isEqualTo("session_email_001")

            val state = viewModel.uiState.value
            assertThat(state.enrollingAuthenticator).isFalse()
            assertThat(state.uiError).isNull()

            coVerify(exactly = 1) {
                enrollAuthenticatorUseCase(AuthenticatorType.EMAIL, emailInput)
            }

            eventJob.cancel()
        }

    @Test
    fun `startEnrollment - complete flow - transitions through loading, success states correctly`() =
        runTest {
            viewModel = EnrollmentViewModel(
                enrollAuthenticatorUseCase = enrollAuthenticatorUseCase,
                verifyAuthenticatorUseCase = verifyAuthenticatorUseCase,
                authenticatorType = AuthenticatorType.TOTP,
                startDefaultEnrollment = false
            )

            coEvery {
                enrollAuthenticatorUseCase(AuthenticatorType.TOTP, EnrollmentInput.None)
            } coAnswers {
                delay(50)
                Result.Success(TestData.totpEnrollmentResult)
            }

            val states = mutableListOf<EnrollmentUiState>()
            val job = launch {
                viewModel.uiState.collect { state ->
                    states.add(state)
                }
            }

            val eventJob = launch {
                viewModel.events.collect { event ->
                }
            }

            viewModel.startEnrollment(AuthenticatorType.TOTP, EnrollmentInput.None)
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(states.size).isAtLeast(2)

            val loadingState = states.find { it.enrollingAuthenticator }
            assertThat(loadingState).isNotNull()
            assertThat(loadingState?.enrollingAuthenticator).isTrue()

            val finalState = states.last()
            assertThat(finalState.enrollingAuthenticator).isFalse()
            assertThat(finalState.verifyingAuthenticator).isFalse()
            assertThat(finalState.otpError).isFalse()
            assertThat(finalState.uiError).isNull()

            job.cancel()
            eventJob.cancel()
        }

    @Test
    fun `startEnrollment - network error - emits error state with retry callback`() = runTest {
        viewModel = EnrollmentViewModel(
            enrollAuthenticatorUseCase = enrollAuthenticatorUseCase,
            verifyAuthenticatorUseCase = verifyAuthenticatorUseCase,
            authenticatorType = AuthenticatorType.PHONE,
            startDefaultEnrollment = false
        )

        val job = launch {
            viewModel.uiState.collect {}
        }

        val networkError = Auth0Error.NetworkError(
            message = "Connection failed",
            cause = Exception("Network timeout")
        )
        coEvery {
            enrollAuthenticatorUseCase(any(), any())
        } returns Result.Error(networkError)

        viewModel.startEnrollment(AuthenticatorType.PHONE, EnrollmentInput.Phone("+15551234567"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.enrollingAuthenticator).isFalse()
        assertThat(state.uiError).isNotNull()
        assertThat(state.uiError?.error).isEqualTo(networkError)
        assertThat(state.uiError?.error?.message).contains("Connection failed")
        assertThat(state.uiError?.onRetry).isNotNull()

        coVerify(exactly = 1) {
            enrollAuthenticatorUseCase(any(), any())
        }

        job.cancel()
    }

    @Test
    fun `startEnrollment - API error - emits error state with retry callback`() = runTest {
        viewModel = EnrollmentViewModel(
            enrollAuthenticatorUseCase = enrollAuthenticatorUseCase,
            verifyAuthenticatorUseCase = verifyAuthenticatorUseCase,
            authenticatorType = AuthenticatorType.EMAIL,
            startDefaultEnrollment = false
        )

        val job = launch {
            viewModel.uiState.collect {}
        }

        val apiError = Auth0Error.ServerError(
            message = "API request failed",
            statusCode = 500,
            cause = Exception("Internal server error")
        )
        coEvery {
            enrollAuthenticatorUseCase(any(), any())
        } returns Result.Error(apiError)

        viewModel.startEnrollment(
            AuthenticatorType.EMAIL,
            EnrollmentInput.Email("test@example.com")
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.enrollingAuthenticator).isFalse()
        assertThat(state.uiError).isNotNull()
        assertThat(state.uiError?.error).isEqualTo(apiError)
        assertThat(state.uiError?.error?.message).contains("API request failed")
        assertThat(state.uiError?.onRetry).isNotNull()

        coVerify(exactly = 1) {
            enrollAuthenticatorUseCase(any(), any())
        }

        job.cancel()
    }


    @Test
    fun `verifyWithOtp - valid OTP - emits VerificationSuccess event and resets state`() =
        runTest {
            viewModel = EnrollmentViewModel(
                enrollAuthenticatorUseCase = enrollAuthenticatorUseCase,
                verifyAuthenticatorUseCase = verifyAuthenticatorUseCase,
                authenticatorType = AuthenticatorType.PHONE,
                startDefaultEnrollment = false
            )

            val inputSlot = slot<com.auth0.android.ui_components.domain.model.VerificationInput>()
            coEvery {
                verifyAuthenticatorUseCase(capture(inputSlot))
            } returns Result.Success(TestData.verifiedPhoneAuthMethod)

            val events = mutableListOf<EnrollmentEvent>()
            val eventJob = launch {
                viewModel.events.collect { event ->
                    events.add(event)
                }
            }

            viewModel.verifyWithOtp("auth_phone_001", "123456", "session_001")
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Verify event
            assertThat(events).hasSize(1)
            val event = events[0]
            assertThat(event).isInstanceOf(EnrollmentEvent.VerificationSuccess::class.java)

            val successEvent = event as EnrollmentEvent.VerificationSuccess
            assertThat(successEvent.authenticationMethod).isEqualTo(TestData.verifiedPhoneAuthMethod)

            val state = viewModel.uiState.value
            assertThat(state.enrollingAuthenticator).isFalse()
            assertThat(state.verifyingAuthenticator).isFalse()
            assertThat(state.otpError).isFalse()
            assertThat(state.uiError).isNull()

            val capturedInput = inputSlot.captured
            assertThat(capturedInput).isInstanceOf(com.auth0.android.ui_components.domain.model.VerificationInput.WithOtp::class.java)
            val otpInput =
                capturedInput as com.auth0.android.ui_components.domain.model.VerificationInput.WithOtp
            assertThat(otpInput.authenticationMethodId).isEqualTo("auth_phone_001")
            assertThat(otpInput.otpCode).isEqualTo("123456")
            assertThat(otpInput.authSession).isEqualTo("session_001")

            coVerify(exactly = 1) {
                verifyAuthenticatorUseCase(any())
            }

            eventJob.cancel()
        }

    @Test
    fun `verifyWithOtp - invalid OTP error - sets otpError to true and clears verifying state`() =
        runTest {
            viewModel = EnrollmentViewModel(
                enrollAuthenticatorUseCase = enrollAuthenticatorUseCase,
                verifyAuthenticatorUseCase = verifyAuthenticatorUseCase,
                authenticatorType = AuthenticatorType.PHONE,
                startDefaultEnrollment = false
            )

            val job = launch {
                viewModel.uiState.collect {}
            }

            val invalidOtpError = Auth0Error.InvalidOTP(
                message = "Invalid OTP code",
                cause = Exception("OTP verification failed")
            )
            coEvery {
                verifyAuthenticatorUseCase(any())
            } returns Result.Error(invalidOtpError)


            viewModel.verifyWithOtp("auth_phone_001", "000000", "session_001")
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.verifyingAuthenticator).isFalse()
            assertThat(state.otpError).isTrue()
            assertThat(state.uiError).isNull()

            coVerify(exactly = 1) {
                verifyAuthenticatorUseCase(any())
            }
            job.cancel()
        }

    @Test
    fun `verifyWithOtp - network error - emits error state with retry callback`() = runTest {
        viewModel = EnrollmentViewModel(
            enrollAuthenticatorUseCase = enrollAuthenticatorUseCase,
            verifyAuthenticatorUseCase = verifyAuthenticatorUseCase,
            authenticatorType = AuthenticatorType.PHONE,
            startDefaultEnrollment = false
        )

        val job = launch {
            viewModel.uiState.collect {}
        }

        val networkError = Auth0Error.NetworkError(
            message = "Verification failed - network error",
            cause = Exception("Connection timeout")
        )
        coEvery {
            verifyAuthenticatorUseCase(any())
        } returns Result.Error(networkError)

        viewModel.verifyWithOtp("auth_phone_001", "123456", "session_001")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.verifyingAuthenticator).isFalse()
        assertThat(state.otpError).isFalse() // NOT an OTP validation error
        assertThat(state.uiError).isNotNull()
        assertThat(state.uiError?.error).isEqualTo(networkError)
        assertThat(state.uiError?.error?.message).contains("Verification failed")
        assertThat(state.uiError?.onRetry).isNotNull()

        coVerify(exactly = 1) {
            verifyAuthenticatorUseCase(any())
        }
        job.cancel()
    }


    @Test
    fun `verifyWithoutOtp - valid verification - emits VerificationSuccess event and resets state`() =
        runTest {
            viewModel = EnrollmentViewModel(
                enrollAuthenticatorUseCase = enrollAuthenticatorUseCase,
                verifyAuthenticatorUseCase = verifyAuthenticatorUseCase,
                authenticatorType = AuthenticatorType.TOTP,
                startDefaultEnrollment = false
            )

            val inputSlot = slot<com.auth0.android.ui_components.domain.model.VerificationInput>()
            coEvery {
                verifyAuthenticatorUseCase(capture(inputSlot))
            } returns Result.Success(TestData.verifiedTotpAuthMethod)

            val events = mutableListOf<EnrollmentEvent>()
            val eventJob = launch {
                viewModel.events.collect { event ->
                    events.add(event)
                }
            }

            viewModel.verifyWithoutOtp("auth_totp_001", "session_totp_001")
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(events).hasSize(1)
            val event = events[0]
            assertThat(event).isInstanceOf(EnrollmentEvent.VerificationSuccess::class.java)

            val successEvent = event as EnrollmentEvent.VerificationSuccess
            assertThat(successEvent.authenticationMethod).isEqualTo(TestData.verifiedTotpAuthMethod)

            val state = viewModel.uiState.value
            assertThat(state.enrollingAuthenticator).isFalse()
            assertThat(state.verifyingAuthenticator).isFalse()
            assertThat(state.otpError).isFalse()
            assertThat(state.uiError).isNull()

            val capturedInput = inputSlot.captured
            assertThat(capturedInput).isInstanceOf(com.auth0.android.ui_components.domain.model.VerificationInput.WithoutOtp::class.java)
            val withoutOtpInput =
                capturedInput as com.auth0.android.ui_components.domain.model.VerificationInput.WithoutOtp
            assertThat(withoutOtpInput.authenticationMethodId).isEqualTo("auth_totp_001")
            assertThat(withoutOtpInput.authSession).isEqualTo("session_totp_001")

            coVerify(exactly = 1) {
                verifyAuthenticatorUseCase(any())
            }

            eventJob.cancel()
        }

    @Test
    fun `verifyWithoutOtp - network error - emits error state with retry callback`() = runTest {
        viewModel = EnrollmentViewModel(
            enrollAuthenticatorUseCase = enrollAuthenticatorUseCase,
            verifyAuthenticatorUseCase = verifyAuthenticatorUseCase,
            authenticatorType = AuthenticatorType.RECOVERY_CODE,
            startDefaultEnrollment = false
        )

        val job = launch {
            viewModel.uiState.collect {}
        }

        val networkError = Auth0Error.NetworkError(
            message = "Verification failed",
            cause = Exception("Network error")
        )
        coEvery {
            verifyAuthenticatorUseCase(any())
        } returns Result.Error(networkError)

        viewModel.verifyWithoutOtp("auth_recovery_001", "session_recovery_001")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.verifyingAuthenticator).isFalse()
        assertThat(state.uiError).isNotNull()
        assertThat(state.uiError?.error).isEqualTo(networkError)
        assertThat(state.uiError?.onRetry).isNotNull()

        coVerify(exactly = 1) {
            verifyAuthenticatorUseCase(any())
        }

        job.cancel()
    }


    @Test
    fun `error retry callback - startEnrollment retry - invokes startEnrollment again with same parameters`() =
        runTest {
            viewModel = EnrollmentViewModel(
                enrollAuthenticatorUseCase = enrollAuthenticatorUseCase,
                verifyAuthenticatorUseCase = verifyAuthenticatorUseCase,
                authenticatorType = AuthenticatorType.PHONE,
                startDefaultEnrollment = false
            )

            val job = launch {
                viewModel.uiState.collect {}
            }

            val eventJob = launch {
                viewModel.events.collect { }
            }

            val phoneInput = EnrollmentInput.Phone("+15551234567")
            val networkError = Auth0Error.NetworkError(
                message = "Connection failed",
                cause = Exception("Network timeout")
            )

            coEvery {
                enrollAuthenticatorUseCase(AuthenticatorType.PHONE, phoneInput)
            } returnsMany listOf(
                Result.Error(networkError),
                Result.Success(TestData.defaultPhoneEnrollmentResult)
            )

            viewModel.startEnrollment(AuthenticatorType.PHONE, phoneInput)
            testDispatcher.scheduler.advanceUntilIdle()

            val errorState = viewModel.uiState.value
            assertThat(errorState.uiError).isNotNull()

            val retryCallback = errorState.uiError!!.onRetry
            retryCallback.invoke()
            testDispatcher.scheduler.advanceUntilIdle()

            val finalState = viewModel.uiState.value
            assertThat(finalState.enrollingAuthenticator).isFalse()
            assertThat(finalState.uiError).isNull()

            coVerify(exactly = 2) {
                enrollAuthenticatorUseCase(AuthenticatorType.PHONE, phoneInput)
            }

            job.cancel()
            eventJob.cancel()
        }

    @Test
    fun `error retry callback - verifyWithOtp retry - invokes verifyWithOtp again with same parameters`() =
        runTest {
            viewModel = EnrollmentViewModel(
                enrollAuthenticatorUseCase = enrollAuthenticatorUseCase,
                verifyAuthenticatorUseCase = verifyAuthenticatorUseCase,
                authenticatorType = AuthenticatorType.PHONE,
                startDefaultEnrollment = false
            )

            val job = launch {
                viewModel.uiState.collect {}
            }

            val networkError = Auth0Error.NetworkError(
                message = "Verification failed",
                cause = Exception("Network error")
            )

            coEvery {
                verifyAuthenticatorUseCase(any())
            } returnsMany listOf(
                Result.Error(networkError),
                Result.Success(TestData.verifiedPhoneAuthMethod)
            )

            viewModel.verifyWithOtp("auth_phone_001", "123456", "session_001")
            testDispatcher.scheduler.advanceUntilIdle()

            val errorState = viewModel.uiState.value
            assertThat(errorState.uiError).isNotNull()

            val retryCallback = errorState.uiError!!.onRetry
            retryCallback.invoke()
            testDispatcher.scheduler.advanceUntilIdle()

            val finalState = viewModel.uiState.value
            assertThat(finalState.verifyingAuthenticator).isFalse()
            assertThat(finalState.uiError).isNull()

            coVerify(exactly = 2) {
                verifyAuthenticatorUseCase(any())
            }
            job.cancel()
        }

    @Test
    fun `state management - enrollment to verification - handles state transitions correctly`() =
        runTest {
            viewModel = EnrollmentViewModel(
                enrollAuthenticatorUseCase = enrollAuthenticatorUseCase,
                verifyAuthenticatorUseCase = verifyAuthenticatorUseCase,
                authenticatorType = AuthenticatorType.PHONE,
                startDefaultEnrollment = false
            )

            coEvery {
                enrollAuthenticatorUseCase(any(), any())
            } returns Result.Success(TestData.defaultPhoneEnrollmentResult)

            coEvery {
                verifyAuthenticatorUseCase(any())
            } returns Result.Success(TestData.verifiedPhoneAuthMethod)

            viewModel.startEnrollment(
                AuthenticatorType.PHONE,
                EnrollmentInput.Phone("+15551234567")
            )
            testDispatcher.scheduler.advanceUntilIdle()

            val afterEnrollState = viewModel.uiState.value
            assertThat(afterEnrollState.enrollingAuthenticator).isFalse()
            assertThat(afterEnrollState.verifyingAuthenticator).isFalse()

            viewModel.verifyWithOtp("auth_phone_001", "123456", "session_001")
            testDispatcher.scheduler.advanceUntilIdle()

            val finalState = viewModel.uiState.value
            assertThat(finalState.enrollingAuthenticator).isFalse()
            assertThat(finalState.verifyingAuthenticator).isFalse()
            assertThat(finalState.otpError).isFalse()
            assertThat(finalState.uiError).isNull()

            coVerify(exactly = 1) { enrollAuthenticatorUseCase(any(), any()) }
            coVerify(exactly = 1) { verifyAuthenticatorUseCase(any()) }
        }
}
