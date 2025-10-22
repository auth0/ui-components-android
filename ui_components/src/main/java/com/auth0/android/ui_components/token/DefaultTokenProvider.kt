package com.auth0.android.ui_components.token

import com.auth0.android.authentication.storage.BaseCredentialsManager
import com.auth0.android.result.APICredentials
import com.auth0.android.result.Credentials

public class DefaultTokenProvider(private val credentialsManager: BaseCredentialsManager) :
    TokenProvider {
    override suspend fun saveApiCredentials(
        audience: String,
        credentials: APICredentials
    ) {
        credentialsManager.saveApiCredentials(credentials, audience)
    }

    override suspend fun fetchCredentials(): Credentials {
        return credentialsManager.awaitCredentials()
    }

    override suspend fun fetchApiCredentials(
        audience: String,
        scope: String?
    ): APICredentials {
        return credentialsManager.awaitApiCredentials(audience, scope)
    }
}