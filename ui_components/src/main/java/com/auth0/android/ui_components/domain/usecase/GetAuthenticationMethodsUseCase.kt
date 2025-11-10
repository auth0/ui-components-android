package com.auth0.android.ui_components.domain.usecase

import com.auth0.android.result.AuthenticationMethod
import com.auth0.android.result.EmailAuthenticationMethod
import com.auth0.android.result.MfaAuthenticationMethod
import com.auth0.android.result.PhoneAuthenticationMethod
import com.auth0.android.result.PushNotificationAuthenticationMethod
import com.auth0.android.result.TotpAuthenticationMethod
import com.auth0.android.ui_components.domain.DispatcherProvider
import com.auth0.android.ui_components.domain.error.Auth0Error
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.model.EnrolledAuthenticationMethod
import com.auth0.android.ui_components.domain.network.Result
import com.auth0.android.ui_components.domain.network.safeCall
import com.auth0.android.ui_components.domain.repository.MyAccountRepository
import kotlinx.coroutines.withContext

/**
 * UseCase that fetches and filters authentication methods by type
 * Returns only confirmed methods for the specified authenticator type
 *
 */
class GetAuthenticationMethodsUseCase(
    private val repository: MyAccountRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    private companion object {
        private const val REQUIRED_SCOPES = "read:me:authentication_methods"
    }

    /**
     * Fetches authentication methods and filters based on type
     * @param type The authenticator type to filter by
     * @return Result containing list of confirmed authentication methods
     */
    suspend operator fun invoke(type: AuthenticatorType): Result<List<EnrolledAuthenticationMethod>, Auth0Error> =
        withContext(dispatcherProvider.io) {
            safeCall {
                val authMethods = repository.getAuthenticatorMethods(REQUIRED_SCOPES)
                filterEnrolledAuthenticationMethods(authMethods, type)
            }
        }

    /**
     * Filters authentication methods based on type and confirmed status
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
            name = when (this) {
                is PushNotificationAuthenticationMethod -> this.name
                is TotpAuthenticationMethod -> this.name
                is PhoneAuthenticationMethod -> this.name ?: this.phoneNumber
                is EmailAuthenticationMethod -> this.name
                else -> null
            }
        )
    }
}
