package com.auth0.android.ui_components.domain.usecase

import com.auth0.android.ui_components.TestData
import com.auth0.android.ui_components.domain.error.Auth0Error
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.model.EnrollmentInput
import com.auth0.android.ui_components.domain.model.EnrollmentResult
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
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EnrollAuthenticatorUseCaseTest {

    private lateinit var repository: MyAccountRepository
    private lateinit var useCase: EnrollAuthenticatorUseCase

    private val testDispatcher = StandardTestDispatcher()
    private val dispatcherProvider = TestDispatcherProvider(testDispatcher)

    private val requiredScope = "create:me:authentication_methods"


    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        repository = mockk()
        useCase = EnrollAuthenticatorUseCase(
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
    fun `invoke - TOTP with None input - returns Success with TotpEnrollment`() = runTest {
        val expectedChallenge = TestData.domainTotpEnrollmentChallenge
        coEvery {
            repository.enrollTotp(any<String>())
        } returns expectedChallenge

        val result = useCase.invoke(
            authenticatorType = AuthenticatorType.TOTP,
            input = EnrollmentInput.None
        )

        Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
        val enrollmentResult = (result as Result.Success).data
        Truth.assertThat(enrollmentResult).isInstanceOf(EnrollmentResult.TotpEnrollment::class.java)

        val totpEnrollment = enrollmentResult as EnrollmentResult.TotpEnrollment
        Truth.assertThat(totpEnrollment.challenge).isEqualTo(expectedChallenge)
        Truth.assertThat(totpEnrollment.authenticationMethodId).isEqualTo("totp_id_123")
        Truth.assertThat(totpEnrollment.authSession).isEqualTo("totp_session_123")

        coVerify(exactly = 1) {
            repository.enrollTotp(requiredScope)
        }
    }

    @Test
    fun `invoke - TOTP repository throws NetworkError - returns Auth0Error with NetworkError`() =
        runTest {
            val expectedError = Auth0Error.NetworkError(
                message = "Network connection failed",
                cause = RuntimeException("No internet")
            )
            coEvery {
                repository.enrollTotp(any<String>())
            } throws expectedError

            val result = useCase.invoke(
                authenticatorType = AuthenticatorType.TOTP,
                input = EnrollmentInput.None
            )

            Truth.assertThat(result).isInstanceOf(Result.Error::class.java)
            val error = (result as Result.Error).error
            Truth.assertThat(error).isInstanceOf(Auth0Error.NetworkError::class.java)
            Truth.assertThat(error.message).contains("Network connection failed")

            coVerify(exactly = 1) {
                repository.enrollTotp(requiredScope)
            }
        }

    @Test
    fun `invoke - PUSH with None input - returns Success with TotpEnrollment`() = runTest {
        val expectedChallenge = TestData.domainPushEnrollmentChallenge

        coEvery {
            repository.enrollPushNotification(any<String>())
        } returns expectedChallenge

        val result = useCase.invoke(
            authenticatorType = AuthenticatorType.PUSH,
            input = EnrollmentInput.None
        )

        Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
        val enrollmentResult = (result as Result.Success).data
        Truth.assertThat(enrollmentResult).isInstanceOf(EnrollmentResult.TotpEnrollment::class.java)

        val pushEnrollment = enrollmentResult as EnrollmentResult.TotpEnrollment
        Truth.assertThat(pushEnrollment.challenge).isEqualTo(expectedChallenge)
        Truth.assertThat(pushEnrollment.authenticationMethodId).isEqualTo("push_id_123")
        Truth.assertThat(pushEnrollment.authSession).isEqualTo("push_session_123")

        coVerify(exactly = 1) {
            repository.enrollPushNotification(requiredScope)
        }
    }

    @Test
    fun `invoke - PUSH repository throws NetworkError - returns Auth0Error with NetworkError`() =
        runTest {
            val expectedError = Auth0Error.NetworkError(
                message = "Network connection failed",
                cause = RuntimeException("No internet")
            )
            coEvery {
                repository.enrollPushNotification(any<String>())
            } throws expectedError

            val result = useCase.invoke(
                authenticatorType = AuthenticatorType.PUSH,
                input = EnrollmentInput.None
            )

            Truth.assertThat(result).isInstanceOf(Result.Error::class.java)
            val error = (result as Result.Error).error
            Truth.assertThat(error).isInstanceOf(Auth0Error.NetworkError::class.java)
            Truth.assertThat(error.message).contains("Network connection failed")

            coVerify(exactly = 1) {
                repository.enrollPushNotification(requiredScope)
            }
        }


    @Test
    fun `invoke - RECOVERY_CODE with None input - returns Success with RecoveryCodeEnrollment`() =
        runTest {
            val expectedChallenge = TestData.domainRecoveryCodeEnrollmentChallenge
            coEvery {
                repository.enrollRecoveryCode(any<String>())
            } returns expectedChallenge

            val result = useCase.invoke(
                authenticatorType = AuthenticatorType.RECOVERY_CODE,
                input = EnrollmentInput.None
            )

            Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
            val enrollmentResult = (result as Result.Success).data
            Truth.assertThat(enrollmentResult)
                .isInstanceOf(EnrollmentResult.RecoveryCodeEnrollment::class.java)

            val recoveryCodeEnrollment = enrollmentResult as EnrollmentResult.RecoveryCodeEnrollment
            Truth.assertThat(recoveryCodeEnrollment.challenge).isEqualTo(expectedChallenge)
            Truth.assertThat(recoveryCodeEnrollment.authenticationMethodId)
                .isEqualTo("recovery_id_123")
            Truth.assertThat(recoveryCodeEnrollment.authSession).isEqualTo("recovery_session_123")

            coVerify(exactly = 1) {
                repository.enrollRecoveryCode(requiredScope)
            }
        }

    @Test
    fun `invoke - RECOVERY_CODE repository throws NetworkError - returns Auth0Error with NetworkError`() =
        runTest {
            val expectedError = Auth0Error.NetworkError(
                message = "Network connection failed",
                cause = RuntimeException("No internet")
            )
            coEvery {
                repository.enrollRecoveryCode(any<String>())
            } throws expectedError

            val result = useCase.invoke(
                authenticatorType = AuthenticatorType.RECOVERY_CODE,
                input = EnrollmentInput.None
            )

            Truth.assertThat(result).isInstanceOf(Result.Error::class.java)
            val error = (result as Result.Error).error
            Truth.assertThat(error).isInstanceOf(Auth0Error.NetworkError::class.java)
            Truth.assertThat(error.message).contains("Network connection failed")

            coVerify(exactly = 1) {
                repository.enrollRecoveryCode(requiredScope)
            }
        }

    @Test
    fun `invoke - EMAIL with valid Email input - returns Success with DefaultEnrollment`() =
        runTest {
            val email = "user@example.com"
            val expectedChallenge = TestData.domainEmailEnrollmentChallenge
            coEvery {
                repository.enrollEmail(email, any<String>())
            } returns expectedChallenge

            val result = useCase.invoke(
                authenticatorType = AuthenticatorType.EMAIL,
                input = EnrollmentInput.Email(email)
            )

            Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
            val enrollmentResult = (result as Result.Success).data
            Truth.assertThat(enrollmentResult)
                .isInstanceOf(EnrollmentResult.DefaultEnrollment::class.java)

            val emailEnrollment = enrollmentResult as EnrollmentResult.DefaultEnrollment
            Truth.assertThat(emailEnrollment.challenge).isEqualTo(expectedChallenge)
            Truth.assertThat(emailEnrollment.authenticationMethodId).isEqualTo("email_123")
            Truth.assertThat(emailEnrollment.authSession).isEqualTo("email_session")

            coVerify(exactly = 1) {
                repository.enrollEmail(email, requiredScope)
            }
        }

    @Test
    fun `invoke - EMAIL with None input - throws IllegalArgumentException`() {
        val exception = Assert.assertThrows(IllegalArgumentException::class.java) {
            runTest {
                useCase.invoke(
                    authenticatorType = AuthenticatorType.EMAIL,
                    input = EnrollmentInput.None
                )
            }
        }

        Truth.assertThat(exception.message)
            .isEqualTo("Email enrollment requires EnrollmentInput.Email")

        coVerify(exactly = 0) {
            repository.enrollEmail(any(), any())
        }
    }

    @Test
    fun `invoke - EMAIL with Phone input - throws IllegalArgumentException`() {
        val exception = Assert.assertThrows(IllegalArgumentException::class.java) {
            runTest {
                useCase.invoke(
                    authenticatorType = AuthenticatorType.EMAIL,
                    input = EnrollmentInput.Phone("+15551234567")
                )
            }
        }

        Truth.assertThat(exception.message)
            .isEqualTo("Email enrollment requires EnrollmentInput.Email")
        coVerify(exactly = 0) {
            repository.enrollEmail(any(), any())
        }
    }


    @Test
    fun `invoke - EMAIL repository throws NetworkError - returns Auth0Error with NetworkError`() =
        runTest {
            val email = "user@example.com"
            val expectedError = Auth0Error.NetworkError(
                message = "Network connection failed",
                cause = RuntimeException("No internet")
            )
            coEvery {
                repository.enrollEmail(email, requiredScope)
            } throws expectedError

            val result = useCase.invoke(
                authenticatorType = AuthenticatorType.EMAIL,
                input = EnrollmentInput.Email(email)
            )

            Truth.assertThat(result).isInstanceOf(Result.Error::class.java)
            val error = (result as Result.Error).error
            Truth.assertThat(error).isInstanceOf(Auth0Error.NetworkError::class.java)
            Truth.assertThat(error.message).contains("Network connection failed")

            coVerify(exactly = 1) {
                repository.enrollEmail(email, requiredScope)
            }
        }

    @Test
    fun `invoke - PHONE with valid Phone input - returns Success with DefaultEnrollment`() =
        runTest {
            val phoneNumber = "+15551234567"
            val expectedChallenge = TestData.domainPhoneEnrollmentChallenge
            coEvery {
                repository.enrollPhone(phoneNumber, any<String>())
            } returns expectedChallenge

            val result = useCase.invoke(
                authenticatorType = AuthenticatorType.PHONE,
                input = EnrollmentInput.Phone(phoneNumber)
            )

            Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
            val enrollmentResult = (result as Result.Success).data
            Truth.assertThat(enrollmentResult)
                .isInstanceOf(EnrollmentResult.DefaultEnrollment::class.java)

            val phoneEnrollment = enrollmentResult as EnrollmentResult.DefaultEnrollment
            Truth.assertThat(phoneEnrollment.challenge).isEqualTo(expectedChallenge)
            Truth.assertThat(phoneEnrollment.authenticationMethodId).isEqualTo("phone_123")
            Truth.assertThat(phoneEnrollment.authSession).isEqualTo("phone_session")

            coVerify(exactly = 1) {
                repository.enrollPhone(phoneNumber, requiredScope)
            }
        }

    @Test
    fun `invoke - PHONE with None input - throws IllegalArgumentException`() {
        val exception = Assert.assertThrows(IllegalArgumentException::class.java) {
            runTest {
                useCase.invoke(
                    authenticatorType = AuthenticatorType.PHONE,
                    input = EnrollmentInput.None
                )
            }
        }

        Truth.assertThat(exception.message)
            .isEqualTo("Phone enrollment requires EnrollmentInput.Phone")

        coVerify(exactly = 0) {
            repository.enrollPhone(any(), any())
        }
    }

    @Test
    fun `invoke - PHONE with Email input - throws IllegalArgumentException`() {
        val exception = Assert.assertThrows(IllegalArgumentException::class.java) {
            runTest {
                useCase.invoke(
                    authenticatorType = AuthenticatorType.PHONE,
                    input = EnrollmentInput.Email("user@example.com")
                )
            }
        }

        Truth.assertThat(exception.message)
            .isEqualTo("Phone enrollment requires EnrollmentInput.Phone")

        coVerify(exactly = 0) {
            repository.enrollPhone(any(), any())
        }
    }

    @Test
    fun `invoke - PHONE repository throws NetworkError - returns Auth0Error with NetworkError`() =
        runTest {
            val phoneNumber = "+15551234567"
            val expectedError = Auth0Error.NetworkError(
                message = "Network connection failed",
                cause = RuntimeException("No internet")
            )
            coEvery {
                repository.enrollPhone(phoneNumber, requiredScope)
            } throws expectedError

            val result = useCase.invoke(
                authenticatorType = AuthenticatorType.PHONE,
                input = EnrollmentInput.Phone(phoneNumber)
            )

            Truth.assertThat(result).isInstanceOf(Result.Error::class.java)
            val error = (result as Result.Error).error
            Truth.assertThat(error).isInstanceOf(Auth0Error.NetworkError::class.java)
            Truth.assertThat(error.message).contains("Network connection failed")

            coVerify(exactly = 1) {
                repository.enrollPhone(phoneNumber, requiredScope)
            }
        }
}
