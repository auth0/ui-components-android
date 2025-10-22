package com.auth0.android.ui_components.domain.model

import com.auth0.android.result.EnrollmentChallenge
import com.auth0.android.result.RecoveryCodeEnrollmentChallenge
import com.auth0.android.result.TotpEnrollmentChallenge

/**
 * Wrapper for different enrollment challenge types
 * Provides a unified interface for various enrollment responses
 */
sealed class EnrollmentResult {
    /**
     * TOTP or Push Notification enrollment result
     * Contains QR code data, secret, barcode URI
     */
    data class TotpEnrollment(
        val challenge: TotpEnrollmentChallenge,
        val authenticationMethodId: String,
        val authSession: String
    ) : EnrollmentResult()

    /**
     * Recovery code enrollment result
     * Contains recovery codes that user must save
     */
    data class RecoveryCodeEnrollment(
        val challenge: RecoveryCodeEnrollmentChallenge,
        val authenticationMethodId: String,
        val authSession: String
    ) : EnrollmentResult()

    /**
     * Email or Phone enrollment result
     * Generic enrollment challenge for OTP-based methods
     */
    data class DefaultEnrollment(
        val challenge: EnrollmentChallenge,
        val authenticationMethodId: String,
        val authSession: String
    ) : EnrollmentResult()
}
