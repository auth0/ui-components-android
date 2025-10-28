package com.auth0.android.ui_components.domain.model

/**
 * Represents an MFA method combining data from both factors and authentication_methods APIs
 */
data class AuthenticatorMethod(
    val type: AuthenticatorType,
    val confirmed: Boolean,
    val usage: List<String>,
    val name: String? = null,
)

enum class AuthenticatorType(val type: String) {
    TOTP("totp"),
    PHONE("phone"),
    EMAIL("email"),
    PUSH("push-notification"),
    RECOVERY_CODE("recovery-code")
}
