package com.auth0.android.ui_components.presentation.navigation

import com.auth0.android.ui_components.domain.model.AuthenticatorType
import kotlinx.serialization.Serializable

@Serializable
sealed interface AuthenticatorRoute {

    @Serializable
    object AuthenticatorMethodList

    @Serializable
    data class EnrolledAuthenticatorMethod(val authenticatorType: AuthenticatorType)

    @Serializable
    data class EnrollAuthenticatorMethod(val authenticatorType: AuthenticatorType)

    @Serializable
    data class OTPVerification(
        val authenticatorType: AuthenticatorType,
        val authenticationId: String,
        val authSession: String,
        val phoneNumberOrEmail: String? = null
    )

    @Serializable
    object PasskeyEnable
}


