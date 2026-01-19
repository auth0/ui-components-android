package com.auth0.android.ui_components.domain.usecase.passkey

import com.auth0.android.result.AuthenticationMethod
import com.auth0.android.ui_components.domain.DispatcherProvider
import com.auth0.android.ui_components.domain.error.Auth0Error
import com.auth0.android.ui_components.domain.model.PasskeyEnrollmentChallenge
import com.auth0.android.ui_components.domain.model.PublicKeyCredentials
import com.auth0.android.ui_components.domain.network.Result
import com.auth0.android.ui_components.domain.network.safeCall
import com.auth0.android.ui_components.domain.repository.MyAccountRepository
import kotlinx.coroutines.withContext

/**
 * Use case for verifying and completing passkey enrollment
 */
class EnrollPasskeyUseCase(
    private val repository: MyAccountRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    private companion object {
        private const val REQUIRED_SCOPES = "create:me:authentication_methods"
    }

    /**
     * Verifies and completes passkey enrollment with the credentials from Credential Manager
     *
     * @param publicKeyCredentials The passkey credentials obtained from the Credential Manager API
     * @param challenge The enrollment challenge obtained from [PasskeyChallengeUseCase]
     * @return [com.auth0.android.ui_components.domain.network.Result] containing [com.auth0.android.result.AuthenticationMethod] on success or [com.auth0.android.ui_components.domain.error.Auth0Error] on failure
     */
    suspend operator fun invoke(
        publicKeyCredentials: PublicKeyCredentials,
        challenge: PasskeyEnrollmentChallenge
    ): Result<AuthenticationMethod, Auth0Error> =
        withContext(dispatcherProvider.io) {
            safeCall {
                repository.verifyPasskey(
                    publicKeyCredentials = publicKeyCredentials,
                    challenge = challenge,
                    scope = REQUIRED_SCOPES
                )
            }
        }
}