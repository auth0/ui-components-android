package com.auth0.universalcomponents.domain.usecase

import com.auth0.android.result.AuthenticationMethod
import com.auth0.universalcomponents.domain.error.Auth0Error
import com.auth0.universalcomponents.domain.model.VerificationInput
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.domain.network.safeCall
import com.auth0.universalcomponents.domain.repository.MyAccountRepository

/**
 * UseCase for verifying enrolled authenticators
 * Handles both OTP-based and non-OTP verifications
 */
class VerifyAuthenticatorUseCase(
    private val repository: MyAccountRepository
) {
    private companion object {
        private const val REQUIRED_SCOPES = "create:me:authentication_methods"
    }

    /**
     * Verifies an enrolled authenticator
     * @param input Verification input (WithOtp or WithoutOtp)
     * @return Result with verified AuthenticationMethod
     */
    suspend operator fun invoke(
        input: VerificationInput
    ): Result<AuthenticationMethod, Auth0Error> = safeCall {
        val authMethod = when (input) {
            is VerificationInput.WithOtp -> {
                repository.verifyOtp(
                    authenticationMethodId = input.authenticationMethodId,
                    otpCode = input.otpCode,
                    authSession = input.authSession,
                    REQUIRED_SCOPES
                )
            }

            is VerificationInput.WithoutOtp -> {
                repository.verifyWithoutOtp(
                    authenticationMethodId = input.authenticationMethodId,
                    authSession = input.authSession,
                    REQUIRED_SCOPES
                )
            }
        }
        authMethod
    }
}
