package com.auth0.android.sample

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.storage.CredentialsManager
import com.auth0.android.authentication.storage.SharedPreferencesStorage
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.request.DefaultClient
import com.auth0.android.sample.ui.screens.AppearanceScreen
import com.auth0.android.sample.ui.screens.ChooseSignInScreen
import com.auth0.android.sample.ui.screens.DashboardDestination
import com.auth0.android.sample.ui.screens.DashboardScreen
import com.auth0.android.sample.ui.screens.DocsScreen
import com.auth0.android.sample.ui.screens.EmbeddedLoginScreen
import com.auth0.android.sample.ui.screens.EnablePasskeyScreen
import com.auth0.android.sample.ui.screens.ExploreLoginScreen
import com.auth0.android.sample.ui.screens.FavoritesScreen
import com.auth0.android.sample.ui.screens.LoginScreen
import com.auth0.android.sample.ui.screens.LoginSecurityScreen
import com.auth0.android.sample.ui.screens.ProfileScreen
import com.auth0.android.sample.ui.screens.SessionsScreen
import com.auth0.android.sample.ui.screens.Settings
import com.auth0.android.sample.ui.screens.TokensScreen
import com.auth0.android.sample.ui.screens.UpdateFullNameScreen
import com.auth0.android.sample.ui.theme.ThemePreset
import com.auth0.android.sample.ui.theme.Ui_components_androidTheme
import com.auth0.android.sample.ui.viewmodels.AuthState
import com.auth0.android.sample.ui.viewmodels.AuthViewModel
import com.auth0.android.ui_components.Auth0UI
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

    private val credentialsManager: CredentialsManager by lazy {
        CredentialsManager(AuthenticationAPIClient(account), SharedPreferencesStorage(this))
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
                        logoutBuilder = logoutBuilder
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
    authViewModel: AuthViewModel = viewModel()
) {
    val navController = rememberNavController()
    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()

    val authState by authViewModel.authState.collectAsStateWithLifecycle()


    when (authState) {
        is AuthState.Authenticated -> {
            credentialsManager.saveCredentials((authState as AuthState.Authenticated).credentials)
            navController.navigate("dashboard") {
                popUpTo("chooseSignIn") { inclusive = true }
            }
        }

        is AuthState.Error -> {
            Toast.makeText(
                LocalContext.current,
                (authState as AuthState.Error).message,
                Toast.LENGTH_SHORT
            )
                .show()
        }

        else -> {
            Log.d("LoginScreen", ": ${authState}")
        }
    }

    // Determine start destination based on existing credentials
    val startDestination = if (credentialsManager.hasValidCredentials()) {
        // Load profile from existing credentials on resume
        authViewModel.loadProfileFromCredentials(credentialsManager)
        "dashboard"
    } else {
        "chooseSignIn"
    }

    Auth0Theme {
        NavHost(
            navController = navController,
            startDestination = startDestination,
        ) {

            composable("chooseSignIn") {
                val context = LocalContext.current
                ChooseSignInScreen(
                    onHostedLogin = { authViewModel.login(context, webAuthProvider) },
                    onEmbeddedLogin = { navController.navigate("embeddedLogin") },
                    onSettings = { navController.navigate("appearance") }
                )
            }

            composable("embeddedLogin") {
                EmbeddedLoginScreen(
                    onGoogleLogin = {
                        navController.navigate("login")
                    },
                    onContinueWithEmail = { _ ->
                        navController.navigate("login")
                    },
                    onOtherMethods = { navController.navigate("exploreLogin") },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("exploreLogin") {
                ExploreLoginScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable("login") {
                LoginScreen(
                    webAuthProvider = webAuthProvider,
                    authViewModel = authViewModel,
                    onLoginSuccess = { credentials ->
                        credentialsManager.saveCredentials(credentials)
                        navController.navigate("dashboard") {
                            popUpTo("chooseSignIn") { inclusive = true }
                        }
                    }
                )
            }

            composable("dashboard") {
                DashboardScreen(
                    userName = userProfile.name,
                    onNavigate = { destination ->
                        when (destination) {
                            DashboardDestination.Profile -> navController.navigate("profile")
                            DashboardDestination.LoginSecurity -> navController.navigate("loginSecurity")
                            DashboardDestination.Tokens -> navController.navigate("tokens")
                            DashboardDestination.Sessions -> navController.navigate("sessions")
                            DashboardDestination.Docs -> navController.navigate("docs")
                            DashboardDestination.Favorites -> navController.navigate("favorites")
                        }
                    },
                    onLogout = {
                        authViewModel.logout(
                            navController.context,
                            credentialsManager,
                            logoutBuilder
                        )
                        navController.navigate("chooseSignIn") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // --- Profile ---
            composable("profile") {
                ProfileScreen(
                    userName = userProfile.name,
                    userEmail = userProfile.email,
                    onBack = { navController.popBackStack() },
                    onEditName = { navController.navigate("updateFullName") },
                    onTheme = { navController.navigate("appearance") }
                )
            }

            composable("updateFullName") {
                UpdateFullNameScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable("enablePasskey") {
                EnablePasskeyScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable("loginSecurity") {
                LoginSecurityScreen(
                    onBack = { navController.navigateUp() },
                    onManageMfa = {
                        navController.navigate("settings/0")
                    }
                )
            }

            // --- Appearance ---
            composable("appearance") {
                AppearanceScreen(
                    onBack = { navController.popBackStack() },
                    onUpdateTheme = { presetIndex ->
                        navController.navigate("settings/$presetIndex")
                    }
                )
            }

            // --- Placeholder Screens ---
            composable("tokens") {
                TokensScreen(onBack = { navController.popBackStack() })
            }
            composable("sessions") {
                SessionsScreen(onBack = { navController.popBackStack() })
            }
            composable("docs") {
                DocsScreen(onBack = { navController.popBackStack() })
            }
            composable("favorites") {
                FavoritesScreen(onBack = { navController.popBackStack() })
            }

            // --- Settings with Theme (existing) ---
            composable(
                route = "settings/{presetIndex}",
                arguments = listOf(navArgument("presetIndex") { type = NavType.IntType })
            ) { backStackEntry ->
                val presetIndex = backStackEntry.arguments?.getInt("presetIndex") ?: 0
                val preset = ThemePreset.all().getOrElse(presetIndex) { ThemePreset.DefaultLight }
                Settings(themeConfiguration = preset.configuration)
            }
        }
    }
}
