package com.auth0.universalcomponents.feature.embedded.presentation.components

import com.auth0.universalcomponents.feature.embedded.EmbeddedEntryMessages
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EntryRoleTest {

    private val defaults = EntryWords(
        loginTitle = null,
        loginOrSignUpTitle = "Log in or sign up",
        signUpTitle = "Create your account",
        signUpPrompt = "Don’t have an account?",
        signUpAction = "Sign up",
        logInPrompt = "Already have an account?",
        logInAction = "Log in",
    )

    // region the words

    @Test
    fun `nothing set - every word is the library's own, and the login title is the way's`() {
        val words = EmbeddedEntryMessages().over(defaults)

        assertThat(words.loginTitle).isNull()
        assertThat(words.loginOrSignUpTitle).isEqualTo("Log in or sign up")
        assertThat(words.signUpTitle).isEqualTo("Create your account")
        assertThat(words.signUpPrompt).isEqualTo("Don’t have an account?")
        assertThat(words.signUpAction).isEqualTo("Sign up")
        assertThat(words.logInPrompt).isEqualTo("Already have an account?")
        assertThat(words.logInAction).isEqualTo("Log in")
    }

    @Test
    fun `words the app set - replace the defaults, one by one`() {
        val words = EmbeddedEntryMessages(
            loginTitle = "Welcome back",
            signUpTitle = "Join Acme",
            signUpAction = "Join",
        ).over(defaults)

        assertThat(words.loginTitle).isEqualTo("Welcome back")
        assertThat(words.signUpTitle).isEqualTo("Join Acme")
        assertThat(words.signUpAction).isEqualTo("Join")
        assertThat(words.loginOrSignUpTitle).isEqualTo("Log in or sign up")
        assertThat(words.logInAction).isEqualTo("Log in")
    }

    @Test
    fun `blank words - count as unset, so a heading or a link is never empty`() {
        val words = EmbeddedEntryMessages(
            loginTitle = " ",
            loginOrSignUpTitle = "",
            signUpTitle = "   ",
            signUpPrompt = "",
            logInAction = " ",
        ).over(defaults)

        assertThat(words.loginTitle).isNull()
        assertThat(words.loginOrSignUpTitle).isEqualTo("Log in or sign up")
        assertThat(words.signUpTitle).isEqualTo("Create your account")
        assertThat(words.signUpPrompt).isEqualTo("Don’t have an account?")
        assertThat(words.logInAction).isEqualTo("Log in")
    }

    // endregion

    // region the title

    @Test
    fun `login without sign-up - the app's title, else the way's own`() {
        val login = EntryRole.Login()

        assertThat(defaults.titleFor(login, "Enter your email")).isEqualTo("Enter your email")
        assertThat(words(loginTitle = "Welcome back").titleFor(login, "Enter your email")).isEqualTo("Welcome back")
    }

    @Test
    fun `login with sign-up offered - names both, whatever login title the app set`() {
        val login = EntryRole.Login(onSignUp = {})

        assertThat(defaults.titleFor(login, "Enter your email")).isEqualTo("Log in or sign up")
        assertThat(words(loginTitle = "Welcome back").titleFor(login, "Enter your email"))
            .isEqualTo("Log in or sign up")
    }

    @Test
    fun `sign-up - its own title, whatever login title the app set`() {
        assertThat(defaults.titleFor(EntryRole.SignUp(), "Enter your email")).isEqualTo("Create your account")
        assertThat(words(loginTitle = "Welcome back").titleFor(EntryRole.SignUp(onLogIn = {}), "Enter your email"))
            .isEqualTo("Create your account")
    }

    // endregion

    // region the link

    @Test
    fun `no other flow to offer - no link`() {
        assertThat(defaults.linkFor(EntryRole.Login())).isNull()
        assertThat(defaults.linkFor(EntryRole.SignUp())).isNull()
    }

    @Test
    fun `login with sign-up offered - links to sign-up, and a tap opens it`() {
        var opened = false

        val link = defaults.linkFor(EntryRole.Login(onSignUp = { opened = true }))

        assertThat(link?.prompt).isEqualTo("Don’t have an account?")
        assertThat(link?.action).isEqualTo("Sign up")
        link?.onClick?.invoke()
        assertThat(opened).isTrue()
    }

    @Test
    fun `sign-up opened from the login - links back to it, and a tap returns`() {
        var returned = false

        val link = defaults.linkFor(EntryRole.SignUp(onLogIn = { returned = true }))

        assertThat(link?.prompt).isEqualTo("Already have an account?")
        assertThat(link?.action).isEqualTo("Log in")
        link?.onClick?.invoke()
        assertThat(returned).isTrue()
    }

    @Test
    fun `the link - uses the app's words`() {
        val link = words(signUpPrompt = "New here?", signUpAction = "Join").linkFor(EntryRole.Login(onSignUp = {}))

        assertThat(link?.prompt).isEqualTo("New here?")
        assertThat(link?.action).isEqualTo("Join")
    }

    // endregion

    private fun words(
        loginTitle: String? = null,
        signUpPrompt: String = defaults.signUpPrompt,
        signUpAction: String = defaults.signUpAction,
    ) = EntryWords(
        loginTitle = loginTitle,
        loginOrSignUpTitle = defaults.loginOrSignUpTitle,
        signUpTitle = defaults.signUpTitle,
        signUpPrompt = signUpPrompt,
        signUpAction = signUpAction,
        logInPrompt = defaults.logInPrompt,
        logInAction = defaults.logInAction,
    )
}
