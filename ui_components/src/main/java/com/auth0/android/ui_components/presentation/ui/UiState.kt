package com.auth0.android.ui_components.presentation.ui

sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val exception: Throwable) : UiState<Nothing>
}