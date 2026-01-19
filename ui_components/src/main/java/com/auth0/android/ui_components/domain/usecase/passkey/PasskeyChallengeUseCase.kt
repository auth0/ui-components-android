package com.auth0.android.ui_components.domain.usecase.passkey

import com.auth0.android.ui_components.domain.DispatcherProvider
import com.auth0.android.ui_components.domain.error.Auth0Error
import com.auth0.android.ui_components.domain.model.PasskeyEnrollmentChallenge
import com.auth0.android.ui_components.domain.network.Result
import com.auth0.android.ui_components.domain.network.safeCall
import com.auth0.android.ui_components.domain.repository.MyAccountRepository
import kotlinx.coroutines.withContext

/**
 * Use case for requesting a passkey enrollment challenge from the server
 */
class PasskeyChallengeUseCase(
    private val repository: MyAccountRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    private companion object {
        private const val REQUIRED_SCOPES = "create:me:authentication_methods"
    }

    /**
     * Requests a challenge for enrolling a new passkey
     *
     * @return [com.auth0.android.ui_components.domain.network.Result] containing [com.auth0.android.ui_components.domain.model.PasskeyEnrollmentChallenge] on success or [com.auth0.android.ui_components.domain.error.Auth0Error] on failure
     */
    suspend operator fun invoke(
    ): Result<PasskeyEnrollmentChallenge, Auth0Error> =
        withContext(dispatcherProvider.io) {
            safeCall {
                repository.enrollPasskey(
                    scope = REQUIRED_SCOPES
                )
            }
        }
}