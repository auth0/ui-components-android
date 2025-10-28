package com.auth0.android.ui_components.presentation.ui.utils

import com.auth0.android.ui_components.domain.model.AuthenticatorMethod
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.presentation.viewmodel.AuthenticatorUiData

internal fun AuthenticatorMethod.toAuthenticatorUiModel(): AuthenticatorUiData {
    return when (type) {
        AuthenticatorType.TOTP -> AuthenticatorUiData(
            "Authenticator App", type, confirmed
        )

        AuthenticatorType.PHONE -> AuthenticatorUiData(
            "SMS OTP", type, confirmed
        )

        AuthenticatorType.EMAIL -> AuthenticatorUiData(
            "Email OTP", type, confirmed
        )

        AuthenticatorType.PUSH -> AuthenticatorUiData(
            "Push Notification", type, confirmed
        )

        AuthenticatorType.RECOVERY_CODE -> AuthenticatorUiData(
            "Recovery Code", type, confirmed
        )
    }

}