package com.auth0.android.ui_components.presentation.ui.mfa

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.model.EnrollmentResult

@Composable
fun AuthenticatorEnrollmentScreen(
    authenticatorType: AuthenticatorType,
    onContinue: (String, String) -> Unit
) {
    when (authenticatorType) {
        AuthenticatorType.PUSH,
        AuthenticatorType.TOTP -> QREnrollmentScreen(
            authenticatorType,
            onContinueClick = { authenticationId,authSession ->
                onContinue(authenticationId, authSession)
            })

        AuthenticatorType.SMS -> TODO()
        AuthenticatorType.EMAIL -> TODO()
        AuthenticatorType.RECOVERY_CODE -> TODO()
    }
}