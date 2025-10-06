package com.auth0.android.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.storage.CredentialsManager
import com.auth0.android.authentication.storage.SharedPreferencesStorage
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.request.DefaultClient
import com.auth0.android.sample.ui.screens.LoginScreen
import com.auth0.android.sample.ui.screens.Settings
import com.auth0.android.sample.ui.theme.Ui_components_androidTheme

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Ui_components_androidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SampleApp(credentialsManager, webAuthProvider)
                }
            }
        }
    }
}

@Composable
fun SampleApp(credentialsManager: CredentialsManager, webAuthProvider: WebAuthProvider.Builder) {

    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "login",
    ) {
        composable("login") {

            if (credentialsManager.hasValidCredentials()) {
                Settings()
            } else
                LoginScreen(
                    webAuthProvider = webAuthProvider,
                    onLoginSuccess = { credentials ->
                        credentialsManager.saveCredentials(credentials)
                        navController.navigate("settings") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
        }

        composable("settings") {
            Settings()
        }
    }

}