package com.auth0.universalcomponents.di.viewmodelfactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.auth0.universalcomponents.PasskeyConfiguration
import com.auth0.universalcomponents.domain.repository.MyAccountRepository
import com.auth0.universalcomponents.presentation.ui.passkeys.PasskeyViewModel

/**
 * Factory for creating PasskeyViewModel with dependencies
 */
class PasskeyViewModelFactory(
    private val repository: MyAccountRepository,
    private val passkeyConfiguration: PasskeyConfiguration,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(PasskeyViewModel::class.java) -> {
                PasskeyViewModel(
                    myAccountRepository = repository,
                    passkeyConfiguration = passkeyConfiguration
                ) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
