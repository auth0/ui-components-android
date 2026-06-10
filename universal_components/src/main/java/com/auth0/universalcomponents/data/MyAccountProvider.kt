package com.auth0.universalcomponents.data

import com.auth0.android.myaccount.MyAccountAPIClient
import com.auth0.android.util.Auth0UserAgent
import com.auth0.universalcomponents.Auth0UniversalComponents
import com.auth0.universalcomponents.BuildConfig

/**
 * Provider class that creates and provides instances of MyAccount from the Auth0 Android SDK.
 */
class MyAccountProvider() {

    /**
     * Creates and returns a MyAccountAPIClient instance configured with the current access token.
     *
     * @return MyAccountAPIClient instance ready to make API calls
     * @throws IllegalStateException if Auth0UniversalComponents is not initialized
     */
    fun getMyAccount(accessToken: String): MyAccountAPIClient {
        val account = Auth0UniversalComponents.account
        val original = account.auth0UserAgent
        account.auth0UserAgent = Auth0UserAgent(
            SDK_NAME,
            BuildConfig.VERSION_NAME,
            original.version
        )
        val client = MyAccountAPIClient(account, accessToken)
        account.auth0UserAgent = original
        return client
    }

    private companion object {
        private const val SDK_NAME = "Auth0.UniversalComponents.Android"
    }
}
