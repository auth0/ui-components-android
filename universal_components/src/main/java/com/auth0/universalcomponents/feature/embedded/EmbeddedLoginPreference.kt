package com.auth0.universalcomponents.feature.embedded

/**
 * Which way leads when the tenant offers both a one-time code and a password for an email address. The other is still
 * one tap away on the same screen. It changes nothing when only one of the two is offered, and a phone number is offered
 * its code only for now.
 *
 * Pass it to [EmbeddedLoginScreen]. A later release may add cases, so give a `when` over it an `else` branch.
 */
enum class EmbeddedLoginPreference {

    /** Whichever the server lists first. The default. */
    Server,

    /** A code is sent at once; the password is the alternative. */
    Code,

    /** The password is asked for; the code is the alternative. */
    Password,
}
