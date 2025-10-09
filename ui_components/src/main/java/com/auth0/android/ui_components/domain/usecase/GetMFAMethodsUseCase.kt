package com.auth0.android.ui_components.domain.usecase

import com.auth0.android.result.AuthenticationMethod
import com.auth0.android.result.Factor
import com.auth0.android.ui_components.domain.DispatcherProvider
import com.auth0.android.ui_components.domain.model.MFAMethod
import com.auth0.android.ui_components.domain.repository.MyAccountRepository
import com.auth0.android.ui_components.domain.util.Result
import kotlinx.coroutines.withContext

class GetMFAMethodsUseCase(
    private val repository: MyAccountRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend operator fun invoke(): Result<List<MFAMethod>> {
        withContext(dispatcherProvider.io) {
            val factors = repository.getFactors()
            val authenticationMethods = repository.getAuthenticatorMethods()
        }
        return Result.Success(emptyList<MFAMethod>())
    }

    private fun mapApiResponseToMFAMethods(
        factors: List<Factor>,
        authMethods: List<AuthenticationMethod>
    ): List<MFAMethod> {
        val mfaMethods = mutableListOf<MFAMethod>()

        return mfaMethods
    }
}
