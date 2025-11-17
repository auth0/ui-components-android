package com.auth0.android.ui_components.domain.usecase

import com.auth0.android.result.AuthenticationMethod
import com.auth0.android.result.Factor
import com.auth0.android.result.MfaAuthenticationMethod
import com.auth0.android.ui_components.domain.DispatcherProvider
import com.auth0.android.ui_components.domain.error.Auth0Error
import com.auth0.android.ui_components.domain.model.AuthenticatorMethod
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.network.Result
import com.auth0.android.ui_components.domain.network.safeCall
import com.auth0.android.ui_components.domain.repository.MyAccountRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * UseCase that fetches enables authentication factors
 */
class GetEnabledAuthenticatorMethodsUseCase(
    private val repository: MyAccountRepository,
    private val dispatcherProvider: DispatcherProvider,
) {
    private companion object {
        private const val REQUIRED_SCOPES_FACTORS = "read:me:factors"
        private const val REQUIRED_SCOPES_AUTHENTICATION = "read:me:authentication_methods"
    }

    suspend operator fun invoke(): Result<List<AuthenticatorMethod>, Auth0Error> =
        withContext(dispatcherProvider.io) {
            safeCall {
                coroutineScope {
                    val factorsDeferred = async {
                        repository.getFactors(REQUIRED_SCOPES_FACTORS)
                    }
                    val authMethodsDeferred = async {
                        repository.getAuthenticatorMethods(REQUIRED_SCOPES_AUTHENTICATION)
                    }

                    val (factors, authMethods) = Pair(
                        factorsDeferred.await(),
                        authMethodsDeferred.await()
                    )
                    mapToMFAMethods(factors, authMethods)
                }
            }
        }

    /**
     * Maps factors to MFA methods
     * Shows only available factors and checks if any authentication method
     * of the same type has confirmed: true`
     */
    private fun mapToMFAMethods(
        factors: List<Factor>,
        authMethods: List<AuthenticationMethod>
    ): List<AuthenticatorMethod> {
        val mfaAuthMethods = authMethods
            .filterIsInstance<MfaAuthenticationMethod>()
            .filter { it.type != "password" }

        val authMethodsByType = mfaAuthMethods.groupBy { it.type }

        return factors.map { factor ->
            val hasConfirmedAuthMethod = authMethodsByType[factor.type]
                ?.any { it.confirmed == true } ?: false

            AuthenticatorMethod(
                type = mapTypeToAuthenticatorType(factor.type),
                confirmed = hasConfirmedAuthMethod,
                usage = factor.usage ?: emptyList()
            )
        }
    }

    /**
     * Maps authentication method type to AuthenticatorType enum
     */
    private fun mapTypeToAuthenticatorType(type: String): AuthenticatorType {
        return when (type.lowercase()) {
            "totp" -> AuthenticatorType.TOTP
            "phone" -> AuthenticatorType.PHONE
            "email" -> AuthenticatorType.EMAIL
            "push-notification" -> AuthenticatorType.PUSH
            "recovery-code" -> AuthenticatorType.RECOVERY_CODE
            else -> AuthenticatorType.TOTP
        }
    }
}
