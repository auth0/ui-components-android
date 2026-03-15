package com.auth0.universalcomponents.presentation

import android.content.Context
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.APICredentials
import com.auth0.universalcomponents.Auth0UI
import com.auth0.universalcomponents.data.TokenManager
import com.auth0.universalcomponents.domain.error.Auth0Error
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.domain.network.safeCall


suspend fun mfaRecoveryHandler(
    context: Context,
    scope: String,
): Result<APICredentials, Auth0Error> {
    return safeCall {
        val credentials = WebAuthProvider.login(Auth0UI.account)
            .withScheme(Auth0UI.scheme)
            .withScope(scope)
            .withAudience(TokenManager.getInstance().getMyAccountAudience())
            .await(context)
        APICredentials(
            credentials.accessToken,
            credentials.type,
            credentials.expiresAt,
            credentials.scope ?: "",
        )
    }
}