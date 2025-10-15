package com.auth0.android.ui_components.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.model.EnrolledAuthenticationMethod
import com.auth0.android.ui_components.domain.usecase.DeleteAuthenticationMethodUseCase
import com.auth0.android.ui_components.domain.usecase.GetAuthenticationMethodsUseCase
import com.auth0.android.ui_components.domain.util.Result
import com.auth0.android.ui_components.presentation.ui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for MFAEnrolledItem screen
 * Fetches and manages confirmed authentication methods for a specific type
 */
class MFAEnrolledItemViewModel(
    private val getAuthenticationMethodsUseCase: GetAuthenticationMethodsUseCase,
    private val deleteAuthenticationMethodUseCase: DeleteAuthenticationMethodUseCase,
) : ViewModel() {

    private companion object {
        private const val TAG = "MFAEnrolledItemViewModel"
    }

    private val _uiState =
        MutableStateFlow<UiState<List<EnrolledAuthenticationMethod>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<EnrolledAuthenticationMethod>>> = _uiState.asStateFlow()

    private var cachedAuthenticators: List<EnrolledAuthenticationMethod> = emptyList()

    /**
     * Fetches authentication methods for the specified authenticator type
     * Only returns confirmed methods
     */
    fun fetchEnrolledAuthenticators(authenticatorType: AuthenticatorType) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            when (val result = getAuthenticationMethodsUseCase(authenticatorType)) {
                is Result.Success -> {
                    Log.d(TAG, "fetchEnrolledAuthenticators: ${result.data} ")
                    cachedAuthenticators = result.data // Update cache
                    _uiState.value = UiState.Success(result.data)
                }

                is Result.Error -> {
                    _uiState.value = UiState.Error(result.exception)
                }
            }
        }
    }

    /**
     * Deletes an authentication method and updates the list
     *
     * @param authenticationMethodId The ID of the authentication method to delete
     */
    fun deleteAuthenticationMethod(authenticationMethodId: String) {
        viewModelScope.launch {

            val updatedList = cachedAuthenticators.filter { it.id != authenticationMethodId }
            _uiState.value = UiState.Loading

            when (val result = deleteAuthenticationMethodUseCase(authenticationMethodId)) {
                is Result.Success -> {
                    Log.d(TAG, "Successfully deleted authentication method")
                    cachedAuthenticators = updatedList
                    _uiState.value = UiState.Success(cachedAuthenticators)
                }

                is Result.Error -> {
                    Log.e(
                        TAG,
                        "Failed to delete authentication method, rolling back",
                        result.exception
                    )
                    _uiState.value = UiState.Error(result.exception)
                }
            }
        }
    }
}
