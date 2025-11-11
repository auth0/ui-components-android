package com.auth0.android.ui_components.domain

import com.auth0.android.result.AuthenticationMethod
import com.auth0.android.result.MfaAuthenticationMethod
import com.auth0.android.result.TotpAuthenticationMethod
import com.auth0.android.ui_components.TestData
import com.auth0.android.ui_components.domain.error.Auth0Error
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.network.Result
import com.auth0.android.ui_components.domain.repository.MyAccountRepository
import com.auth0.android.ui_components.domain.usecase.GetAuthenticationMethodsUseCase
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
class GetAuthenticationMethodsUseCaseTest {

    private lateinit var repository: MyAccountRepository
    private lateinit var useCase: GetAuthenticationMethodsUseCase

    private val testDispatcher = StandardTestDispatcher()
    private val dispatcherProvider = TestDispatcherProvider(testDispatcher)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        repository = mockk()
        useCase = GetAuthenticationMethodsUseCase(
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
    fun `invoke - TOTP type with confirmed methods - returns filtered TOTP list`() = runTest {
        val totpMethod = TestData.totpAuthMethod

        val authMethods = listOf<AuthenticationMethod>(totpMethod)
        coEvery {
            repository.getAuthenticatorMethods("read:me:authentication_methods")
        } returns authMethods

        val result = useCase.invoke(AuthenticatorType.TOTP)

        Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
        val enrolledMethods = (result as Result.Success).data
        Truth.assertThat(enrolledMethods).hasSize(1)
        Truth.assertThat(enrolledMethods[0].id).isEqualTo("auth_totp_789")
        Truth.assertThat(enrolledMethods[0].type).isEqualTo("totp")
        Truth.assertThat(enrolledMethods[0].confirmed).isTrue()
        Truth.assertThat(enrolledMethods[0].name).isEqualTo("Authenticator App")

        coVerify(exactly = 1) {
            repository.getAuthenticatorMethods("read:me:authentication_methods")
        }
    }

    @Test
    fun `invoke - TOTP type with unconfirmed methods - returns empty list`() = runTest {
        val unconfirmedTotp = TestData.totpAuthMethod.copy(confirmed = false)

        val authMethods = listOf<AuthenticationMethod>(unconfirmedTotp)
        coEvery {
            repository.getAuthenticatorMethods("read:me:authentication_methods")
        } returns authMethods

        val result = useCase.invoke(AuthenticatorType.TOTP)

        Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
        val enrolledMethods = (result as Result.Success).data
        Truth.assertThat(enrolledMethods).isEmpty()

        coVerify(exactly = 1) {
            repository.getAuthenticatorMethods("read:me:authentication_methods")
        }
    }

    @Test
    fun `invoke - PHONE type with confirmed methods - returns filtered PHONE list`() = runTest {
        val phoneMethod = TestData.phoneAuthMethod

        val authMethods = listOf<AuthenticationMethod>(phoneMethod)
        coEvery {
            repository.getAuthenticatorMethods("read:me:authentication_methods")
        } returns authMethods

        val result = useCase.invoke(AuthenticatorType.PHONE)

        Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
        val enrolledMethods = (result as Result.Success).data
        Truth.assertThat(enrolledMethods).hasSize(1)
        Truth.assertThat(enrolledMethods[0].id).isEqualTo("auth_phone_123")
        Truth.assertThat(enrolledMethods[0].type).isEqualTo("phone")
        Truth.assertThat(enrolledMethods[0].confirmed).isTrue()
        Truth.assertThat(enrolledMethods[0].name).isEqualTo("My Phone")

        coVerify(exactly = 1) {
            repository.getAuthenticatorMethods("read:me:authentication_methods")
        }
    }

    @Test
    fun `invoke - PHONE type with null name - uses phoneNumber as name`() = runTest {
        val phoneMethod = TestData.phoneAuthMethod.copy(name = null)

        val authMethods = listOf<AuthenticationMethod>(phoneMethod)
        coEvery {
            repository.getAuthenticatorMethods("read:me:authentication_methods")
        } returns authMethods

        val result = useCase.invoke(AuthenticatorType.PHONE)

        Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
        val enrolledMethods = (result as Result.Success).data
        Truth.assertThat(enrolledMethods).hasSize(1)
        Truth.assertThat(enrolledMethods[0].name).isEqualTo("+15551234567")
    }


    @Test
    fun `invoke - EMAIL type with confirmed methods - returns filtered EMAIL list`() = runTest {
        val emailMethod = TestData.emailAuthMethod

        val authMethods = listOf<AuthenticationMethod>(emailMethod)
        coEvery {
            repository.getAuthenticatorMethods("read:me:authentication_methods")
        } returns authMethods

        val result = useCase.invoke(AuthenticatorType.EMAIL)

        Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
        val enrolledMethods = (result as Result.Success).data
        Truth.assertThat(enrolledMethods).hasSize(1)
        Truth.assertThat(enrolledMethods[0].id).isEqualTo("auth_email_456")
        Truth.assertThat(enrolledMethods[0].type).isEqualTo("email")
        Truth.assertThat(enrolledMethods[0].confirmed).isTrue()

        coVerify(exactly = 1) {
            repository.getAuthenticatorMethods("read:me:authentication_methods")
        }
    }


    @Test
    fun `invoke - PUSH type with confirmed methods - returns filtered PUSH list`() = runTest {
        val pushMethod = TestData.pushNotificationAuthMethod

        val authMethods = listOf<AuthenticationMethod>(pushMethod)
        coEvery {
            repository.getAuthenticatorMethods("read:me:authentication_methods")
        } returns authMethods

        val result = useCase.invoke(AuthenticatorType.PUSH)

        Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
        val enrolledMethods = (result as Result.Success).data
        Truth.assertThat(enrolledMethods).hasSize(1)
        Truth.assertThat(enrolledMethods[0].id).isEqualTo("auth_push_012")
        Truth.assertThat(enrolledMethods[0].type).isEqualTo("push-notification")
        Truth.assertThat(enrolledMethods[0].confirmed).isTrue()
        Truth.assertThat(enrolledMethods[0].name).isEqualTo("My Push Device")

        coVerify(exactly = 1) {
            repository.getAuthenticatorMethods("read:me:authentication_methods")
        }
    }


    @Test
    fun `invoke - RECOVERY_CODE type with confirmed methods - returns filtered RECOVERY_CODE list`() =
        runTest {
            val recoveryCodeMethod = TestData.recoveryAuthMethod

            val authMethods = listOf<AuthenticationMethod>(recoveryCodeMethod)
            coEvery {
                repository.getAuthenticatorMethods("read:me:authentication_methods")
            } returns authMethods

            val result = useCase.invoke(AuthenticatorType.RECOVERY_CODE)

            Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
            val enrolledMethods = (result as Result.Success).data
            Truth.assertThat(enrolledMethods).hasSize(1)
            Truth.assertThat(enrolledMethods[0].id).isEqualTo("auth_recovery_789")
            Truth.assertThat(enrolledMethods[0].type).isEqualTo("recovery-code")
            Truth.assertThat(enrolledMethods[0].confirmed).isTrue()

            coVerify(exactly = 1) {
                repository.getAuthenticatorMethods("read:me:authentication_methods")
            }
        }

    // ========== Mixed Authentication Methods Tests ==========

    @Test
    fun `invoke - mixed types with TOTP filter - returns only confirmed TOTP methods`() = runTest {
        val totpMethod = TestData.totpAuthMethod

        val phoneMethod = TestData.phoneAuthMethod

        val authMethods = listOf<AuthenticationMethod>(totpMethod, phoneMethod)
        coEvery {
            repository.getAuthenticatorMethods("read:me:authentication_methods")
        } returns authMethods

        val result = useCase.invoke(AuthenticatorType.TOTP)

        Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
        val enrolledMethods = (result as Result.Success).data
        Truth.assertThat(enrolledMethods).hasSize(1)
        Truth.assertThat(enrolledMethods[0].type).isEqualTo("totp")
    }

    @Test
    fun `invoke - password type excluded - filters out password methods`() = runTest {
        val passwordMethod = mockk<MfaAuthenticationMethod> {
            coEvery { id } returns "pwd_123"
            coEvery { type } returns "password"
            coEvery { confirmed } returns true
            coEvery { createdAt } returns "2024-01-01T00:00:00Z"
        }

        val totpMethod = TestData.totpAuthMethod
        val authMethods = listOf<AuthenticationMethod>(passwordMethod, totpMethod)
        coEvery {
            repository.getAuthenticatorMethods("read:me:authentication_methods")
        } returns authMethods

        val result = useCase.invoke(AuthenticatorType.TOTP)

        Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
        val enrolledMethods = (result as Result.Success).data
        Truth.assertThat(enrolledMethods).hasSize(1)
        Truth.assertThat(enrolledMethods[0].type).isEqualTo("totp")
    }

    @Test
    fun `invoke - no matching type found - returns empty list`() = runTest {
        val phoneMethod = TestData.phoneAuthMethod

        val authMethods = listOf<AuthenticationMethod>(phoneMethod)
        coEvery {
            repository.getAuthenticatorMethods("read:me:authentication_methods")
        } returns authMethods

        val result = useCase.invoke(AuthenticatorType.TOTP)

        Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
        val enrolledMethods = (result as Result.Success).data
        Truth.assertThat(enrolledMethods).isEmpty()
    }


    @Test
    fun `invoke - repository throws NetworkError - returns Error with Auth0Error`() = runTest {
        val expectedError = Auth0Error.NetworkError(
            message = "Network connection failed",
            cause = RuntimeException("No internet")
        )
        coEvery {
            repository.getAuthenticatorMethods("read:me:authentication_methods")
        } throws expectedError

        val result = useCase.invoke(AuthenticatorType.TOTP)

        Truth.assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = (result as Result.Error).error
        Truth.assertThat(error).isInstanceOf(Auth0Error.NetworkError::class.java)
        Truth.assertThat(error.message).contains("Network connection failed")

        coVerify(exactly = 1) {
            repository.getAuthenticatorMethods("read:me:authentication_methods")
        }
    }

    @Test
    fun `invoke - repository throws UnknownError - returns Error with Auth0Error`() = runTest {
        // Given
        val expectedError = Auth0Error.Unknown(
            message = "Unknown error occurred",
            cause = RuntimeException("Unexpected error")
        )
        coEvery {
            repository.getAuthenticatorMethods("read:me:authentication_methods")
        } throws expectedError

        // When
        val result = useCase.invoke(AuthenticatorType.EMAIL)

        // Then
        Truth.assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = (result as Result.Error).error
        Truth.assertThat(error).isInstanceOf(Auth0Error.Unknown::class.java)
        Truth.assertThat(error.message).contains("Unknown error occurred")

        coVerify(exactly = 1) {
            repository.getAuthenticatorMethods("read:me:authentication_methods")
        }
    }

    @Test
    fun `invoke - verifies correct scopes are passed to repository`() = runTest {
        val expectedScope = "read:me:authentication_methods"
        val totpMethod = mockk<TotpAuthenticationMethod> {
            coEvery { id } returns "totp_123"
            coEvery { type } returns "totp"
            coEvery { confirmed } returns true
            coEvery { createdAt } returns "2024-01-01T00:00:00Z"
            coEvery { name } returns "My Authenticator"
        }

        coEvery {
            repository.getAuthenticatorMethods(expectedScope)
        } returns listOf(totpMethod)

        useCase.invoke(AuthenticatorType.TOTP)

        coVerify(exactly = 1) {
            repository.getAuthenticatorMethods(expectedScope)
        }
    }
}