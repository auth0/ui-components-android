package com.auth0.universalcomponents.data.repository

import com.auth0.android.result.Credentials
import com.auth0.android.result.UserProfile
import com.auth0.universalcomponents.domain.error.Auth0Error
import com.auth0.universalcomponents.token.TokenProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class UserRepositoryImplTest {

    private lateinit var tokenProvider: TokenProvider
    private lateinit var repository: UserRepositoryImpl

    @Before
    fun setup() {
        tokenProvider = mockk()
        repository = UserRepositoryImpl(tokenProvider)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `getUserInfo - credentials with email and name - returns UserInfo with all fields`() =
        runTest {
            val mockCredentials = mockk<Credentials>()
            val mockUser = mockk<UserProfile>()
            coEvery { tokenProvider.fetchCredentials() } returns mockCredentials
            every { mockCredentials.user } returns mockUser
            every { mockUser.email } returns "user@example.com"
            every { mockUser.name } returns "John Doe"
            every { mockUser.pictureURL } returns "https://example.com/picture.jpg"

            val result = repository.getUserInfo()

            assertThat(result.email).isEqualTo("user@example.com")
            assertThat(result.name).isEqualTo("John Doe")
            assertThat(result.pictureUrl).isEqualTo("https://example.com/picture.jpg")
            coVerify(exactly = 1) { tokenProvider.fetchCredentials() }
        }

    @Test
    fun `getUserInfo - credentials with null fields - returns UserInfo with null fields`() =
        runTest {
            val mockCredentials = mockk<Credentials>()
            val mockUser = mockk<UserProfile>()
            coEvery { tokenProvider.fetchCredentials() } returns mockCredentials
            every { mockCredentials.user } returns mockUser
            every { mockUser.email } returns null
            every { mockUser.name } returns null
            every { mockUser.pictureURL } returns null

            val result = repository.getUserInfo()

            assertThat(result.email).isNull()
            assertThat(result.name).isNull()
            assertThat(result.pictureUrl).isNull()
        }

    @Test
    fun `getUserInfo - tokenProvider throws exception - propagates as Auth0Error`() = runTest {
        coEvery { tokenProvider.fetchCredentials() } throws RuntimeException("Credentials not available")

        var caughtError: Throwable? = null
        try {
            repository.getUserInfo()
        } catch (e: Auth0Error) {
            caughtError = e
        }

        assertThat(caughtError).isInstanceOf(Auth0Error::class.java)
        coVerify(exactly = 1) { tokenProvider.fetchCredentials() }
    }

    @Test
    fun `getUserInfo - maps email from UserProfile to UserInfo`() = runTest {
        val mockCredentials = mockk<Credentials>()
        val mockUser = mockk<UserProfile>()
        coEvery { tokenProvider.fetchCredentials() } returns mockCredentials
        every { mockCredentials.user } returns mockUser
        every { mockUser.email } returns "another@test.com"
        every { mockUser.name } returns "Jane Smith"
        every { mockUser.pictureURL } returns null

        val result = repository.getUserInfo()

        assertThat(result.email).isEqualTo("another@test.com")
        assertThat(result.name).isEqualTo("Jane Smith")
        assertThat(result.pictureUrl).isNull()
    }
}
