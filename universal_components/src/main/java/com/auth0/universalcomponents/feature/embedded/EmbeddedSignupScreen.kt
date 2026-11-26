package com.auth0.universalcomponents.feature.embedded

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.di.UniversalComponentsModule
import com.auth0.universalcomponents.feature.embedded.presentation.LocalEmbeddedEntry
import com.auth0.universalcomponents.feature.embedded.signup.presentation.SignupHost
import com.auth0.universalcomponents.feature.embedded.signup.presentation.SignupViewModel
import com.auth0.universalcomponents.theme.Auth0Theme
import com.auth0.universalcomponents.theme.Auth0ThemeConfiguration

/**
 * Embedded sign-up on its own: the user enters their email, confirms it with a one-time code, and creates the account.
 * The server decides what it asks for and in what order; a connection that needs something this screen can't ask for yet
 * shows "Sign-up isn't available". To offer sign-up from the login instead, leave `signup` at its default in
 * [EmbeddedLoginScreen].
 *
 * The library stores nothing. When the account is created, the credentials go to [onAuthenticated], once, and keeping
 * them is the app's job. If the email already has an account, the server signs that user in instead, and
 * [onAuthenticated] is called the same way. [onExit] is called when the user leaves: Back from the email, loading, error
 * or success screen, or "Go back" when sign-up isn't available. Back from the code screen returns to the email instead,
 * and Back waits while a request is running.
 *
 * It looks as the login does, for the same [themeConfiguration] and [entry].
 */
@Composable
fun EmbeddedSignupScreen(
    connection: String,
    onAuthenticated: (Credentials) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    themeConfiguration: Auth0ThemeConfiguration = Auth0ThemeConfiguration.Default,
    entry: EmbeddedEntryConfiguration = EmbeddedEntryConfiguration.Default,
) {
    Auth0Theme(configuration = themeConfiguration) {
        val viewModel: SignupViewModel = viewModel(
            key = "embedded-signup:$connection",
            factory = remember(connection) { UniversalComponentsModule.provideSignupViewModelFactory(connection) },
        )
        CompositionLocalProvider(LocalEmbeddedEntry provides entry) {
            SignupHost(
                viewModel = viewModel,
                onAuthenticated = onAuthenticated,
                onExit = onExit,
                modifier = modifier,
            )
        }
    }
}
