package com.auth0.android.ui_components.domain.model

data class MFAMethod(
    val id: String,
    val name: String,
    val type: AuthenticatorType,
    val isActive: Boolean,
    val createdAt: String,
    val lastUsed: String?
)

enum class AuthenticatorType {
    OTP,
    SMS,
    EMAIL,
    PUSH,
    WEBAUTHN
}
