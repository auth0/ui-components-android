package com.auth0.android.ui_components.domain.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

/**
 * The passkey credentials obtained from the [Credential Manager API](https://developer.android.com/identity/sign-in/credential-manager).
 * Used for passkey enrollment verification
 */
@Serializable
data class PublicKeyCredentials(
    val authenticatorAttachment: String,
    val clientExtensionResults: ClientExtensionResults? = null,
    val id: String,
    val rawId: String,
    val response: PublicKeyResponse,
    val type: String
)

/**
 * Client extension results from passkey credential
 */
@Serializable
data class ClientExtensionResults(
    val credProps: CredProps? = null
)

/**
 * Credential properties
 */
@Serializable
data class CredProps(
    val rk: Boolean
)

/**
 * Response data from passkey credential creation
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class PublicKeyResponse(
    val attestationObject: String,
    val authenticatorData: String,
    val clientDataJSON: String,
    val transports: List<String>,
    val signature: String? = null,
    val userHandle: String? = null
)
