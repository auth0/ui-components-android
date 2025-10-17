package com.auth0.android.ui_components.presentation.ui.mfa

import androidx.compose.runtime.Composable
import com.auth0.android.ui_components.domain.model.AuthenticatorType

@Composable
fun AuthenticatorEnrollmentScreen(
    authenticatorType: AuthenticatorType,
    onContinue: (String, String, String?) -> Unit,
) {
    when (authenticatorType) {
        AuthenticatorType.PUSH,
        AuthenticatorType.TOTP -> QREnrollmentScreen(
            authenticatorType,
            onContinueClick = { authenticationId, authSession ->
                onContinue(authenticationId, authSession, null)
            })

        AuthenticatorType.SMS -> PhoneEnrollmentScreen(
            authenticatorType = authenticatorType,
            onContinueToOTP = { authenticationId, authSession, phoneNumber ->
                onContinue(authenticationId, authSession, phoneNumber)
            }
        )

        AuthenticatorType.EMAIL -> EmailEnrollmentScreen(
            authenticatorType = authenticatorType,
            onContinueToOTP = { authenticationId, authSession, email ->
                onContinue(authenticationId, authSession, email)
            }
        )

        AuthenticatorType.RECOVERY_CODE -> RecoveryCodeEnrollmentScreen(
            authenticatorType = authenticatorType,
            onContinue = { id, type ->
                onContinue(
                    id, type, null
                )
            }
        )
    }
}