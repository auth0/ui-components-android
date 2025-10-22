package com.auth0.android.ui_components.domain.network

import com.auth0.android.ui_components.domain.error.Auth0Error

/**
 * Result wrapper for success/error states with Auth0-specific errors
 */
sealed interface Result<out D, out E : Auth0Error> {
    data class Success<out D>(val data: D) : Result<D, Nothing>
    data class Error<out E : Auth0Error>(val error: E) : Result<Nothing, E>
}


inline fun <T, E : Auth0Error> Result<T, E>.onSuccess(action: (T) -> Unit): Result<T, E> {
    return when (this) {
        is Result.Error -> this
        is Result.Success -> {
            action(data)
            this
        }
    }
}

inline fun <T, E : Auth0Error> Result<T, E>.onError(action: (E) -> Unit): Result<T, E> {
    return when (this) {
        is Result.Error -> {
            action(error)
            this
        }

        is Result.Success -> this
    }
}

typealias EmptyResult<E> = Result<Unit, E>