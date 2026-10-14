package com.auth0.universalcomponents.feature.embedded.domain.repository

import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginException
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption

/**
 * The only way the flow talks to Auth0. One instance per login flow, because the SDK keeps state.
 * Every failure is thrown as an [EmbeddedLoginException].
 */
internal interface EmbeddedLoginRepository {

    /** Opens a login session and returns the sign-in methods the server offers. */
    suspend fun discover(): List<LoginOption>

    /** Sends a code to [email]; a resend or an edited email opens a new session first. */
    suspend fun startEmailOtp(email: String)

    /**
     * Verifies a code the user received. Email and SMS codes are checked the same way, so this serves both.
     * A further step, such as MFA, isn't supported yet and fails.
     */
    suspend fun verifyOtp(code: String): Credentials
}
