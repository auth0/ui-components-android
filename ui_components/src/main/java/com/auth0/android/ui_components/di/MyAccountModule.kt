package com.auth0.android.ui_components.di

import com.auth0.android.ui_components.data.MyAccountProvider
import com.auth0.android.ui_components.data.TokenManager
import com.auth0.android.ui_components.data.repository.MyAccountRepositoryImpl
import com.auth0.android.ui_components.di.viewmodelfactory.AuthenticatorMethodViewModelFactory
import com.auth0.android.ui_components.di.viewmodelfactory.EnrollmentViewModelFactory
import com.auth0.android.ui_components.di.viewmodelfactory.MFAEnrolledItemViewModelFactory
import com.auth0.android.ui_components.domain.DispatcherProvider
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.repository.MyAccountRepository
import com.auth0.android.ui_components.domain.usecase.DeleteAuthenticationMethodUseCase
import com.auth0.android.ui_components.domain.usecase.EnrollAuthenticatorUseCase
import com.auth0.android.ui_components.domain.usecase.GetAuthenticationMethodsUseCase
import com.auth0.android.ui_components.domain.usecase.GetEnabledAuthenticatorMethodsUseCase
import com.auth0.android.ui_components.domain.usecase.VerifyAuthenticatorUseCase
import com.auth0.android.ui_components.helper.DispatcherProviderImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

object MyAccountModule {


    //Viewmodel factories
    fun provideAuthenticatorMethodViewModelFactory(): AuthenticatorMethodViewModelFactory {
        return AuthenticatorMethodViewModelFactory(
            getEnabledAuthenticatorMethodsUseCase = provideEnabledAuthenticatorMethodsUseCase()
        )
    }

    fun provideMFAEnrolledItemViewModelFactory(authenticatorType: AuthenticatorType): MFAEnrolledItemViewModelFactory {
        return MFAEnrolledItemViewModelFactory(
            getAuthenticationMethodsUseCase = provideGetAuthenticationMethodsUseCase(),
            deleteAuthenticationMethodUseCase = provideDeleteAuthenticationMethodUseCase(),
            authenticatorType = authenticatorType
        )
    }

    fun provideEnrollmentViewModelFactory(
        authenticatorType: AuthenticatorType,
        startDefaultEnrollment: Boolean = true
    ): EnrollmentViewModelFactory {
        return EnrollmentViewModelFactory(
            enrollAuthenticatorUseCase = provideEnrollAuthenticatorUseCase(),
            verifyAuthenticatorUseCase = provideVerifyAuthenticatorUseCase(),
            authenticatorType, startDefaultEnrollment
        )
    }

    //Use cases
    private fun provideEnabledAuthenticatorMethodsUseCase(): GetEnabledAuthenticatorMethodsUseCase {
        return GetEnabledAuthenticatorMethodsUseCase(
            repository = provideMyAccountRepository(),
            dispatcherProvider = provideDispatcherProvider(),
            backgroundScope = CoroutineScope(Job())
        )
    }

    private fun provideGetAuthenticationMethodsUseCase(): GetAuthenticationMethodsUseCase {
        return GetAuthenticationMethodsUseCase(
            repository = provideMyAccountRepository(),
            dispatcherProvider = provideDispatcherProvider()
        )
    }

    private fun provideDeleteAuthenticationMethodUseCase(): DeleteAuthenticationMethodUseCase {
        return DeleteAuthenticationMethodUseCase(
            repository = provideMyAccountRepository(),
            dispatcherProvider = provideDispatcherProvider()
        )
    }


    private fun provideEnrollAuthenticatorUseCase(): EnrollAuthenticatorUseCase {
        return EnrollAuthenticatorUseCase(
            repository = provideMyAccountRepository(),
            dispatcherProvider = provideDispatcherProvider()
        )
    }

    private fun provideVerifyAuthenticatorUseCase(): VerifyAuthenticatorUseCase {
        return VerifyAuthenticatorUseCase(
            repository = provideMyAccountRepository(),
            dispatcherProvider = provideDispatcherProvider()
        )
    }

    private fun provideMyAccountRepository(): MyAccountRepository {
        return MyAccountRepositoryImpl(MyAccountProvider(), provideTokenManager())
    }

    private fun provideTokenManager(): TokenManager {
        return TokenManager.getInstance()
    }

    private fun provideDispatcherProvider(): DispatcherProvider {
        return DispatcherProviderImpl()
    }

}
