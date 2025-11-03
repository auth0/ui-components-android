package com.auth0.android.ui_components.domain.error

import com.auth0.android.ui_components.domain.error.Auth0Error.ValidationError.FieldError

/**
 * Domain-level error types that represent business-meaningful errors
 * These are mapped from infrastructure exceptions (AuthenticationException, MyAccountException, etc.)
 */
sealed interface Auth0Error {
    val message: String
    val cause: Throwable

    data class AccessDenied(
        override val message: String = "Access denied",
        override val cause: Throwable
    ) : Auth0Error

    // MFA Related Errors
    data class MfaRequired(
        override val message: String = "Multi-factor authentication required",
        override val cause: Throwable,
        val mfaScope: String
    ) : Auth0Error

    data class MfaEnrollRequired(
        override val message: String = "MFA enrollment required",
        override val cause: Throwable
    ) : Auth0Error

    data class InvalidMfaCode(
        override val message: String = "Invalid or expired MFA code",
        override val cause: Throwable
    ) : Auth0Error

    data class InvalidMfaToken(
        override val message: String = "Invalid or expired MFA token",
        override val cause: Throwable
    ) : Auth0Error

    // Token & Session Errors
    data class RefreshTokenInvalid(
        override val message: String = "Invalid or expired refresh token",
        override val cause: Throwable
    ) : Auth0Error

    data class RefreshTokenDeleted(
        override val message: String = "Refresh token no longer exists",
        override val cause: Throwable
    ) : Auth0Error

    data class SessionExpired(
        override val message: String = "Session has expired, please login again",
        override val cause: Throwable
    ) : Auth0Error

    // Rate Limiting & Security
    data class TooManyAttempts(
        override val message: String = "Too many login attempts, account temporarily blocked",
        override val cause: Throwable
    ) : Auth0Error

    // Network Errors
    data class NetworkError(
        override val message: String = "Network connection failed",
        override val cause: Throwable
    ) : Auth0Error

    data class Timeout(
        override val message: String = "Request timed out",
        override val cause: Throwable
    ) : Auth0Error

    // Validation Errors
    data class ValidationError(
        override val message: String = "Validation failed",
        override val cause: Throwable,
        val errors: List<FieldError> = emptyList()
    ) : Auth0Error {
        data class FieldError(
            val field: String?,
            val detail: String?,
            val pointer: String?,
            val source: String?
        )
    }

    data class Forbidden(
        override val message: String = "Forbidden",
        override val cause: Throwable,
    ) : Auth0Error

    data class InvalidOTP(
        override val message: String = "Invalid passcode",
        override val cause: Throwable,
    ) : Auth0Error

    // Server Errors
    data class ServerError(
        override val message: String = "Server error occurred",
        val statusCode: Int,
        override val cause: Throwable
    ) : Auth0Error


    // Generic/Unknown Errors
    data class Unknown(
        override val message: String = "An unknown error occurred",
        override val cause: Throwable
    ) : Auth0Error
}
