package com.auth0.android.ui_components.di

import com.auth0.android.ui_components.data.MyAccountProvider
import com.auth0.android.ui_components.data.TokenManager
import com.auth0.android.ui_components.data.repository.MyAccountRepositoryImpl
import com.auth0.android.ui_components.di.viewmodelfactory.EnrollmentViewModelFactory
import com.auth0.android.ui_components.di.viewmodelfactory.MFAEnrolledItemViewModelFactory
import com.auth0.android.ui_components.di.viewmodelfactory.MFAMethodViewModelFactory
import com.auth0.android.ui_components.domain.DispatcherProvider
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.repository.MyAccountRepository
import com.auth0.android.ui_components.domain.usecase.DeleteAuthenticationMethodUseCase
import com.auth0.android.ui_components.domain.usecase.EnrollAuthenticatorUseCase
import com.auth0.android.ui_components.domain.usecase.GetAuthenticationMethodsUseCase
import com.auth0.android.ui_components.domain.usecase.GetMFAMethodsUseCase
import com.auth0.android.ui_components.domain.usecase.VerifyAuthenticatorUseCase
import com.auth0.android.ui_components.helper.DispatcherProviderImpl

object MyAccountModule {


    //Viewmodel factories
    fun provideMFAMethodViewModelFactory(): MFAMethodViewModelFactory {
        return MFAMethodViewModelFactory(
            getMFAMethodsUseCase = provideGetMFAMethodsUseCase()
        )
    }

    fun provideMFAEnrolledItemViewModelFactory(authenticatorType: AuthenticatorType): MFAEnrolledItemViewModelFactory {
        return MFAEnrolledItemViewModelFactory(
            getAuthenticationMethodsUseCase = provideGetAuthenticationMethodsUseCase(),
            deleteAuthenticationMethodUseCase = provideDeleteAuthenticationMethodUseCase(),
            authenticatorType = authenticatorType
        )
    }

    /**
     * Provides EnrollmentViewModelFactory for creating EnrollmentViewModel
     */
    fun provideEnrollmentViewModelFactory(authenticatorType: AuthenticatorType): EnrollmentViewModelFactory {
        return EnrollmentViewModelFactory(
            enrollAuthenticatorUseCase = provideEnrollAuthenticatorUseCase(),
            verifyAuthenticatorUseCase = provideVerifyAuthenticatorUseCase(),
            authenticatorType
        )
    }

    // Usecases
    private fun provideGetMFAMethodsUseCase(): GetMFAMethodsUseCase {
        return GetMFAMethodsUseCase(
            repository = provideMyAccountRepository(),
            tokenManager = provideTokenManager(),
            dispatcherProvider = provideDispatcherProvider()
        )
    }

    private fun provideGetAuthenticationMethodsUseCase(): GetAuthenticationMethodsUseCase {
        return GetAuthenticationMethodsUseCase(
            repository = provideMyAccountRepository(),
            tokenManager = provideTokenManager(),
            dispatcherProvider = provideDispatcherProvider()
        )
    }

    private fun provideDeleteAuthenticationMethodUseCase(): DeleteAuthenticationMethodUseCase {
        return DeleteAuthenticationMethodUseCase(
            repository = provideMyAccountRepository(),
            tokenManager = provideTokenManager(),
            dispatcherProvider = provideDispatcherProvider()
        )
    }

    /**
     * Provides EnrollAuthenticatorUseCase for enrollment operations
     */
    private fun provideEnrollAuthenticatorUseCase(): EnrollAuthenticatorUseCase {
        return EnrollAuthenticatorUseCase(
            repository = provideMyAccountRepository(),
            tokenManager = provideTokenManager(),
            dispatcherProvider = provideDispatcherProvider()
        )
    }

    /**
     * Provides VerifyAuthenticatorUseCase for verification operations
     */
    private fun provideVerifyAuthenticatorUseCase(): VerifyAuthenticatorUseCase {
        return VerifyAuthenticatorUseCase(
            repository = provideMyAccountRepository(),
            tokenManager = provideTokenManager(),
            dispatcherProvider = provideDispatcherProvider()
        )
    }

    private fun provideMyAccountRepository(): MyAccountRepository {
        return MyAccountRepositoryImpl(MyAccountProvider())
    }

    private fun provideTokenManager(): TokenManager {
        return TokenManager.getInstance()
    }

    private fun provideDispatcherProvider(): DispatcherProvider {
        return DispatcherProviderImpl()
    }

}
