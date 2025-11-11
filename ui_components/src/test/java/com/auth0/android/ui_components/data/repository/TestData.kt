package com.auth0.android.ui_components.data.repository

import com.auth0.android.result.Factor
import com.auth0.android.result.MfaEnrollmentChallenge
import com.auth0.android.result.RecoveryCodeEnrollmentChallenge
import com.auth0.android.result.TotpEnrollmentChallenge

/**
 * Test data objects for repository tests
 */
object TestData {


    val phoneFactor = Factor(
        type = "phone",
        usage = listOf("primary")
    )

    val totpFactor = Factor(
        type = "totp",
        usage = listOf("secondary")
    )

    val sampleFactorList = listOf(phoneFactor, totpFactor)

    // ========== Enrollment Challenge Test Data ==========

    val totpEnrollmentChallenge = TotpEnrollmentChallenge(
        id = "totp_id_123",
        authSession = "totp_session_123",
        barcodeUri = "otpauth://totp/...",
        manualInputCode = "MANUAL_CODE_123"
    )

    val pushEnrollmentChallenge = TotpEnrollmentChallenge(
        id = "push_id_123",
        authSession = "push_session_123",
        barcodeUri = "push://uri",
        manualInputCode = "PUSH_CODE"
    )

    val recoveryCodeEnrollmentChallenge = RecoveryCodeEnrollmentChallenge(
        id = "recovery_id_123",
        authSession = "recovery_session_123",
        recoveryCode = "RECOVERY-CODE-123"
    )

    val emailEnrollmentChallenge = MfaEnrollmentChallenge(
        id = "email_123",
        authSession = "email_session"
    )

    val phoneEnrollmentChallenge = MfaEnrollmentChallenge(
        id = "phone_123",
        authSession = "phone_session"
    )

    val phoneVoiceEnrollmentChallenge = MfaEnrollmentChallenge(
        id = "phone_voice_123",
        authSession = "phone_voice_session"
    )
}
