package com.auth0.universalcomponents.domain.model

/**
 * Represents identity information about the currently authenticated user,
 * derived from the session credentials.
 */
data class UserInfo(
    val email: String?,
    val name: String?,
    val pictureUrl: String?
)
