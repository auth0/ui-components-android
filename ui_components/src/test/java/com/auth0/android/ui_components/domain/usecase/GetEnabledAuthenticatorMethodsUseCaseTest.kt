package com.auth0.android.ui_components.domain.usecase

import com.auth0.android.result.AuthenticationMethod
import com.auth0.android.result.Factor
import com.auth0.android.ui_components.TestData
import com.auth0.android.ui_components.domain.error.Auth0Error
import com.auth0.android.ui_components.domain.model.AuthenticatorType
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
class GetEnabledAuthenticatorMethodsUseCaseTest {

    private lateinit var repository: MyAccountRepository
    private lateinit var useCase: GetEnabledAuthenticatorMethodsUseCase

    private val testDispatcher = StandardTestDispatcher()
    private val dispatcherProvider = TestDispatcherProvider(testDispatcher)

    private val requiredScopesFactor = "read:me:factors"
    private val requiredScopesAuthentication = "read:me:authentication_methods"

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        useCase = GetEnabledAuthenticatorMethodsUseCase(
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
    fun `invoke - multiple factors with confirmed auth methods - returns list with confirmed true`() =
        runTest {
            val factors = listOf(TestData.phoneFactor, TestData.totpFactor)
            val authMethods = listOf<AuthenticationMethod>(
                TestData.phoneAuthMethod,
                TestData.totpAuthMethod
            )

            coEvery { repository.getFactors(any<String>()) } returns factors
            coEvery { repository.getAuthenticatorMethods(any<String>()) } returns authMethods

            val result = useCase.invoke()

            Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
            val authenticatorMethods = (result as Result.Success).data

            Truth.assertThat(authenticatorMethods).hasSize(2)

            val phoneMethod = authenticatorMethods.find { it.type == AuthenticatorType.PHONE }
            Truth.assertThat(phoneMethod).isNotNull()
            Truth.assertThat(phoneMethod?.confirmed).isTrue()
            Truth.assertThat(phoneMethod?.usage).containsExactly("primary")

            val totpMethod = authenticatorMethods.find { it.type == AuthenticatorType.TOTP }
            Truth.assertThat(totpMethod).isNotNull()
            Truth.assertThat(totpMethod?.confirmed).isTrue()
            Truth.assertThat(totpMethod?.usage).containsExactly("secondary")

            coVerify(exactly = 1) { repository.getFactors(requiredScopesFactor) }
            coVerify(exactly = 1) { repository.getAuthenticatorMethods(requiredScopesAuthentication) }
        }

    @Test
    fun `invoke - multiple factors with mixed confirmed and unconfirmed auth methods - returns list with appropriate confirmed flags`() =
        runTest {
            val factors = listOf(TestData.phoneFactor, TestData.totpFactor)
            val confirmedPhoneAuth = TestData.phoneAuthMethod
            val unconfirmedTotpAuth = TestData.totpAuthMethod.copy(confirmed = false)
            val authMethods = listOf<AuthenticationMethod>(
                confirmedPhoneAuth,
                unconfirmedTotpAuth
            )

            coEvery { repository.getFactors(any<String>()) } returns factors
            coEvery { repository.getAuthenticatorMethods(any<String>()) } returns authMethods

            val result = useCase.invoke()

            Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
            val authenticatorMethods = (result as Result.Success).data

            Truth.assertThat(authenticatorMethods).hasSize(2)

            val phoneMethod = authenticatorMethods.find { it.type == AuthenticatorType.PHONE }
            Truth.assertThat(phoneMethod?.confirmed).isTrue()

            val totpMethod = authenticatorMethods.find { it.type == AuthenticatorType.TOTP }
            Truth.assertThat(totpMethod?.confirmed).isFalse()

            coVerify(exactly = 1) { repository.getFactors(requiredScopesFactor) }
            coVerify(exactly = 1) { repository.getAuthenticatorMethods(requiredScopesAuthentication) }
        }

    @Test
    fun `invoke - factor with no matching auth method - returns AuthenticatorMethod with default confirmed value false`() =
        runTest {
            val factors = listOf(TestData.phoneFactor, TestData.totpFactor)
            val authMethods = listOf<AuthenticationMethod>(TestData.phoneAuthMethod)

            coEvery { repository.getFactors(any<String>()) } returns factors
            coEvery { repository.getAuthenticatorMethods(any<String>()) } returns authMethods

            val result = useCase.invoke()

            Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
            val authenticatorMethods = (result as Result.Success).data

            Truth.assertThat(authenticatorMethods).hasSize(2)

            val phoneMethod = authenticatorMethods.find { it.type == AuthenticatorType.PHONE }
            Truth.assertThat(phoneMethod?.confirmed).isTrue()

            val totpMethod = authenticatorMethods.find { it.type == AuthenticatorType.TOTP }
            Truth.assertThat(totpMethod?.confirmed).isFalse()

            coVerify(exactly = 1) { repository.getFactors(requiredScopesFactor) }
            coVerify(exactly = 1) { repository.getAuthenticatorMethods(requiredScopesAuthentication) }
        }

    @Test
    fun `invoke - empty factors and with auth methods - returns empty list`() = runTest {
        coEvery { repository.getFactors(any<String>()) } returns emptyList()
        coEvery { repository.getAuthenticatorMethods(any<String>()) } returns listOf<AuthenticationMethod>(
            TestData.totpAuthMethod,
            TestData.phoneAuthMethod
        )

        val result = useCase.invoke()

        Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
        val authenticatorMethods = (result as Result.Success).data

        Truth.assertThat(authenticatorMethods).isEmpty()

        coVerify(exactly = 1) { repository.getFactors(requiredScopesFactor) }
        coVerify(exactly = 1) { repository.getAuthenticatorMethods(requiredScopesAuthentication) }
    }

    @Test
    fun `invoke - TOTP factor with confirmed TOTP auth method - returns TOTP AuthenticatorMethod with confirmed true`() =
        runTest {
            val factors = listOf(TestData.totpFactor)
            val authMethods = listOf<AuthenticationMethod>(TestData.totpAuthMethod)

            coEvery { repository.getFactors(any<String>()) } returns factors
            coEvery { repository.getAuthenticatorMethods(any<String>()) } returns authMethods

            val result = useCase.invoke()

            Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
            val authenticatorMethods = (result as Result.Success).data

            Truth.assertThat(authenticatorMethods).hasSize(1)
            Truth.assertThat(authenticatorMethods[0].type).isEqualTo(AuthenticatorType.TOTP)
            Truth.assertThat(authenticatorMethods[0].confirmed).isTrue()
            Truth.assertThat(authenticatorMethods[0].usage).containsExactly("secondary")

            coVerify(exactly = 1) { repository.getFactors(requiredScopesFactor) }
            coVerify(exactly = 1) { repository.getAuthenticatorMethods(requiredScopesAuthentication) }
        }

    @Test
    fun `invoke - Phone factor with confirmed phone auth method - returns PHONE AuthenticatorMethod with confirmed true`() =
        runTest {
            val factors = listOf(TestData.phoneFactor)
            val authMethods = listOf<AuthenticationMethod>(TestData.phoneAuthMethod)

            coEvery { repository.getFactors(any<String>()) } returns factors
            coEvery { repository.getAuthenticatorMethods(any<String>()) } returns authMethods

            val result = useCase.invoke()

            Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
            val authenticatorMethods = (result as Result.Success).data

            Truth.assertThat(authenticatorMethods).hasSize(1)
            Truth.assertThat(authenticatorMethods[0].type).isEqualTo(AuthenticatorType.PHONE)
            Truth.assertThat(authenticatorMethods[0].confirmed).isTrue()
            Truth.assertThat(authenticatorMethods[0].usage).containsExactly("primary")

            coVerify(exactly = 1) { repository.getFactors(requiredScopesFactor) }
            coVerify(exactly = 1) { repository.getAuthenticatorMethods(requiredScopesAuthentication) }
        }

    @Test
    fun `invoke - Email factor with confirmed email auth method - returns EMAIL AuthenticatorMethod with confirmed true`() =
        runTest {
            val emailFactor = Factor(type = "email", usage = listOf("primary"))
            val factors = listOf(emailFactor)
            val authMethods = listOf<AuthenticationMethod>(TestData.emailAuthMethod)

            coEvery { repository.getFactors(any<String>()) } returns factors
            coEvery { repository.getAuthenticatorMethods(any<String>()) } returns authMethods

            val result = useCase.invoke()

            Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
            val authenticatorMethods = (result as Result.Success).data

            Truth.assertThat(authenticatorMethods).hasSize(1)
            Truth.assertThat(authenticatorMethods[0].type).isEqualTo(AuthenticatorType.EMAIL)
            Truth.assertThat(authenticatorMethods[0].confirmed).isTrue()
            Truth.assertThat(authenticatorMethods[0].usage).containsExactly("primary")

            coVerify(exactly = 1) { repository.getFactors(requiredScopesFactor) }
            coVerify(exactly = 1) { repository.getAuthenticatorMethods(requiredScopesAuthentication) }
        }

    @Test
    fun `invoke - Push factor with confirmed push auth method - returns PUSH AuthenticatorMethod with confirmed true`() =
        runTest {
            val pushFactor = Factor(type = "push-notification", usage = listOf("secondary"))
            val factors = listOf(pushFactor)
            val authMethods = listOf<AuthenticationMethod>(TestData.pushNotificationAuthMethod)

            coEvery { repository.getFactors(any<String>()) } returns factors
            coEvery { repository.getAuthenticatorMethods(any<String>()) } returns authMethods

            val result = useCase.invoke()

            Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
            val authenticatorMethods = (result as Result.Success).data

            Truth.assertThat(authenticatorMethods).hasSize(1)
            Truth.assertThat(authenticatorMethods[0].type).isEqualTo(AuthenticatorType.PUSH)
            Truth.assertThat(authenticatorMethods[0].confirmed).isTrue()
            Truth.assertThat(authenticatorMethods[0].usage).containsExactly("secondary")

            coVerify(exactly = 1) { repository.getFactors(requiredScopesFactor) }
            coVerify(exactly = 1) { repository.getAuthenticatorMethods(requiredScopesAuthentication) }
        }

    @Test
    fun `invoke - Recovery-code factor with confirmed recovery auth method - returns RECOVERY_CODE AuthenticatorMethod with confirmed true`() =
        runTest {
            val recoveryFactor = Factor(type = "recovery-code", usage = listOf("secondary"))
            val factors = listOf(recoveryFactor)
            val authMethods = listOf<AuthenticationMethod>(TestData.recoveryAuthMethod)

            coEvery { repository.getFactors(any<String>()) } returns factors
            coEvery { repository.getAuthenticatorMethods(any<String>()) } returns authMethods

            val result = useCase.invoke()

            // Then
            Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
            val authenticatorMethods = (result as Result.Success).data

            Truth.assertThat(authenticatorMethods).hasSize(1)
            Truth.assertThat(authenticatorMethods[0].type)
                .isEqualTo(AuthenticatorType.RECOVERY_CODE)
            Truth.assertThat(authenticatorMethods[0].confirmed).isTrue()
            Truth.assertThat(authenticatorMethods[0].usage).containsExactly("secondary")

            coVerify(exactly = 1) { repository.getFactors(requiredScopesFactor) }
            coVerify(exactly = 1) { repository.getAuthenticatorMethods(requiredScopesAuthentication) }
        }

    @Test
    fun `invoke - auth methods include password type - filters out password type`() = runTest {
        val factors = listOf(TestData.totpFactor)
        val passwordAuthMethod = TestData.totpAuthMethod.copy(type = "password")
        val authMethods = listOf<AuthenticationMethod>(
            TestData.totpAuthMethod,
            passwordAuthMethod
        )

        coEvery { repository.getFactors(any<String>()) } returns factors
        coEvery { repository.getAuthenticatorMethods(any<String>()) } returns authMethods

        val result = useCase.invoke()

        Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
        val authenticatorMethods = (result as Result.Success).data

        Truth.assertThat(authenticatorMethods).hasSize(1)
        Truth.assertThat(authenticatorMethods[0].type).isEqualTo(AuthenticatorType.TOTP)
        Truth.assertThat(authenticatorMethods[0].confirmed).isTrue()

        coVerify(exactly = 1) { repository.getFactors(requiredScopesFactor) }
        coVerify(exactly = 1) { repository.getAuthenticatorMethods(requiredScopesAuthentication) }
    }

    @Test
    fun `invoke - unknown factor type - maps to TOTP as default`() = runTest {
        val unknownFactor = Factor(type = "unknown-type", usage = listOf("secondary"))
        val factors = listOf(unknownFactor)
        val authMethods = emptyList<AuthenticationMethod>()

        coEvery { repository.getFactors(any<String>()) } returns factors
        coEvery { repository.getAuthenticatorMethods(any<String>()) } returns authMethods

        val result = useCase.invoke()

        Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
        val authenticatorMethods = (result as Result.Success).data

        Truth.assertThat(authenticatorMethods).hasSize(1)
        Truth.assertThat(authenticatorMethods[0].type).isEqualTo(AuthenticatorType.TOTP)
        Truth.assertThat(authenticatorMethods[0].confirmed).isFalse()
        Truth.assertThat(authenticatorMethods[0].usage).containsExactly("secondary")

        coVerify(exactly = 1) { repository.getFactors(requiredScopesFactor) }
        coVerify(exactly = 1) { repository.getAuthenticatorMethods(requiredScopesAuthentication) }
    }

    @Test
    fun `invoke - repository getFactors throws exception - returns Auth0Error`() =
        runTest {
            val expectedError = Auth0Error.NetworkError(
                message = "Network connection failed",
                cause = RuntimeException("No internet")
            )

            coEvery { repository.getFactors(any<String>()) } throws expectedError
            coEvery { repository.getAuthenticatorMethods(any<String>()) } returns emptyList()

            val result = useCase.invoke()

            Truth.assertThat(result).isInstanceOf(Result.Error::class.java)
            val error = (result as Result.Error).error

            Truth.assertThat(error).isInstanceOf(Auth0Error.NetworkError::class.java)
            Truth.assertThat(error.message).isEqualTo("Network connection failed")

            coVerify(exactly = 1) { repository.getFactors(requiredScopesFactor) }
        }

    @Test
    fun `invoke - repository getAuthenticatorMethods throws exception - returns Auth0Error`() =
        runTest {
            val expectedError = Auth0Error.NetworkError(
                message = "Failed to fetch authentication methods",
                cause = RuntimeException("Timeout")
            )
            coEvery { repository.getFactors(any<String>()) } returns listOf(TestData.totpFactor)
            coEvery { repository.getAuthenticatorMethods(any<String>()) } throws expectedError

            val result = useCase.invoke()

            Truth.assertThat(result).isInstanceOf(Result.Error::class.java)
            val error = (result as Result.Error).error

            Truth.assertThat(error).isInstanceOf(Auth0Error.NetworkError::class.java)
            Truth.assertThat(error.message).isEqualTo("Failed to fetch authentication methods")

            coVerify(exactly = 1) { repository.getFactors(requiredScopesFactor) }
            coVerify(exactly = 1) { repository.getAuthenticatorMethods(requiredScopesAuthentication) }
        }
}
