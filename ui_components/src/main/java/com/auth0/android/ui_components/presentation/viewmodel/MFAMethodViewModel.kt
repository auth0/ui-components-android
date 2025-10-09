package com.auth0.android.ui_components.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.android.ui_components.domain.model.MFAMethod
import com.auth0.android.ui_components.domain.usecase.GetMFAMethodsUseCase
import com.auth0.android.ui_components.domain.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MFAMethodUiState {
    object Idle : MFAMethodUiState()
    object Loading : MFAMethodUiState()
    data class Success(val mfaMethods: List<MFAMethod>) : MFAMethodUiState()
    data class Error(val message: String) : MFAMethodUiState()
}

class MFAMethodViewModel(
    private val getMFAMethodsUseCase: GetMFAMethodsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MFAMethodUiState>(MFAMethodUiState.Idle)
    val uiState: StateFlow<MFAMethodUiState> = _uiState.asStateFlow()


    fun fetchMFAMethods() {
        viewModelScope.launch {
            _uiState.value = MFAMethodUiState.Loading
            when (val result = getMFAMethodsUseCase()) {
                is Result.Success -> {
                    _uiState.value = MFAMethodUiState.Success(result.data)
                }

                is Result.Error -> {
                    _uiState.value = MFAMethodUiState.Error(
                        result.message ?: "Failed to load authenticators"
                    )
                }
            }
        }
    }
}
