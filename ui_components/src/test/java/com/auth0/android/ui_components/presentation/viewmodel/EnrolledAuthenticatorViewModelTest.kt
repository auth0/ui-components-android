package com.auth0.android.ui_components.presentation.viewmodel

import com.auth0.android.ui_components.TestData
import com.auth0.android.ui_components.domain.error.Auth0Error
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.network.Result
import com.auth0.android.ui_components.domain.usecase.DeleteAuthenticationMethodUseCase
import com.auth0.android.ui_components.domain.usecase.GetEnrolledAuthenticatorsUseCase
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
class EnrolledAuthenticatorViewModelTest {

    private lateinit var getEnrolledAuthenticatorsUseCase: GetEnrolledAuthenticatorsUseCase
    private lateinit var deleteAuthenticationMethodUseCase: DeleteAuthenticationMethodUseCase

    private lateinit var viewModel: EnrolledAuthenticatorViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val testAuthenticatorType = AuthenticatorType.PHONE

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        getEnrolledAuthenticatorsUseCase = mockk()
        deleteAuthenticationMethodUseCase = mockk()

        viewModel = EnrolledAuthenticatorViewModel(
            getEnrolledAuthenticatorsUseCase = getEnrolledAuthenticatorsUseCase,
            deleteAuthenticationMethodUseCase = deleteAuthenticationMethodUseCase,
            authenticatorType = testAuthenticatorType
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }


    @Test
    fun `initialization - uiState collected - starts with default state and automatically fetches enrolled authenticators`() =
        runTest {
            coEvery { getEnrolledAuthenticatorsUseCase(any()) } returns Result.Success(
                TestData.allEnrolledMethods
            )

            val job = launch {
                viewModel.uiState.collect { }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) { getEnrolledAuthenticatorsUseCase(testAuthenticatorType) }

            val finalState = viewModel.uiState.value
            assertThat(finalState.loading).isFalse()
            assertThat(finalState.authenticators).hasSize(3)
            assertThat(finalState.uiError).isNull()

            job.cancel()
        }

    @Test
    fun `fetchEnrolledAuthenticators - successful response with multiple authenticators - emits success state`() =
        runTest {
            coEvery { getEnrolledAuthenticatorsUseCase(testAuthenticatorType) } returns Result.Success(
                TestData.allEnrolledMethods
            )
            val job = launch {
                viewModel.uiState.collect { }
            }

            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.loading).isFalse()
            assertThat(state.authenticators).hasSize(3)
            assertThat(state.authenticators).containsExactly(
                TestData.enrolledPhoneMethod,
                TestData.enrolledTotpMethod,
                TestData.enrolledEmailMethod
            )
            assertThat(state.uiError).isNull()

            coVerify(exactly = 1) { getEnrolledAuthenticatorsUseCase(testAuthenticatorType) }
            job.cancel()
        }

    @Test
    fun `fetchEnrolledAuthenticators - successful response with empty list - emits success state with empty list`() =
        runTest {
            coEvery { getEnrolledAuthenticatorsUseCase(testAuthenticatorType) } returns Result.Success(
                emptyList()
            )

            val job = launch {
                viewModel.uiState.collect { }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.loading).isFalse()
            assertThat(state.authenticators).isEmpty()
            assertThat(state.uiError).isNull()

            coVerify(exactly = 1) { getEnrolledAuthenticatorsUseCase(testAuthenticatorType) }
            job.cancel()
        }

    @Test
    fun `fetchEnrolledAuthenticators - invalid token error - emits error state with retry callback`() =
        runTest {
            val tokenError = Auth0Error.RefreshTokenInvalid(
                message = "Invalid or expired refresh token",
                cause = Exception("Token validation failed")
            )
            coEvery { getEnrolledAuthenticatorsUseCase(testAuthenticatorType) } returns Result.Error(
                tokenError
            )

            val job = launch {
                viewModel.uiState.collect { }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.loading).isFalse()
            assertThat(state.uiError).isNotNull()
            assertThat(state.uiError?.error).isEqualTo(tokenError)
            assertThat(state.uiError?.error?.message).contains("Invalid or expired refresh token")
            assertThat(state.uiError?.onRetry).isNotNull()

            coVerify(exactly = 1) { getEnrolledAuthenticatorsUseCase(testAuthenticatorType) }
            job.cancel()
        }

    @Test
    fun `fetchEnrolledAuthenticators - called multiple times - always starts with loading state`() =
        runTest {
            coEvery { getEnrolledAuthenticatorsUseCase(testAuthenticatorType) } coAnswers {
                delay(10)
                Result.Success(listOf(TestData.enrolledPhoneMethod))
            }

            val states = mutableListOf<EnrolledUiState>()
            val job = launch {
                viewModel.uiState.collect { state ->
                    states.add(state)
                }
            }

            testDispatcher.scheduler.advanceUntilIdle()

            val firstState = viewModel.uiState.value
            assertThat(firstState.loading).isFalse()
            assertThat(firstState.authenticators).hasSize(1)
            assertThat(firstState.authenticators[0]).isEqualTo(TestData.enrolledPhoneMethod)

            states.clear()

            coEvery { getEnrolledAuthenticatorsUseCase(testAuthenticatorType) } coAnswers {
                delay(10)
                Result.Success(listOf(TestData.enrolledTotpMethod))
            }
            viewModel.fetchEnrolledAuthenticators(testAuthenticatorType)
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(states.size).isAtLeast(2)
            assertThat(states[0].loading).isTrue()

            val lastState = states.last()
            assertThat(lastState.loading).isFalse()
            assertThat(lastState.authenticators).hasSize(1)
            assertThat(lastState.authenticators[0]).isEqualTo(TestData.enrolledTotpMethod)

            coVerify(exactly = 2) { getEnrolledAuthenticatorsUseCase(testAuthenticatorType) }

            job.cancel()
        }

    @Test
    fun `deleteAuthenticationMethod - valid ID - successfully deletes and updates authenticators list`() =
        runTest {
            coEvery { getEnrolledAuthenticatorsUseCase(testAuthenticatorType) } returns Result.Success(
                TestData.allEnrolledMethods
            )
            val job = launch {
                viewModel.uiState.collect { }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            val initialState = viewModel.uiState.value
            assertThat(initialState.authenticators).hasSize(3)

            // Mock successful delete
            coEvery { deleteAuthenticationMethodUseCase(any()) } returns Result.Success(Unit)

            val idToDelete = TestData.enrolledPhoneMethod.id
            viewModel.deleteAuthenticationMethod(idToDelete)
            testDispatcher.scheduler.advanceUntilIdle()

            val finalState = viewModel.uiState.value
            assertThat(finalState.loading).isFalse()
            assertThat(finalState.authenticators).hasSize(2)
            assertThat(finalState.authenticators).doesNotContain(TestData.enrolledPhoneMethod)
            assertThat(finalState.authenticators).containsExactly(
                TestData.enrolledTotpMethod,
                TestData.enrolledEmailMethod
            )
            assertThat(finalState.uiError).isNull()

            coVerify(exactly = 1) { deleteAuthenticationMethodUseCase(idToDelete) }
            job.cancel()
        }

    @Test
    fun `deleteAuthenticationMethod - last item in list - results in empty list`() =
        runTest {
            coEvery { getEnrolledAuthenticatorsUseCase(testAuthenticatorType) } returns Result.Success(
                listOf(TestData.enrolledPhoneMethod)
            )
            val job = launch {
                viewModel.uiState.collect { }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(viewModel.uiState.value.authenticators).hasSize(1)

            coEvery { deleteAuthenticationMethodUseCase(any()) } returns Result.Success(Unit)

            viewModel.deleteAuthenticationMethod(TestData.enrolledPhoneMethod.id)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.loading).isFalse()
            assertThat(state.authenticators).isEmpty()
            assertThat(state.uiError).isNull()

            coVerify(exactly = 1) { deleteAuthenticationMethodUseCase(TestData.enrolledPhoneMethod.id) }
            job.cancel()
        }

    @Test
    fun `deleteAuthenticationMethod - network error - emits error and does NOT update cache`() =
        runTest {
            coEvery { getEnrolledAuthenticatorsUseCase(testAuthenticatorType) } returns Result.Success(
                TestData.allEnrolledMethods
            )
            val job = launch {
                viewModel.uiState.collect { }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            val beforeDeleteState = viewModel.uiState.value
            assertThat(beforeDeleteState.authenticators).hasSize(3)

            val networkError = Auth0Error.NetworkError(
                message = "Delete failed - network error",
                cause = Exception("Connection timeout")
            )
            coEvery { deleteAuthenticationMethodUseCase(any()) } returns Result.Error(networkError)

            viewModel.deleteAuthenticationMethod(TestData.enrolledPhoneMethod.id)
            testDispatcher.scheduler.advanceUntilIdle()

            val afterDeleteState = viewModel.uiState.value
            assertThat(afterDeleteState.loading).isFalse()
            assertThat(afterDeleteState.authenticators).hasSize(3)
            assertThat(afterDeleteState.authenticators).containsExactly(
                TestData.enrolledPhoneMethod,
                TestData.enrolledTotpMethod,
                TestData.enrolledEmailMethod
            )
            assertThat(afterDeleteState.uiError).isNotNull()
            assertThat(afterDeleteState.uiError?.error).isEqualTo(networkError)
            assertThat(afterDeleteState.uiError?.onRetry).isNotNull()

            coVerify(exactly = 1) { deleteAuthenticationMethodUseCase(TestData.enrolledPhoneMethod.id) }
            job.cancel()
        }

    @Test
    fun `deleteAuthenticationMethod - API error - emits error state with retry callback`() =
        runTest {
            coEvery { getEnrolledAuthenticatorsUseCase(testAuthenticatorType) } returns Result.Success(
                TestData.allEnrolledMethods
            )
            val job = launch {
                viewModel.uiState.collect { }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            val unauthorizedError = Auth0Error.ServerError(
                message = "API request failed",
                statusCode = 500,
                cause = Exception("Internal server error")
            )
            coEvery { deleteAuthenticationMethodUseCase(any()) } returns Result.Error(
                unauthorizedError
            )

            viewModel.deleteAuthenticationMethod(TestData.enrolledPhoneMethod.id)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.loading).isFalse()
            assertThat(state.uiError).isNotNull()
            assertThat(state.uiError?.error).isEqualTo(unauthorizedError)
            assertThat(state.uiError?.error?.message).isEqualTo("API request failed")
            assertThat(state.uiError?.onRetry).isNotNull()

            coVerify(exactly = 1) { deleteAuthenticationMethodUseCase(TestData.enrolledPhoneMethod.id) }
            job.cancel()
        }


    @Test
    fun `error state retry callback for fetch - invoked after error - triggers fetchEnrolledAuthenticators again and can succeed`() =
        runTest {
            val networkError = Auth0Error.NetworkError(
                message = "Connection failed",
                cause = Exception("Network timeout")
            )
            coEvery { getEnrolledAuthenticatorsUseCase(testAuthenticatorType) } returnsMany listOf(
                Result.Error(networkError),
                Result.Success(listOf(TestData.enrolledPhoneMethod))
            )

            val job = launch {
                viewModel.uiState.collect { }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            val errorState = viewModel.uiState.value
            assertThat(errorState.uiError).isNotNull()

            val retryCallback = errorState.uiError!!.onRetry
            retryCallback.invoke()
            testDispatcher.scheduler.advanceUntilIdle()

            val finalState = viewModel.uiState.value
            assertThat(finalState.loading).isFalse()
            assertThat(finalState.authenticators).hasSize(1)
            assertThat(finalState.authenticators[0]).isEqualTo(TestData.enrolledPhoneMethod)
            assertThat(finalState.uiError).isNull()

            coVerify(exactly = 2) { getEnrolledAuthenticatorsUseCase(testAuthenticatorType) }

            job.cancel()
        }

    @Test
    fun `error state retry callback for delete - invoked after delete error - triggers deleteAuthenticationMethod again and can succeed`() =
        runTest {
            coEvery { getEnrolledAuthenticatorsUseCase(testAuthenticatorType) } returns Result.Success(
                TestData.allEnrolledMethods
            )
            val job = launch {
                viewModel.uiState.collect { }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            val deleteError = Auth0Error.NetworkError(
                message = "Delete failed",
                cause = Exception("Network error")
            )
            coEvery { deleteAuthenticationMethodUseCase(any()) } returnsMany listOf(
                Result.Error(deleteError),
                Result.Success(Unit)
            )

            val idToDelete = TestData.enrolledPhoneMethod.id
            viewModel.deleteAuthenticationMethod(idToDelete)
            testDispatcher.scheduler.advanceUntilIdle()

            val errorState = viewModel.uiState.value
            assertThat(errorState.uiError).isNotNull()
            assertThat(errorState.authenticators).hasSize(3) // Unchanged

            val retryCallback = errorState.uiError!!.onRetry
            retryCallback.invoke()
            testDispatcher.scheduler.advanceUntilIdle()

            val finalState = viewModel.uiState.value
            assertThat(finalState.loading).isFalse()
            assertThat(finalState.authenticators).hasSize(2)
            assertThat(finalState.authenticators).doesNotContain(TestData.enrolledPhoneMethod)
            assertThat(finalState.uiError).isNull()

            coVerify(exactly = 2) { deleteAuthenticationMethodUseCase(idToDelete) }
            job.cancel()
        }
}
