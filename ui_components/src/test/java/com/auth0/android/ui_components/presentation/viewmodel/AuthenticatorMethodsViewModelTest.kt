package com.auth0.android.ui_components.presentation.viewmodel

import com.auth0.android.ui_components.TestData
import com.auth0.android.ui_components.domain.error.Auth0Error
import com.auth0.android.ui_components.domain.network.Result
import com.auth0.android.ui_components.domain.usecase.GetEnabledAuthenticatorMethodsUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

        getEnabledAuthenticatorMethodsUseCase = mockk(relaxed = true)

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
                TestData.allAuthenticatorMethods
            )

            val initialState = viewModel.uiState.value

            // Then - Initial state should be Loading
            assertThat(initialState).isInstanceOf(AuthenticatorUiState.Loading::class.java)

            // When - Start collecting the flow to trigger onStart
            val job = backgroundScope.launch {
                viewModel.uiState.collect { /* Keep collecting */ }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Use case should be called automatically via onStart
            coVerify(exactly = 1) { getEnabledAuthenticatorMethodsUseCase() }

            // And - State should transition to Success after fetch completes
            val finalState = viewModel.uiState.value
            assertThat(finalState).isInstanceOf(AuthenticatorUiState.Success::class.java)

            // Cleanup
            job.cancel()
        }

    @Test
    fun `fetchAuthenticatorMethods - successful response - emits Success state with correctly mapped AuthenticatorUiData`() =
        runTest {
            coEvery { getEnabledAuthenticatorMethodsUseCase() } returns Result.Success(
                TestData.allAuthenticatorMethods
            )

            val job = backgroundScope.launch {
                viewModel.uiState.collect { /* Keep collecting */ }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(AuthenticatorUiState.Success::class.java)

            val successState = state as AuthenticatorUiState.Success
            assertThat(successState.data).hasSize(5)

            val totpData =
                successState.data.find { it.type == com.auth0.android.ui_components.domain.model.AuthenticatorType.TOTP }
            assertThat(totpData).isNotNull()
            assertThat(totpData?.title).isEqualTo("Authenticator App")
            assertThat(totpData?.confirmed).isTrue()

            val phoneData =
                successState.data.find { it.type == com.auth0.android.ui_components.domain.model.AuthenticatorType.PHONE }
            assertThat(phoneData).isNotNull()
            assertThat(phoneData?.title).isEqualTo("SMS OTP")
            assertThat(phoneData?.confirmed).isFalse()

            val emailData =
                successState.data.find { it.type == com.auth0.android.ui_components.domain.model.AuthenticatorType.EMAIL }
            assertThat(emailData).isNotNull()
            assertThat(emailData?.title).isEqualTo("Email OTP")
            assertThat(emailData?.confirmed).isTrue()

            val pushData =
                successState.data.find { it.type == com.auth0.android.ui_components.domain.model.AuthenticatorType.PUSH }
            assertThat(pushData).isNotNull()
            assertThat(pushData?.title).isEqualTo("Push Notification")
            assertThat(pushData?.confirmed).isFalse()

            val recoveryData =
                successState.data.find { it.type == com.auth0.android.ui_components.domain.model.AuthenticatorType.RECOVERY_CODE }
            assertThat(recoveryData).isNotNull()
            assertThat(recoveryData?.title).isEqualTo("Recovery Code")
            assertThat(recoveryData?.confirmed).isTrue()

            coVerify(exactly = 1) { getEnabledAuthenticatorMethodsUseCase() }
            job.cancel()
        }

    @Test
    fun `fetchAuthenticatorMethods - successful response with empty list - emits Success state with empty list`() =
        runTest {
            coEvery { getEnabledAuthenticatorMethodsUseCase() } returns Result.Success(emptyList())

            val job = backgroundScope.launch {
                viewModel.uiState.collect { /* Keep collecting */ }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(AuthenticatorUiState.Success::class.java)

            val successState = state as AuthenticatorUiState.Success
            assertThat(successState.data).isEmpty()

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

            val job = backgroundScope.launch {
                viewModel.uiState.collect { /* Keep collecting */ }
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
            // Given
            val tokenError = Auth0Error.RefreshTokenInvalid(
                message = "Invalid or expired refresh token",
                cause = Exception("Token validation failed")
            )
            coEvery { getEnabledAuthenticatorMethodsUseCase() } returns Result.Error(tokenError)

            // When - Call fetchAuthenticatorMethods directly

            val job = backgroundScope.launch {
                viewModel.uiState.collect { /* Keep collecting */ }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - State should be Error
            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(AuthenticatorUiState.Error::class.java)

            val errorState = state as AuthenticatorUiState.Error
            assertThat(errorState.error.error).isEqualTo(tokenError)
            assertThat(errorState.error.error.message).contains("Invalid or expired refresh token")
            assertThat(errorState.error.onRetry).isNotNull()

            // Verify use case was called once
            coVerify(exactly = 1) { getEnabledAuthenticatorMethodsUseCase() }
            job.cancel()
        }


    @Test
    fun `error state retry callback - invoked after error - triggers fetchAuthenticatorMethods again and can succeed`() =
        runTest {
            // Given - First call returns error, second call returns success
            val networkError = Auth0Error.NetworkError(
                message = "Connection failed",
                cause = Exception("Network timeout")
            )
            coEvery { getEnabledAuthenticatorMethodsUseCase() } returnsMany listOf(
                Result.Error(networkError),
                Result.Success(listOf(TestData.totpAuthenticatorMethod))
            )

            // When - First call triggers error
            val job = backgroundScope.launch {
                viewModel.uiState.collect { /* Keep collecting */ }
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
            assertThat(successState.data).hasSize(1)
            assertThat(successState.data[0].type).isEqualTo(com.auth0.android.ui_components.domain.model.AuthenticatorType.TOTP)

            coVerify(exactly = 2) { getEnabledAuthenticatorMethodsUseCase() }
            job.cancel()
        }


    @Test
    fun `fetchAuthenticatorMethods - called multiple times - always starts with Loading state`() =
        runTest {
            // Given - First call returns success
            coEvery { getEnabledAuthenticatorMethodsUseCase() } returnsMany listOf(
                Result.Success(
                    listOf(TestData.totpAuthenticatorMethod)
                ),
                Result.Success(
                    listOf(TestData.phoneAuthenticatorMethod)
                )
            )

            // When - First call
            val job = backgroundScope.launch {
                viewModel.uiState.collect { /* Keep collecting */ }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - State should be Success
            val firstState = viewModel.uiState.value
            assertThat(firstState).isInstanceOf(AuthenticatorUiState.Success::class.java)

            // Given - Second call will return different data
//            coEvery { getEnabledAuthenticatorMethodsUseCase() } returns Result.Success(
//                listOf(TestData.phoneAuthenticatorMethod)
//            )

            // When - Second call is made
            viewModel.fetchAuthenticatorMethods()

//             Then - State should immediately transition to Loading
            val loadingState = viewModel.uiState.value
            assertThat(loadingState).isInstanceOf(AuthenticatorUiState.Loading::class.java)

            // When - Coroutines complete
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - State should be Success with new data
            val finalState = viewModel.uiState.value
            assertThat(finalState).isInstanceOf(AuthenticatorUiState.Success::class.java)

            val successState = finalState as AuthenticatorUiState.Success
            assertThat(successState.data).hasSize(1)
            assertThat(successState.data[0].type).isEqualTo(com.auth0.android.ui_components.domain.model.AuthenticatorType.PHONE)

            // Verify use case was called twice (first call + second call)
            coVerify(exactly = 2) { getEnabledAuthenticatorMethodsUseCase() }
            job.cancel()
        }
}
