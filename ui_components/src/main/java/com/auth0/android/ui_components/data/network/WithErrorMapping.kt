package com.auth0.android.ui_components.data.network

import com.auth0.android.ui_components.data.mapper.ErrorMapper


/**
 * Executes a suspend request and maps exceptions to Auth0Error
 *
 * @param execute The suspend block to execute
 * @throws [com.auth0.android.ui_components.domain.error.Auth0Error]
 */
internal suspend inline fun <reified T> withErrorMapping(
    scope: String,
    execute: suspend () -> T
): T {
    return try {
        execute()
    } catch (e: Throwable) {
        throw ErrorMapper.mapToAuth0Error(e, scope)
    }
}