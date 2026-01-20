package com.auth0.android.ui_components.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Local sealed class hierarchy for enrollment challenges.
 */
@Parcelize
@Serializable
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

/**
 * Passkey enrollment challenge
 * Contains the authentication method ID and public key parameters for passkey enrollment
 */
@Serializable
data class PasskeyEnrollmentChallenge(
    val authenticationMethodId: String,
    override val authSession: String,
    val authParamsPublicKey: AuthnParamsPublicKey
) : EnrollmentChallenge() {
    override val id: String
        get() = authenticationMethodId
}

/**
 * Public key parameters for passkey authentication
 */
@Parcelize
@Serializable
data class AuthnParamsPublicKey(
    val authenticatorSelection: AuthenticatorSelection,
    val challenge: String,
    val pubKeyCredParams: List<PubKeyCredParam>,
    val relyingParty: RelyingParty,
    val timeout: Long,
    val user: PasskeyUser
) : Parcelable

/**
 * Authenticator selection criteria for passkey
 */
@Parcelize
@Serializable
data class AuthenticatorSelection(
    val residentKey: String,
    val userVerification: String
) : Parcelable

/**
 * Public key credential parameters
 */
@Parcelize
@Serializable
data class PubKeyCredParam(
    val alg: Int,
    val type: String
) : Parcelable

/**
 * Relying party information for passkey
 */
@Parcelize
@Serializable
data class RelyingParty(
    val id: String,
    val name: String
) : Parcelable

/**
 * User information for passkey
 */
@Parcelize
@Serializable
data class PasskeyUser(
    val displayName: String,
    val id: String,
    val name: String
) : Parcelable

/**
 * Passkey authentication method after successful enrollment
 */
@Parcelize
data class PasskeyAuthenticationMethod(
    val id: String,
    val type: String,
    val credentialDeviceType: String?,
    val credentialBackedUp: Boolean,
    val publicKey: String?
) : Parcelable

