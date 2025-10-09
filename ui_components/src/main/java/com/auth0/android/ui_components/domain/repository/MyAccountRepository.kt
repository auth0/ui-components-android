package com.auth0.android.ui_components.domain.repository

import com.auth0.android.result.AuthenticationMethod
import com.auth0.android.result.Factor
import com.auth0.android.ui_components.domain.util.Result

interface MyAccountRepository {
    suspend fun getFactors(): Result<List<Factor>>
    suspend fun getAuthenticatorMethods(): Result<List<AuthenticationMethod>>
}
