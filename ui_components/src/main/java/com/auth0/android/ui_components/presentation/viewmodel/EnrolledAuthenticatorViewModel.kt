package com.auth0.android.ui_components.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.model.EnrolledAuthenticationMethod
import com.auth0.android.ui_components.domain.network.onError
import com.auth0.android.ui_components.domain.network.onSuccess
import com.auth0.android.ui_components.domain.usecase.DeleteAuthenticationMethodUseCase
import com.auth0.android.ui_components.domain.usecase.GetAuthenticationMethodsUseCase
import com.auth0.android.ui_components.presentation.ui.UiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class EnrolledUiState(
    val loading: Boolean = false,
    val authenticators: List<EnrolledAuthenticationMethod> = emptyList(),
    val uiError: UiError? = null
)

/**
 * ViewModel for MFAEnrolledItem screen
 * Fetches and manages confirmed authentication methods for a specific type
 */
class EnrolledAuthenticatorViewModel(
    private val getAuthenticationMethodsUseCase: GetAuthenticationMethodsUseCase,
    private val deleteAuthenticationMethodUseCase: DeleteAuthenticationMethodUseCase,
    private val authenticatorType: AuthenticatorType
) : ViewModel() {

    private companion object {
        private const val TAG = "MFAEnrolledItemViewModel"
    }

    private val _uiState =
        MutableStateFlow(EnrolledUiState())

    val uiState: StateFlow<EnrolledUiState> = _uiState
        .onStart {
            fetchEnrolledAuthenticators(authenticatorType)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = EnrolledUiState()
        )

    private var cachedAuthenticators: List<EnrolledAuthenticationMethod> = emptyList()

    /**
     * Fetches authentication methods for the specified authenticator type
     * Only returns confirmed methods
     */
    fun fetchEnrolledAuthenticators(authenticatorType: AuthenticatorType) {

        _uiState.update {
            it.copy(loading = true, uiError = null)
        }

        viewModelScope.launch {

            getAuthenticationMethodsUseCase(authenticatorType)
                .onSuccess { data ->
                    cachedAuthenticators = data // Update cache
                    _uiState.update {
                        it.copy(
                            loading = false,
                            authenticators = data
                        )
                    }
                }
                .onError { error ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            uiError = UiError(
                                error, {
                                    fetchEnrolledAuthenticators(authenticatorType)
                                }
                            )
                        )
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

        _uiState.update {
            it.copy(loading = true, uiError = null)
        }

        viewModelScope.launch {

            val updatedList = cachedAuthenticators.filter { it.id != authenticationMethodId }

            deleteAuthenticationMethodUseCase(authenticationMethodId)
                .onSuccess {
                    Log.d(TAG, "Successfully deleted authentication method")
                    cachedAuthenticators = updatedList
                    _uiState.update {
                        it.copy(
                            loading = false,
                            authenticators = cachedAuthenticators

                        )
                    }
                }
                .onError { err ->
                    Log.d(
                        TAG,
                        "Failed to delete authentication method, rolling back",
                        err.cause
                    )
                    _uiState.update {
                        it.copy(
                            loading = false,
                            uiError = UiError(
                                err, {
                                    deleteAuthenticationMethod(authenticationMethodId)
                                }
                            )
                        )
                    }
                }
        }
    }
}
