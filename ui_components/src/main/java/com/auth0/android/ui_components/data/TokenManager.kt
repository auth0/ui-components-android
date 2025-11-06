package com.auth0.android.ui_components.data

import android.util.Log
import com.auth0.android.result.APICredentials
import com.auth0.android.ui_components.Auth0UI
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages token fetching operations as a thread-safe singleton
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
        saveToScopeCache(scopeMap, scope, credentials)
        return credentials.accessToken
    }

    fun saveToken(audience: String, scope: String, credentials: APICredentials) {
        val scopeMap = tokenMap.getOrPut(audience) { ConcurrentHashMap() }
        saveToScopeCache(scopeMap, scope, credentials)
    }


    private fun saveToScopeCache(
        scopeMap: ConcurrentHashMap<String, APICredentials>,
        scope: String,
        credentials: APICredentials
    ) {
        scopeMap[scope] = credentials
        val splitScope = scope.split(" ")
        if (splitScope.size > 1) {
            splitScope.forEach {
                Log.d(TAG, "token:$it ")
                scopeMap[it] = credentials
            }
        }
    }

    private fun willTokenExpire(expiresAt: Long): Boolean {
        val currentTimeInMillis = System.currentTimeMillis()
        return expiresAt <= currentTimeInMillis
    }
}