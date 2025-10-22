package com.auth0.android.ui_components.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.model.MFAMethod
import com.auth0.android.ui_components.domain.usecase.GetMFAMethodsUseCase
import com.auth0.android.ui_components.domain.network.onError
import com.auth0.android.ui_components.domain.network.onSuccess
import com.auth0.android.ui_components.presentation.ui.UiError
import com.auth0.android.ui_components.presentation.ui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


data class MFAUiModel(
    val title: String,
    val type: AuthenticatorType,
    val confirmed: Boolean
)

class AuthenticatorMethodsViewModel(
    private val getMFAMethodsUseCase: GetMFAMethodsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<MFAUiModel>>>(UiState.Loading)

    val uiState: StateFlow<UiState<List<MFAUiModel>>> = _uiState
        .onStart {
            fetchMFAMethods()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = UiState.Loading
        )


    fun fetchMFAMethods() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            getMFAMethodsUseCase()
                .onSuccess {
                    _uiState.value = UiState.Success(it.map {
                        it.toMFAUiModel()
                    })
                }
                .onError {
                    _uiState.value = UiState.Error(
                        UiError(it, { fetchMFAMethods() })
                    )
                }
        }
    }

    private fun MFAMethod.toMFAUiModel(): MFAUiModel {
        return when (type) {
            AuthenticatorType.TOTP -> MFAUiModel(
                "Authenticator App", type, confirmed
            )

            AuthenticatorType.PHONE -> MFAUiModel(
                "SMS OTP", type, confirmed
            )

            AuthenticatorType.EMAIL -> MFAUiModel(
                "Email OTP", type, confirmed
            )

            AuthenticatorType.PUSH -> MFAUiModel(
                "Push Notification", type, confirmed
            )

            AuthenticatorType.RECOVERY_CODE -> MFAUiModel(
                "Recovery Code", type, confirmed
            )
        }

    }
}
