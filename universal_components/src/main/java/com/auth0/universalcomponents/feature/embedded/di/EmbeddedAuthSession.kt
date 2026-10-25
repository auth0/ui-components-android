package com.auth0.universalcomponents.feature.embedded.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.auth0.android.embedded.EmbeddedAuthClient
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.embedded.domain.repository.MfaRepository
import com.auth0.universalcomponents.feature.embedded.presentation.mfa.MfaViewModel

/**
 * Holds the one SDK client of a login flow. The client keeps the session, so the first factor and MFA must share it;
 * as a ViewModel it lives as long as the login screen, through rotations.
 */
internal class EmbeddedAuthSession(val client: EmbeddedAuthClient) : ViewModel() {

    class Factory(private val client: () -> EmbeddedAuthClient) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = EmbeddedAuthSession(client()) as T
    }
}

/** Builds the MFA ViewModel for the factors the server offered. */
internal class MfaViewModelFactory(
    private val factors: List<MfaFactor>,
    private val repository: () -> MfaRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MfaViewModel(factors, repository()) as T
}
