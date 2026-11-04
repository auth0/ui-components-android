package com.auth0.universalcomponents.feature.mfachallenge

/**
 * Which kind of second factor a row or a screen is about. Open, so a later release adds a factor without breaking an
 * app's `when`: give it an `else` branch.
 */
@JvmInline
value class MfaFactorId(val value: String) {

    companion object {
        /** Approve a sign-in request on another device. */
        val Push = MfaFactorId("push")
    }
}
