package com.auth0.android.ui_components.data.repository

import android.util.Log
import com.auth0.android.result.AuthenticationMethod
import com.auth0.android.result.Factor
import com.auth0.android.ui_components.data.MyAccountProvider
import com.auth0.android.ui_components.domain.repository.MyAccountRepository

/**
 * Repository that handles MyAccount API calls
 * Receives access token from caller - does NOT fetch tokens
 */
class MyAccountRepositoryImpl(private val myAccountProvider: MyAccountProvider) :
    MyAccountRepository {

    companion object {
        private const val TAG = "MyAccountRepository"
    }

    /**
     * Fetches factors using provided access token
     * @param accessToken Pre-fetched access token with required scopes
     * @throws Exception if API call fails
     */
    override suspend fun getFactors(accessToken: String): List<Factor> {
        Log.d(TAG, "Fetching factors")
        val client = myAccountProvider.getMyAccount(accessToken)
        return client.getFactors().await()
    }

    /**
     * Fetches authentication methods using provided access token
     * @param accessToken Pre-fetched access token with required scopes
     * @throws Exception if API call fails
     */
    override suspend fun getAuthenticatorMethods(accessToken: String): List<AuthenticationMethod> {
        Log.d(TAG, "Fetching authentication methods")
        val client = myAccountProvider.getMyAccount(accessToken)
        return client.getAuthenticationMethods().await()
    }

    /**
     * Deletes an authentication method
     *
     */
    override suspend fun deleteAuthenticationMethod(
        authenticationMethodId: String,
        accessToken: String
    ): Void? {
        Log.d(TAG, "Deleting authentication method $authenticationMethodId")
        val client = myAccountProvider.getMyAccount(accessToken)
        return client.deleteAuthenticationMethod(authenticationMethodId).await()
    }
}
