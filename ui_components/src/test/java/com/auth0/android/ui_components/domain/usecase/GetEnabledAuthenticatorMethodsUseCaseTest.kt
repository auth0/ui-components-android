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
    fun `invoke - multiple secondary factors with confirmed auth methods - returns secondary authenticators with confirmed true`() =
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
            val authenticatorMethod = (result as Result.Success).data

            // Assert on secondaryAuthenticators
            Truth.assertThat(authenticatorMethod.secondaryAuthenticators).hasSize(2)

            val phoneMethod = authenticatorMethod.secondaryAuthenticators.find { it.type == AuthenticatorType.PHONE }
            Truth.assertThat(phoneMethod).isNotNull()
            Truth.assertThat(phoneMethod?.confirmed).isTrue()
            Truth.assertThat(phoneMethod?.usage).containsExactly("primary")

            val totpMethod = authenticatorMethod.secondaryAuthenticators.find { it.type == AuthenticatorType.TOTP }
            Truth.assertThat(totpMethod).isNotNull()
            Truth.assertThat(totpMethod?.confirmed).isTrue()
            Truth.assertThat(totpMethod?.usage).containsExactly("secondary")

            coVerify(exactly = 1) { repository.getFactors(requiredScopesFactor) }
            coVerify(exactly = 1) { repository.getAuthenticatorMethods(requiredScopesAuthentication) }
        }

    @Test
    fun `invoke - multiple secondary factors with mixed confirmed status - returns secondary authenticators with appropriate confirmed flags`() =
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
            val authenticatorMethod = (result as Result.Success).data

            Truth.assertThat(authenticatorMethod.secondaryAuthenticators).hasSize(2)

            val phoneMethod = authenticatorMethod.secondaryAuthenticators.find { it.type == AuthenticatorType.PHONE }
            Truth.assertThat(phoneMethod?.confirmed).isTrue()

            val totpMethod = authenticatorMethod.secondaryAuthenticators.find { it.type == AuthenticatorType.TOTP }
            Truth.assertThat(totpMethod?.confirmed).isFalse()

            coVerify(exactly = 1) { repository.getFactors(requiredScopesFactor) }
            coVerify(exactly = 1) { repository.getAuthenticatorMethods(requiredScopesAuthentication) }
        }

    @Test
    fun `invoke - empty factors with MFA auth methods - returns empty secondary authenticators list`() = runTest {
        coEvery { repository.getFactors(any<String>()) } returns emptyList()
        coEvery { repository.getAuthenticatorMethods(any<String>()) } returns listOf<AuthenticationMethod>(
            TestData.totpAuthMethod,
            TestData.phoneAuthMethod
        )

        val result = useCase.invoke()

        Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
        val authenticatorMethod = (result as Result.Success).data

        Truth.assertThat(authenticatorMethod.secondaryAuthenticators).isEmpty()
        Truth.assertThat(authenticatorMethod.primaryAuthenticators).isEmpty()

        coVerify(exactly = 1) { repository.getFactors(requiredScopesFactor) }
        coVerify(exactly = 1) { repository.getAuthenticatorMethods(requiredScopesAuthentication) }
    }

    @Test
    fun `invoke - TOTP secondary factor with confirmed auth method - returns secondary TOTP authenticator with confirmed true`() =
        runTest {
            val factors = listOf(TestData.totpFactor)
            val authMethods = listOf<AuthenticationMethod>(TestData.totpAuthMethod)

            coEvery { repository.getFactors(any<String>()) } returns factors
            coEvery { repository.getAuthenticatorMethods(any<String>()) } returns authMethods

            val result = useCase.invoke()

            Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
            val authenticatorMethod = (result as Result.Success).data

            Truth.assertThat(authenticatorMethod.secondaryAuthenticators).hasSize(1)
            Truth.assertThat(authenticatorMethod.secondaryAuthenticators[0].type).isEqualTo(AuthenticatorType.TOTP)
            Truth.assertThat(authenticatorMethod.secondaryAuthenticators[0].confirmed).isTrue()
            Truth.assertThat(authenticatorMethod.secondaryAuthenticators[0].usage).containsExactly("secondary")

            coVerify(exactly = 1) { repository.getFactors(requiredScopesFactor) }
            coVerify(exactly = 1) { repository.getAuthenticatorMethods(requiredScopesAuthentication) }
        }

    @Test
    fun `invoke - Phone secondary factor with confirmed auth method - returns secondary PHONE authenticator with confirmed true`() =
        runTest {
            val factors = listOf(TestData.phoneFactor)
            val authMethods = listOf<AuthenticationMethod>(TestData.phoneAuthMethod)

            coEvery { repository.getFactors(any<String>()) } returns factors
            coEvery { repository.getAuthenticatorMethods(any<String>()) } returns authMethods

            val result = useCase.invoke()

            Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
            val authenticatorMethod = (result as Result.Success).data

            Truth.assertThat(authenticatorMethod.secondaryAuthenticators).hasSize(1)
            Truth.assertThat(authenticatorMethod.secondaryAuthenticators[0].type).isEqualTo(AuthenticatorType.PHONE)
            Truth.assertThat(authenticatorMethod.secondaryAuthenticators[0].confirmed).isTrue()
            Truth.assertThat(authenticatorMethod.secondaryAuthenticators[0].usage).containsExactly("primary")

            coVerify(exactly = 1) { repository.getFactors(requiredScopesFactor) }
            coVerify(exactly = 1) { repository.getAuthenticatorMethods(requiredScopesAuthentication) }
        }

    @Test
    fun `invoke - Email secondary factor with confirmed auth method - returns secondary EMAIL authenticator with confirmed true`() =
        runTest {
            val emailFactor = Factor(type = "email", usage = listOf("primary"))
            val factors = listOf(emailFactor)
            val authMethods = listOf<AuthenticationMethod>(TestData.emailAuthMethod)

            coEvery { repository.getFactors(any<String>()) } returns factors
            coEvery { repository.getAuthenticatorMethods(any<String>()) } returns authMethods

            val result = useCase.invoke()

            Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
            val authenticatorMethod = (result as Result.Success).data

            Truth.assertThat(authenticatorMethod.secondaryAuthenticators).hasSize(1)
            Truth.assertThat(authenticatorMethod.secondaryAuthenticators[0].type).isEqualTo(AuthenticatorType.EMAIL)
            Truth.assertThat(authenticatorMethod.secondaryAuthenticators[0].confirmed).isTrue()
            Truth.assertThat(authenticatorMethod.secondaryAuthenticators[0].usage).containsExactly("primary")

            coVerify(exactly = 1) { repository.getFactors(requiredScopesFactor) }
            coVerify(exactly = 1) { repository.getAuthenticatorMethods(requiredScopesAuthentication) }
        }

    @Test
    fun `invoke - Push secondary factor with confirmed auth method - returns secondary PUSH authenticator with confirmed true`() =
        runTest {
            val pushFactor = Factor(type = "push-notification", usage = listOf("secondary"))
            val factors = listOf(pushFactor)
            val authMethods = listOf<AuthenticationMethod>(TestData.pushNotificationAuthMethod)

            coEvery { repository.getFactors(any<String>()) } returns factors
            coEvery { repository.getAuthenticatorMethods(any<String>()) } returns authMethods

            val result = useCase.invoke()

            Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
            val authenticatorMethod = (result as Result.Success).data

            Truth.assertThat(authenticatorMethod.secondaryAuthenticators).hasSize(1)
            Truth.assertThat(authenticatorMethod.secondaryAuthenticators[0].type).isEqualTo(AuthenticatorType.PUSH)
            Truth.assertThat(authenticatorMethod.secondaryAuthenticators[0].confirmed).isTrue()
            Truth.assertThat(authenticatorMethod.secondaryAuthenticators[0].usage).containsExactly("secondary")

            coVerify(exactly = 1) { repository.getFactors(requiredScopesFactor) }
            coVerify(exactly = 1) { repository.getAuthenticatorMethods(requiredScopesAuthentication) }
        }

    @Test
    fun `invoke - Recovery-code secondary factor with confirmed auth method - returns secondary RECOVERY_CODE authenticator with confirmed true`() =
        runTest {
            val recoveryFactor = Factor(type = "recovery-code", usage = listOf("secondary"))
            val factors = listOf(recoveryFactor)
            val authMethods = listOf<AuthenticationMethod>(TestData.recoveryAuthMethod)

            coEvery { repository.getFactors(any<String>()) } returns factors
            coEvery { repository.getAuthenticatorMethods(any<String>()) } returns authMethods

            val result = useCase.invoke()

            Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
            val authenticatorMethod = (result as Result.Success).data

            Truth.assertThat(authenticatorMethod.secondaryAuthenticators).hasSize(1)
            Truth.assertThat(authenticatorMethod.secondaryAuthenticators[0].type)
                .isEqualTo(AuthenticatorType.RECOVERY_CODE)
            Truth.assertThat(authenticatorMethod.secondaryAuthenticators[0].confirmed).isTrue()
            Truth.assertThat(authenticatorMethod.secondaryAuthenticators[0].usage).containsExactly("secondary")

            coVerify(exactly = 1) { repository.getFactors(requiredScopesFactor) }
            coVerify(exactly = 1) { repository.getAuthenticatorMethods(requiredScopesAuthentication) }
        }

    @Test
    fun `invoke - unknown secondary factor type - maps to TOTP as default in secondary authenticators`() = runTest {
        val unknownFactor = Factor(type = "unknown-type", usage = listOf("secondary"))
        val factors = listOf(unknownFactor)
        val authMethods = emptyList<AuthenticationMethod>()

        coEvery { repository.getFactors(any<String>()) } returns factors
        coEvery { repository.getAuthenticatorMethods(any<String>()) } returns authMethods

        val result = useCase.invoke()

        Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
        val authenticatorMethod = (result as Result.Success).data

        Truth.assertThat(authenticatorMethod.secondaryAuthenticators).hasSize(1)
        Truth.assertThat(authenticatorMethod.secondaryAuthenticators[0].type).isEqualTo(AuthenticatorType.TOTP)
        Truth.assertThat(authenticatorMethod.secondaryAuthenticators[0].confirmed).isFalse()
        Truth.assertThat(authenticatorMethod.secondaryAuthenticators[0].usage).containsExactly("secondary")

        coVerify(exactly = 1) { repository.getFactors(requiredScopesFactor) }
        coVerify(exactly = 1) { repository.getAuthenticatorMethods(requiredScopesAuthentication) }
    }


    @Test
    fun `invoke - single passkey auth method present - returns primary authenticator with passkey`() =
        runTest {
            val factors = emptyList<Factor>()
            val authMethods = listOf<AuthenticationMethod>(TestData.passkeyAuthMethod)

            coEvery { repository.getFactors(any<String>()) } returns factors
            coEvery { repository.getAuthenticatorMethods(any<String>()) } returns authMethods

            val result = useCase.invoke()

            Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
            val authenticatorMethod = (result as Result.Success).data

            Truth.assertThat(authenticatorMethod.primaryAuthenticators).hasSize(1)
            Truth.assertThat(authenticatorMethod.primaryAuthenticators[0].type).isEqualTo("passkey")
            Truth.assertThat(authenticatorMethod.primaryAuthenticators[0].id).isEqualTo("passkey_001")
            
            Truth.assertThat(authenticatorMethod.secondaryAuthenticators).isEmpty()

            coVerify(exactly = 1) { repository.getFactors(requiredScopesFactor) }
            coVerify(exactly = 1) { repository.getAuthenticatorMethods(requiredScopesAuthentication) }
        }

    @Test
    fun `invoke - multiple passkeys present - returns all primary authenticators`() =
        runTest {
            val factors = emptyList<Factor>()
            val authMethods = listOf<AuthenticationMethod>(
                TestData.passkeyAuthMethod,
                TestData.passkeyAuthMethod2
            )

            coEvery { repository.getFactors(any<String>()) } returns factors
            coEvery { repository.getAuthenticatorMethods(any<String>()) } returns authMethods

            val result = useCase.invoke()

            Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
            val authenticatorMethod = (result as Result.Success).data

            Truth.assertThat(authenticatorMethod.primaryAuthenticators).hasSize(2)
            val ids = authenticatorMethod.primaryAuthenticators.map { it.id }
            Truth.assertThat(ids).containsExactly("passkey_001", "passkey_002")
            
            Truth.assertThat(authenticatorMethod.secondaryAuthenticators).isEmpty()

            coVerify(exactly = 1) { repository.getFactors(requiredScopesFactor) }
            coVerify(exactly = 1) { repository.getAuthenticatorMethods(requiredScopesAuthentication) }
        }


    @Test
    fun `invoke - mixed passkey primary and MFA secondary methods - returns both primary and secondary authenticators`() =
        runTest {
            val factors = listOf(TestData.totpFactor, TestData.phoneFactor)
            val authMethods = listOf<AuthenticationMethod>(
                TestData.passkeyAuthMethod,
                TestData.totpAuthMethod,
                TestData.phoneAuthMethod
            )

            coEvery { repository.getFactors(any<String>()) } returns factors
            coEvery { repository.getAuthenticatorMethods(any<String>()) } returns authMethods

            val result = useCase.invoke()

            Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
            val authenticatorMethod = (result as Result.Success).data

            // Verify primary authenticators (passkeys)
            Truth.assertThat(authenticatorMethod.primaryAuthenticators).hasSize(1)
            Truth.assertThat(authenticatorMethod.primaryAuthenticators[0].type).isEqualTo("passkey")
            Truth.assertThat(authenticatorMethod.primaryAuthenticators[0].id).isEqualTo("passkey_001")

            // Verify secondary authenticators (MFA)
            Truth.assertThat(authenticatorMethod.secondaryAuthenticators).hasSize(2)
            val secondaryTypes = authenticatorMethod.secondaryAuthenticators.map { it.type }
            Truth.assertThat(secondaryTypes).containsExactly(AuthenticatorType.TOTP, AuthenticatorType.PHONE)

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
