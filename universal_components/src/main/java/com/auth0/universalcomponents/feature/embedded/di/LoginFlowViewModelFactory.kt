package com.auth0.universalcomponents.feature.embedded.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.auth0.universalcomponents.feature.embedded.domain.repository.AuthInteracting
import com.auth0.universalcomponents.feature.embedded.presentation.LoginFlowViewModel

internal class LoginFlowViewModelFactory(
    private val interactor: () -> AuthInteracting,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginFlowViewModel::class.java) -> LoginFlowViewModel(interactor()) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
