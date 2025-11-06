package com.auth0.android.ui_components.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Local sealed class hierarchy for enrollment challenges.
 */
@Parcelize
sealed class EnrollmentChallenge : Parcelable {
    abstract val id: String
    abstract val authSession: String
}

/**
 * Default MFA enrollment challenge
 * Used for basic enrollment flows
 */
data class MfaEnrollmentChallenge(
    override val id: String,
    override val authSession: String
) : EnrollmentChallenge()

/**
 * TOTP or Push Notification enrollment challenge
 * Contains QR code data and manual input code for authenticator apps
 */
data class TotpEnrollmentChallenge(
    override val id: String,
    override val authSession: String,
    val barcodeUri: String,
    val manualInputCode: String?
) : EnrollmentChallenge()

/**
 * Recovery code enrollment challenge
 * Contains the recovery code that users should save securely
 */
data class RecoveryCodeEnrollmentChallenge(
    override val id: String,
    override val authSession: String,
    val recoveryCode: String
) : EnrollmentChallenge()
