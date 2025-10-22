package com.auth0.android.ui_components.di.viewmodelfactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.auth0.android.ui_components.domain.usecase.GetMFAMethodsUseCase
import com.auth0.android.ui_components.presentation.viewmodel.AuthenticatorMethodsViewModel

class MFAMethodViewModelFactory(private val getMFAMethodsUseCase: GetMFAMethodsUseCase) :
    ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthenticatorMethodsViewModel::class.java) -> {
                AuthenticatorMethodsViewModel(getMFAMethodsUseCase) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}