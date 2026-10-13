package com.auth0.universalcomponents.feature.embedded.domain.model

import com.auth0.android.result.Credentials

/** A sign-in method the server offered. */
internal sealed interface LoginOption {

    data class EmailOtp(val connection: String) : LoginOption

    /** Anything this flow can't drive yet; the ViewModel skips it. */
    data class Unsupported(val name: String) : LoginOption
}

internal enum class MfaFactor { OTP, EMAIL }

/** A step-up code request; [destination] is masked, for example s***@atko.email. */
internal data class MfaChallenge(val factor: MfaFactor, val destination: String? = null)

/** What a verified code leads to. */
internal sealed interface AuthOutcome {

    data class Authenticated(val credentials: Credentials) : AuthOutcome

    data class MfaRequired(val challenge: MfaChallenge) : AuthOutcome
}
