package com.auth0.android.ui_components.domain.usecase

import com.auth0.android.result.AuthenticationMethod
import com.auth0.android.result.Factor
import com.auth0.android.result.MfaAuthenticationMethod
import com.auth0.android.ui_components.domain.DispatcherProvider
import com.auth0.android.ui_components.domain.error.Auth0Error
import com.auth0.android.ui_components.domain.model.AuthenticatorMethod
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.model.PrimaryAuthenticator
import com.auth0.android.ui_components.domain.model.SecondaryAuthenticator
import com.auth0.android.ui_components.domain.network.Result
import com.auth0.android.ui_components.domain.network.safeCall
import com.auth0.android.ui_components.domain.repository.MyAccountRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * UseCase that fetches enabled primary and secondary authenticator methods
 */
class GetEnabledAuthenticatorMethodsUseCase(
    private val repository: MyAccountRepository,
    private val dispatcherProvider: DispatcherProvider,
) {
    private companion object {
        private const val REQUIRED_SCOPES_FACTORS = "read:me:factors"
        private const val REQUIRED_SCOPES_AUTHENTICATION = "read:me:authentication_methods"
    }

    suspend operator fun invoke(): Result<AuthenticatorMethod, Auth0Error> =
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
                    mapAuthenticatorMethods(factors, authMethods)
                }
            }
        }

    /**
     * Separates [PrimaryAuthenticator] and [SecondaryAuthenticator] and maps factors to
     * [SecondaryAuthenticator] methods
     */
    private fun mapAuthenticatorMethods(
        factors: List<Factor>,
        authMethods: List<AuthenticationMethod>
    ): AuthenticatorMethod {
        val (secondaryAuthenticator, primaryAuthenticator) = authMethods
            .partition {
                it is MfaAuthenticationMethod
            }

        val primaryAuthMethods = primaryAuthenticator.filter {
            it.type == "passkey" && it is com.auth0.android.result.PasskeyAuthenticationMethod
        }.map {
            PrimaryAuthenticator(
                it.id,
                it.type,
                it.createdAt,
                (it as com.auth0.android.result.PasskeyAuthenticationMethod).identityUserId
            )
        }

        val authMethodsByType = secondaryAuthenticator.groupBy { it.type }
        val secondaryAuthMethods = factors.map { factor ->
            val hasConfirmedAuthMethod = authMethodsByType[factor.type]
                ?.any {
                    it is MfaAuthenticationMethod && it.confirmed == true
                } ?: false

            SecondaryAuthenticator(
                type = mapTypeToAuthenticatorType(factor.type),
                confirmed = hasConfirmedAuthMethod,
                usage = factor.usage ?: emptyList()
            )
        }

        return AuthenticatorMethod(primaryAuthMethods, secondaryAuthMethods)
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
            "passkey" -> AuthenticatorType.PASSKEY
            else -> AuthenticatorType.TOTP
        }
    }
}
