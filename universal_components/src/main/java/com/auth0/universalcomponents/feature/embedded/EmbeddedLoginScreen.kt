package com.auth0.universalcomponents.feature.embedded

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.di.UniversalComponentsModule
import com.auth0.universalcomponents.feature.embedded.presentation.LoginFlowViewModel
import com.auth0.universalcomponents.feature.embedded.presentation.LoginRenderingHost
import com.auth0.universalcomponents.theme.Auth0Theme
import com.auth0.universalcomponents.theme.Auth0ThemeConfiguration

/**
 * Embedded login with an emailed code, followed by MFA when the account needs it; the server decides
 * which screen comes next. [onAuthenticated] receives the credentials; storing them is up to the app.
 */
@Composable
fun EmbeddedLoginScreen(
    connection: String,
    onAuthenticated: (Credentials) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    themeConfiguration: Auth0ThemeConfiguration = Auth0ThemeConfiguration.Default,
) {
    Auth0Theme(configuration = themeConfiguration) {
        // Remembered so a recomposition doesn't build an SDK client that's never used.
        val factory = remember(connection) { UniversalComponentsModule.provideLoginFlowViewModelFactory(connection) }
        val loginViewModel: LoginFlowViewModel = viewModel(key = "embedded-login:$connection", factory = factory)
        LoginRenderingHost(
            viewModel = loginViewModel,
            onAuthenticated = onAuthenticated,
            onExit = onExit,
            modifier = modifier,
        )
    }
}
