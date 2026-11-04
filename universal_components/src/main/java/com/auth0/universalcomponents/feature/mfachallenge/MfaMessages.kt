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
) {

    /** A title and a description, either of which can be changed. */
    @MfaDsl
    class Text internal constructor() {
        var title: String? = null
        var description: String? = null
    }

    @MfaDsl
    class AuthenticatorList internal constructor() {
        internal val headers = Headers()

        fun headers(configure: Headers.() -> Unit) = headers.configure()

        @MfaDsl
        class Headers internal constructor() {
            internal val challenge = Text()
            internal val challengeAdditional = Text()

            /** Above the list in the first round. */
            fun challenge(configure: Text.() -> Unit) = challenge.configure()

            /** Above the list when the server asks for one more factor. */
            fun challengeAdditional(configure: Text.() -> Unit) = challengeAdditional.configure()
        }
    }

    @MfaDsl
    class Loading internal constructor() {
        /** What a screen reader says for the loading spinner. */
        var accessibilityLabel: String? = null
    }

    /** One group per [MfaError] case, plus the words for the way out of the error screen. */
    @MfaDsl
    class Errors internal constructor() {
        var startOver: String? = null
        var close: String? = null

        internal val cancelled = Text()
        internal val tokenExpired = Text()
        internal val tokenInvalid = Text()
        internal val rejected = Text()
        internal val tooManyAttempts = Text()
        internal val noEligibleFactors = Text()
        internal val network = Text()
        internal val unexpected = Text()

        fun cancelled(configure: Text.() -> Unit) = cancelled.configure()

        fun tokenExpired(configure: Text.() -> Unit) = tokenExpired.configure()

        fun tokenInvalid(configure: Text.() -> Unit) = tokenInvalid.configure()

        fun rejected(configure: Text.() -> Unit) = rejected.configure()

        fun tooManyAttempts(configure: Text.() -> Unit) = tooManyAttempts.configure()

        fun noEligibleFactors(configure: Text.() -> Unit) = noEligibleFactors.configure()

        fun network(configure: Text.() -> Unit) = network.configure()

        /** Shown with the server's own explanation instead of the description, when it gave one. */
        fun unexpected(configure: Text.() -> Unit) = unexpected.configure()
    }

    @MfaDsl
    class PushChallenge internal constructor() {
        internal val row = Text()

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
        fun row(configure: Text.() -> Unit) = row.configure()

        @MfaDsl
        class Resend internal constructor() {
            var button: String? = null

            /** Takes the seconds left until Resend opens. */
            var cooldown: ((seconds: Int) -> String)? = null
        }
    }

    companion object {

        /** Builds the words to change, from [configure]. With nothing set, every word is its default. */
        operator fun invoke(configure: Builder.() -> Unit = {}): MfaMessages = Builder().apply(configure).build()
    }

    @MfaDsl
    class Builder internal constructor() {
        private val authenticatorList = AuthenticatorList()
        private val loading = Loading()
        private val errors = Errors()
        private val pushChallenge = PushChallenge()

        fun authenticatorList(configure: AuthenticatorList.() -> Unit) = authenticatorList.configure()

        fun loading(configure: Loading.() -> Unit) = loading.configure()

        fun errors(configure: Errors.() -> Unit) = errors.configure()

        fun pushChallenge(configure: PushChallenge.() -> Unit) = pushChallenge.configure()

        internal fun build() = MfaMessages(authenticatorList, loading, errors, pushChallenge)
    }
}
