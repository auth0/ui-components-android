package com.auth0.android.ui_components.di

import com.auth0.android.ui_components.data.TokenManager
import com.auth0.android.ui_components.data.repository.MyAccountRepositoryImpl
import com.auth0.android.ui_components.di.viewmodelfactory.MFAMethodViewModelFactory
import com.auth0.android.ui_components.domain.DispatcherProvider
import com.auth0.android.ui_components.domain.repository.MyAccountRepository
import com.auth0.android.ui_components.domain.usecase.GetMFAMethodsUseCase
import com.auth0.android.ui_components.helper.DispatcherProviderImpl

object MyAccountModule {

    fun provideMFAMethodViewModelFactory(): MFAMethodViewModelFactory {
        return MFAMethodViewModelFactory(
            getMFAMethodsUseCase = GetMFAMethodsUseCase(
                repository = provideMyAccountRepository(),
                dispatcherProvider = provideDispatcherProvider()
            )
        )
    }

    private fun provideMyAccountRepository(): MyAccountRepository {
        return MyAccountRepositoryImpl(provideTokenManager())
    }


    private fun provideTokenManager(): TokenManager {
        return TokenManager()
    }

    private fun provideDispatcherProvider(): DispatcherProvider {
        return DispatcherProviderImpl()
    }

}
