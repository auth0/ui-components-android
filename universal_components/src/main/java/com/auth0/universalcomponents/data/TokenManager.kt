package com.auth0.universalcomponents.data

import android.util.Log
import androidx.annotation.VisibleForTesting
import com.auth0.android.result.APICredentials
import com.auth0.universalcomponents.Auth0UniversalComponents
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

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal fun setInstance(tokenManager: TokenManager?) {
            instance = tokenManager
        }
    }

    private val tokenProvider = Auth0UniversalComponents.tokenProvider
    private val account = Auth0UniversalComponents.account

    private val tokenMap: ConcurrentHashMap<String, APICredentials> = ConcurrentHashMap()

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

        val cachedCredentials = tokenMap[cacheKey(audience, scope)]
        if (cachedCredentials != null) {
            if (!willTokenExpire(cachedCredentials.expiresAt.time)) {
                Log.d(TAG, "Returning cached token for audience: $audience, scope: $scope")
                return cachedCredentials.accessToken
            }
        }

        Log.d(TAG, "Fetching new token from provider for audience: $audience, scope: $scope")
        val credentials = tokenProvider.fetchApiCredentials(audience, scope)

        // Saving the same token for scenario where we request multiple scopes together
        saveToScopeCache(audience, scope, credentials)
        return credentials.accessToken
    }

    fun saveToken(audience: String, scope: String, credentials: APICredentials) {
        saveToScopeCache(audience, scope, credentials)
    }

    private fun cacheKey(audience: String, scope: String): String = "$audience|$scope"

    private fun saveToScopeCache(
        audience: String,
        scope: String,
        credentials: APICredentials
    ) {
        tokenMap[cacheKey(audience, scope)] = credentials
        val splitScope = scope.split(" ")
        if (splitScope.size > 1) {
            splitScope.forEach {
                Log.d(TAG, "token:$it ")
                tokenMap[cacheKey(audience, it)] = credentials
            }
        }
    }

    private fun willTokenExpire(expiresAt: Long): Boolean {
        val currentTimeInMillis = System.currentTimeMillis()
        return expiresAt <= currentTimeInMillis
    }
}