package com.auth0.android.ui_components.utils

import android.content.Context
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import com.auth0.android.ui_components.Auth0UI


suspend fun createCredential(
    context: Context,
    authParamsJson: String
): String {
    val credentialsManager =
        Auth0UI.passkeyConfiguration.credentialManager ?: CredentialManager.create(context)
    val request = CreatePublicKeyCredentialRequest(authParamsJson)
    val response = credentialsManager.createCredential(context, request)
    return (response as CreatePublicKeyCredentialResponse).registrationResponseJson
}

