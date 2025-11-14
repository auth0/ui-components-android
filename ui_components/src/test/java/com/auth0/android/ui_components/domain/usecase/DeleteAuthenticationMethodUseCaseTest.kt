package com.auth0.android.ui_components.domain.usecase

import com.auth0.android.ui_components.domain.error.Auth0Error
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
class DeleteAuthenticationMethodUseCaseTest {

    private lateinit var repository: MyAccountRepository
    private lateinit var useCase: DeleteAuthenticationMethodUseCase

    private val testDispatcher = StandardTestDispatcher()
    private val dispatcherProvider = TestDispatcherProvider(testDispatcher)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        repository = mockk()
        useCase = DeleteAuthenticationMethodUseCase(
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
    fun `invoke - valid authentication method ID - returns Success with Unit`() = runTest {
        val authMethodId = "auth_method_123"
        coEvery {
            repository.deleteAuthenticationMethod(
                authMethodId,
                "delete:me:authentication_methods"
            )
        } returns null

        val result = useCase.invoke(authMethodId)

        Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
        Truth.assertThat((result as Result.Success).data).isEqualTo(Unit)

        coVerify(exactly = 1) {
            repository.deleteAuthenticationMethod(
                authMethodId,
                "delete:me:authentication_methods"
            )
        }
    }

    @Test
    fun `invoke - deletion completes successfully - verifies correct scopes passed`() = runTest {
        val authMethodId = "auth_method_456"
        val expectedScope = "delete:me:authentication_methods"
        coEvery {
            repository.deleteAuthenticationMethod(authMethodId, expectedScope)
        } returns null

        val result = useCase.invoke(authMethodId)

        Truth.assertThat(result).isInstanceOf(Result.Success::class.java)
        coVerify(exactly = 1) {
            repository.deleteAuthenticationMethod(authMethodId, expectedScope)
        }
    }

    @Test
    fun `invoke - repository throws AccessDenied error - returns Error with Auth0Error`() =
        runTest {
            // Given
            val authMethodId = "auth_method_789"
            val expectedError = Auth0Error.AccessDenied(
                message = "Access denied to delete authentication method",
                cause = RuntimeException("Forbidden")
            )
            coEvery {
                repository.deleteAuthenticationMethod(
                    authMethodId,
                    "delete:me:authentication_methods"
                )
            } throws expectedError

            val result = useCase.invoke(authMethodId)

            Truth.assertThat(result).isInstanceOf(Result.Error::class.java)
            val error = (result as Result.Error).error
            Truth.assertThat(error).isInstanceOf(Auth0Error.AccessDenied::class.java)
            Truth.assertThat(error.message)
                .isEqualTo("Access denied to delete authentication method")

            coVerify(exactly = 1) {
                repository.deleteAuthenticationMethod(
                    authMethodId,
                    "delete:me:authentication_methods"
                )
            }
        }

    @Test
    fun `invoke - repository throws NetworkError - returns Error with Auth0Error`() = runTest {
        val authMethodId = "auth_method_network_fail"
        val expectedError = Auth0Error.NetworkError(
            message = "Network connection failed",
            cause = RuntimeException("No internet")
        )
        coEvery {
            repository.deleteAuthenticationMethod(
                authMethodId,
                "delete:me:authentication_methods"
            )
        } throws expectedError

        val result = useCase.invoke(authMethodId)

        Truth.assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = (result as Result.Error).error
        Truth.assertThat(error).isInstanceOf(Auth0Error.NetworkError::class.java)
        Truth.assertThat(error.message).contains("Network connection failed")

        coVerify(exactly = 1) {
            repository.deleteAuthenticationMethod(
                authMethodId,
                "delete:me:authentication_methods"
            )
        }
    }
}