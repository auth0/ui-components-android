package com.auth0.android.ui_components.presentation.ui.mfa

import androidx.compose.runtime.Composable
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.presentation.ui.components.ErrorScreen
import com.auth0.android.ui_components.presentation.ui.mfa.phone.PhoneEnrollmentScreen

@Composable
fun AuthenticatorEnrollmentScreen(
    authenticatorType: AuthenticatorType,
    onContinue: (String, String, String?) -> Unit,
    onBackClick: () -> Unit,
) {
    when (authenticatorType) {
        AuthenticatorType.PUSH,
        AuthenticatorType.TOTP -> QREnrollmentScreen(
            authenticatorType,
            onBackClick = onBackClick,
            onContinueClick = { authenticationId, authSession ->
                onContinue(authenticationId, authSession, null)
            }
        )

        AuthenticatorType.PHONE -> PhoneEnrollmentScreen(
            authenticatorType = authenticatorType,
            onBackClick = onBackClick,
            onContinueToOTP = { authenticationId, authSession, phoneNumber ->
                onContinue(authenticationId, authSession, phoneNumber)
            }
        )

        AuthenticatorType.EMAIL -> EmailEnrollmentScreen(
            authenticatorType = authenticatorType,
            onBackClick = onBackClick,
            onContinueToOTP = { authenticationId, authSession, email ->
                onContinue(authenticationId, authSession, email)
            }
        )

        AuthenticatorType.RECOVERY_CODE -> RecoveryCodeEnrollmentScreen(
            authenticatorType = authenticatorType,
            onBackClick = onBackClick,
            onContinue = { id, type ->
                onContinue(
                    id, type, null
                )
            }
        )

        else -> ErrorScreen(mainErrorMessage = "Invalid State")
    }
}