package com.auth0.android.ui_components.data.mapper

import com.auth0.android.ui_components.domain.model.AuthenticatorSelection
import com.auth0.android.ui_components.domain.model.AuthnParamsPublicKey
import com.auth0.android.ui_components.domain.model.EnrollmentChallenge
import com.auth0.android.ui_components.domain.model.MfaEnrollmentChallenge
import com.auth0.android.ui_components.domain.model.PasskeyAuthenticationMethod
import com.auth0.android.ui_components.domain.model.PasskeyEnrollmentChallenge
import com.auth0.android.ui_components.domain.model.PasskeyUser
import com.auth0.android.ui_components.domain.model.PubKeyCredParam
import com.auth0.android.ui_components.domain.model.PublicKeyCredentials
import com.auth0.android.ui_components.domain.model.RecoveryCodeEnrollmentChallenge
import com.auth0.android.ui_components.domain.model.RelyingParty
import com.auth0.android.ui_components.domain.model.TotpEnrollmentChallenge
import com.auth0.android.request.PublicKeyCredentials as SdkPublicKeyCredentials
import com.auth0.android.result.AuthnParamsPublicKey as SdkAuthParamsPublicKey
import com.auth0.android.result.EnrollmentChallenge as SdkEnrollmentChallenge
import com.auth0.android.result.MfaEnrollmentChallenge as SdkMfaEnrollmentChallenge
import com.auth0.android.result.PasskeyAuthenticationMethod as SdkPasskeyAuthenticationMethod
import com.auth0.android.result.PasskeyEnrollmentChallenge as SdkPasskeyEnrollmentChallenge
import com.auth0.android.result.RecoveryCodeEnrollmentChallenge as SdkRecoveryCodeEnrollmentChallenge
import com.auth0.android.result.TotpEnrollmentChallenge as SdkTotpEnrollmentChallenge

/**
 * Mapper functions to convert Auth0 SDK enrollment challenges to local domain models.
 * This decouples the UI components from SDK types for better flexibility.
 */

fun SdkMfaEnrollmentChallenge.toDomainModel(): MfaEnrollmentChallenge {
    return MfaEnrollmentChallenge(
        id = this.id,
        authSession = this.authSession
    )
}


fun SdkTotpEnrollmentChallenge.toDomainModel(): TotpEnrollmentChallenge {
    return TotpEnrollmentChallenge(
        id = this.id,
        authSession = this.authSession,
        barcodeUri = this.barcodeUri,
        manualInputCode = this.manualInputCode
    )
}

fun SdkRecoveryCodeEnrollmentChallenge.toDomainModel(): RecoveryCodeEnrollmentChallenge {
    return RecoveryCodeEnrollmentChallenge(
        id = this.id,
        authSession = this.authSession,
        recoveryCode = this.recoveryCode
    )
}

fun SdkEnrollmentChallenge.toDomainModel(): EnrollmentChallenge {
    return when (this) {
        is SdkTotpEnrollmentChallenge -> this.toDomainModel()
        is SdkRecoveryCodeEnrollmentChallenge -> this.toDomainModel()
        is SdkMfaEnrollmentChallenge -> this.toDomainModel()
    }
}

fun SdkPasskeyEnrollmentChallenge.toDomainModel(): PasskeyEnrollmentChallenge {
    return PasskeyEnrollmentChallenge(
        authenticationMethodId = this.authenticationMethodId,
        authSession = this.authSession,
        authParamsPublicKey = this.authParamsPublicKey.toDomainModel()
    )
}

fun SdkAuthParamsPublicKey.toDomainModel(): AuthnParamsPublicKey {
    return AuthnParamsPublicKey(
        authenticatorSelection = AuthenticatorSelection(
            residentKey = this.authenticatorSelection.residentKey,
            userVerification = this.authenticatorSelection.userVerification
        ),
        challenge = this.challenge,
        pubKeyCredParams = this.pubKeyCredParams.map { param ->
            PubKeyCredParam(
                alg = param.alg,
                type = param.type
            )
        },
        relyingParty = RelyingParty(
            id = this.relyingParty.id,
            name = this.relyingParty.name
        ),
        timeout = this.timeout,
        user = PasskeyUser(
            displayName = this.user.displayName,
            id = this.user.id,
            name = this.user.name
        )
    )
}

fun SdkPasskeyAuthenticationMethod.toDomainModel(): PasskeyAuthenticationMethod {
    return PasskeyAuthenticationMethod(
        id = this.id,
        type = this.type,
        credentialDeviceType = this.credentialDeviceType,
        credentialBackedUp = this.credentialBackedUp ?: false,
        publicKey = this.publicKey
    )
}

/**
 * Reverse mapper to convert domain model back to SDK type for API calls
 */
fun AuthnParamsPublicKey.toSdkModel(): SdkAuthParamsPublicKey {
    return com.auth0.android.result.AuthnParamsPublicKey(
        authenticatorSelection = com.auth0.android.result.AuthenticatorSelection(
            residentKey = this.authenticatorSelection.residentKey,
            userVerification = this.authenticatorSelection.userVerification
        ),
        challenge = this.challenge,
        pubKeyCredParams = this.pubKeyCredParams.map { param ->
            com.auth0.android.result.PubKeyCredParam(
                alg = param.alg,
                type = param.type
            )
        },
        relyingParty = com.auth0.android.result.RelyingParty(
            id = this.relyingParty.id,
            name = this.relyingParty.name
        ),
        timeout = this.timeout,
        user = com.auth0.android.result.PasskeyUser(
            displayName = this.user.displayName,
            id = this.user.id,
            name = this.user.name
        )
    )
}

/**
 * Reverse mapper to convert domain PublicKeyCredentials to SDK type for API calls
 */
fun PublicKeyCredentials.toSdkModel(): SdkPublicKeyCredentials {
    return SdkPublicKeyCredentials(
        authenticatorAttachment = this.authenticatorAttachment,
        clientExtensionResults = com.auth0.android.request.ClientExtensionResults(
            credProps = com.auth0.android.request.CredProps(
                rk = this.clientExtensionResults.credProps.rk
            )
        ),
        id = this.id,
        rawId = this.rawId,
        response = com.auth0.android.request.Response(
            attestationObject = this.response.attestationObject,
            authenticatorData = this.response.authenticatorData,
            clientDataJSON = this.response.clientDataJSON,
            transports = this.response.transports,
            signature = this.response.signature,
            userHandle = this.response.userHandle
        ),
        type = this.type
    )
}
