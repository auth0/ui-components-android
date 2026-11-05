package com.auth0.universalcomponents.feature.mfachallenge.domain.repository

import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.mfachallenge.MfaError
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.CodeStatus
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.PushStatus

/**
 * The second factor, on the same SDK session the first factor opened. It speaks MFA's own terms, so MFA never sees
 * a login type. Every failure comes back as an [MfaError] inside the [Result]; nothing is thrown but cancellation.
 */
internal interface MfaRepository {

    /** Sends the push; the answer says when to check it first and whether the server offers another push. */
    suspend fun sendPush(factor: MfaFactor.Push): Result<PushStatus.Pending, MfaError>

    /** Checks once whether the user has answered the push. */
    suspend fun checkPushStatus(): Result<PushStatus, MfaError>

    /** Sends the code to the phone; the answer is true while the server still offers to send it again. */
    suspend fun sendPhoneCode(factor: MfaFactor.Phone): Result<Boolean, MfaError>

    /** Sends the code the user typed for the phone code that went out last. */
    suspend fun verifyCode(code: String): Result<CodeStatus, MfaError>
}
