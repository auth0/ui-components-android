package com.auth0.universalcomponents.feature.embedded.domain.repository

import com.auth0.android.result.Credentials
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
     * Tells the server who is signing in, an email address, an E.164 phone number or a username as [method] says, and
     * returns what it offers next: [LoginOption.EmailOtp] or [LoginOption.PhoneOtp] when a code can be sent to this
     * identifier, and [LoginOption.Password]. Nothing is sent to the user. The first one is what leads: the server's
     * order, unless the app chose which of a code and a password goes first. A username with no password step fails
     * with [EmbeddedLoginError.InvalidPassword], the same as a wrong password.
     */
    suspend fun identify(method: LoginOption, identifier: String): Result<List<LoginOption>, LoginFailure>

    /**
     * Sends a code to [identifier], an email address or an E.164 phone number as [method] says. Straight after
     * [identify] it goes on the same session; a resend, an edited identifier or a code after a wrong password opens a
     * new one first. A phone gets a text message, or a call when the server offers no text.
     *
     * Returns where the server says the code went, as it hides it (`XXXXXXXXX0128`), for a phone; null for an email, which
     * is shown as typed, and when the server names nothing.
     */
    suspend fun startOtp(method: LoginOption, identifier: String): Result<String?, LoginFailure>

    /**
     * Verifies a code the user received. A correct code signs in, or fails with [EmbeddedLoginError.MfaRequired]
     * when the server wants more, even if none of the factors it offers can be used here: MFA tells the user.
     * Fails with [EmbeddedLoginError.CodeNoLongerValid], without asking the server, once it has ended the sign-in.
     */
    suspend fun verifyOtp(code: String): Result<Credentials, LoginFailure>

    /**
     * Verifies the [password] of [identifier], typed as [method]. It goes on the session [identify] opened; when
     * that has ended (a rate limit, a breach, a timeout) a new one is opened for the same person first. A correct
     * password signs in or hands over to MFA, as [verifyOtp]; a wrong one fails with
     * [EmbeddedLoginError.InvalidPassword] and keeps the session.
     */
    suspend fun verifyPassword(
        method: LoginOption,
        identifier: String,
        password: String,
    ): Result<Credentials, LoginFailure>
}
