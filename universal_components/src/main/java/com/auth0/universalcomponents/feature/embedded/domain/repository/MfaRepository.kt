package com.auth0.universalcomponents.feature.embedded.domain.repository

import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.embedded.domain.model.PushStatus

/**
 * The second factor, on the same SDK session the first factor opened.
 * Every failure comes back as a [LoginFailure] inside the [Result]; nothing is thrown but cancellation.
 */
internal interface MfaRepository {

    /** Sends the push and returns how long to wait, in milliseconds, before the first [checkPushStatus]. */
    suspend fun sendPush(factor: MfaFactor.Push): Result<Long, LoginFailure>

    /** Checks once whether the user has answered the push. */
    suspend fun checkPushStatus(): Result<PushStatus, LoginFailure>
}
