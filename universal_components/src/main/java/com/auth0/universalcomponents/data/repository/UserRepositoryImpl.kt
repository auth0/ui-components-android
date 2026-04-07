package com.auth0.universalcomponents.data.repository

import com.auth0.universalcomponents.data.network.withErrorMapping
import com.auth0.universalcomponents.domain.model.UserInfo
import com.auth0.universalcomponents.domain.repository.UserRepository
import com.auth0.universalcomponents.token.TokenProvider

/**
 * Implementation of [UserRepository] that derives user identity
 * from the current session credentials via [TokenProvider].
 */
class UserRepositoryImpl(
    private val tokenProvider: TokenProvider
) : UserRepository {

    override suspend fun getUserInfo(): UserInfo {
        return withErrorMapping {
            val user = tokenProvider.fetchCredentials().user
            UserInfo(
                email = user.email,
                name = user.name,
                pictureUrl = user.pictureURL
            )
        }
    }
}
