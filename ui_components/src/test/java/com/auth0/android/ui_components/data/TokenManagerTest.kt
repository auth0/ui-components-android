package com.auth0.android.ui_components.data

import com.auth0.android.Auth0
import com.auth0.android.ui_components.Auth0UI
import com.auth0.android.ui_components.TestData
import com.auth0.android.ui_components.token.TokenProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TokenManagerTest {

    private lateinit var tokenManager: TokenManager
    private lateinit var mockTokenProvider: TokenProvider
    private lateinit var mockAccount: Auth0

    private val testAudience = "https://test.auth0.com/api/"
    private val testScope = "read:test write:test"

    @Before
    fun setup() {

        mockkObject(Auth0UI)

        mockTokenProvider = mockk()
        mockAccount = mockk()

        every { Auth0UI.tokenProvider } returns mockTokenProvider
        every { Auth0UI.account } returns mockAccount
        every { mockAccount.domain } returns "test.auth0.com"

        tokenManager = TokenManager.getInstance()
    }

    @After
    fun tearDown() {
        TokenManager.setInstance(null)
        clearAllMocks()
    }

    @Test
    fun `getInstance - called multiple times - returns same instance`() {
        val instance1 = TokenManager.getInstance()
        val instance2 = TokenManager.getInstance()
        assertThat(instance1).isSameInstanceAs(instance2)
    }


    @Test
    fun `getMyAccountAudience - returns correct audience format`() {
        val audience = tokenManager.getMyAccountAudience()
        assertThat(audience).isEqualTo("https://test.auth0.com/me/")
    }

    @Test
    fun `getMyAccountAudience - different domain - returns correct audience`() {
        every { mockAccount.domain } returns "example.auth0.com"
        val audience = tokenManager.getMyAccountAudience()
        assertThat(audience).isEqualTo("https://example.auth0.com/me/")
    }


    @Test
    fun `fetchToken - no cached token - fetches from provider and returns access token`() =
        runTest {
            coEvery {
                mockTokenProvider.fetchApiCredentials(testAudience, testScope)
            } returns TestData.validApiCredentials

            val token = tokenManager.fetchToken(testAudience, testScope)

            assertThat(token).isEqualTo("valid_access_token_123")
            coVerify(exactly = 1) {
                mockTokenProvider.fetchApiCredentials(testAudience, testScope)
            }
        }

    @Test
    fun `fetchToken - valid cached token exists - returns cached token without fetching`() =
        runTest {
            coEvery {
                mockTokenProvider.fetchApiCredentials(testAudience, testScope)
            } returns TestData.validApiCredentials

            //Fetching the first time
            tokenManager.fetchToken(testAudience, testScope)

            //Fetching token the second time
            val token = tokenManager.fetchToken(testAudience, testScope)

            assertThat(token).isEqualTo("valid_access_token_123")
            coVerify(exactly = 1) {
                mockTokenProvider.fetchApiCredentials(testAudience, testScope)
            }
        }

    @Test
    fun `fetchToken - expired cached token - fetches new token from provider`() = runTest {
        coEvery {
            mockTokenProvider.fetchApiCredentials(testAudience, testScope)
        } returnsMany listOf(
            TestData.goingToExpireApiCredentials,
            TestData.validApiCredentials
        )

        tokenManager.fetchToken(testAudience, testScope)

        advanceTimeBy(2000) // Simulate time passing to expire the token

        val token = tokenManager.fetchToken(testAudience, testScope)

        assertThat(token).isEqualTo("valid_access_token_123")
        coVerify(exactly = 2) {
            mockTokenProvider.fetchApiCredentials(testAudience, testScope)
        }
    }

    @Test
    fun `fetchToken - different audiences - caches tokens separately`() = runTest {
        val audience1 = "https://api1.auth0.com/"
        val audience2 = "https://api2.auth0.com/"

        coEvery {
            mockTokenProvider.fetchApiCredentials(audience1, testScope)
        } returns TestData.validApiCredentials

        coEvery {
            mockTokenProvider.fetchApiCredentials(audience2, testScope)
        } returns TestData.anotherValidApiCredentials

        val token1 = tokenManager.fetchToken(audience1, testScope)
        val token2 = tokenManager.fetchToken(audience2, testScope)

        assertThat(token1).isEqualTo("valid_access_token_123")
        assertThat(token2).isEqualTo("another_valid_token_789")

        coVerify(exactly = 1) { mockTokenProvider.fetchApiCredentials(audience1, testScope) }
        coVerify(exactly = 1) { mockTokenProvider.fetchApiCredentials(audience2, testScope) }
    }

    @Test
    fun `fetchToken - different scopes same audience - caches tokens separately`() = runTest {
        val scope1 = "read:users"
        val scope2 = "write:users"

        coEvery {
            mockTokenProvider.fetchApiCredentials(testAudience, scope1)
        } returns TestData.validApiCredentials

        coEvery {
            mockTokenProvider.fetchApiCredentials(testAudience, scope2)
        } returns TestData.anotherValidApiCredentials

        val token1 = tokenManager.fetchToken(testAudience, scope1)
        val token2 = tokenManager.fetchToken(testAudience, scope2)

        assertThat(token1).isEqualTo("valid_access_token_123")
        assertThat(token2).isEqualTo("another_valid_token_789")

        coVerify(exactly = 1) { mockTokenProvider.fetchApiCredentials(testAudience, scope1) }
        coVerify(exactly = 1) { mockTokenProvider.fetchApiCredentials(testAudience, scope2) }
    }


    @Test
    fun `saveToken - saves token to cache - subsequent fetch returns cached token`() = runTest {
        tokenManager.saveToken(testAudience, testScope, TestData.validApiCredentials)

        val token = tokenManager.fetchToken(testAudience, testScope)

        assertThat(token).isEqualTo("valid_access_token_123")

        coVerify(exactly = 0) {
            mockTokenProvider.fetchApiCredentials(any(), any())
        }
    }

    @Test
    fun `saveToken - saves expired token - fetchToken fetches new token`() = runTest {
        tokenManager.saveToken(testAudience, testScope, TestData.expiredApiCredentials)

        coEvery {
            mockTokenProvider.fetchApiCredentials(testAudience, testScope)
        } returns TestData.validApiCredentials

        val token = tokenManager.fetchToken(testAudience, testScope)

        assertThat(token).isEqualTo("valid_access_token_123")
        coVerify(exactly = 1) {
            mockTokenProvider.fetchApiCredentials(testAudience, testScope)
        }
    }

    @Test
    fun `saveToken - multi-scope token - saves token for each individual scope`() = runTest {
        val multiScope = "read:users write:users delete:users"
        tokenManager.saveToken(testAudience, multiScope, TestData.multiScopeApiCredentials)

        val tokenForRead = tokenManager.fetchToken(testAudience, "read:users")
        val tokenForWrite = tokenManager.fetchToken(testAudience, "write:users")
        val tokenForDelete = tokenManager.fetchToken(testAudience, "delete:users")

        assertThat(tokenForRead).isEqualTo("multi_scope_token_abc")
        assertThat(tokenForWrite).isEqualTo("multi_scope_token_abc")
        assertThat(tokenForDelete).isEqualTo("multi_scope_token_abc")

        coVerify(exactly = 0) {
            mockTokenProvider.fetchApiCredentials(any(), any())
        }
    }

    @Test
    fun `fetchToken - multi-scope response - caches for each individual scope`() = runTest {
        val multiScope = "read:data write:data"
        coEvery {
            mockTokenProvider.fetchApiCredentials(testAudience, multiScope)
        } returns TestData.multiScopeApiCredentials

        tokenManager.fetchToken(testAudience, multiScope)

        val tokenForRead = tokenManager.fetchToken(testAudience, "read:data")
        val tokenForWrite = tokenManager.fetchToken(testAudience, "write:data")

        assertThat(tokenForRead).isEqualTo("multi_scope_token_abc")
        assertThat(tokenForWrite).isEqualTo("multi_scope_token_abc")

        coVerify(exactly = 1) {
            mockTokenProvider.fetchApiCredentials(testAudience, multiScope)
        }
    }


    @Test
    fun `fetchToken - token expires at boundary - treats as expired and fetches new token`() =
        runTest {
            val boundaryCredentials = TestData.goingToExpireApiCredentials

            coEvery {
                mockTokenProvider.fetchApiCredentials(testAudience, testScope)
            } returnsMany listOf(boundaryCredentials, TestData.validApiCredentials)

            tokenManager.fetchToken(testAudience, testScope)

            val token = tokenManager.fetchToken(testAudience, testScope)

            assertThat(token).isEqualTo("valid_access_token_123")
            coVerify(exactly = 2) {
                mockTokenProvider.fetchApiCredentials(testAudience, testScope)
            }
        }


    @Test
    fun `fetchToken - concurrent requests for same token - handles thread-safe access`() = runTest {
        coEvery {
            mockTokenProvider.fetchApiCredentials(testAudience, testScope)
        } returns TestData.validApiCredentials

        val tokens = (1..10).map {
            async {
                tokenManager.fetchToken(testAudience, testScope)
            }
        }.awaitAll()

        coVerify(exactly = 1) {
            mockTokenProvider.fetchApiCredentials(testAudience, testScope)
        }

        tokens.forEach { token ->
            assertThat(token).isEqualTo("valid_access_token_123")
        }

        assertThat(tokens).hasSize(10)
        assertThat(tokens.distinct()).hasSize(1)
    }


    @Test
    fun `fetchToken - empty scope - fetches and caches token`() = runTest {
        val emptyScope = ""
        coEvery {
            mockTokenProvider.fetchApiCredentials(testAudience, emptyScope)
        } returns TestData.validApiCredentials

        val token = tokenManager.fetchToken(testAudience, emptyScope)

        assertThat(token).isEqualTo("valid_access_token_123")
        coVerify(exactly = 1) {
            mockTokenProvider.fetchApiCredentials(testAudience, emptyScope)
        }
    }


    @Test
    fun `saveToken - overwrites existing cached token - uses new token`() = runTest {
        tokenManager.saveToken(testAudience, testScope, TestData.validApiCredentials)

        tokenManager.saveToken(testAudience, testScope, TestData.anotherValidApiCredentials)

        val token = tokenManager.fetchToken(testAudience, testScope)

        assertThat(token).isEqualTo("another_valid_token_789")

        coVerify(exactly = 0) {
            mockTokenProvider.fetchApiCredentials(any(), any())
        }
    }

    @Test
    fun `fetchToken - provider throws exception - exception propagates to caller`() {
        val exception = RuntimeException("Network error")
        coEvery {
            mockTokenProvider.fetchApiCredentials(testAudience, testScope)
        } throws exception

        val error = Assert.assertThrows(RuntimeException::class.java) {
            runTest {
                tokenManager.fetchToken(testAudience, testScope)
            }
        }

        assertThat(error.message).isEqualTo("Network error")
        coVerify(exactly = 1) {
            mockTokenProvider.fetchApiCredentials(testAudience, testScope)
        }
    }


    @Test
    fun `fetchToken - multiple audiences and scopes - maintains separate cache entries`() =
        runTest {
            val audience1 = "https://api1.com/"
            val audience2 = "https://api2.com/"
            val scope1 = "read:data"
            val scope2 = "write:data"

            val credentials1 = TestData.validApiCredentials.copy(accessToken = "token1")
            val credentials2 = TestData.validApiCredentials.copy(accessToken = "token2")
            val credentials3 = TestData.validApiCredentials.copy(accessToken = "token3")
            val credentials4 = TestData.validApiCredentials.copy(accessToken = "token4")

            coEvery {
                mockTokenProvider.fetchApiCredentials(
                    audience1,
                    scope1
                )
            } returns credentials1
            coEvery {
                mockTokenProvider.fetchApiCredentials(
                    audience1,
                    scope2
                )
            } returns credentials2
            coEvery {
                mockTokenProvider.fetchApiCredentials(
                    audience2,
                    scope1
                )
            } returns credentials3
            coEvery {
                mockTokenProvider.fetchApiCredentials(
                    audience2,
                    scope2
                )
            } returns credentials4

            val token1 = tokenManager.fetchToken(audience1, scope1)
            val token2 = tokenManager.fetchToken(audience1, scope2)
            val token3 = tokenManager.fetchToken(audience2, scope1)
            val token4 = tokenManager.fetchToken(audience2, scope2)

            assertThat(token1).isEqualTo("token1")
            assertThat(token2).isEqualTo("token2")
            assertThat(token3).isEqualTo("token3")
            assertThat(token4).isEqualTo("token4")

            coVerify(exactly = 1) { mockTokenProvider.fetchApiCredentials(audience1, scope1) }
            coVerify(exactly = 1) { mockTokenProvider.fetchApiCredentials(audience1, scope2) }
            coVerify(exactly = 1) { mockTokenProvider.fetchApiCredentials(audience2, scope1) }
            coVerify(exactly = 1) { mockTokenProvider.fetchApiCredentials(audience2, scope2) }
        }
}
