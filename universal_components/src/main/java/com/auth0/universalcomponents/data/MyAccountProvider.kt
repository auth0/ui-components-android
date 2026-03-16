package com.auth0.universalcomponents.data

import com.auth0.android.myaccount.MyAccountAPIClient
import com.auth0.universalcomponents.Auth0UniversalComponents

/**
 * Provider class that creates and provides instances of MyAccount from the Auth0 Android SDK.
 */
class MyAccountProvider(
) {

    /**
     * Creates and returns a MyAccountAPIClient instance configured with the current access token.
     *
     * @return MyAccountAPIClient instance ready to make API calls
     * @throws IllegalStateException if Auth0UniversalComponents is not initialized
     */
    fun getMyAccount(accessToken: String): MyAccountAPIClient {
        val account = Auth0UniversalComponents.account
        return MyAccountAPIClient(account, accessToken)
    }
}
