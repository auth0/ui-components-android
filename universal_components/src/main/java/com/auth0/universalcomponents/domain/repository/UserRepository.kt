package com.auth0.universalcomponents.domain.repository

import com.auth0.universalcomponents.domain.model.UserInfo

/**
 * Repository interface for accessing user identity information
 * derived from the current session credentials.
 */
interface UserRepository {
    suspend fun getUserInfo(): UserInfo
}
