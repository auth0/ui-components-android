package com.auth0.universalcomponents.feature.embedded.signup.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.auth0.universalcomponents.feature.embedded.signup.domain.repository.EmbeddedSignupRepository
import com.auth0.universalcomponents.feature.embedded.signup.presentation.SignupViewModel

/**
 * [createRepository] runs only when the ViewModel is first created, so a screen that asks for the factory again, as
 * every recomposition does, never builds an SDK client it won't use.
 */
internal class SignupViewModelFactory(
    private val createRepository: () -> EmbeddedSignupRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SignupViewModel::class.java) -> SignupViewModel(createRepository()) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
