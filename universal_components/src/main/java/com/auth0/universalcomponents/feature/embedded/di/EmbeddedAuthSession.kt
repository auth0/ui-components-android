package com.auth0.universalcomponents.feature.embedded.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.auth0.universalcomponents.feature.embedded.data.repository.EmbeddedAuthSteps

/**
 * Holds the [steps] of one login flow: its SDK client, which keeps the session, and the one-step-at-a-time rule.
 * The first factor and MFA must share them; as a ViewModel it lives as long as the login screen, through rotations.
 */
internal class EmbeddedAuthSession(val steps: EmbeddedAuthSteps) : ViewModel() {

    class Factory(private val steps: EmbeddedAuthSteps) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when {
                modelClass.isAssignableFrom(EmbeddedAuthSession::class.java) -> EmbeddedAuthSession(steps) as T

                else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
