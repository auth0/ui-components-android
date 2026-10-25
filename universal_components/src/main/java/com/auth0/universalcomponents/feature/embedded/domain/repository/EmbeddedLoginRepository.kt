package com.auth0.universalcomponents.feature.embedded.domain.repository

import com.auth0.universalcomponents.domain.network.EmptyResult
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.embedded.domain.model.PushStatus
import com.auth0.universalcomponents.feature.embedded.domain.model.VerifyOutcome

/**
 * The only way the flow talks to Auth0. One instance per login flow, because the SDK keeps state.
 * Every failure comes back as a [LoginFailure] inside the [Result]; nothing is thrown but cancellation.
 */
internal interface EmbeddedLoginRepository {

    /** Opens a login session and returns the sign-in methods the server offers. */
    suspend fun discover(): Result<List<LoginOption>, LoginFailure>

    /** Sends a code to [email]; a resend or an edited email opens a new session first. */
    suspend fun startEmailOtp(email: String): EmptyResult<LoginFailure>

    /**
     * Verifies a code the user received. A correct code either signs in or asks for a second factor;
     * any other further step isn't supported and fails.
     */
    suspend fun verifyOtp(code: String): Result<VerifyOutcome, LoginFailure>
}

/**
 * The second factor, on the same SDK session the first factor opened.
 * Every failure comes back as a [LoginFailure] inside the [Result]; nothing is thrown but cancellation.
 */
internal interface MfaRepository {

    /** Sends the push and returns how long to wait, in milliseconds, before the first [checkPush]. */
    suspend fun sendPush(factor: MfaFactor.Push): Result<Long, LoginFailure>

    /** One check on the push; never call another step while this is in flight. */
    suspend fun checkPush(): Result<PushStatus, LoginFailure>
}
