package com.auth0.android.ui_components.domain.repository

import com.auth0.android.result.AuthenticationMethod
import com.auth0.android.result.Factor

/**
 * Repository interface for MyAccount operations
 * Methods accept access token - no token management here
 */
interface MyAccountRepository {
    suspend fun getFactors(accessToken: String): List<Factor>
    suspend fun getAuthenticatorMethods(accessToken: String): List<AuthenticationMethod>
}
