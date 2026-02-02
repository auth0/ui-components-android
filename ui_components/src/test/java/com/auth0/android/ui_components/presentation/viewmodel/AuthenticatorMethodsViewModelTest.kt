package com.auth0.android.ui_components.presentation.viewmodel

import com.auth0.android.ui_components.TestData
import com.auth0.android.ui_components.domain.error.Auth0Error
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.network.Result
import com.auth0.android.ui_components.domain.usecase.GetEnabledAuthenticatorMethodsUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
class AuthenticatorMethodsViewModelTest {

    private lateinit var getEnabledAuthenticatorMethodsUseCase: GetEnabledAuthenticatorMethodsUseCase

    private lateinit var viewModel: AuthenticatorMethodsViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        getEnabledAuthenticatorMethodsUseCase = mockk()

        viewModel = AuthenticatorMethodsViewModel(
            getEnabledAuthenticatorMethodsUseCase = getEnabledAuthenticatorMethodsUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }


    @Test
    fun `initialization - uiState collected - starts with Loading state and automatically fetches authenticator methods`() =
        runTest {
            coEvery { getEnabledAuthenticatorMethodsUseCase() } returns Result.Success(
                TestData.authenticatorMethod
            )

            val initialState = viewModel.uiState.value

            assertThat(initialState).isInstanceOf(AuthenticatorUiState.Loading::class.java)

            val job = launch {
                viewModel.uiState.collect { }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) { getEnabledAuthenticatorMethodsUseCase() }

            val finalState = viewModel.uiState.value
            assertThat(finalState).isInstanceOf(AuthenticatorUiState.Success::class.java)

            job.cancel()
        }


    @Test
    fun `fetchAuthenticatorMethods - successful response - emits Success state with correctly mapped secondary MFA authenticators`() =
        runTest {
            coEvery { getEnabledAuthenticatorMethodsUseCase() } returns Result.Success(
                TestData.authenticatorMethod
            )

            val job = launch {
                viewModel.uiState.collect { }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(AuthenticatorUiState.Success::class.java)

            val successState = state as AuthenticatorUiState.Success
            assertThat(successState.secondaryData).hasSize(5)

            val totpData = successState.secondaryData.find { it.type == AuthenticatorType.TOTP }
            assertThat(totpData).isNotNull()
            assertThat(totpData?.title).isEqualTo("Authenticator App")
            assertThat(totpData?.confirmed).isTrue()

            val phoneData = successState.secondaryData.find { it.type == AuthenticatorType.PHONE }
            assertThat(phoneData).isNotNull()
            assertThat(phoneData?.title).isEqualTo("SMS OTP")
            assertThat(phoneData?.confirmed).isFalse()

            val emailData = successState.secondaryData.find { it.type == AuthenticatorType.EMAIL }
            assertThat(emailData).isNotNull()
            assertThat(emailData?.title).isEqualTo("Email OTP")
            assertThat(emailData?.confirmed).isTrue()

            val pushData = successState.secondaryData.find { it.type == AuthenticatorType.PUSH }
            assertThat(pushData).isNotNull()
            assertThat(pushData?.title).isEqualTo("Push Notification")
            assertThat(pushData?.confirmed).isFalse()

            val recoveryData = successState.secondaryData.find { it.type == AuthenticatorType.RECOVERY_CODE }
            assertThat(recoveryData).isNotNull()
            assertThat(recoveryData?.title).isEqualTo("Recovery Code")
            assertThat(recoveryData?.confirmed).isTrue()

            coVerify(exactly = 1) { getEnabledAuthenticatorMethodsUseCase() }
            job.cancel()
        }

    @Test
    fun `fetchAuthenticatorMethods - only secondary MFA methods - emits Success with empty primary and populated secondary data`() =
        runTest {
            coEvery { getEnabledAuthenticatorMethodsUseCase() } returns Result.Success(
                TestData.singleSecondaryAuthenticatorMethod
            )

            val job = launch {
                viewModel.uiState.collect { }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(AuthenticatorUiState.Success::class.java)

            val successState = state as AuthenticatorUiState.Success
            assertThat(successState.primaryData).isEmpty()
            assertThat(successState.secondaryData).hasSize(1)
            assertThat(successState.secondaryData[0].type).isEqualTo(AuthenticatorType.TOTP)

            job.cancel()
        }


    @Test
    fun `fetchAuthenticatorMethods - successful response - emits Success state with correctly mapped primary authenticators`() =
        runTest {
            coEvery { getEnabledAuthenticatorMethodsUseCase() } returns Result.Success(
                TestData.authenticatorMethod
            )

            val job = launch {
                viewModel.uiState.collect { }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(AuthenticatorUiState.Success::class.java)

            val successState = state as AuthenticatorUiState.Success
            assertThat(successState.primaryData).hasSize(1)

            val passkeyData = successState.primaryData[0]
            assertThat(passkeyData.id).isEqualTo("passkey_001")
            assertThat(passkeyData.title).isEqualTo("Passkey")
            assertThat(passkeyData.createdAt).isEqualTo("2025-11-10T10:00:00.000Z")

            coVerify(exactly = 1) { getEnabledAuthenticatorMethodsUseCase() }
            job.cancel()
        }

    @Test
    fun `fetchAuthenticatorMethods - only primary passkeys - emits Success with populated primary and empty secondary data`() =
        runTest {
            coEvery { getEnabledAuthenticatorMethodsUseCase() } returns Result.Success(
                TestData.multiplePrimaryAuthenticatorMethod
            )

            val job = launch {
                viewModel.uiState.collect { }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(AuthenticatorUiState.Success::class.java)

            val successState = state as AuthenticatorUiState.Success
            assertThat(successState.primaryData).hasSize(2)
            assertThat(successState.secondaryData).isEmpty()

            assertThat(successState.primaryData[0].id).isEqualTo("passkey_001")
            assertThat(successState.primaryData[1].id).isEqualTo("passkey_002")

            job.cancel()
        }

    @Test
    fun `fetchAuthenticatorMethods - multiple primary passkeys - all are correctly mapped to PrimaryAuthenticatorUiData`() =
        runTest {
            coEvery { getEnabledAuthenticatorMethodsUseCase() } returns Result.Success(
                TestData.multiplePrimaryAuthenticatorMethod
            )

            val job = launch {
                viewModel.uiState.collect { }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(AuthenticatorUiState.Success::class.java)

            val successState = state as AuthenticatorUiState.Success
            assertThat(successState.primaryData).hasSize(2)

            // Verify each passkey is mapped correctly
            successState.primaryData.forEach { primaryData ->
                assertThat(primaryData.title).isEqualTo("Passkey")
                assertThat(primaryData.id).isNotEmpty()
                assertThat(primaryData.createdAt).isNotEmpty()
            }

            val ids = successState.primaryData.map { it.id }
            assertThat(ids).containsExactly("passkey_001", "passkey_002")

            job.cancel()
        }


    @Test
    fun `fetchAuthenticatorMethods correctly maps both authenticator types`() =
        runTest {
            coEvery { getEnabledAuthenticatorMethodsUseCase() } returns Result.Success(
                TestData.mixedAuthenticatorMethod
            )

            val job = launch {
                viewModel.uiState.collect { }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(AuthenticatorUiState.Success::class.java)

            val successState = state as AuthenticatorUiState.Success

            // Verify primary data (passkeys)
            assertThat(successState.primaryData).hasSize(1)
            assertThat(successState.primaryData[0].title).isEqualTo("Passkey")

            // Verify secondary data (MFA)
            assertThat(successState.secondaryData).hasSize(2)
            assertThat(successState.secondaryData.map { it.type }).containsExactly(
                AuthenticatorType.TOTP,
                AuthenticatorType.PHONE
            )

            job.cancel()
        }

    @Test
    fun `fetchAuthenticatorMethods - successful response with empty lists - emits Success state with empty primary and secondary data`() =
        runTest {
            coEvery { getEnabledAuthenticatorMethodsUseCase() } returns Result.Success(
                TestData.emptyAuthenticatorMethod
            )

            val job = launch {
                viewModel.uiState.collect { }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(AuthenticatorUiState.Success::class.java)

            val successState = state as AuthenticatorUiState.Success
            assertThat(successState.primaryData).isEmpty()
            assertThat(successState.secondaryData).isEmpty()

            coVerify(exactly = 1) { getEnabledAuthenticatorMethodsUseCase() }
            job.cancel()
        }


    @Test
    fun `fetchAuthenticatorMethods - network error - emits Error state with error and retry callback`() =
        runTest {
            val networkError = Auth0Error.NetworkError(
                message = "Connection failed",
                cause = Exception("Network timeout")
            )
            coEvery { getEnabledAuthenticatorMethodsUseCase() } returns Result.Error(networkError)

            val job = launch {
                viewModel.uiState.collect { }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(AuthenticatorUiState.Error::class.java)

            val errorState = state as AuthenticatorUiState.Error
            assertThat(errorState.error.error).isEqualTo(networkError)
            assertThat(errorState.error.error.message).contains("Connection failed")
            assertThat(errorState.error.onRetry).isNotNull()

            coVerify(exactly = 1) { getEnabledAuthenticatorMethodsUseCase() }
            job.cancel()
        }

    @Test
    fun `fetchAuthenticatorMethods - invalid token error - emits Error state with error and retry callback`() =
        runTest {
            val tokenError = Auth0Error.RefreshTokenInvalid(
                message = "Invalid or expired refresh token",
                cause = Exception("Token validation failed")
            )
            coEvery { getEnabledAuthenticatorMethodsUseCase() } returns Result.Error(tokenError)

            val job = launch {
                viewModel.uiState.collect { }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(AuthenticatorUiState.Error::class.java)

            val errorState = state as AuthenticatorUiState.Error
            assertThat(errorState.error.error).isEqualTo(tokenError)
            assertThat(errorState.error.error.message).contains("Invalid or expired refresh token")
            assertThat(errorState.error.onRetry).isNotNull()

            coVerify(exactly = 1) { getEnabledAuthenticatorMethodsUseCase() }
            job.cancel()
        }

    @Test
    fun `error state retry callback - invoked after error - triggers fetchAuthenticatorMethods again`() =
        runTest {
            val networkError = Auth0Error.NetworkError(
                message = "Connection failed",
                cause = Exception("Network timeout")
            )
            coEvery { getEnabledAuthenticatorMethodsUseCase() } returnsMany listOf(
                Result.Error(networkError),
                Result.Success(TestData.singleSecondaryAuthenticatorMethod)
            )

            val job = launch {
                viewModel.uiState.collect { }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            val errorState = viewModel.uiState.value
            assertThat(errorState).isInstanceOf(AuthenticatorUiState.Error::class.java)

            val retryCallback = (errorState as AuthenticatorUiState.Error).error.onRetry
            retryCallback.invoke()
            testDispatcher.scheduler.advanceUntilIdle()

            val finalState = viewModel.uiState.value
            assertThat(finalState).isInstanceOf(AuthenticatorUiState.Success::class.java)

            val successState = finalState as AuthenticatorUiState.Success
            assertThat(successState.secondaryData).hasSize(1)
            assertThat(successState.secondaryData[0].type).isEqualTo(AuthenticatorType.TOTP)

            coVerify(exactly = 2) { getEnabledAuthenticatorMethodsUseCase() }
            job.cancel()
        }

    @Test
    fun `fetchAuthenticatorMethods - called multiple times - always starts with Loading state`() =
        runTest {
            coEvery { getEnabledAuthenticatorMethodsUseCase() } coAnswers {
                delay(10)
                Result.Success(TestData.singleSecondaryAuthenticatorMethod)
            }

            val states = mutableListOf<AuthenticatorUiState>()
            val job = launch {
                viewModel.uiState.collect { state ->
                    states.add(state)
                }
            }

            testDispatcher.scheduler.advanceUntilIdle()

            val firstState = viewModel.uiState.value
            assertThat(firstState).isInstanceOf(AuthenticatorUiState.Success::class.java)
            val firstSuccess = firstState as AuthenticatorUiState.Success
            assertThat(firstSuccess.secondaryData).hasSize(1)
            assertThat(firstSuccess.secondaryData[0].type).isEqualTo(AuthenticatorType.TOTP)

            states.clear()

            coEvery { getEnabledAuthenticatorMethodsUseCase() } coAnswers {
                delay(10)
                Result.Success(TestData.singlePhoneSecondaryAuthenticatorMethod)
            }
            viewModel.fetchAuthenticatorMethods()
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(states.size).isAtLeast(2)
            assertThat(states[0]).isInstanceOf(AuthenticatorUiState.Loading::class.java)

            val lastState = states.last()
            assertThat(lastState).isInstanceOf(AuthenticatorUiState.Success::class.java)
            val successState = lastState as AuthenticatorUiState.Success
            assertThat(successState.secondaryData).hasSize(1)
            assertThat(successState.secondaryData[0].type).isEqualTo(AuthenticatorType.PHONE)

            coVerify(exactly = 2) { getEnabledAuthenticatorMethodsUseCase() }

            job.cancel()
        }
}
