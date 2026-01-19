package com.auth0.android.ui_components.di.viewmodelfactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.auth0.android.ui_components.domain.repository.MyAccountRepository
import com.auth0.android.ui_components.presentation.ui.passkeys.PasskeyViewModel

/**
 * Factory for creating PasskeyViewModel with dependencies
 */
class PasskeyViewModelFactory(
    private val repository: MyAccountRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(PasskeyViewModel::class.java) -> {
                PasskeyViewModel(
                    myAccountRepository = repository
                ) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
