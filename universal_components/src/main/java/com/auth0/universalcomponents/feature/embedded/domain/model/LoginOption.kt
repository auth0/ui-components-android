package com.auth0.universalcomponents.feature.embedded.domain.model

/** A sign-in method the server offered. */
internal sealed interface LoginOption {

    data object EmailOtp : LoginOption

    /** A code sent to a phone, by text message. */
    data object PhoneOtp : LoginOption

    /** A username, then its password. */
    data object Username : LoginOption

    /** The password of an email or username the server has just identified. It is never a first screen. */
    data object Password : LoginOption

    /** The server offers to create an account instead of signing in. It is a link on the first screen, never a screen of its own. */
    data object Signup : LoginOption

    /** Anything this flow can't drive yet; the ViewModel skips it. */
    data class Unsupported(val name: String) : LoginOption
}
