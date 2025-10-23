package com.auth0.android.ui_components.data

import android.util.Log
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.result.APICredentials
import com.auth0.android.ui_components.Auth0UI
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

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

    private val tokenMap: ConcurrentHashMap<String, ConcurrentHashMap<String, APICredentials>> =
        ConcurrentHashMap()

    /**
     * Gets the audience for MyAccount API
     */
    fun getMyAccountAudience(): String {
        return "https://${account.domain}/me/"
    }

    /**
     * Fetches token for given audience and scope
     * First checks the cache for a valid (non-expired) token
     * If not found or expired, fetches a new token and caches it
     * @throws AuthenticationException if auth fails
     * @throws IOException if network fails
     */
    suspend fun fetchToken(audience: String, scope: String): String {

        val scopeMap = tokenMap.getOrPut(audience) { ConcurrentHashMap() }

        val cachedCredentials = scopeMap[scope]
        if (cachedCredentials != null) {
            if (!willTokenExpire(cachedCredentials.expiresAt.time)) {
                Log.d(TAG, "Returning cached token for audience: $audience, scope: $scope")
                return cachedCredentials.accessToken
            }
        }

        Log.d(TAG, "Fetching new token from provider for audience: $audience, scope: $scope")
        val credentials = tokenProvider.fetchApiCredentials(audience, scope)

        // Saving the same token for scenario where we request multiple scopes together
        scopeMap[scope] = credentials
        val splitScope = scope.split(" ")
        if (splitScope.size > 1) {
            splitScope.forEach {
                Log.d(TAG, "token:$it ")
                scopeMap[it] = credentials
            }
        }

        return credentials.accessToken
    }

    suspend fun saveToken(audience: String, credentials: APICredentials) {
        tokenProvider.saveApiCredentials(audience, credentials)
    }

    private fun willTokenExpire(expiresAt: Long): Boolean {
        val currentTimeInMillis = System.currentTimeMillis()
        return expiresAt <= currentTimeInMillis
    }
}