package com.auth0.universalcomponents.utils

import android.content.Context
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import com.auth0.universalcomponents.Auth0UI


suspend fun createCredential(
    context: Context,
    authParamsJson: String
): String {
    val credentialsManager =
        Auth0UI.passkeyConfiguration.credentialManager ?: CredentialManager.create(context)
    val request = CreatePublicKeyCredentialRequest(authParamsJson)
    val response = credentialsManager.createCredential(context, request)
    val publicKeyResponse = response as? CreatePublicKeyCredentialResponse
        ?: throw IllegalStateException("Unexpected credential response type: ${response::class.java.name}")
    return publicKeyResponse.registrationResponseJson
}

