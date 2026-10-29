package com.auth0.universalcomponents.feature.embedded.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.embedded.domain.repository.MfaRepository
import com.auth0.universalcomponents.feature.embedded.presentation.mfa.MfaViewModel

/** Builds the MFA ViewModel for the factors the server offered. */
internal class MfaViewModelFactory(
    private val factors: List<MfaFactor>,
    private val repository: MfaRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MfaViewModel::class.java) -> MfaViewModel(factors, repository) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
