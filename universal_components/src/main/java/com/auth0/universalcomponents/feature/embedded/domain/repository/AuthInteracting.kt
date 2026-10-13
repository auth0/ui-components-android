package com.auth0.universalcomponents.feature.embedded.domain.repository

import com.auth0.universalcomponents.feature.embedded.domain.model.AuthOutcome
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor

/** The only way the flow talks to Auth0. One instance per login flow, because the SDK keeps state. */
internal interface AuthInteracting {

    /** Opens a login session and returns the sign-in methods the server offers. */
    suspend fun discover(): List<LoginOption>

    /** Sends a code to [email]; a resend or an edited email opens a new session first. */
    suspend fun startOtp(connection: String, email: String)

    /** Verifies the emailed code. */
    suspend fun verifyOtp(code: String): AuthOutcome

    /** Verifies a step-up code. */
    suspend fun verifyMfa(code: String, factor: MfaFactor): AuthOutcome
}
