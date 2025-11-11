package com.auth0.android.ui_components

import com.auth0.android.result.EmailAuthenticationMethod
import com.auth0.android.result.Factor
import com.auth0.android.result.MfaEnrollmentChallenge
import com.auth0.android.result.PhoneAuthenticationMethod
import com.auth0.android.result.PushNotificationAuthenticationMethod
import com.auth0.android.result.RecoveryCodeAuthenticationMethod
import com.auth0.android.result.RecoveryCodeEnrollmentChallenge
import com.auth0.android.result.TotpAuthenticationMethod
import com.auth0.android.result.TotpEnrollmentChallenge

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


    val phoneAuthMethod = PhoneAuthenticationMethod(
        id = "auth_phone_123",
        type = "phone",
        createdAt = "2025-11-10T10:00:00.000Z",
        usage = listOf("mfa"),
        confirmed = true,
        name = "My Phone",
        phoneNumber = "+15551234567",
        preferredAuthenticationMethod = "sms"
    )

    val emailAuthMethod = EmailAuthenticationMethod(
        id = "auth_email_456",
        type = "email",
        createdAt = "2025-11-10T10:00:00.000Z",
        usage = listOf("mfa"),
        confirmed = true,
        name = "My Email",
        email = "user@example.com"
    )

    val recoveryAuthMethod = RecoveryCodeAuthenticationMethod(
        id = "auth_recovery_789",
        type = "recovery-code",
        createdAt = "2025-11-10T10:00:00.000Z",
        usage = listOf("mfa"),
        confirmed = true,
    )

    val totpAuthMethod = TotpAuthenticationMethod(
        id = "auth_totp_789",
        type = "totp",
        createdAt = "2025-11-10T10:00:00.000Z",
        usage = listOf("mfa"),
        confirmed = true,
        name = "Authenticator App"
    )

    val pushNotificationAuthMethod = PushNotificationAuthenticationMethod(
        id = "auth_push_012",
        type = "push-notification",
        createdAt = "2025-11-10T10:00:00.000Z",
        usage = listOf("mfa"),
        confirmed = true,
        name = "My Push Device"
    )


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
}