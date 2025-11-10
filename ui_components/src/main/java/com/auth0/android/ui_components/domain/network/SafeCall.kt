package com.auth0.android.ui_components.domain.network

import com.auth0.android.ui_components.domain.error.Auth0Error

/**
 * Executes a suspend block and maps them to [Result] type
 *
 *
 * @param execute The suspend block to execute
 * @return Result.Success with data or Result.Error with Auth0Error
 */
suspend inline fun <reified T> safeCall(
    execute: suspend () -> T
): Result<T, Auth0Error> {
    return try {
        val result = execute()
        Result.Success(result)
    } catch (e: Auth0Error) {
        Result.Error(e)
    }
}
