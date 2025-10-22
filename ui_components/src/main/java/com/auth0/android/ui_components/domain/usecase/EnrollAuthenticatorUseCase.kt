package com.auth0.android.ui_components.domain.usecase

import android.util.Log
import com.auth0.android.ui_components.data.TokenManager
import com.auth0.android.ui_components.domain.DispatcherProvider
import com.auth0.android.ui_components.domain.error.Auth0Error
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.model.EnrollmentInput
import com.auth0.android.ui_components.domain.model.EnrollmentResult
import com.auth0.android.ui_components.domain.repository.MyAccountRepository
import com.auth0.android.ui_components.domain.network.Result
import com.auth0.android.ui_components.domain.network.safeCall
import kotlinx.coroutines.withContext

/**
 * Generic UseCase for enrolling authenticators
 * Handles TOTP, Push Notification, Recovery Code, Email, and Phone enrollments
 * Fetches token and calls appropriate repository method based on enrollment type
 */
class EnrollAuthenticatorUseCase(
    private val repository: MyAccountRepository,
    private val tokenManager: TokenManager,
    private val dispatcherProvider: DispatcherProvider
) {
    private companion object {
        private const val TAG = "EnrollAuthenticatorUseCase"
        private const val REQUIRED_SCOPES = "create:me:authentication_methods openid"
    }

    /**
     * Enrolls an authenticator based on type
     * @param authenticatorType Type of authenticator to enroll
     * @param input Additional input required (email/phone), or EnrollmentInput.None
     * @return Result with EnrollmentResult containing challenge data
     */
    suspend operator fun invoke(
        authenticatorType: AuthenticatorType,
        input: EnrollmentInput = EnrollmentInput.None
    ): Result<EnrollmentResult, Auth0Error> = withContext(dispatcherProvider.io) {
        safeCall(REQUIRED_SCOPES) {
            Log.d(TAG, "Starting enrollment for type: $authenticatorType")

            // Fetch access token with required scopes
            val audience = tokenManager.getMyAccountAudience()
            val accessToken = tokenManager.fetchToken(
                audience = audience,
                scope = REQUIRED_SCOPES
            )

            // Call appropriate repository method based on enrollment type
            val result = when (authenticatorType) {
                AuthenticatorType.TOTP -> {
                    val challenge = repository.enrollTotp(accessToken)
                    EnrollmentResult.TotpEnrollment(
                        challenge = challenge,
                        authenticationMethodId = challenge.id,
                        authSession = challenge.authSession
                    )
                }

                AuthenticatorType.PUSH -> {
                    val challenge = repository.enrollPushNotification(accessToken)
                    EnrollmentResult.TotpEnrollment(
                        challenge = challenge,
                        authenticationMethodId = challenge.id,
                        authSession = challenge.authSession
                    )
                }

                AuthenticatorType.RECOVERY_CODE -> {
                    val challenge = repository.enrollRecoveryCode(accessToken)
                    EnrollmentResult.RecoveryCodeEnrollment(
                        challenge = challenge,
                        authenticationMethodId = challenge.id,
                        authSession = challenge.authSession
                    )
                }

                AuthenticatorType.EMAIL -> {
                    require(input is EnrollmentInput.Email) {
                        "Email enrollment requires EnrollmentInput.Email"
                    }
                    val challenge = repository.enrollEmail(input.email, accessToken)
                    EnrollmentResult.DefaultEnrollment(
                        challenge = challenge,
                        authenticationMethodId = challenge.id,
                        authSession = challenge.authSession
                    )
                }

                AuthenticatorType.PHONE -> {
                    require(input is EnrollmentInput.Phone) {
                        "Phone enrollment requires EnrollmentInput.Phone"
                    }
                    val challenge = repository.enrollPhone(
                        phoneNumber = input.phoneNumber,
                        preferredMethod = input.preferredMethod,
                        accessToken = accessToken
                    )
                    EnrollmentResult.DefaultEnrollment(
                        challenge = challenge,
                        authenticationMethodId = challenge.id,
                        authSession = challenge.authSession
                    )
                }
            }

            Log.d(TAG, "Enrollment successful for type: $authenticatorType")
            result
        }
    }
}
