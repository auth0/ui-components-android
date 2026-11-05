package com.auth0.universalcomponents.data.embeddedauth

import com.auth0.android.Auth0Exception
import com.auth0.android.embedded.EmbeddedAuthError
import com.auth0.android.embedded.EmbeddedAuthException

/**
 * The server's own explanation of the error, when it sent one in words; null otherwise. For every error the SDK
 * recognises, the description is the server's machine token (`no_eligible_factors`, `ip_blocked`, ...), which is
 * how the SDK recognises it, so only an error it left unclassified carries text meant to be read.
 */
internal val EmbeddedAuthException.serverDescription: String?
    get() = description.takeIf {
        (error == EmbeddedAuthError.AccessDenied || error == EmbeddedAuthError.Unknown) &&
            code !in SDK_ERROR_CODES && it.isNotBlank()
    }

/** Codes the SDK makes up when the server sent no usable error. */
private val SDK_ERROR_CODES = setOf(
    Auth0Exception.UNKNOWN_ERROR,
    Auth0Exception.NON_JSON_ERROR,
    Auth0Exception.EMPTY_BODY_ERROR,
)
