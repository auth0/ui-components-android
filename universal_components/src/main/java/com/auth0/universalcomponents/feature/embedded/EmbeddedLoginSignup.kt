package com.auth0.universalcomponents.feature.embedded

/** Whether the first screen of [EmbeddedLoginScreen] offers to create an account. */
enum class EmbeddedLoginSignup {

    /**
     * The first screen links to sign-up, "Don't have an account? Sign up", when the tenant allows it, and its title
     * names both ways. The login tells the server it can send the user to sign-up, so the server offers it next to the
     * sign-in methods. The default.
     */
    Offered,

    /** The login is only for signing in, and tells the server nothing about sign-up. */
    Hidden,
}
