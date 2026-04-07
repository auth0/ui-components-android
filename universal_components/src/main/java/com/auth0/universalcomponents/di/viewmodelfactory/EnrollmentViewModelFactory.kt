package com.auth0.universalcomponents.di.viewmodelfactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.auth0.universalcomponents.domain.model.AuthenticatorType
import com.auth0.universalcomponents.domain.usecase.EnrollAuthenticatorUseCase
import com.auth0.universalcomponents.domain.usecase.GetUserInfoUseCase
import com.auth0.universalcomponents.domain.usecase.VerifyAuthenticatorUseCase
import com.auth0.universalcomponents.presentation.viewmodel.EnrollmentViewModel

/**
 * Factory for creating EnrollmentViewModel with dependencies
 */
class EnrollmentViewModelFactory(
    private val enrollAuthenticatorUseCase: EnrollAuthenticatorUseCase,
    private val verifyAuthenticatorUseCase: VerifyAuthenticatorUseCase,
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val authenticatorType: AuthenticatorType,
    private val startDefaultEnrollment: Boolean = true
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(EnrollmentViewModel::class.java) -> {
                EnrollmentViewModel(
                    enrollAuthenticatorUseCase,
                    verifyAuthenticatorUseCase,
                    getUserInfoUseCase,
                    authenticatorType,
                    startDefaultEnrollment
                ) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
