package com.auth0.universalcomponents.feature.embedded.signup.domain.repository

import com.auth0.android.embedded.authorize.PhoneDeliveryMethod
import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.domain.network.EmptyResult
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupChallenge
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupError
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupFailure
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupIdentifier
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupNext
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupProgress

/**
 * The only way a sign-up talks to Auth0. One instance per sign-up, because the SDK keeps a session of its own, which
 * is not the login's. Every failure comes back as a [SignupFailure] inside the [Result]; nothing is thrown but
 * cancellation.
 */
internal interface EmbeddedSignupRepository {

    /**
     * Opens a sign-up, which also starts it over, and says what to ask for first. Fails with [SignupError.Unavailable]
     * when the connection offers no sign-up this flow can finish.
     */
    suspend fun open(): Result<SignupNext, SignupFailure>

    /**
     * Tells the server what the new account is identified by and returns the code it will send. Nothing is sent to
     * the user yet. Fails with [SignupError.CodeNotSent] when the server offers no way to send one.
     */
    suspend fun identify(identifier: SignupIdentifier): Result<SignupChallenge, SignupFailure>

    /**
     * Sends the code [challenge] stands for, the way given: [delivery] is null for an email, which is only ever emailed,
     * and for a phone one of [SignupChallenge.deliveries]. Sending it again is the same call.
     */
    suspend fun sendCode(challenge: SignupChallenge, delivery: PhoneDeliveryMethod?): EmptyResult<SignupFailure>

    /**
     * A new code for [identifier] once the last one timed out, which ended the server's sign-up with it: opens a new
     * sign-up, identifies it again and sends the code, the way given. Returns the fresh challenge.
     */
    suspend fun replaceExpiredCode(
        identifier: SignupIdentifier,
        delivery: PhoneDeliveryMethod?,
    ): Result<SignupChallenge, SignupFailure>

    /** Checks a code the user received. A wrong one fails with [SignupError.InvalidCode] and keeps the session. */
    suspend fun verify(code: String): Result<SignupProgress, SignupFailure>

    /**
     * Creates the account once every required identifier is verified. Fails with [SignupError.MoreStepsRequired] when
     * the tenant wants more than a sign-up gives.
     */
    suspend fun createAccount(): Result<Credentials, SignupFailure>
}
