package com.auth0.universalcomponents.data.network

import com.auth0.universalcomponents.data.mapper.ErrorMapper
import kotlin.coroutines.cancellation.CancellationException


/**
 * Executes a suspend request and maps exceptions to Auth0Error.
 * Rethrows [CancellationException] to preserve structured concurrency.
 *
 * @param execute The suspend block to execute
 * @throws [CancellationException] if the coroutine is cancelled
 * @throws [com.auth0.universalcomponents.domain.error.Auth0Error]
 */
internal suspend inline fun <reified T> withErrorMapping(
    scope: String? = null,
    execute: suspend () -> T
): T {
    return try {
        execute()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        throw ErrorMapper.mapToAuth0Error(e, scope)
    }
}