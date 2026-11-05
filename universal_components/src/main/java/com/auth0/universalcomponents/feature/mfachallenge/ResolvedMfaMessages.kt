package com.auth0.universalcomponents.feature.mfachallenge

import android.content.Context
import com.auth0.universalcomponents.feature.mfachallenge.presentation.resolve

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
    val phoneChallenge: PhoneChallenge,
) {

    companion object {

        /**
         * The words as [context] shows them now: the string resources, with [messages] applied over them. For what
         * has no MFA screen to hand them in: a `@Preview` or a test of a replaced screen, or a headless app that
         * wants the same words the standard screens use. Call it again when the language changes.
         */
        @JvmOverloads
        fun from(context: Context, messages: MfaMessages = MfaMessages()): ResolvedMfaMessages =
            messages.resolve(context.resources)
    }

    class Message internal constructor(val title: String, val description: String)

    class AuthenticatorList internal constructor(val headers: Headers) {
        class Headers internal constructor(val challenge: Message, val challengeAdditional: Message)
    }

    class Loading internal constructor(val accessibilityLabel: String)

    class Errors internal constructor(
        val startOver: String,
        val close: String,
        private val messageByError: Map<Class<out MfaError>, Message>,
    ) {

        /**
         * The words for [error]. An error with none of its own gets the [MfaError.Unexpected] ones: one this version
         * does not know, or [MfaError.PresentationUnavailable], which is never shown.
         */
        fun messageFor(error: MfaError): Message =
            messageByError[error.javaClass] ?: messageByError.getValue(MfaError.Unexpected::class.java)
    }

    class PushChallenge internal constructor(
        val row: Message,
        val title: String,
        val description: (deviceName: String) -> String,
        val descriptionUnnamed: String,
        val waiting: String,
        val resend: Resend,
        val verifyAnotherWay: String,
    ) {
        class Resend internal constructor(val button: String, val cooldown: (seconds: Int) -> String)
    }

    @Suppress("LongParameterList") // one word per line of the screen; they belong together
    class PhoneChallenge internal constructor(
        val title: String,
        val description: (phoneNumber: String) -> String,
        val descriptionVoice: (phoneNumber: String) -> String,
        val codeLabel: String,
        val verifyButton: String,
        val wrongCode: String,
        val resend: Resend,
        val verifyAnotherWay: String,
        val rowText: (phoneNumber: String) -> String,
        val rowVoice: (phoneNumber: String) -> String,
    ) {
        class Resend internal constructor(val button: String, val cooldown: (seconds: Int) -> String)
    }
}
