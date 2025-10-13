package com.auth0.android.ui_components.domain.util

/**
 * Simple Result wrapper for success/error states
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}
