package com.auth0.universalcomponents.domain.usecase

import com.auth0.universalcomponents.domain.DispatcherProvider
import com.auth0.universalcomponents.domain.error.Auth0Error
import com.auth0.universalcomponents.domain.model.UserInfo
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.domain.network.safeCall
import com.auth0.universalcomponents.domain.repository.UserRepository

/**
 * UseCase that retrieves identity information about the currently authenticated user
 * from session credentials.
 */
class GetUserInfoUseCase(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(): Result<UserInfo, Auth0Error> =
        safeCall {
            userRepository.getUserInfo()
        }
}
