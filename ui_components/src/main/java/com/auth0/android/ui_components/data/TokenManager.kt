package com.auth0.android.ui_components.data

import android.util.Log
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.ui_components.Auth0UI
import java.io.IOException

/**
 * Manages token fetching operations
 * Throws exceptions on failure - let caller handle
 */
class TokenManager {
    
    companion object {
        private const val TAG = "TokenManager"
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
        return try {
            Log.d(TAG, "Fetching token for audience: $audience with scopes: $scope")
            val credentials = tokenProvider.fetchApiCredentials(audience, scope)
            credentials.accessToken
        } catch (e: AuthenticationException) {
            Log.e(TAG, "Authentication failed: ${e.getDescription()}", e)
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Network error fetching token", e)
            throw e
        }
    }
}