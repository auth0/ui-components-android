package com.auth0.android.ui_components.domain.usecase

import android.util.Log
import com.auth0.android.ui_components.data.TokenManager
import com.auth0.android.ui_components.domain.DispatcherProvider
import com.auth0.android.ui_components.domain.error.Auth0Error
import com.auth0.android.ui_components.domain.repository.MyAccountRepository
import com.auth0.android.ui_components.domain.network.EmptyResult
import com.auth0.android.ui_components.domain.network.Result
import com.auth0.android.ui_components.domain.network.safeCall
import kotlinx.coroutines.withContext
import java.lang.Exception

/**
 * UseCase that handles deletion of an authentication method
 * Fetches token and calls repository to delete the specified authentication method
 */
class DeleteAuthenticationMethodUseCase(
    private val repository: MyAccountRepository,
    private val tokenManager: TokenManager,
    private val dispatcherProvider: DispatcherProvider
) {
    private companion object {
        private const val TAG = "DeleteAuthenticationMethodUseCase"
        private const val REQUIRED_SCOPES = "delete:me:authentication_methods"
    }

    /**
     * Deletes an authentication method by ID
     * @param authenticationMethodId The ID of the authentication method to delete
     * @return Result indicating success or error
     */
    suspend operator fun invoke(authenticationMethodId: String): EmptyResult<Auth0Error> =
        withContext(dispatcherProvider.io) {
            safeCall(REQUIRED_SCOPES) {
                val audience = tokenManager.getMyAccountAudience()
                val accessToken = tokenManager.fetchToken(
                    audience = audience,
                    scope = REQUIRED_SCOPES
                )

                Log.d(TAG, "Successfully deleted authentication method: with token ${accessToken}")
                repository.deleteAuthenticationMethod(authenticationMethodId, accessToken)
                return@withContext Result.Success(Unit)
            }
        }
}
