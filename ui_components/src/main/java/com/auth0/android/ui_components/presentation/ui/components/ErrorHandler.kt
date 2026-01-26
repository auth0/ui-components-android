package com.auth0.android.ui_components.presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.auth0.android.ui_components.R
import com.auth0.android.ui_components.data.TokenManager
import com.auth0.android.ui_components.domain.error.Auth0Error
import com.auth0.android.ui_components.domain.network.onError
import com.auth0.android.ui_components.domain.network.onSuccess
import com.auth0.android.ui_components.presentation.mfaRecoveryHandler
import com.auth0.android.ui_components.presentation.ui.UiError

@Composable
fun ErrorHandler(
    uiError: UiError,
    modifier: Modifier = Modifier,
    shouldRetry: Boolean = false
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
                }.onError {

                }
            }
        }

        is Auth0Error.NetworkError -> {
            ErrorScreen(
                mainErrorMessage = stringResource(R.string.connection_problem),
                description = stringResource(R.string.check_internet_connection),
                modifier = modifier,
                onRetryClick = uiError.onRetry
            )
        }

        is Auth0Error.InvalidMfaCode -> {
            ErrorScreen(
                mainErrorMessage = stringResource(R.string.invalid_verification_code),
                description = stringResource(R.string.code_incorrect_expired),
                modifier = modifier,
                onRetryClick = uiError.onRetry
            )
        }

        is Auth0Error.SessionExpired -> {
            ErrorScreen(
                mainErrorMessage = stringResource(R.string.session_expired),
                description = stringResource(R.string.session_expired_login),
                modifier = modifier,
                onRetryClick = uiError.onRetry

            )
        }

        is Auth0Error.TooManyAttempts -> {
            ErrorScreen(
                mainErrorMessage = stringResource(R.string.too_many_attempts_error),
                description = stringResource(R.string.account_temporarily_blocked),
                modifier = modifier,
                onRetryClick = uiError.onRetry
            )
        }

        is Auth0Error.PasskeyError -> {
            ErrorScreen(
                mainErrorMessage = error.message,
                description = stringResource(R.string.unable_to_process_contact),
                modifier = modifier,
                clickableString = stringResource(R.string.contact_us),
                shouldRetry = shouldRetry
            )

        }

        else -> {
            ErrorScreen(
                mainErrorMessage = error.message,
                description = stringResource(R.string.unable_to_process_contact),
                modifier = modifier,
                clickableString = stringResource(R.string.contact_us),
                onRetryClick = uiError.onRetry
            )
        }

    }
}

