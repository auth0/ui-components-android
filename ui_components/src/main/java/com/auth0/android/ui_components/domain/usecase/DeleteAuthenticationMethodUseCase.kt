package com.auth0.android.ui_components.domain.usecase

import android.util.Log
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.ui_components.data.TokenManager
import com.auth0.android.ui_components.domain.DispatcherProvider
import com.auth0.android.ui_components.domain.repository.MyAccountRepository
import com.auth0.android.ui_components.domain.util.Result
import kotlinx.coroutines.withContext
import java.io.IOException

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
    suspend operator fun invoke(authenticationMethodId: String): Result<Unit> =

        withContext(dispatcherProvider.io) {
            try {
                val audience = tokenManager.getMyAccountAudience()
                val accessToken = tokenManager.fetchToken(
                    audience = audience,
                    scope = REQUIRED_SCOPES
                )

                repository.deleteAuthenticationMethod(authenticationMethodId, accessToken)
                Log.d(TAG, "Successfully deleted authentication method: $authenticationMethodId")
                Result.Success(Unit)
            } catch (e: AuthenticationException) {
                Log.e(TAG, "Authentication error: ${e.getDescription()}", e)
                Result.Error(e)
            } catch (e: IOException) {
                Log.e(TAG, "Network error", e)
                Result.Error(e)
            } catch (e: Exception) {
                Log.e(TAG, "Unknown error", e)
                Result.Error(e)
            }
        }
}
