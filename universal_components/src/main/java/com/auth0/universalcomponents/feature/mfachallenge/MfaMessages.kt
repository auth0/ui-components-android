package com.auth0.universalcomponents.feature.mfachallenge

/**
 * The words MFA shows. Set only what you change; anything unset keeps its default, a string resource named
 * `auth0_mfa_<group>_<key>` (so `pushChallenge.resend.cooldown` is `auth0_mfa_push_challenge_resend_cooldown`). Declare
 * a string with that name in your app to translate or change a default for the whole app; use this class to change one
 * place, or to build text that depends on a value.
 *
 * ```
 * MfaMessages {
 *     pushChallenge {
 *         title = "Approve on your phone"
 *         description = { device -> "We sent a request to $device." }
 *         resend { cooldown = { seconds -> "Again in $seconds s" } }
 *     }
 *     errors { rejected { title = "Denied" } }
 * }
 * ```
 *
 * The key paths are the same as on iOS.
 */
class MfaMessages private constructor(
    internal val authenticatorList: AuthenticatorList,
    internal val loading: Loading,
    internal val errors: Errors,
    internal val pushChallenge: PushChallenge,
    internal val phoneChallenge: PhoneChallenge,
) {

    /** A title and a description, either of which can be changed. */
    class Message internal constructor() {
        var title: String? = null
        var description: String? = null
    }

    class AuthenticatorList internal constructor() {
        internal val headers = Headers()

        fun headers(configure: Headers.() -> Unit) = headers.configure()

        class Headers internal constructor() {
            internal val challenge = Message()
            internal val challengeAdditional = Message()

            /** Above the list in the first round. */
            fun challenge(configure: Message.() -> Unit) = challenge.configure()

            /** Above the list when the server asks for one more factor. */
            fun challengeAdditional(configure: Message.() -> Unit) = challengeAdditional.configure()
        }
    }

    class Loading internal constructor() {
        /** What a screen reader says for the loading spinner. */
        var accessibilityLabel: String? = null
    }

    /** One group per [MfaError] case, plus the words for the way out of the error screen. */
    class Errors internal constructor() {
        var startOver: String? = null
        var close: String? = null

        internal val cancelled = Message()
        internal val tokenExpired = Message()
        internal val tokenInvalid = Message()
        internal val rejected = Message()
        internal val tooManyAttempts = Message()
        internal val noEligibleFactors = Message()
        internal val network = Message()
        internal val unexpected = Message()

        fun cancelled(configure: Message.() -> Unit) = cancelled.configure()

        fun tokenExpired(configure: Message.() -> Unit) = tokenExpired.configure()

        fun tokenInvalid(configure: Message.() -> Unit) = tokenInvalid.configure()

        fun rejected(configure: Message.() -> Unit) = rejected.configure()

        fun tooManyAttempts(configure: Message.() -> Unit) = tooManyAttempts.configure()

        fun noEligibleFactors(configure: Message.() -> Unit) = noEligibleFactors.configure()

        fun network(configure: Message.() -> Unit) = network.configure()

        /** Shown with the server's own explanation instead of the description, when it gave one. */
        fun unexpected(configure: Message.() -> Unit) = unexpected.configure()
    }

    class PushChallenge internal constructor() {
        internal val row = Message()

        var title: String? = null

        /** Takes the device's name. */
        var description: ((deviceName: String) -> String)? = null

        /** For a device with no name. */
        var descriptionUnnamed: String? = null
        var waiting: String? = null
        var verifyAnotherWay: String? = null
        internal val resend = Resend()

        fun resend(configure: Resend.() -> Unit) = resend.configure()

        /**
         * How the factor looks in the list. The title is the factor's name, and the description sits under it for
         * a device with no name of its own (a named device leads, with the factor's name under it).
         */
        fun row(configure: Message.() -> Unit) = row.configure()

        class Resend internal constructor() {
            var button: String? = null

            /** Takes the seconds left until Resend opens. */
            var cooldown: ((seconds: Int) -> String)? = null
        }
    }

    /** Typing the code that was sent to a phone, by text or by a call. */
    class PhoneChallenge internal constructor() {
        var title: String? = null

        /** For a code sent by text; takes the phone number as the server hides it. */
        var description: ((phoneNumber: String) -> String)? = null

        /** For a code sent by a call; takes the phone number as the server hides it. */
        var descriptionVoice: ((phoneNumber: String) -> String)? = null

        /** The label above the code boxes. */
        var codeLabel: String? = null
        var verifyButton: String? = null

        /** Under the code field after the server refused a code. */
        var wrongCode: String? = null
        var verifyAnotherWay: String? = null

        /**
         * The first line of the factor's tile in the list for a code sent by text. Takes the phone number as the server
         * hides it. The default words ignore it: the standard list shows the number on the second line.
         */
        var rowText: ((phoneNumber: String) -> String)? = null

        /** The same, for a code sent by a call. */
        var rowVoice: ((phoneNumber: String) -> String)? = null
        internal val resend = Resend()

        fun resend(configure: Resend.() -> Unit) = resend.configure()

        class Resend internal constructor() {
            var button: String? = null

            /** Takes the seconds left until Resend opens. */
            var cooldown: ((seconds: Int) -> String)? = null
        }
    }

    companion object {

        /**
         * Every word its default. One shared instance, so the words are resolved once, not each time the screen that
         * holds the default configuration is drawn again.
         */
        internal val Default: MfaMessages = MfaMessages()

        /** Builds the words to change, from [configure]. With nothing set, every word is its default. */
        operator fun invoke(configure: Builder.() -> Unit = {}): MfaMessages = Builder().apply(configure).build()
    }

    class Builder internal constructor() {
        private val authenticatorList = AuthenticatorList()
        private val loading = Loading()
        private val errors = Errors()
        private val pushChallenge = PushChallenge()
        private val phoneChallenge = PhoneChallenge()

        fun authenticatorList(configure: AuthenticatorList.() -> Unit) = authenticatorList.configure()

        fun loading(configure: Loading.() -> Unit) = loading.configure()

        fun errors(configure: Errors.() -> Unit) = errors.configure()

        fun pushChallenge(configure: PushChallenge.() -> Unit) = pushChallenge.configure()

        fun phoneChallenge(configure: PhoneChallenge.() -> Unit) = phoneChallenge.configure()

        internal fun build() = MfaMessages(authenticatorList, loading, errors, pushChallenge, phoneChallenge)
    }
}
