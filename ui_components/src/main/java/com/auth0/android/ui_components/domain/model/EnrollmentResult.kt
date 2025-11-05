package com.auth0.android.ui_components.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


/**
 * Wrapper for different enrollment challenge types
 */
@Parcelize
sealed class EnrollmentResult : Parcelable {
    /**
     * TOTP or Push Notification enrollment result
     */
    data class TotpEnrollment(
        val challenge: TotpEnrollmentChallenge,
        val authenticationMethodId: String,
        val authSession: String
    ) : EnrollmentResult()

    /**
     * Recovery code enrollment result
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
        val challenge: MfaEnrollmentChallenge,
        val authenticationMethodId: String,
        val authSession: String
    ) : EnrollmentResult()
}
