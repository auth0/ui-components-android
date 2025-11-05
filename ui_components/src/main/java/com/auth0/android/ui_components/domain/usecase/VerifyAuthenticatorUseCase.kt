package com.auth0.android.ui_components.domain.usecase

import android.util.Log
import com.auth0.android.result.AuthenticationMethod
import com.auth0.android.ui_components.data.TokenManager
import com.auth0.android.ui_components.domain.DispatcherProvider
import com.auth0.android.ui_components.domain.error.Auth0Error
import com.auth0.android.ui_components.domain.model.VerificationInput
import com.auth0.android.ui_components.domain.repository.MyAccountRepository
import com.auth0.android.ui_components.domain.network.Result
import com.auth0.android.ui_components.domain.network.safeCall
import kotlinx.coroutines.withContext

/**
 * Generic UseCase for verifying enrolled authenticators
 * Handles both OTP-based and non-OTP verifications
 */
class VerifyAuthenticatorUseCase(
    private val repository: MyAccountRepository,
    private val tokenManager: TokenManager,
    private val dispatcherProvider: DispatcherProvider
) {
    private companion object {
        private const val TAG = "VerifyAuthenticatorUseCase"
        private const val REQUIRED_SCOPES = "create:me:authentication_methods"
    }

    /**
     * Verifies an enrolled authenticator
     * @param input Verification input (WithOtp or WithoutOtp)
     * @return Result with verified AuthenticationMethod
     */
    suspend operator fun invoke(
        input: VerificationInput
    ): Result<AuthenticationMethod, Auth0Error> = withContext(dispatcherProvider.io) {
       safeCall(REQUIRED_SCOPES) {
            Log.d(TAG, "Starting verification for: ${input::class.simpleName}")

            val audience = tokenManager.getMyAccountAudience()
            val accessToken = tokenManager.fetchToken(
                audience = audience,
                scope = REQUIRED_SCOPES
            )

            val authMethod = when (input) {
                is VerificationInput.WithOtp -> {
                    Log.d(TAG, "Verifying with OTP code")
                    repository.verifyOtp(
                        authenticationMethodId = input.authenticationMethodId,
                        otpCode = input.otpCode,
                        authSession = input.authSession,
                        accessToken = accessToken
                    )
                }

                is VerificationInput.WithoutOtp -> {
                    repository.verifyWithoutOtp(
                        authenticationMethodId = input.authenticationMethodId,
                        authSession = input.authSession,
                        accessToken = accessToken
                    )
                }
            }

            Log.d(TAG, "Verification successful")
            authMethod
        }
    }
}
