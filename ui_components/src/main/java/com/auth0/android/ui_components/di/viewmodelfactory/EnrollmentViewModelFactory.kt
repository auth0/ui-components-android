package com.auth0.android.ui_components.di.viewmodelfactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.auth0.android.ui_components.domain.usecase.EnrollAuthenticatorUseCase
import com.auth0.android.ui_components.domain.usecase.VerifyAuthenticatorUseCase
import com.auth0.android.ui_components.presentation.viewmodel.EnrollmentViewModel

/**
 * Factory for creating EnrollmentViewModel with dependencies
 */
class EnrollmentViewModelFactory(
    private val enrollAuthenticatorUseCase: EnrollAuthenticatorUseCase,
    private val verifyAuthenticatorUseCase: VerifyAuthenticatorUseCase
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(EnrollmentViewModel::class.java) -> {
                EnrollmentViewModel(
                    enrollAuthenticatorUseCase,
                    verifyAuthenticatorUseCase
                ) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
