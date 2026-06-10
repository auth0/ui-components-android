package com.auth0.universalcomponents.data

import android.text.TextUtils
import android.util.Base64
import com.auth0.android.Auth0
import com.auth0.android.util.Auth0UserAgent
import com.auth0.universalcomponents.Auth0UniversalComponents
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before
import org.junit.Test

class MyAccountProviderTest {

    private lateinit var provider: MyAccountProvider
    private lateinit var mockAccount: Auth0
    private lateinit var originalUserAgent: Auth0UserAgent

    @Before
    fun setup() {
        mockkStatic(TextUtils::class)
        mockkStatic(Base64::class)
        every { TextUtils.isEmpty(any()) } answers { (firstArg<CharSequence?>()).isNullOrEmpty() }
        every { Base64.encode(any(), any()) } returns "mocked-base64".toByteArray()

        mockkObject(Auth0UniversalComponents)
        mockAccount = mockk(relaxed = true)
        originalUserAgent = Auth0UserAgent("Auth0.Android", "3.14.0")

        every { mockAccount.auth0UserAgent } returns originalUserAgent
        every { mockAccount.networkingClient } returns mockk(relaxed = true)
        every { Auth0UniversalComponents.account } returns mockAccount

        provider = MyAccountProvider()
    }

    @After
    fun tearDown() {
        unmockkStatic(TextUtils::class)
        unmockkStatic(Base64::class)
        clearAllMocks()
    }

    @Test
    fun `getMyAccount - returns a non-null client`() {
        val client = provider.getMyAccount("test-access-token")
        assertThat(client).isNotNull()
    }

    @Test
    fun `getMyAccount - restores original user agent after client creation`() {
        val agents = mutableListOf<Auth0UserAgent>()
        every { mockAccount.auth0UserAgent = capture(agents) } answers { }

        provider.getMyAccount("test-access-token")

        assertThat(agents.last()).isEqualTo(originalUserAgent)
    }

    @Test
    fun `getMyAccount - sets custom user agent then restores original`() {
        val agents = mutableListOf<Auth0UserAgent>()
        every { mockAccount.auth0UserAgent = capture(agents) } answers { }

        provider.getMyAccount("test-access-token")

        assertThat(agents).hasSize(2)
        assertThat(agents[0]).isNotEqualTo(originalUserAgent)
        assertThat(agents[1]).isEqualTo(originalUserAgent)
    }

    @Test
    fun `getMyAccount - custom user agent has correct SDK name`() {
        val agents = mutableListOf<Auth0UserAgent>()
        every { mockAccount.auth0UserAgent = capture(agents) } answers { }

        provider.getMyAccount("test-access-token")

        assertThat(agents[0].name).isEqualTo("Auth0.UniversalComponents.Android")
    }
}
