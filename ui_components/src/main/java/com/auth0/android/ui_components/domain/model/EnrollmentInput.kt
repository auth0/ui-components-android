package com.auth0.android.ui_components.domain.model

import com.auth0.android.myaccount.PhoneAuthenticationMethodType

/**
 * Input parameters for enrollment operations
 */
sealed class EnrollmentInput {
    /**
     * No additional input required (TOTP, Push, Recovery Code)
     */
    object None : EnrollmentInput()

    /**
     * Email address for email-based authentication
     */
    data class Email(val email: String) : EnrollmentInput()

    /**
     * Phone number and preferred method for SMS/Voice authentication
     */
    data class Phone(
        val phoneNumber: String,
        val preferredMethod: PhoneAuthenticationMethodType
    ) : EnrollmentInput()
}
