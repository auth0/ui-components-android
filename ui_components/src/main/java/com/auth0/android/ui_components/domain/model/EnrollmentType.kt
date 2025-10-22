package com.auth0.android.ui_components.domain.model

/**
 * Represents different types of authenticator enrollment
 */
enum class EnrollmentType {
    TOTP,
    PUSH_NOTIFICATION,
    RECOVERY_CODE,
    EMAIL,
    PHONE
}
