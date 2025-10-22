package com.auth0.android.ui_components.data

import android.util.Log
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.result.APICredentials
import com.auth0.android.ui_components.Auth0UI
import com.auth0.android.ui_components.domain.error.Auth0Error
import com.auth0.android.ui_components.domain.error.ErrorMapper
import com.auth0.android.ui_components.domain.network.Result
import java.io.IOException

/**
 * Manages token fetching operations as a thread-safe singleton
 * Uses double-checked locking pattern for optimal performance in multi-threaded scenarios
 * Throws exceptions on failure - let caller handle
 */
class TokenManager private constructor() {

    companion object {
        private const val TAG = "TokenManager"

        @Volatile
        private var instance: TokenManager? = null

        @JvmStatic
        fun getInstance(): TokenManager {
            return instance ?: synchronized(this) {
                instance ?: TokenManager().also { instance = it }
            }
        }
    }

    private val tokenProvider = Auth0UI.tokenProvider
    private val account = Auth0UI.account

    /**
     * Gets the audience for MyAccount API
     */
    fun getMyAccountAudience(): String {
        return "https://${account.domain}/me/"
    }

    /**
     * Fetches token for given audience and scope
     * @throws AuthenticationException if auth fails
     * @throws IOException if network fails
     */
    suspend fun fetchToken(audience: String, scope: String): String {
        Log.d(TAG, "Fetching token for audience: $audience with scopes: $scope")
        val credentials = tokenProvider.fetchApiCredentials(audience, scope)
        return credentials.accessToken
    }

    suspend fun saveToken(audience: String, credentials: APICredentials) {
        tokenProvider.saveApiCredentials(audience, credentials)
    }
}