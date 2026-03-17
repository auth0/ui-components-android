package com.auth0.universalcomponents.di.viewmodelfactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.auth0.universalcomponents.domain.model.AuthenticatorType
import com.auth0.universalcomponents.domain.usecase.DeleteAuthenticationMethodUseCase
import com.auth0.universalcomponents.domain.usecase.GetEnrolledAuthenticatorsUseCase
import com.auth0.universalcomponents.presentation.viewmodel.EnrolledAuthenticatorViewModel

/**
 * Factory for creating MFAEnrolledItemViewModel with dependencies
 */
class MFAEnrolledItemViewModelFactory(
    private val getEnrolledAuthenticatorsUseCase: GetEnrolledAuthenticatorsUseCase,
    private val deleteAuthenticationMethodUseCase: DeleteAuthenticationMethodUseCase,
    private val authenticatorType: AuthenticatorType
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(EnrolledAuthenticatorViewModel::class.java) -> {
                EnrolledAuthenticatorViewModel(
                    getEnrolledAuthenticatorsUseCase,
                    deleteAuthenticationMethodUseCase,
                    authenticatorType
                ) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
