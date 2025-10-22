package com.auth0.android.ui_components.presentation

import android.content.Context
import com.auth0.android.result.APICredentials
import com.auth0.android.ui_components.Auth0UI
import com.auth0.android.ui_components.data.TokenManager
import com.auth0.android.ui_components.domain.error.Auth0Error
import com.auth0.android.ui_components.domain.network.Result
import com.auth0.android.ui_components.domain.network.safeCall


suspend fun mfaRecoveryHandler(
    context: Context,
    scope: String,
): Result<APICredentials, Auth0Error> {
    return safeCall(scope) {
        val credentials = Auth0UI.loginHandler
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