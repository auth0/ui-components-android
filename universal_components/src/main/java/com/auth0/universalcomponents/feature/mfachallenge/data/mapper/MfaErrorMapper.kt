package com.auth0.universalcomponents.feature.mfachallenge.data.mapper

import com.auth0.android.embedded.EmbeddedAuthError
import com.auth0.android.embedded.EmbeddedAuthException
import com.auth0.universalcomponents.feature.embedded.data.mapper.serverDescription
import com.auth0.universalcomponents.feature.mfachallenge.MfaError

/**
 * Names the SDK error in MFA's terms, as iOS does. IP blocks, consent and the like have no case of their own, so
 * they're [MfaError.Unexpected], with the server's words when it gave some.
 */
internal fun EmbeddedAuthException.toMfaError(): MfaError = when (error) {
    EmbeddedAuthError.Network -> MfaError.Network(this)
    EmbeddedAuthError.AuthorizationRejected -> MfaError.Rejected(this)
    EmbeddedAuthError.ChallengeExpired -> MfaError.TokenExpired(this)
    // The session ended, or a late approval already finished it: never the SDK's developer text.
    EmbeddedAuthError.SessionExpired, EmbeddedAuthError.NoActiveSession -> MfaError.TokenInvalid(this)
    EmbeddedAuthError.NoEligibleFactors -> MfaError.NoEligibleFactors(this)
    EmbeddedAuthError.TooManyWrongOtpAttempts,
    EmbeddedAuthError.TooManyAttempts,
    EmbeddedAuthError.TooManyLogins -> MfaError.TooManyAttempts(this)
    else -> MfaError.Unexpected(serverDescription, this)
}
