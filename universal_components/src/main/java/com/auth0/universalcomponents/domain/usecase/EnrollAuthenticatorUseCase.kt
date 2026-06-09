package com.auth0.universalcomponents.domain.usecase

import com.auth0.universalcomponents.domain.error.Auth0Error
import com.auth0.universalcomponents.domain.model.AuthenticatorType
import com.auth0.universalcomponents.domain.model.EnrollmentInput
import com.auth0.universalcomponents.domain.model.EnrollmentResult
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.domain.network.safeCall
import com.auth0.universalcomponents.domain.repository.MyAccountRepository

/**
 * UseCase for enrolling authenticators
 * Handles TOTP, Push Notification, Recovery Code, Email, and Phone enrollments
 */
class EnrollAuthenticatorUseCase(
    private val repository: MyAccountRepository
) {
    private companion object {
        private const val REQUIRED_SCOPES = "create:me:authentication_methods"
    }

    /**
     * Enrolls an authenticator based on type
     * @param authenticatorType Type of authenticator to enroll
     * @param input [EnrollmentInput] based on authenticator type or defaults to EnrollmentInput.None
     * @return [EnrollmentResult] containing challenge data
     */
    suspend operator fun invoke(
        authenticatorType: AuthenticatorType,
        input: EnrollmentInput = EnrollmentInput.None
    ): Result<EnrollmentResult, Auth0Error> = safeCall {
        val result = when (authenticatorType) {
            AuthenticatorType.TOTP -> {
                val challenge = repository.enrollTotp(REQUIRED_SCOPES)
                EnrollmentResult.TotpEnrollment(
                    challenge = challenge,
                    authenticationMethodId = challenge.id,
                    authSession = challenge.authSession
                )
            }

            AuthenticatorType.PUSH -> {
                val challenge = repository.enrollPushNotification(REQUIRED_SCOPES)
                EnrollmentResult.TotpEnrollment(
                    challenge = challenge,
                    authenticationMethodId = challenge.id,
                    authSession = challenge.authSession
                )
            }

            AuthenticatorType.RECOVERY_CODE -> {
                val challenge = repository.enrollRecoveryCode(REQUIRED_SCOPES)
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
                val challenge =
                    repository.enrollEmail(input.email, REQUIRED_SCOPES)
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
                    scope = REQUIRED_SCOPES
                )
                EnrollmentResult.DefaultEnrollment(
                    challenge = challenge,
                    authenticationMethodId = challenge.id,
                    authSession = challenge.authSession
                )
            }

            else -> {
                throw UnsupportedOperationException("Unsupported enrollment type: $authenticatorType")
            }
        }
        result
    }
}
