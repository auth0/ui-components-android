package com.auth0.android.ui_components

import com.auth0.android.result.APICredentials
import com.auth0.android.result.EmailAuthenticationMethod
import com.auth0.android.result.Factor
import com.auth0.android.result.PhoneAuthenticationMethod
import com.auth0.android.result.PushNotificationAuthenticationMethod
import com.auth0.android.result.RecoveryCodeAuthenticationMethod
import com.auth0.android.result.TotpAuthenticationMethod
import com.auth0.android.result.MfaEnrollmentChallenge as SdkMfaEnrollmentChallenge
import com.auth0.android.result.RecoveryCodeEnrollmentChallenge as SdkRecoveryCodeEnrollmentChallenge
import com.auth0.android.result.TotpEnrollmentChallenge as SdkTotpEnrollmentChallenge
import com.auth0.android.result.PasskeyEnrollmentChallenge as SdkPasskeyEnrollmentChallenge
import com.auth0.android.result.AuthnParamsPublicKey as SdkAuthnParamsPublicKey
import com.auth0.android.result.AuthenticatorSelection as SdkAuthenticatorSelection
import com.auth0.android.result.PubKeyCredParam as SdkPubKeyCredParam
import com.auth0.android.result.RelyingParty as SdkRelyingParty
import com.auth0.android.result.PasskeyUser as SdkPasskeyUser
import com.auth0.android.result.PasskeyAuthenticationMethod as SdkPasskeyAuthenticationMethod
import com.auth0.android.ui_components.domain.model.AuthenticatorMethod
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.model.MfaEnrollmentChallenge as DomainMfaEnrollmentChallenge
import com.auth0.android.ui_components.domain.model.RecoveryCodeEnrollmentChallenge as DomainRecoveryCodeEnrollmentChallenge
import com.auth0.android.ui_components.domain.model.TotpEnrollmentChallenge as DomainTotpEnrollmentChallenge
import com.auth0.android.ui_components.domain.model.PasskeyEnrollmentChallenge as DomainPasskeyEnrollmentChallenge
import com.auth0.android.ui_components.domain.model.AuthnParamsPublicKey as DomainAuthnParamsPublicKey
import com.auth0.android.ui_components.domain.model.AuthenticatorSelection as DomainAuthenticatorSelection
import com.auth0.android.ui_components.domain.model.PubKeyCredParam as DomainPubKeyCredParam
import com.auth0.android.ui_components.domain.model.RelyingParty as DomainRelyingParty
import com.auth0.android.ui_components.domain.model.PasskeyUser as DomainPasskeyUser
import com.auth0.android.ui_components.domain.model.PublicKeyCredentials as DomainPublicKeyCredentials
import com.auth0.android.ui_components.domain.model.ClientExtensionResults as DomainClientExtensionResults
import com.auth0.android.ui_components.domain.model.CredProps as DomainCredProps
import com.auth0.android.ui_components.domain.model.PublicKeyResponse as DomainPublicKeyResponse
import java.util.Date

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
    val enrolledPhoneMethod =
        com.auth0.android.ui_components.domain.model.EnrolledAuthenticationMethod(
            id = "enrolled_phone_001",
            type = "phone",
            confirmed = true,
            createdAt = "2025-11-10T10:00:00.000Z",
            name = "+15551234567"
        )

    val enrolledTotpMethod =
        com.auth0.android.ui_components.domain.model.EnrolledAuthenticationMethod(
            id = "enrolled_totp_002",
            type = "totp",
            confirmed = true,
            createdAt = "2025-11-10T11:00:00.000Z",
            name = "My Authenticator"
        )

    val enrolledEmailMethod =
        com.auth0.android.ui_components.domain.model.EnrolledAuthenticationMethod(
            id = "enrolled_email_003",
            type = "email",
            confirmed = true,
            createdAt = "2025-11-10T12:00:00.000Z",
            name = "user@example.com"
        )


    val allEnrolledMethods = listOf(
        enrolledPhoneMethod,
        enrolledTotpMethod,
        enrolledEmailMethod
    )

    // EnrollmentResult test data for EnrollmentViewModel
    val totpEnrollmentResult =
        com.auth0.android.ui_components.domain.model.EnrollmentResult.TotpEnrollment(
            challenge = domainTotpEnrollmentChallenge,
            authenticationMethodId = "auth_totp_new_001",
            authSession = "session_totp_001"
        )


    val defaultPhoneEnrollmentResult =
        com.auth0.android.ui_components.domain.model.EnrollmentResult.DefaultEnrollment(
            challenge = domainPhoneEnrollmentChallenge,
            authenticationMethodId = "auth_phone_new_001",
            authSession = "session_phone_001"
        )

    val defaultEmailEnrollmentResult =
        com.auth0.android.ui_components.domain.model.EnrollmentResult.DefaultEnrollment(
            challenge = domainEmailEnrollmentChallenge,
            authenticationMethodId = "auth_email_new_001",
            authSession = "session_email_001"
        )

    // AuthenticationMethod for verification success
    val verifiedPhoneAuthMethod = PhoneAuthenticationMethod(
        id = "verified_phone_001",
        type = "phone",
        createdAt = "2025-11-16T10:00:00.000Z",
        usage = listOf("mfa"),
        confirmed = true,
        name = "Verified Phone",
        phoneNumber = "+15551234567",
        preferredAuthenticationMethod = "sms"
    )

    val verifiedTotpAuthMethod = TotpAuthenticationMethod(
        id = "verified_totp_001",
        type = "totp",
        createdAt = "2025-11-16T10:00:00.000Z",
        usage = listOf("mfa"),
        confirmed = true,
        name = "Verified Authenticator"
    )

    // APICredentials test data for TokenManager
    val validApiCredentials = APICredentials(
        accessToken = "valid_access_token_123",
        type = "Bearer",
        expiresAt = Date(System.currentTimeMillis() + 3600000),
        scope = "read:profile write:profile"
    )

    val goingToExpireApiCredentials = APICredentials(
        accessToken = "expired_access_token_456",
        type = "Bearer",
        expiresAt = Date(System.currentTimeMillis()),
        scope = "read:profile write:profile"
    )

    val expiredApiCredentials = APICredentials(
        accessToken = "expired_access_token_456",
        type = "Bearer",
        expiresAt = Date(System.currentTimeMillis() - 1000),
        scope = "read:profile write:profile"
    )

    val anotherValidApiCredentials = APICredentials(
        accessToken = "another_valid_token_789",
        type = "Bearer",
        expiresAt = Date(System.currentTimeMillis() + 7200000),
        scope = "read:profile write:profile"
    )

    val multiScopeApiCredentials = APICredentials(
        accessToken = "multi_scope_token_abc",
        type = "Bearer",
        expiresAt = Date(System.currentTimeMillis() + 1800000),
        scope = "read:profile write:profile"
    )

    // Passkey test data
    val sdkPasskeyEnrollmentChallenge = SdkPasskeyEnrollmentChallenge(
        authenticationMethodId = "passkey_method_123",
        authSession = "passkey_session_abc",
        authParamsPublicKey = SdkAuthnParamsPublicKey(
            authenticatorSelection = SdkAuthenticatorSelection(
                residentKey = "required",
                userVerification = "preferred"
            ),
            challenge = "challenge_string_xyz",
            pubKeyCredParams = listOf(
                SdkPubKeyCredParam(alg = -7, type = "public-key"),
                SdkPubKeyCredParam(alg = -257, type = "public-key")
            ),
            relyingParty = SdkRelyingParty(id = "example.auth0.com", name = "Example App"),
            timeout = 60000L,
            user = SdkPasskeyUser(
                displayName = "John Doe",
                id = "user_id_123",
                name = "johndoe@example.com"
            )
        )
    )

    val sdkPasskeyAuthMethod = SdkPasskeyAuthenticationMethod(
        id = "passkey_auth_method_456",
        type = "passkey",
        createdAt = "2025-11-10T10:00:00.000Z",
        usage = listOf("mfa"),
        credentialBackedUp = true,
        credentialDeviceType = "platform",
        identityUserId = null,
        keyId = "key_id_789",
        publicKey = "public_key_abc",
        transports = listOf("internal"),
        userAgent = "Android",
        userHandle = "user_handle_def",
    )

    val domainPublicKeyCredentials = DomainPublicKeyCredentials(
        authenticatorAttachment = "platform",
        clientExtensionResults = DomainClientExtensionResults(
            credProps = DomainCredProps(rk = true)
        ),
        id = "credential_id_123",
        rawId = "raw_credential_id_123",
        response = DomainPublicKeyResponse(
            attestationObject = "attestation_object_abc",
            authenticatorData = "authenticator_data_def",
            clientDataJSON = "client_data_json_ghi",
            transports = listOf("internal"),
            signature = "signature_jkl",
            userHandle = "user_handle_mno"
        ),
        type = "public-key"
    )

    val domainPasskeyEnrollmentChallenge = DomainPasskeyEnrollmentChallenge(
        authenticationMethodId = "passkey_method_123",
        authSession = "passkey_session_abc",
        authParamsPublicKey = DomainAuthnParamsPublicKey(
            authenticatorSelection = DomainAuthenticatorSelection(
                residentKey = "required",
                userVerification = "preferred"
            ),
            challenge = "challenge_string_xyz",
            pubKeyCredParams = listOf(
                DomainPubKeyCredParam(alg = -7, type = "public-key"),
                DomainPubKeyCredParam(alg = -257, type = "public-key")
            ),
            relyingParty = DomainRelyingParty(id = "example.auth0.com", name = "Example App"),
            timeout = 60000L,
            user = DomainPasskeyUser(
                displayName = "John Doe",
                id = "user_id_123",
                name = "johndoe@example.com"
            )
        )
    )
}