package com.auth0.android.sample

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.storage.CredentialsManager
import com.auth0.android.authentication.storage.SharedPreferencesStorage
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.request.DefaultClient
import com.auth0.android.sample.ui.navigation.AppRoute
import com.auth0.android.sample.ui.screens.AppearanceScreen
import com.auth0.android.sample.ui.screens.ChooseSignInScreen
import com.auth0.android.sample.ui.screens.DashboardDestination
import com.auth0.android.sample.ui.screens.DashboardScreen
import com.auth0.android.sample.ui.screens.DocsScreen
import com.auth0.android.sample.ui.screens.EmbeddedLoginScreen
import com.auth0.android.sample.ui.screens.ExploreLoginScreen
import com.auth0.android.sample.ui.screens.FavoritesScreen
import com.auth0.android.sample.ui.screens.LoginSecurityScreen
import com.auth0.android.sample.ui.screens.ProfileScreen
import com.auth0.android.sample.ui.screens.SessionsScreen
import com.auth0.android.sample.ui.screens.Settings
import com.auth0.android.sample.ui.screens.TokensScreen
import com.auth0.android.sample.ui.screens.UpdateFullNameScreen
import com.auth0.android.sample.ui.theme.Ui_components_androidTheme
import com.auth0.android.sample.ui.viewmodels.AppearanceViewModel
import com.auth0.android.sample.ui.viewmodels.AuthState
import com.auth0.android.sample.ui.viewmodels.AuthViewModel
import com.auth0.android.ui_components.Auth0UI
import com.auth0.android.ui_components.presentation.ui.components.ErrorScreen
import com.auth0.android.ui_components.theme.Auth0Theme
import com.auth0.android.ui_components.token.DefaultTokenProvider

class MainActivity : ComponentActivity() {

    private val account: Auth0 by lazy {
        val account = Auth0.getInstance(
            getString(R.string.com_auth0_client_id),
            getString(R.string.com_auth0_domain)
        )
        account.networkingClient = DefaultClient(enableLogging = true)
        account
    }

    private val authClient: AuthenticationAPIClient by lazy {
        AuthenticationAPIClient(account)
    }

    private val credentialsManager: CredentialsManager by lazy {
        CredentialsManager(authClient, SharedPreferencesStorage(this))
    }

    private val audience: String by lazy {
        "https://${getString(R.string.com_auth0_domain)}/api/v2/"
    }

    val webAuthProvider by lazy {
        WebAuthProvider.login(account)
            .withScheme(getString(R.string.com_auth0_scheme))
            .withAudience(audience)
    }

    private val logoutBuilder by lazy {
        WebAuthProvider.logout(account)
            .withScheme(getString(R.string.com_auth0_scheme))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Auth0UI.initialize(
            account,
            DefaultTokenProvider(
                credentialsManager,
            ),
            scheme = getString(R.string.com_auth0_scheme)
        )
        setContent {
            Ui_components_androidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SampleApp(
                        credentialsManager = credentialsManager,
                        webAuthProvider = webAuthProvider,
                        logoutBuilder = logoutBuilder,
                        authClient = authClient,
                        audience = audience
                    )
                }
            }
        }
    }
}

@Composable
fun SampleApp(
    credentialsManager: CredentialsManager,
    webAuthProvider: WebAuthProvider.Builder,
    logoutBuilder: WebAuthProvider.LogoutBuilder,
    authClient: AuthenticationAPIClient,
    audience: String,
    authViewModel: AuthViewModel = viewModel(),
    appearanceViewModel: AppearanceViewModel = viewModel()
) {
    val navController = rememberNavController()
    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
    val appliedTheme by appearanceViewModel.appliedOption.collectAsStateWithLifecycle()

    val authState by authViewModel.authState.collectAsStateWithLifecycle()


    when (authState) {
        is AuthState.Authenticated -> {
            credentialsManager.saveCredentials((authState as AuthState.Authenticated).credentials)
            navController.navigate(AppRoute.Dashboard) {
                popUpTo<AppRoute.ChooseSignIn> { inclusive = true }
            }
        }

        is AuthState.Error -> {
            ErrorScreen(
                mainErrorMessage = (authState as AuthState.Error).message
            )
        }


        else -> {
            Log.d("LoginScreen", ": ${authState}")
        }
    }

    // Determine start destination based on existing credentials
    val startDestination = if (credentialsManager.hasValidCredentials()) {
        // Load profile from existing credentials on resume
        authViewModel.loadProfileFromCredentials(credentialsManager)
        AppRoute.Dashboard
    } else {
        AppRoute.ChooseSignIn
    }

    Auth0Theme(
        configuration = appliedTheme.configuration,
        darkTheme = appliedTheme.darkTheme
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
        ) {

            composable<AppRoute.ChooseSignIn> {
                val context = LocalContext.current
                ChooseSignInScreen(
                    onHostedLogin = { authViewModel.login(context, webAuthProvider) },
                    onEmbeddedLogin = { navController.navigate(AppRoute.EmbeddedLogin) },
                    onSettings = { navController.navigate(AppRoute.Appearance) }
                )
            }

            composable<AppRoute.EmbeddedLogin> {
                val context = LocalContext.current
                EmbeddedLoginScreen(
                    onGoogleLogin = {
                        Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show()
                    },
                    onContinueWithEmail = { email, password ->
                        authViewModel.loginWithPassword(email, password, authClient, audience)
                    },
                    onOtherMethods = { navController.navigate(AppRoute.ExploreLogin) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<AppRoute.ExploreLogin> {
                ExploreLoginScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable<AppRoute.GoogleLogin> {
                // Add support for Google Login
            }

            composable<AppRoute.Dashboard> {
                DashboardScreen(
                    userName = userProfile.name,
                    onNavigate = { destination ->
                        when (destination) {
                            DashboardDestination.Profile -> navController.navigate(AppRoute.Profile)
                            DashboardDestination.LoginSecurity -> navController.navigate(AppRoute.LoginSecurity)
                            DashboardDestination.Tokens -> navController.navigate(AppRoute.Tokens)
                            DashboardDestination.Sessions -> navController.navigate(AppRoute.Sessions)
                            DashboardDestination.Docs -> navController.navigate(AppRoute.Docs)
                            DashboardDestination.Favorites -> navController.navigate(AppRoute.Favorites)
                        }
                    },
                    onLogout = {
                        authViewModel.logout(
                            navController.context,
                            credentialsManager,
                            logoutBuilder
                        )
                        navController.navigate(AppRoute.ChooseSignIn) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }

            // --- Profile ---
            composable<AppRoute.Profile> {
                ProfileScreen(
                    userName = userProfile.name,
                    userEmail = userProfile.email,
                    currentTheme = appliedTheme.label,
                    onBack = { navController.popBackStack() },
                    onEditName = { navController.navigate(AppRoute.UpdateFullName) },
                    onTheme = { navController.navigate(AppRoute.Appearance) }
                )
            }

            composable<AppRoute.UpdateFullName> {
                UpdateFullNameScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable<AppRoute.LoginSecurity> {
                LoginSecurityScreen(
                    onBack = { navController.navigateUp() },
                    onManageMfa = {
                        navController.navigate(AppRoute.Settings)
                    },
                    themeConfiguration = appliedTheme.configuration
                )
            }

            // --- Appearance ---
            composable<AppRoute.Appearance> {
                AppearanceScreen(
                    onBack = { navController.popBackStack() },
                    appearanceViewModel = appearanceViewModel
                )
            }

            // --- Placeholder Screens ---
            composable<AppRoute.Tokens> {
                TokensScreen(onBack = { navController.popBackStack() })
            }
            composable<AppRoute.Sessions> {
                SessionsScreen(onBack = { navController.popBackStack() })
            }
            composable<AppRoute.Docs> {
                DocsScreen(onBack = { navController.popBackStack() })
            }
            composable<AppRoute.Favorites> {
                FavoritesScreen(onBack = { navController.popBackStack() })
            }

            // --- Settings (MFA) ---
            composable<AppRoute.Settings> {
                Settings(themeConfiguration = appliedTheme.configuration)
            }
        }
    }
}
