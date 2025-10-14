package com.auth0.android.ui_components.domain.usecase

import android.util.Log
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.result.AuthenticationMethod
import com.auth0.android.result.Factor
import com.auth0.android.result.MfaAuthenticationMethod
import com.auth0.android.ui_components.data.TokenManager
import com.auth0.android.ui_components.domain.DispatcherProvider
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.model.MFAMethod
import com.auth0.android.ui_components.domain.repository.MyAccountRepository
import com.auth0.android.ui_components.domain.util.Result
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * UseCase that orchestrates MFA method fetching
 * Handles token fetching ONCE before making parallel API calls
 */
class GetMFAMethodsUseCase(
    private val repository: MyAccountRepository,
    private val tokenManager: TokenManager,
    private val dispatcherProvider: DispatcherProvider
) {
    private companion object {
        private const val TAG = "GetMFAMethodsUseCase"
        private const val REQUIRED_SCOPES = "read:me:factors read:me:authentication_methods"
    }

    suspend operator fun invoke(): Result<List<MFAMethod>> = withContext(dispatcherProvider.io) {
        try {
            val audience = tokenManager.getMyAccountAudience()
            val accessToken = tokenManager.fetchToken(
                audience = audience,
                scope = REQUIRED_SCOPES
            )

            val (factors, authMethods) = coroutineScope {
                val factorsDeferred = async {
                    repository.getFactors(accessToken)
                }
                val authMethodsDeferred = async {
                    repository.getAuthenticatorMethods(accessToken)
                }

                Pair(
                    factorsDeferred.await(),
                    authMethodsDeferred.await()
                )
            }

            val mfaMethods = mapToMFAMethods(factors, authMethods)
            Result.Success(mfaMethods)

        } catch (e: AuthenticationException) {
            // Token fetch or API authentication failed
            Log.e(TAG, "Authentication error: ${e.getDescription()}", e)
            Result.Error(e)
        } catch (e: IOException) {
            // Network error
            Log.e(TAG, "Network error", e)
            Result.Error(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unknown error", e)
            Result.Error(e)
        }
    }

    /**
     * Maps factors to MFA methods
     * Shows only available factors and checks if any authentication method
     * of the same type has confirmed: true
     */
    private fun mapToMFAMethods(
        factors: List<Factor>,
        authMethods: List<AuthenticationMethod>
    ): List<MFAMethod> {
        val mfaAuthMethods = authMethods
            .filterIsInstance<MfaAuthenticationMethod>()
            .filter { it.type != "password" }

        val authMethodsByType = mfaAuthMethods.groupBy { normalizeType(it.type) }

        return factors.map { factor ->
            val normalizedFactorType = normalizeType(factor.type)

            val hasConfirmedAuthMethod = authMethodsByType[normalizedFactorType]
                ?.any { it.confirmed == true } ?: false

            MFAMethod(
                type = mapTypeToAuthenticatorType(factor.type),
                confirmed = hasConfirmedAuthMethod,
                usage = factor.usage ?: emptyList()
            )
        }
    }

    /**
     * Normalizes type strings for comparison
     * Maps similar type names to a common format
     */
    private fun normalizeType(type: String): String {
        return when (type.lowercase()) {
            "push", "push-notification" -> "push-notification"
            else -> type.lowercase()
        }
    }

    /**
     * Maps authentication method type to AuthenticatorType enum
     */
    private fun mapTypeToAuthenticatorType(type: String): AuthenticatorType {
        return when (type.lowercase()) {
            "totp" -> AuthenticatorType.TOTP
            "phone" -> AuthenticatorType.SMS
            "email" -> AuthenticatorType.EMAIL
            "push", "push-notification" -> AuthenticatorType.PUSH
            "recovery_code" -> AuthenticatorType.RECOVERY_CODE
            else -> AuthenticatorType.TOTP
        }
    }
}
