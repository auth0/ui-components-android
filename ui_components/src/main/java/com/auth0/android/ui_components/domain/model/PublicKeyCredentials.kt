package com.auth0.android.ui_components.domain.model

/**
 * The passkey credentials obtained from the [Credential Manager API](https://developer.android.com/identity/sign-in/credential-manager).
 * Used for passkey enrollment verification
 */
data class PublicKeyCredentials(
    val authenticatorAttachment: String,
    val clientExtensionResults: ClientExtensionResults,
    val id: String,
    val rawId: String,
    val response: PublicKeyResponse,
    val type: String
)

/**
 * Client extension results from passkey credential
 */
data class ClientExtensionResults(
    val credProps: CredProps
)

/**
 * Credential properties
 */
data class CredProps(
    val rk: Boolean
)

/**
 * Response data from passkey credential creation
 */
data class PublicKeyResponse(
    val attestationObject: String,
    val authenticatorData: String,
    val clientDataJSON: String,
    val transports: List<String>,
    val signature: String,
    val userHandle: String
)
