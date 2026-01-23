package com.auth0.android.ui_components.presentation.ui.utils

import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.model.PrimaryAuthenticator
import com.auth0.android.ui_components.domain.model.SecondaryAuthenticator
import com.auth0.android.ui_components.presentation.viewmodel.PrimaryAuthenticatorUiData
import com.auth0.android.ui_components.presentation.viewmodel.SecondaryAuthenticatorUiData

internal fun SecondaryAuthenticator.toAuthenticatorUiModel(): SecondaryAuthenticatorUiData {
    return when (type) {
        AuthenticatorType.TOTP -> SecondaryAuthenticatorUiData(
            "Authenticator App", type, confirmed
        )

        AuthenticatorType.PHONE -> SecondaryAuthenticatorUiData(
            "SMS OTP", type, confirmed
        )

        AuthenticatorType.EMAIL -> SecondaryAuthenticatorUiData(
            "Email OTP", type, confirmed
        )

        AuthenticatorType.PUSH -> SecondaryAuthenticatorUiData(
            "Push Notification", type, confirmed
        )

        AuthenticatorType.RECOVERY_CODE -> SecondaryAuthenticatorUiData(
            "Recovery Code", type, confirmed
        )

        else -> {
            SecondaryAuthenticatorUiData(
                "Authenticator App", type, confirmed
            )
        }
    }
}

internal fun PrimaryAuthenticator.toPrimaryAuthenticatorUiModel(): PrimaryAuthenticatorUiData {
    return PrimaryAuthenticatorUiData(
        id = id,
        title = "Passkey",
        createdAt = createdAt
    )
}
