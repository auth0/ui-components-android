package com.auth0.android.ui_components.domain.usecase

import com.auth0.android.result.PhoneAuthenticationMethod
import com.auth0.android.result.TotpAuthenticationMethod
import com.auth0.android.ui_components.TestData
import com.auth0.android.ui_components.domain.error.Auth0Error
import com.auth0.android.ui_components.domain.model.VerificationInput
import com.auth0.android.ui_components.domain.network.Result
import com.auth0.android.ui_components.domain.repository.MyAccountRepository
import com.auth0.android.ui_components.helpers.TestDispatcherProvider
import com.google.common.truth.Truth
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VerifyAuthenticatorUseCaseTest {

    private lateinit var repository: MyAccountRepository
    private lateinit var useCase: VerifyAuthenticatorUseCase

    private val testDispatcher = StandardTestDispatcher()
    private val dispatcherProvider = TestDispatcherProvider(testDispatcher)

    private val requiredScope = "create:me:authentication_methods"


    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        repository = mockk()
        useCase = VerifyAuthenticatorUseCase(
            repository = repository,
            dispatcherProvider = dispatcherProvider
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `invoke - WithOtp valid input - returns Success with AuthenticationMethod`() = runTest {
        val input = VerificationInput.WithOtp(
            authenticationMethodId = "auth_method_123",
            otpCode = "123456",
            authSession = TestData.totpEnrollmentChallenge.authSession
        )
        val expectedAuthMethod = TestData.totpAuthMethod

        coEvery {
            repository.verifyOtp(
                authenticationMethodId = "auth_method_123",
                otpCode = "123456",
                authSession = TestData.totpEnrollmentChallenge.authSession,
                scope = requiredScope
            )
        } returns expectedAuthMethod

        val result = useCase.invoke(input)

        Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
        val authMethod = (result as Result.Success).data as TotpAuthenticationMethod
        Truth.assertThat(authMethod).isEqualTo(expectedAuthMethod)
        Truth.assertThat(authMethod.id).isEqualTo("auth_totp_789")
        Truth.assertThat(authMethod.type).isEqualTo("totp")
        Truth.assertThat(authMethod.confirmed).isTrue()

        coVerify(exactly = 1) {
            repository.verifyOtp(
                authenticationMethodId = "auth_method_123",
                otpCode = "123456",
                authSession = TestData.totpEnrollmentChallenge.authSession,
                scope = requiredScope
            )
        }
    }

    @Test
    fun `invoke - WithOtp verifies correct scopes passed to repository`() = runTest {
        val input = VerificationInput.WithOtp(
            authenticationMethodId = "auth_method_456",
            otpCode = "654321",
            authSession = "session_456"
        )
        coEvery {
            repository.verifyOtp(
                authenticationMethodId = any(),
                otpCode = any(),
                authSession = any(),
                scope = requiredScope
            )
        } returns TestData.totpAuthMethod

        val result = useCase.invoke(input)

        // Then
        Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
        coVerify(exactly = 1) {
            repository.verifyOtp(
                authenticationMethodId = "auth_method_456",
                otpCode = "654321",
                authSession = "session_456",
                scope = requiredScope
            )
        }
    }

    @Test
    fun `invoke - WithOtp repository throws InvalidOTP error - returns Error with InvalidOTP`() =
        runTest {
            val input = VerificationInput.WithOtp(
                authenticationMethodId = "auth_method_789",
                otpCode = "000000",
                authSession = "session_789"
            )
            val expectedError = Auth0Error.InvalidOTP(
                message = "Invalid passcode",
                cause = RuntimeException("OTP verification failed")
            )
            coEvery {
                repository.verifyOtp(
                    authenticationMethodId = "auth_method_789",
                    otpCode = "000000",
                    authSession = "session_789",
                    scope = requiredScope
                )
            } throws expectedError

            val result = useCase.invoke(input)

            Truth.assertThat(result).isInstanceOf(Result.Error::class.java)
            val error = (result as Result.Error).error
            Truth.assertThat(error).isInstanceOf(Auth0Error.InvalidOTP::class.java)
            Truth.assertThat(error.message).isEqualTo("Invalid passcode")

            coVerify(exactly = 1) {
                repository.verifyOtp(
                    authenticationMethodId = "auth_method_789",
                    otpCode = "000000",
                    authSession = "session_789",
                    scope = requiredScope
                )
            }
        }

    @Test
    fun `invoke - WithOtp repository throws NetworkError - returns Error with NetworkError`() =
        runTest {
            val input = VerificationInput.WithOtp(
                authenticationMethodId = "auth_method_network",
                otpCode = "123456",
                authSession = "session_network"
            )
            val expectedError = Auth0Error.NetworkError(
                message = "Network connection failed",
                cause = RuntimeException("No internet")
            )
            coEvery {
                repository.verifyOtp(
                    authenticationMethodId = any(),
                    otpCode = any(),
                    authSession = any(),
                    scope = requiredScope
                )
            } throws expectedError

            val result = useCase.invoke(input)

            Truth.assertThat(result).isInstanceOf(Result.Error::class.java)
            val error = (result as Result.Error).error
            Truth.assertThat(error).isInstanceOf(Auth0Error.NetworkError::class.java)
            Truth.assertThat(error.message).contains("Network connection failed")

            coVerify(exactly = 1) {
                repository.verifyOtp(
                    authenticationMethodId = "auth_method_network",
                    otpCode = "123456",
                    authSession = "session_network",
                    scope = requiredScope
                )
            }
        }

    @Test
    fun `invoke - WithOtp empty OTP code - handles gracefully`() = runTest {
        val input = VerificationInput.WithOtp(
            authenticationMethodId = "auth_method_123",
            otpCode = "",
            authSession = "session_123"
        )
        coEvery {
            repository.verifyOtp(
                authenticationMethodId = any(),
                otpCode = "",
                authSession = any(),
                scope = requiredScope
            )
        } returns TestData.totpAuthMethod

        val result = useCase.invoke(input)

        Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
        coVerify(exactly = 1) {
            repository.verifyOtp(
                authenticationMethodId = "auth_method_123",
                otpCode = "",
                authSession = "session_123",
                scope = requiredScope
            )
        }
    }


    @Test
    fun `invoke - WithoutOtp valid input - returns Success with AuthenticationMethod`() =
        runTest {
            val input = VerificationInput.WithoutOtp(
                authenticationMethodId = "auth_method_phone_123",
                authSession = TestData.phoneEnrollmentChallenge.authSession
            )
            val expectedAuthMethod = TestData.phoneAuthMethod

            coEvery {
                repository.verifyWithoutOtp(
                    authenticationMethodId = "auth_method_phone_123",
                    authSession = TestData.phoneEnrollmentChallenge.authSession,
                    scope = requiredScope
                )
            } returns expectedAuthMethod

            val result = useCase.invoke(input)

            Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
            val authMethod = (result as Result.Success).data as PhoneAuthenticationMethod
            Truth.assertThat(authMethod).isEqualTo(expectedAuthMethod)
            Truth.assertThat(authMethod.id).isEqualTo("auth_phone_123")
            Truth.assertThat(authMethod.type).isEqualTo("phone")
            Truth.assertThat(authMethod.confirmed).isTrue()

            coVerify(exactly = 1) {
                repository.verifyWithoutOtp(
                    authenticationMethodId = "auth_method_phone_123",
                    authSession = TestData.phoneEnrollmentChallenge.authSession,
                    scope = requiredScope
                )
            }
        }

    @Test
    fun `invoke - WithoutOtp verifies correct scopes passed to repository`() = runTest {
        val input = VerificationInput.WithoutOtp(
            authenticationMethodId = "auth_method_email_456",
            authSession = "email_session_456"
        )
        coEvery {
            repository.verifyWithoutOtp(
                authenticationMethodId = any(),
                authSession = any(),
                scope = requiredScope
            )
        } returns TestData.emailAuthMethod

        val result = useCase.invoke(input)

        Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
        coVerify(exactly = 1) {
            repository.verifyWithoutOtp(
                authenticationMethodId = "auth_method_email_456",
                authSession = "email_session_456",
                scope = requiredScope
            )
        }
    }

    @Test
    fun `invoke - WithoutOtp repository throws NetworkError - returns Error with NetworkError`() =
        runTest {
            val input = VerificationInput.WithoutOtp(
                authenticationMethodId = "auth_method_network",
                authSession = "session_network"
            )
            val expectedError = Auth0Error.NetworkError(
                message = "Network connection failed",
                cause = RuntimeException("No internet")
            )
            coEvery {
                repository.verifyWithoutOtp(
                    authenticationMethodId = any(),
                    authSession = any(),
                    scope = requiredScope
                )
            } throws expectedError

            val result = useCase.invoke(input)

            Truth.assertThat(result).isInstanceOf(Result.Error::class.java)
            val error = (result as Result.Error).error
            Truth.assertThat(error).isInstanceOf(Auth0Error.NetworkError::class.java)
            Truth.assertThat(error.message).contains("Network connection failed")

            coVerify(exactly = 1) {
                repository.verifyWithoutOtp(
                    authenticationMethodId = "auth_method_network",
                    authSession = "session_network",
                    scope = requiredScope
                )
            }
        }
}
