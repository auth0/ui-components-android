package com.auth0.android.ui_components.presentation.ui

import com.auth0.android.ui_components.domain.error.Auth0Error

/**
 * Universal UI State that works for all ViewModels
 * Uses Auth0Error instead of Throwable for type-safe error handling
 */
sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val error: UiError) : UiState<Nothing>
}


data class UiError(val error: Auth0Error, val onRetry: () -> Unit)