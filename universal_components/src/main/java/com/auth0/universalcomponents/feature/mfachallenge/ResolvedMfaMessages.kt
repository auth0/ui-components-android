package com.auth0.universalcomponents.feature.mfachallenge

/**
 * Every word MFA shows, after the app's [MfaMessages] and the string resources are applied, in the language the screen
 * is in now. A screen the app replaced receives these together with its model, so it need not look any word up.
 * The paths are those of [MfaMessages].
 */
class ResolvedMfaMessages internal constructor(
    val authenticatorList: AuthenticatorList,
    val loading: Loading,
    val errors: Errors,
    val pushChallenge: PushChallenge,
) {

    class Text(val title: String, val description: String)

    class AuthenticatorList(val headers: Headers) {
        class Headers(val challenge: Text, val challengeAdditional: Text)
    }

    class Loading(val accessibilityLabel: String)

    class Errors(
        val startOver: String,
        val close: String,
        private val texts: Map<Class<out MfaError>, Text>,
    ) {

        /**
         * The words for [error]. An error with none of its own gets the [MfaError.Unexpected] ones: one this version
         * does not know, or [MfaError.PresentationUnavailable], which is never shown.
         */
        fun textFor(error: MfaError): Text = texts[error.javaClass] ?: texts.getValue(MfaError.Unexpected::class.java)
    }

    class PushChallenge(
        val row: Text,
        val title: String,
        val description: (deviceName: String) -> String,
        val descriptionUnnamed: String,
        val waiting: String,
        val resend: Resend,
        val verifyAnotherWay: String,
    ) {
        class Resend(val button: String, val cooldown: (seconds: Int) -> String)
    }
}
