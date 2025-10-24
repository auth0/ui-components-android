package com.auth0.android.ui_components.di.viewmodelfactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.usecase.DeleteAuthenticationMethodUseCase
import com.auth0.android.ui_components.domain.usecase.GetAuthenticationMethodsUseCase
import com.auth0.android.ui_components.presentation.viewmodel.EnrolledAuthenticatorViewModel

/**
 * Factory for creating MFAEnrolledItemViewModel with dependencies
 */
class MFAEnrolledItemViewModelFactory(
    private val getAuthenticationMethodsUseCase: GetAuthenticationMethodsUseCase,
    private val deleteAuthenticationMethodUseCase: DeleteAuthenticationMethodUseCase,
    private val authenticatorType: AuthenticatorType
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(EnrolledAuthenticatorViewModel::class.java) -> {
                EnrolledAuthenticatorViewModel(
                    getAuthenticationMethodsUseCase,
                    deleteAuthenticationMethodUseCase,
                    authenticatorType
                ) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
