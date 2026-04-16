package com.auth0.universalcomponents.domain.usecase

import com.auth0.universalcomponents.domain.error.Auth0Error
import com.auth0.universalcomponents.domain.network.EmptyResult
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.domain.network.safeCall
import com.auth0.universalcomponents.domain.repository.MyAccountRepository

/**
 * UseCase that handles deletion of an authentication method
 * Fetches token and calls repository to delete the specified authentication method
 */
class DeleteAuthenticationMethodUseCase(
    private val repository: MyAccountRepository
) {
    private companion object {
        private const val REQUIRED_SCOPES = "delete:me:authentication_methods"
    }

    /**
     * Deletes an authentication method by ID
     * @param authenticationMethodId The ID of the authentication method to delete
     * @return Result indicating success or error
     */
    suspend operator fun invoke(authenticationMethodId: String): EmptyResult<Auth0Error> =
        safeCall {
            repository.deleteAuthenticationMethod(
                authenticationMethodId,
                REQUIRED_SCOPES
            )
            Result.Success(Unit)
        }
}
