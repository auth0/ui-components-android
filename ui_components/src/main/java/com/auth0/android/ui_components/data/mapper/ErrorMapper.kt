package com.auth0.android.ui_components.data.mapper

import android.util.Log
import com.auth0.android.NetworkErrorException
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.authentication.storage.CredentialsManagerException
import com.auth0.android.myaccount.MyAccountException
import com.auth0.android.ui_components.domain.error.Auth0Error

internal object ErrorMapper {

    private const val TAG = "ErrorMapper"

    /**
     * Maps any exception to an Auth0Error
     * Handles AuthenticationException, MyAccountException, and other exceptions
     */
    fun mapToAuth0Error(
        exception: Throwable,
        scope: String? = null
    ): Auth0Error {
        Log.e(TAG, "Mapping error", exception)

        return when (exception) {
            is AuthenticationException -> mapAuthenticationException(exception, scope)
            is MyAccountException -> mapMyAccountException(exception)
            is CredentialsManagerException -> mapCredentialsManagerException(exception, scope)
            is NetworkErrorException -> Auth0Error.NetworkError(cause = exception)
            else -> Auth0Error.Unknown(
                message = exception.message ?: "Unknown error occurred",
                cause = exception
            )

        }
    }

    /**
     * Maps AuthenticationException to specific Auth0Error types
     * Uses the comprehensive boolean properties available in AuthenticationException
     */
    private fun mapAuthenticationException(
        exception: AuthenticationException,
        scope: String?
    ): Auth0Error {
        return when {
            // Network errors
            exception.isNetworkError -> Auth0Error.NetworkError(
                message = "Network connection failed",
                cause = exception
            )

            // MFA related errors
            exception.isMultifactorRequired -> Auth0Error.MfaRequired(
                message = "Multi-factor authentication is required",
                cause = exception,
                mfaScope = scope ?: "openid profile email"
            )

            exception.isMultifactorEnrollRequired -> Auth0Error.MfaEnrollRequired(
                message = "MFA enrollment is required to continue",
                cause = exception
            )

            exception.isMultifactorCodeInvalid -> Auth0Error.InvalidMfaCode(
                message = "The MFA code is invalid or has expired",
                cause = exception
            )

            exception.isMultifactorTokenInvalid -> Auth0Error.InvalidMfaToken(
                message = "The MFA token is invalid or has expired",
                cause = exception
            )

            exception.isAccessDenied -> Auth0Error.AccessDenied(
                message = "Access denied by authorization server",
                cause = exception
            )

            exception.isLoginRequired -> Auth0Error.SessionExpired(
                message = "Session expired, please login again",
                cause = exception
            )

            // Token errors
            exception.isInvalidRefreshToken -> Auth0Error.RefreshTokenInvalid(
                message = "Refresh token is invalid or expired",
                cause = exception
            )

            exception.isRefreshTokenDeleted -> Auth0Error.RefreshTokenDeleted(
                message = "User account no longer exists",
                cause = exception
            )

            // Security & Rate Limiting
            exception.isTooManyAttempts -> Auth0Error.TooManyAttempts(
                message = "Too many failed attempts, please try again later",
                cause = exception
            )
            // Default fallback
            else -> Auth0Error.ServerError(
                message = exception.getDescription(),
                statusCode = exception.statusCode,
                cause = exception
            )
        }
    }


    private fun mapCredentialsManagerException(
        exception: CredentialsManagerException,
        scope: String?
    ): Auth0Error {
        Log.e(TAG, "mapCredentialsManagerException: ")
        return when {
            exception.cause as? AuthenticationException != null -> {
                mapAuthenticationException(exception.cause as AuthenticationException, scope)
            }

            else -> Auth0Error.Unknown(
                message = exception.message ?: "Unknown error",
                cause = exception
            )
        }
    }

    /**
     * Maps MyAccountException to specific Auth0Error types
     */
    private fun mapMyAccountException(exception: MyAccountException): Auth0Error {
        return when {
            // Network errors
            exception.isNetworkError -> Auth0Error.NetworkError(
                message = "Network connection failed",
                cause = exception
            )

            exception.getCode() == "Forbidden" && exception.detail == "invalid code" -> {
                Auth0Error.InvalidOTP(
                    message = exception.message ?: "Invalid passcode",
                    cause = exception
                )
            }

            // Validation errors
            !exception.validationErrors.isNullOrEmpty() -> {
                Auth0Error.ValidationError(
                    message = exception.detail ?: "Validation failed",
                    errors = (exception.validationErrors as List<MyAccountException.ValidationError>).map {
                        Auth0Error.ValidationError.FieldError(
                            field = it.field,
                            detail = it.detail,
                            pointer = it.pointer,
                            source = it.source
                        )
                    },
                    cause = exception
                )
            }

            exception.statusCode >= 500 -> Auth0Error.ServerError(
                message = "Server error, please try again later",
                statusCode = exception.statusCode,
                cause = exception
            )

            // Default fallback
            else -> Auth0Error.Unknown(
                message = exception.detail ?: exception.getDescription(),
                cause = exception
            )
        }
    }
}