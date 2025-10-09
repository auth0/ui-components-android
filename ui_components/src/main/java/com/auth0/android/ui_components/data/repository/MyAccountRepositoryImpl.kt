package com.auth0.android.ui_components.data.repository

import com.auth0.android.result.AuthenticationMethod
import com.auth0.android.result.Factor
import com.auth0.android.result.Factors
import com.auth0.android.ui_components.data.TokenManager
import com.auth0.android.ui_components.domain.repository.MyAccountRepository
import com.auth0.android.ui_components.domain.util.Result

class MyAccountRepositoryImpl(
    private val tokenManager: TokenManager
) : MyAccountRepository {

    override suspend fun getFactors(): Result<List<Factor>> {
        TODO("Not yet implemented")
    }

    override suspend fun getAuthenticatorMethods(): Result<List<AuthenticationMethod>> {
        TODO("Not yet implemented")
    }
}
