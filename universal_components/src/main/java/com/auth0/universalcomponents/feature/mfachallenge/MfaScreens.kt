package com.auth0.universalcomponents.feature.mfachallenge

import androidx.compose.runtime.Composable

/** A screen the app draws in place of a standard one: its [model], and the resolved words to draw with. */
internal typealias MfaScreen<Model> = @Composable (model: Model, messages: ResolvedMfaMessages) -> Unit

/**
 * The screens an app replaces; any it leaves unset stays standard. A replaced screen gets the same model, with the same
 * actions, as the standard one, so what it draws stays up to date (a resend wait still counts down) and its taps work
 * the same.
 *
 * ```
 * MfaScreens {
 *     pushChallenge { model, messages -> MyPushScreen(model, messages) }
 * }
 * ```
 *
 * Each factor adds a typed shortcut (`pushChallenge`), so the list of screens never names a factor. Built with a block,
 * rather than a constructor with a parameter per screen, so a later release can add a screen without breaking an app
 * that is already compiled.
 */
class MfaScreens private constructor(
    internal val authenticatorList: MfaScreen<AuthenticatorListModel>?,
    internal val error: MfaScreen<ErrorModel>?,
    internal val factors: Map<MfaFactorId, MfaScreen<MfaFactorModel>>,
) {

    companion object {

        /** Builds the screens to replace, from [configure]. With nothing set, every screen is standard. */
        operator fun invoke(configure: Builder.() -> Unit = {}): MfaScreens = Builder().apply(configure).build()
    }

    class Builder internal constructor() {
        private var authenticatorList: MfaScreen<AuthenticatorListModel>? = null
        private var error: MfaScreen<ErrorModel>? = null
        private val factors = mutableMapOf<MfaFactorId, MfaScreen<MfaFactorModel>>()

        /** The list of factors to pick from. */
        fun authenticatorList(content: @Composable (AuthenticatorListModel, ResolvedMfaMessages) -> Unit) {
            authenticatorList = content
        }

        /** MFA ended in an error. */
        fun error(content: @Composable (ErrorModel, ResolvedMfaMessages) -> Unit) {
            error = content
        }

        /**
         * The screen of [factor], for the factor shortcuts. [content] gets that factor's model, which a shortcut casts to
         * its type (such as [PushChallengeModel]); a model of another type draws nothing.
         */
        internal fun replace(factor: MfaFactorId, content: @Composable (MfaFactorModel, ResolvedMfaMessages) -> Unit) {
            factors[factor] = content
        }

        /** Waiting for a push to be approved. */
        fun pushChallenge(content: @Composable (PushChallengeModel, ResolvedMfaMessages) -> Unit) {
            replace(MfaFactorId.Push) { model, messages -> if (model is PushChallengeModel) content(model, messages) }
        }

        internal fun build() = MfaScreens(authenticatorList, error, factors.toMap())
    }
}
