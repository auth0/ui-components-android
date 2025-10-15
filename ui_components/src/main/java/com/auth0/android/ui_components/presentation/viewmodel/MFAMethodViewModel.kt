package com.auth0.android.ui_components.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.model.MFAMethod
import com.auth0.android.ui_components.domain.usecase.GetMFAMethodsUseCase
import com.auth0.android.ui_components.domain.util.Result
import com.auth0.android.ui_components.presentation.ui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


data class MFAUiModel(
    val title: String,
    val type: AuthenticatorType, val confirmed: Boolean
)

class MFAMethodViewModel(
    private val getMFAMethodsUseCase: GetMFAMethodsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<MFAUiModel>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<MFAUiModel>>> = _uiState.asStateFlow()


    fun fetchMFAMethods() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = getMFAMethodsUseCase()) {
                is Result.Success -> {
                    _uiState.value = UiState.Success(result.data.map {
                        it.toMFAUiModel()
                    })
                }

                is Result.Error -> {
                    _uiState.value = UiState.Error(
                        result.exception
                    )
                }
            }
        }
    }

    private fun MFAMethod.toMFAUiModel(): MFAUiModel {
        return when (type) {
            AuthenticatorType.TOTP -> MFAUiModel(
                "Authenticator App", type, confirmed
            )

            AuthenticatorType.SMS -> MFAUiModel(
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
