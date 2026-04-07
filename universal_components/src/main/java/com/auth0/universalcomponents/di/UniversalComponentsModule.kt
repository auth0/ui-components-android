package com.auth0.universalcomponents.di

import com.auth0.universalcomponents.Auth0UniversalComponents
import com.auth0.universalcomponents.data.MyAccountProvider
import com.auth0.universalcomponents.data.TokenManager
import com.auth0.universalcomponents.data.repository.MyAccountRepositoryImpl
import com.auth0.universalcomponents.data.repository.UserRepositoryImpl
import com.auth0.universalcomponents.di.viewmodelfactory.AuthenticatorMethodViewModelFactory
import com.auth0.universalcomponents.di.viewmodelfactory.EnrollmentViewModelFactory
import com.auth0.universalcomponents.di.viewmodelfactory.MFAEnrolledItemViewModelFactory
import com.auth0.universalcomponents.di.viewmodelfactory.PasskeyViewModelFactory
import com.auth0.universalcomponents.domain.DispatcherProvider
import com.auth0.universalcomponents.domain.model.AuthenticatorType
import com.auth0.universalcomponents.helper.DispatcherProviderImpl
import com.auth0.universalcomponents.domain.repository.MyAccountRepository
import com.auth0.universalcomponents.domain.repository.UserRepository
import com.auth0.universalcomponents.domain.usecase.DeleteAuthenticationMethodUseCase
import com.auth0.universalcomponents.domain.usecase.EnrollAuthenticatorUseCase
import com.auth0.universalcomponents.domain.usecase.GetEnabledAuthenticatorMethodsUseCase
import com.auth0.universalcomponents.domain.usecase.GetEnrolledAuthenticatorsUseCase
import com.auth0.universalcomponents.domain.usecase.GetUserInfoUseCase
import com.auth0.universalcomponents.domain.usecase.VerifyAuthenticatorUseCase

object UniversalComponentsModule {


    //Viewmodel factories
    fun provideAuthenticatorMethodViewModelFactory(): AuthenticatorMethodViewModelFactory {
        return AuthenticatorMethodViewModelFactory(
            getEnabledAuthenticatorMethodsUseCase = provideEnabledAuthenticatorMethodsUseCase()
        )
    }

    fun provideMFAEnrolledItemViewModelFactory(authenticatorType: AuthenticatorType): MFAEnrolledItemViewModelFactory {
        return MFAEnrolledItemViewModelFactory(
            getEnrolledAuthenticatorsUseCase = provideGetEnrolledAuthenticatorsUseCase(),
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
            getUserInfoUseCase = provideGetUserInfoUseCase(),
            authenticatorType = authenticatorType,
            startDefaultEnrollment = startDefaultEnrollment
        )
    }

    fun providePasskeyViewModelFactory(): PasskeyViewModelFactory {
        return PasskeyViewModelFactory(
            repository = provideMyAccountRepository(),
            passkeyConfiguration = com.auth0.universalcomponents.Auth0UniversalComponents.passkeyConfiguration,
        )
    }

    //Use cases
    private fun provideEnabledAuthenticatorMethodsUseCase(): GetEnabledAuthenticatorMethodsUseCase {
        return GetEnabledAuthenticatorMethodsUseCase(
            repository = provideMyAccountRepository(),
            dispatcherProvider = provideDispatcherProvider(),
        )
    }

    private fun provideGetEnrolledAuthenticatorsUseCase(): GetEnrolledAuthenticatorsUseCase {
        return GetEnrolledAuthenticatorsUseCase(
            repository = provideMyAccountRepository()
        )
    }

    private fun provideDeleteAuthenticationMethodUseCase(): DeleteAuthenticationMethodUseCase {
        return DeleteAuthenticationMethodUseCase(
            repository = provideMyAccountRepository()
        )
    }


    private fun provideEnrollAuthenticatorUseCase(): EnrollAuthenticatorUseCase {
        return EnrollAuthenticatorUseCase(
            repository = provideMyAccountRepository()
        )
    }

    private fun provideVerifyAuthenticatorUseCase(): VerifyAuthenticatorUseCase {
        return VerifyAuthenticatorUseCase(
            repository = provideMyAccountRepository()
        )
    }

    private fun provideMyAccountRepository(): MyAccountRepository {
        return MyAccountRepositoryImpl(MyAccountProvider(), provideTokenManager())
    }

    private fun provideCredentialsRepository(): UserRepository {
        return UserRepositoryImpl(Auth0UniversalComponents.tokenProvider)
    }

    fun provideGetUserInfoUseCase(): GetUserInfoUseCase {
        return GetUserInfoUseCase(
            userRepository = provideCredentialsRepository()
        )
    }

    private fun provideTokenManager(): TokenManager {
        return TokenManager.getInstance()
    }

    private fun provideDispatcherProvider(): DispatcherProvider {
        return DispatcherProviderImpl()
    }

}
