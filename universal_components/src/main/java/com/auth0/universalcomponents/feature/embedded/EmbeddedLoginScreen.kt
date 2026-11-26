package com.auth0.universalcomponents.feature.embedded

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.di.UniversalComponentsModule
import com.auth0.universalcomponents.feature.embedded.presentation.LocalEmbeddedEntry
import com.auth0.universalcomponents.feature.embedded.presentation.LoginFlowViewModel
import com.auth0.universalcomponents.feature.embedded.presentation.LoginRenderingHost
import com.auth0.universalcomponents.feature.embedded.signup.presentation.SignupHost
import com.auth0.universalcomponents.feature.embedded.signup.presentation.SignupViewModel
import com.auth0.universalcomponents.feature.mfachallenge.MfaConfiguration
import com.auth0.universalcomponents.feature.mfachallenge.MfaError
import com.auth0.universalcomponents.feature.mfachallenge.MfaErrorHandling
import com.auth0.universalcomponents.presentation.ui.utils.overlayTransition
import com.auth0.universalcomponents.theme.Auth0Theme
import com.auth0.universalcomponents.theme.Auth0ThemeConfiguration

/**
 * Embedded login with an emailed code, followed by MFA when the account needs it; the server decides
 * which screen comes next. [onAuthenticated] receives the credentials; storing them is up to the app.
 *
 * [onMfaError] is told, once, each time MFA ends with an error; the user leaving MFA is not one. The answer matters
 * for [MfaError.NoEligibleFactors] only: [MfaErrorHandling.Handled] means the app takes over (the user sees a spinner,
 * not an error), and [MfaErrorHandling.NotHandled], the default, shows MFA's own "not available" screen. For any
 * other error the screen already shows, and the answer changes nothing. A later release may add error cases, so give
 * the `when` an `else` branch.
 *
 * [mfa] customizes MFA: change its words, replace a screen, or draw it all yourself (headless).
 *
 * [preference] chooses which way leads when the tenant offers both a code and a password for an email address; by
 * default, the order the server lists them in. Changing it starts the sign-in again.
 *
 * [entry] customizes the first screen: its logo, layout and title. To change the logo for every screen that shows one,
 * set it in [themeConfiguration] instead (`Auth0ThemeConfiguration.brand`).
 *
 * [signup] chooses whether the first screen links to creating an account when the tenant allows it: [Offered] by
 * default, which also changes its title to name both ways. The link opens sign-up in this same screen, with the same
 * [entry] and theme, and [onAuthenticated] is called when the account is created, as it is after a sign-in. Changing it
 * starts the sign-in again. Use [EmbeddedSignupScreen] to open sign-up on its own.
 */
@Composable
fun EmbeddedLoginScreen(
    connection: String,
    onAuthenticated: (Credentials) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    themeConfiguration: Auth0ThemeConfiguration = Auth0ThemeConfiguration.Default,
    onMfaError: (MfaError) -> MfaErrorHandling = { MfaErrorHandling.NotHandled },
    mfa: MfaConfiguration = MfaConfiguration(),
    preference: EmbeddedLoginPreference = EmbeddedLoginPreference.Server,
    entry: EmbeddedEntryConfiguration = EmbeddedEntryConfiguration.Default,
    signup: EmbeddedLoginSignup = EmbeddedLoginSignup.Offered,
) {
    Auth0Theme(configuration = themeConfiguration) {
        // Remembered so a recomposition doesn't build an SDK client that's never used.
        val factory = remember(connection, preference, signup) {
            UniversalComponentsModule.provideLoginFlowViewModelFactory(
                connection = connection,
                preference = preference,
                offersSignup = signup == EmbeddedLoginSignup.Offered,
            )
        }
        val loginViewModel: LoginFlowViewModel = viewModel(
            key = "embedded-login:$connection:$preference:$signup",
            factory = factory,
        )
        CompositionLocalProvider(LocalEmbeddedEntry provides entry) {
            if (signup == EmbeddedLoginSignup.Offered) {
                LoginWithSignup(
                    connection = connection,
                    loginViewModel = loginViewModel,
                    onAuthenticated = onAuthenticated,
                    onExit = onExit,
                    modifier = modifier,
                    onMfaError = onMfaError,
                    mfa = mfa,
                )
            } else {
                // Nothing to slide to, so the login is drawn on its own, as it was before sign-up existed.
                LoginRenderingHost(
                    viewModel = loginViewModel,
                    onAuthenticated = onAuthenticated,
                    onExit = onExit,
                    modifier = modifier,
                    onMfaError = onMfaError,
                    mfa = mfa,
                )
            }
        }
    }
}

/** Which of the two the user is on. Kept across a rotation and a process restart. */
private enum class Route { Login, Signup }

/**
 * The login, with sign-up one tap away. Sign-up slides in over the login and slides back out, and each keeps its
 * place meanwhile: the login's own state is its ViewModel's and what is typed on it is saved by
 * [rememberSaveableStateHolder], so returning shows it exactly as it was, with nothing loaded again. The Back handler
 * that is active is the one of the screen on show.
 */
@Composable
private fun LoginWithSignup(
    connection: String,
    loginViewModel: LoginFlowViewModel,
    onAuthenticated: (Credentials) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier,
    onMfaError: (MfaError) -> MfaErrorHandling,
    mfa: MfaConfiguration,
) {
    // A change of connection, preference or sign-up setting starts a new login (see the ViewModel's key), and a new
    // login starts on the login, not on a sign-up opened over the one before it.
    var route by rememberSaveable(loginViewModel) { mutableStateOf(Route.Login) }
    val stateHolder = rememberSaveableStateHolder()
    AnimatedContent(
        targetState = route,
        transitionSpec = { overlayTransition(opening = targetState == Route.Signup) },
        modifier = modifier.fillMaxSize(),
        label = "loginOrSignup",
    ) { shown ->
        stateHolder.SaveableStateProvider(shown) {
            when (shown) {
                Route.Login -> LoginRenderingHost(
                    viewModel = loginViewModel,
                    onAuthenticated = onAuthenticated,
                    onExit = onExit,
                    onMfaError = onMfaError,
                    mfa = mfa,
                    onSignUp = { route = Route.Signup },
                )

                Route.Signup -> {
                    // Built the first time sign-up opens, so a login that never needs it never builds its client.
                    val signupViewModel: SignupViewModel = viewModel(
                        key = "embedded-login-signup:$connection",
                        factory = remember(connection) {
                            UniversalComponentsModule.provideSignupViewModelFactory(connection)
                        },
                    )
                    // What was typed goes with the sign-up, so the next one opens blank, as its ViewModel does.
                    val leaveSignup = {
                        stateHolder.removeState(Route.Signup)
                        route = Route.Login
                    }
                    SignupHost(
                        viewModel = signupViewModel,
                        onAuthenticated = onAuthenticated,
                        onExit = leaveSignup,
                        onLogIn = leaveSignup,
                    )
                }
            }
        }
    }
}
