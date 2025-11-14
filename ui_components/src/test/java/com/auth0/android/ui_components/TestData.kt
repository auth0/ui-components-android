package com.auth0.android.ui_components

import com.auth0.android.result.EmailAuthenticationMethod
import com.auth0.android.result.Factor
import com.auth0.android.result.PhoneAuthenticationMethod
import com.auth0.android.result.PushNotificationAuthenticationMethod
import com.auth0.android.result.RecoveryCodeAuthenticationMethod
import com.auth0.android.result.TotpAuthenticationMethod
import com.auth0.android.result.MfaEnrollmentChallenge as SdkMfaEnrollmentChallenge
import com.auth0.android.result.RecoveryCodeEnrollmentChallenge as SdkRecoveryCodeEnrollmentChallenge
import com.auth0.android.result.TotpEnrollmentChallenge as SdkTotpEnrollmentChallenge
import com.auth0.android.ui_components.domain.model.AuthenticatorMethod
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.model.MfaEnrollmentChallenge as DomainMfaEnrollmentChallenge
import com.auth0.android.ui_components.domain.model.RecoveryCodeEnrollmentChallenge as DomainRecoveryCodeEnrollmentChallenge
import com.auth0.android.ui_components.domain.model.TotpEnrollmentChallenge as DomainTotpEnrollmentChallenge

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


    val totpEnrollmentChallenge = SdkTotpEnrollmentChallenge(
        id = "totp_id_123",
        authSession = "totp_session_123",
        barcodeUri = "otpauth://totp/...",
        manualInputCode = "MANUAL_CODE_123"
    )

    val pushEnrollmentChallenge = SdkTotpEnrollmentChallenge(
        id = "push_id_123",
        authSession = "push_session_123",
        barcodeUri = "push://uri",
        manualInputCode = "PUSH_CODE"
    )

    val recoveryCodeEnrollmentChallenge = SdkRecoveryCodeEnrollmentChallenge(
        id = "recovery_id_123",
        authSession = "recovery_session_123",
        recoveryCode = "RECOVERY-CODE-123"
    )

    val emailEnrollmentChallenge = SdkMfaEnrollmentChallenge(
        id = "email_123",
        authSession = "email_session"
    )

    val phoneEnrollmentChallenge = SdkMfaEnrollmentChallenge(
        id = "phone_123",
        authSession = "phone_session"
    )


    val domainTotpEnrollmentChallenge = DomainTotpEnrollmentChallenge(
        id = "totp_id_123",
        authSession = "totp_session_123",
        barcodeUri = "otpauth://totp/Auth0:user@example.com?secret=ABCDEFGH&issuer=Auth0",
        manualInputCode = "DOMAIN_MANUAL_CODE_123"
    )

    val domainPushEnrollmentChallenge = DomainTotpEnrollmentChallenge(
        id = "push_id_123",
        authSession = "push_session_123",
        barcodeUri = "push://notification/uri",
        manualInputCode = "DOMAIN_PUSH_CODE"
    )

    val domainRecoveryCodeEnrollmentChallenge = DomainRecoveryCodeEnrollmentChallenge(
        id = "recovery_id_123",
        authSession = "recovery_session_123",
        recoveryCode = "DOMAIN-RECOVERY-CODE-123-456-789"
    )

    val domainEmailEnrollmentChallenge = DomainMfaEnrollmentChallenge(
        id = "email_123",
        authSession = "email_session"
    )

    val domainPhoneEnrollmentChallenge = DomainMfaEnrollmentChallenge(
        id = "phone_123",
        authSession = "phone_session"
    )

    // AuthenticatorMethod test data
    val totpAuthenticatorMethod = AuthenticatorMethod(
        type = AuthenticatorType.TOTP,
        confirmed = true,
        usage = listOf("mfa"),
        name = "Authenticator App"
    )

    val phoneAuthenticatorMethod = AuthenticatorMethod(
        type = AuthenticatorType.PHONE,
        confirmed = false,
        usage = listOf("mfa"),
        name = "My Phone"
    )

    val emailAuthenticatorMethod = AuthenticatorMethod(
        type = AuthenticatorType.EMAIL,
        confirmed = true,
        usage = listOf("mfa"),
        name = "My Email"
    )

    val pushAuthenticatorMethod = AuthenticatorMethod(
        type = AuthenticatorType.PUSH,
        confirmed = false,
        usage = listOf("mfa"),
        name = "Push Device"
    )

    val recoveryCodeAuthenticatorMethod = AuthenticatorMethod(
        type = AuthenticatorType.RECOVERY_CODE,
        confirmed = true,
        usage = listOf("mfa"),
        name = null
    )

    val allAuthenticatorMethods = listOf(
        totpAuthenticatorMethod,
        phoneAuthenticatorMethod,
        emailAuthenticatorMethod,
        pushAuthenticatorMethod,
        recoveryCodeAuthenticatorMethod
    )

    // EnrolledAuthenticationMethod test data
    val enrolledPhoneMethod = com.auth0.android.ui_components.domain.model.EnrolledAuthenticationMethod(
        id = "enrolled_phone_001",
        type = "phone",
        confirmed = true,
        createdAt = "2025-11-10T10:00:00.000Z",
        name = "+15551234567"
    )

    val enrolledTotpMethod = com.auth0.android.ui_components.domain.model.EnrolledAuthenticationMethod(
        id = "enrolled_totp_002",
        type = "totp",
        confirmed = true,
        createdAt = "2025-11-10T11:00:00.000Z",
        name = "My Authenticator"
    )

    val enrolledEmailMethod = com.auth0.android.ui_components.domain.model.EnrolledAuthenticationMethod(
        id = "enrolled_email_003",
        type = "email",
        confirmed = true,
        createdAt = "2025-11-10T12:00:00.000Z",
        name = "user@example.com"
    )

    val enrolledPushMethod = com.auth0.android.ui_components.domain.model.EnrolledAuthenticationMethod(
        id = "enrolled_push_004",
        type = "push-notification",
        confirmed = true,
        createdAt = "2025-11-10T13:00:00.000Z",
        name = "My Device"
    )

    val allEnrolledMethods = listOf(
        enrolledPhoneMethod,
        enrolledTotpMethod,
        enrolledEmailMethod
    )
}