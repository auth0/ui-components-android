package com.auth0.universalcomponents.feature.embedded.domain.repository

import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.domain.network.EmptyResult
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption

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
     * Verifies a code the user received. Email and SMS codes are checked the same way, so this serves both.
     * A further step, such as MFA, isn't supported yet and fails.
     */
    suspend fun verifyOtp(code: String): Result<Credentials, LoginFailure>
}
