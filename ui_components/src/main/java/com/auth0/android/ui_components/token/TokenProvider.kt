package com.auth0.android.ui_components.token

import com.auth0.android.result.APICredentials
import com.auth0.android.result.Credentials


interface TokenProvider {

    suspend fun fetchCredentials(): Credentials

    suspend fun fetchApiCredentials(audience: String, scope: String? = null): APICredentials

    suspend fun saveApiCredentials(audience: String, credentials: APICredentials)
}