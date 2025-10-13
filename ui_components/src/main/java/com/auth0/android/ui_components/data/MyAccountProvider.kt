package com.auth0.android.ui_components.data

import com.auth0.android.myaccount.MyAccountAPIClient
import com.auth0.android.ui_components.Auth0UI

/**
 * Provider class that creates and provides instances of MyAccount from the Auth0 Android SDK.
 * This class handles fetching the access token via TokenManager and initializing MyAccount.
 */
class MyAccountProvider(
) {

    /**
     * Creates and returns a MyAccountAPIClient instance configured with the current access token.
     *
     * @return MyAccountAPIClient instance ready to make API calls
     * @throws IllegalStateException if Auth0UI is not initialized
     */
    suspend fun getMyAccount(accessToken: String): MyAccountAPIClient {
        val account = Auth0UI.account
        return MyAccountAPIClient(account, accessToken)
    }
}
