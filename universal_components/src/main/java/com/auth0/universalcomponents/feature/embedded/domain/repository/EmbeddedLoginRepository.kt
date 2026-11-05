package com.auth0.universalcomponents.feature.embedded.domain.repository

import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.domain.network.EmptyResult
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption

/**
 * The only way the flow talks to Auth0. One instance per login flow, because the SDK keeps state.
 * Every failure comes back as a [LoginFailure] inside the [Result]; nothing is thrown but cancellation.
 */
internal interface EmbeddedLoginRepository {

    /** Opens a login session and returns the sign-in methods the server offers. */
    suspend fun discover(): Result<List<LoginOption>, LoginFailure>

    /**
     * Sends a code to [identifier], an email address or an E.164 phone number as [method] says; a resend or an edited
     * identifier opens a new session first. A phone gets a text message, or a call when the server offers no text.
     */
    suspend fun startOtp(method: LoginOption, identifier: String): EmptyResult<LoginFailure>

    /**
     * Verifies a code the user received. A correct code signs in, or fails with [EmbeddedLoginError.MfaRequired]
     * when the server wants more, even if none of the factors it offers can be used here: MFA tells the user.
     */
    suspend fun verifyOtp(code: String): Result<Credentials, LoginFailure>
}
