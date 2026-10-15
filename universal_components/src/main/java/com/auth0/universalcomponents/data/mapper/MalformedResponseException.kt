package com.auth0.universalcomponents.data.mapper

/**
 * Thrown by a mapper when a response lacks a value it always carries.
 * [ErrorMapper] surfaces it as [com.auth0.universalcomponents.domain.error.Auth0Error.MalformedResponse].
 */
internal class MalformedResponseException(message: String) : IllegalStateException(message)
