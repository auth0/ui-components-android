package com.auth0.android.ui_components.domain.model

/**
 * Input parameters for verification operations
 */
sealed class VerificationInput {
    /**
     * OTP-based verification
     */
    data class WithOtp(
        val authenticationMethodId: String,
        val otpCode: String,
        val authSession: String
    ) : VerificationInput()

    /**
     * Non-OTP verification
     */
    data class WithoutOtp(
        val authenticationMethodId: String,
        val authSession: String
    ) : VerificationInput()
}
