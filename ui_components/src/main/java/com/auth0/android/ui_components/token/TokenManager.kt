package com.auth0.android.ui_components.token

import com.auth0.android.result.APICredentials
import com.auth0.android.result.Credentials


interface TokenManager {

    suspend fun fetchCredentials(): Credentials

    suspend fun fetchApiCredentials(audience: String, scope: String): APICredentials

    suspend fun saveCredentials(audience: String, credentials: APICredentials)


}