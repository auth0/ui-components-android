package com.auth0.universalcomponents.domain.usecase

import com.auth0.universalcomponents.domain.error.Auth0Error
import com.auth0.universalcomponents.domain.model.UserInfo
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.domain.repository.UserRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class GetUserInfoUseCaseTest {

    private lateinit var userRepository: UserRepository
    private lateinit var useCase: GetUserInfoUseCase

    @Before
    fun setup() {
        userRepository = mockk()
        useCase = GetUserInfoUseCase(userRepository = userRepository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `invoke - repository returns user info - returns Success with UserInfo`() = runTest {
        val expectedUserInfo = UserInfo(
            email = "user@example.com",
            name = "John Doe",
            pictureUrl = "https://example.com/picture.jpg"
        )
        coEvery { userRepository.getUserInfo() } returns expectedUserInfo

        val result = useCase.invoke()

        assertThat(result).isInstanceOf(Result.Success::class.java)
        val userInfo = (result as Result.Success).data
        assertThat(userInfo.email).isEqualTo("user@example.com")
        assertThat(userInfo.name).isEqualTo("John Doe")
        assertThat(userInfo.pictureUrl).isEqualTo("https://example.com/picture.jpg")

        coVerify(exactly = 1) { userRepository.getUserInfo() }
    }

    @Test
    fun `invoke - repository returns user info with null fields - returns Success with null fields`() =
        runTest {
            val userInfoWithNulls = UserInfo(email = null, name = null, pictureUrl = null)
            coEvery { userRepository.getUserInfo() } returns userInfoWithNulls

            val result = useCase.invoke()

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val userInfo = (result as Result.Success).data
            assertThat(userInfo.email).isNull()
            assertThat(userInfo.name).isNull()
            assertThat(userInfo.pictureUrl).isNull()
        }

    @Test
    fun `invoke - repository throws Auth0Error - returns Result Error with that error`() = runTest {
        val auth0Error = Auth0Error.NetworkError(
            message = "No network connection",
            cause = Exception("Connection refused")
        )
        coEvery { userRepository.getUserInfo() } throws auth0Error

        val result = useCase.invoke()

        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = (result as Result.Error).error
        assertThat(error).isEqualTo(auth0Error)
        assertThat(error.message).isEqualTo("No network connection")
        coVerify(exactly = 1) { userRepository.getUserInfo() }
    }

    @Test
    fun `invoke - repository throws ServerError - returns Result Error`() = runTest {
        val serverError = Auth0Error.ServerError(
            message = "Internal server error",
            statusCode = 500,
            cause = Exception("Server down")
        )
        coEvery { userRepository.getUserInfo() } throws serverError

        val result = useCase.invoke()

        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = (result as Result.Error).error
        assertThat(error).isEqualTo(serverError)
    }
}
