package com.auth0.android.ui_components.presentation.navigation

import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.model.EnrollmentResult
import kotlinx.serialization.Serializable


@Serializable
object MFAMethodList

@Serializable
data class EnrolledAuthenticator(val authenticatorType: AuthenticatorType)

@Serializable
data class EnrollAuthenticator(val authenticatorType: AuthenticatorType)

@Serializable
data class OTPVerification(
    val authenticatorType: AuthenticatorType,
    val authenticationId: String,
    val authSession: String,
)

