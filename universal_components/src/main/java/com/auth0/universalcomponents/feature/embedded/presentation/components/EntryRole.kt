package com.auth0.universalcomponents.feature.embedded.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.EmbeddedEntryMessages

/** What an entry screen is for. It sets the title and the link to the other flow. */
internal sealed interface EntryRole {

    /**
     * The first screen of the login. [onSignUp] is set when the server offered sign-up, which adds "Don't have an
     * account? Sign up" and the title that names both.
     */
    class Login(val onSignUp: (() -> Unit)? = null) : EntryRole

    /** The first screen of sign-up. [onLogIn] is set when it was opened from the login: "Already have an account? Log in". */
    class SignUp(val onLogIn: (() -> Unit)? = null) : EntryRole
}

/** The prompt and the action of the link to the other flow. */
internal class EntryLink(val prompt: String, val action: String, val onClick: () -> Unit)

/** The words of an entry screen, each the app's own or the default; a title or prompt the app left blank counts as unset. */
internal class EntryWords(
    /** Null when the app set none: the screen then uses the way's own title. */
    val loginTitle: String?,
    val loginOrSignUpTitle: String,
    val signUpTitle: String,
    val signUpPrompt: String,
    val signUpAction: String,
    val logInPrompt: String,
    val logInAction: String,
)

/**
 * The title: sign-up has its own; the login names both flows when it offers sign-up, and otherwise is the app's
 * [EntryWords.loginTitle] or [wayTitle], the way's own.
 */
internal fun EntryWords.titleFor(role: EntryRole, wayTitle: String): String = when (role) {
    is EntryRole.Login -> if (role.onSignUp != null) loginOrSignUpTitle else loginTitle ?: wayTitle
    is EntryRole.SignUp -> signUpTitle
}

/** The link to the other flow, when there is one to offer. */
internal fun EntryWords.linkFor(role: EntryRole): EntryLink? = when (role) {
    is EntryRole.Login -> role.onSignUp?.let { EntryLink(signUpPrompt, signUpAction, it) }
    is EntryRole.SignUp -> role.onLogIn?.let { EntryLink(logInPrompt, logInAction, it) }
}

/** The words as the app set them in [EmbeddedEntryMessages], and the library's own for the rest. */
@Composable
internal fun EmbeddedEntryMessages.resolved(): EntryWords = over(
    EntryWords(
        loginTitle = null,
        loginOrSignUpTitle = stringResource(R.string.embedded_entry_login_or_sign_up_title),
        signUpTitle = stringResource(R.string.embedded_entry_sign_up_title),
        signUpPrompt = stringResource(R.string.embedded_entry_sign_up_prompt),
        signUpAction = stringResource(R.string.embedded_entry_sign_up_action),
        logInPrompt = stringResource(R.string.embedded_entry_log_in_prompt),
        logInAction = stringResource(R.string.embedded_entry_log_in_action),
    ),
)

/** What the app set, over the [defaults] for what it left unset or blank. */
internal fun EmbeddedEntryMessages.over(defaults: EntryWords): EntryWords = EntryWords(
    loginTitle = loginTitle.ifSet(),
    loginOrSignUpTitle = loginOrSignUpTitle.ifSet() ?: defaults.loginOrSignUpTitle,
    signUpTitle = signUpTitle.ifSet() ?: defaults.signUpTitle,
    signUpPrompt = signUpPrompt.ifSet() ?: defaults.signUpPrompt,
    signUpAction = signUpAction.ifSet() ?: defaults.signUpAction,
    logInPrompt = logInPrompt.ifSet() ?: defaults.logInPrompt,
    logInAction = logInAction.ifSet() ?: defaults.logInAction,
)

private fun String?.ifSet(): String? = this?.takeUnless { it.isBlank() }
