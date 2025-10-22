package com.auth0.android.ui_components.domain.network

import com.auth0.android.ui_components.domain.error.Auth0Error
import com.auth0.android.ui_components.domain.error.ErrorMapper

/**
 * Executes a suspend block with automatic error handling and mapping
 *
 * Catches all exceptions and maps them to Auth0Error types
 * using ErrorMapper for centralized error handling
 *
 * @param execute The suspend block to execute
 * @return Result.Success with data or Result.Error with Auth0Error
 */
suspend inline fun <reified T> safeCall(
    scope: String,
    execute: suspend () -> T
): Result<T, Auth0Error> {
    return try {
        val result = execute()
        Result.Success(result)
    } catch (e: Throwable) {
        val auth0Error = ErrorMapper.mapToAuth0Error(e, scope)
        Result.Error(auth0Error)
    }
}
