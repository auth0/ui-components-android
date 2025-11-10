package com.auth0.android.ui_components.domain.error

/**
 * Domain-level error types that represent business-meaningful errors
 * These are mapped from infrastructure exceptions (AuthenticationException, MyAccountException, etc.)
 */
sealed class Auth0Error(
    override val message: String, override val cause: Throwable
) : Throwable(message, cause) {

    data class AccessDenied(
        override val message: String = "Access denied",
        override val cause: Throwable
    ) : Auth0Error(message, cause)

    // MFA Related Errors
    data class MfaRequired(
        override val message: String = "Multi-factor authentication required",
        override val cause: Throwable,
        val mfaScope: String
    ) : Auth0Error(message, cause)

    data class MfaEnrollRequired(
        override val message: String = "MFA enrollment required",
        override val cause: Throwable
    ) : Auth0Error(message, cause)

    data class InvalidMfaCode(
        override val message: String = "Invalid or expired MFA code",
        override val cause: Throwable
    ) : Auth0Error(message, cause)

    data class InvalidMfaToken(
        override val message: String = "Invalid or expired MFA token",
        override val cause: Throwable
    ) : Auth0Error(message, cause)

    // Token & Session Errors
    data class RefreshTokenInvalid(
        override val message: String = "Invalid or expired refresh token",
        override val cause: Throwable
    ) : Auth0Error(message, cause)

    data class RefreshTokenDeleted(
        override val message: String = "Refresh token no longer exists",
        override val cause: Throwable
    ) : Auth0Error(message, cause)

    data class SessionExpired(
        override val message: String = "Session has expired, please login again",
        override val cause: Throwable
    ) : Auth0Error(message, cause)

    // Rate Limiting & Security
    data class TooManyAttempts(
        override val message: String = "Too many login attempts, account temporarily blocked",
        override val cause: Throwable
    ) : Auth0Error(message, cause)

    // Network Errors
    data class NetworkError(
        override val message: String = "Network connection failed",
        override val cause: Throwable
    ) : Auth0Error(message, cause)


    // Validation Errors
    data class ValidationError(
        override val message: String = "Validation failed",
        override val cause: Throwable,
        val errors: List<FieldError> = emptyList()
    ) : Auth0Error(message, cause) {
        data class FieldError(
            val field: String?,
            val detail: String?,
            val pointer: String?,
            val source: String?
        )
    }

    data class InvalidOTP(
        override val message: String = "Invalid passcode",
        override val cause: Throwable,
    ) : Auth0Error(message, cause)

    // Server Errors
    data class ServerError(
        override val message: String = "Server error occurred",
        val statusCode: Int,
        override val cause: Throwable
    ) : Auth0Error(message, cause)


    // Generic/Unknown Errors
    data class Unknown(
        override val message: String = "An unknown error occurred",
        override val cause: Throwable
    ) : Auth0Error(message, cause)
}
