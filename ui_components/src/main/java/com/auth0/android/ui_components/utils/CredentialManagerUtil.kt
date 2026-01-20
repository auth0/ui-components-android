package com.auth0.android.ui_components.utils

import android.content.Context
import androidx.credentials.CreateCredentialRequest
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CredentialManager
import com.auth0.android.ui_components.Auth0UI


suspend fun createCredential(
    context: Context,
    request: CreateCredentialRequest
): CreateCredentialResponse {
    val credentialsManager =
        Auth0UI.passkeyConfiguration.credentialManager ?: CredentialManager.create(context)
    return credentialsManager.createCredential(context, request)
}

