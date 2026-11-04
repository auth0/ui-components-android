package com.auth0.universalcomponents.feature.embedded.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.auth0.universalcomponents.feature.embedded.domain.repository.EmbeddedLoginRepository
import com.auth0.universalcomponents.feature.embedded.presentation.LoginFlowViewModel
import com.auth0.universalcomponents.feature.mfachallenge.domain.repository.MfaRepository

internal class LoginFlowViewModelFactory(
    private val repository: EmbeddedLoginRepository,
    private val mfaRepository: MfaRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginFlowViewModel::class.java) ->
                LoginFlowViewModel(repository, mfaRepository) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
