package com.auth0.universalcomponents.domain.network

import com.auth0.universalcomponents.domain.error.ResultError

/**
 * Result wrapper for success/error states; the error is any [ResultError], such as [com.auth0.universalcomponents.domain.error.Auth0Error]
 */
sealed interface Result<out D, out E : ResultError> {
    data class Success<out D>(val data: D) : Result<D, Nothing>
    data class Error<out E : ResultError>(val error: E) : Result<Nothing, E>
}

inline fun <T, E : ResultError> Result<T, E>.onSuccess(action: (T) -> Unit): Result<T, E> {
    return when (this) {
        is Result.Error -> this
        is Result.Success -> {
            action(data)
            this
        }
    }
}

inline fun <T, E : ResultError> Result<T, E>.onError(action: (E) -> Unit): Result<T, E> {
    return when (this) {
        is Result.Error -> {
            action(error)
            this
        }

        is Result.Success -> this
    }
}

typealias EmptyResult<E> = Result<Unit, E>
