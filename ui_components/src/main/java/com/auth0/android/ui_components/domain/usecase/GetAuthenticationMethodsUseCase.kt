package com.auth0.android.ui_components.domain.usecase

import android.util.Log
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.result.AuthenticationMethod
import com.auth0.android.result.MfaAuthenticationMethod
import com.auth0.android.ui_components.data.TokenManager
import com.auth0.android.ui_components.domain.DispatcherProvider
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.model.EnrolledAuthenticationMethod
import com.auth0.android.ui_components.domain.repository.MyAccountRepository
import com.auth0.android.ui_components.domain.util.Result
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * UseCase that fetches and filters authentication methods by type
 * Returns only confirmed methods for the specified authenticator type
 * Special case: PUSH type returns all confirmed methods regardless of type
 */
class GetAuthenticationMethodsUseCase(
    private val repository: MyAccountRepository,
    private val tokenManager: TokenManager,
    private val dispatcherProvider: DispatcherProvider
) {
    private companion object {
        private const val TAG = "GetAuthenticationMethodsUseCase"
        private const val REQUIRED_SCOPES = "read:me:authentication_methods"
    }

    /**
     * Fetches authentication methods and filters based on type
     * @param type The authenticator type to filter by
     * @return Result containing list of confirmed authentication methods
     */
    suspend operator fun invoke(type: AuthenticatorType): Result<List<EnrolledAuthenticationMethod>> =
        withContext(dispatcherProvider.io) {
            try {
                val audience = tokenManager.getMyAccountAudience()
                val accessToken = tokenManager.fetchToken(
                    audience = audience,
                    scope = REQUIRED_SCOPES
                )

                val authMethods = repository.getAuthenticatorMethods(accessToken)
                val filteredMethods = filterEnrolledAuthenticationMethods(authMethods, type)
                Result.Success(filteredMethods)

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

    /**
     * Filters authentication methods based on type and confirmed status
     * For PUSH type: returns all confirmed methods
     * For other types: returns only confirmed methods matching the specified type
     */
    private fun filterEnrolledAuthenticationMethods(
        authMethods: List<AuthenticationMethod>,
        type: AuthenticatorType
    ): List<EnrolledAuthenticationMethod> {
        return authMethods
            .filterIsInstance<MfaAuthenticationMethod>()
            .filter { it.type != "password" }
            .filter { it.confirmed == true }
            .filter { it.type == type.type }
            .map { it.toEnrolledAuthenticationMethod() }
    }

    /**
     * Maps MfaAuthenticationMethod to AuthenticationMethodItem
     */
    private fun MfaAuthenticationMethod.toEnrolledAuthenticationMethod(): EnrolledAuthenticationMethod {
        return EnrolledAuthenticationMethod(
            id = this.id,
            type = this.type,
            confirmed = this.confirmed ?: false,
            createdAt = this.createdAt,
        )
    }
}
