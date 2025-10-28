package com.auth0.android.ui_components.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.network.onError
import com.auth0.android.ui_components.domain.network.onSuccess
import com.auth0.android.ui_components.domain.usecase.GetEnabledAuthenticatorMethodsUseCase
import com.auth0.android.ui_components.presentation.ui.UiError
import com.auth0.android.ui_components.presentation.ui.utils.toAuthenticatorUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


/**
 * Data class representing the UI data for different authenticator methods
 * @property title Display title for the authenticator method
 * @property type [AuthenticatorType] for the authenticator
 * @property confirmed Whether the authenticator has been enrolled or not
 */
data class AuthenticatorUiData(
    val title: String,
    val type: AuthenticatorType,
    val confirmed: Boolean
)

/**
 * Sealed interface representing the different UI states for authenticator methods
 */
sealed interface AuthenticatorUiState {
    object Loading : AuthenticatorUiState
    data class Success(val data: List<AuthenticatorUiData>) : AuthenticatorUiState
    data class Error(val error: UiError) : AuthenticatorUiState
}

class AuthenticatorMethodsViewModel(
    private val getEnabledAuthenticatorMethodsUseCase: GetEnabledAuthenticatorMethodsUseCase,
) : ViewModel() {

    private val _uiState: MutableStateFlow<AuthenticatorUiState> =
        MutableStateFlow(AuthenticatorUiState.Loading)

    val uiState: StateFlow<AuthenticatorUiState> = _uiState
        .onStart {
            fetchAuthenticatorMethods()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = AuthenticatorUiState.Loading
        )


    fun fetchAuthenticatorMethods() {
        viewModelScope.launch {
            _uiState.value = AuthenticatorUiState.Loading

            getEnabledAuthenticatorMethodsUseCase()
                .onSuccess {
                    _uiState.value = AuthenticatorUiState.Success(it.map { data ->
                        data.toAuthenticatorUiModel()
                    })
                }
                .onError {
                    _uiState.value = AuthenticatorUiState.Error(
                        UiError(it, { fetchAuthenticatorMethods() })
                    )
                }
        }
    }
}
