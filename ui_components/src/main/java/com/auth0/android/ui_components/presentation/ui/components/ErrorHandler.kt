package com.auth0.android.ui_components.presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.auth0.android.ui_components.data.TokenManager
import com.auth0.android.ui_components.domain.error.Auth0Error
import com.auth0.android.ui_components.domain.network.onError
import com.auth0.android.ui_components.domain.network.onSuccess
import com.auth0.android.ui_components.presentation.mfaRecoveryHandler
import com.auth0.android.ui_components.presentation.ui.UiError

@Composable
fun ErrorHandler(
    uiError: UiError,
    modifier: Modifier = Modifier
) {
    when (val error = uiError.error) {
        is Auth0Error.MfaRequired -> {
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                mfaRecoveryHandler(
                    context,
                    error.mfaScope
                ).onSuccess {
                    TokenManager.getInstance().apply {
                        saveToken(getMyAccountAudience(), error.mfaScope, it)
                    }
                    uiError.onRetry.invoke()
                }
                    .onError {

                    }
            }

        }

        is Auth0Error.NetworkError -> {
            ErrorScreen(
                mainErrorMessage = "Connection problem",
                description = "Please check your internet connection and try again.",
                modifier = modifier,
                onRetryClick = uiError.onRetry
            )
        }

        is Auth0Error.InvalidMfaCode -> {
            ErrorScreen(
                mainErrorMessage = "Invalid verification code",
                description = "The code you entered is incorrect or has expired. Please try again.",
                modifier = modifier,
                onRetryClick = uiError.onRetry
            )
        }

        is Auth0Error.SessionExpired -> {
            ErrorScreen(
                mainErrorMessage = "Session expired",
                description = "Your session has expired. Please login again to continue.",
                modifier = modifier,
                onRetryClick = uiError.onRetry

            )
        }

        is Auth0Error.TooManyAttempts -> {
            ErrorScreen(
                mainErrorMessage = "Too many attempts",
                description = "Your account has been temporarily blocked due to too many failed attempts. Please try again later.",
                modifier = modifier,
                onRetryClick = uiError.onRetry
            )
        }

        else -> {
            ErrorScreen(
                mainErrorMessage = error.message,
                description = "We are unable to process your request. Please try again in a few minutes.",
                modifier = modifier,
                clickableString = "contact us.",
                onRetryClick = uiError.onRetry
            )
        }

    }
}

