package com.auth0.android.ui_components.presentation.ui.mfa

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auth0.android.ui_components.R
import com.auth0.android.ui_components.di.MyAccountModule
import com.auth0.android.ui_components.presentation.ui.components.CircularLoader
import com.auth0.android.ui_components.presentation.ui.components.ErrorHandler
import com.auth0.android.ui_components.presentation.ui.components.TopBar
import com.auth0.android.ui_components.presentation.ui.mfa.authenticator_methods.PrimaryAuthenticatorListScreen
import com.auth0.android.ui_components.presentation.ui.mfa.authenticator_methods.SecondaryAuthenticatorListScreen
import com.auth0.android.ui_components.presentation.ui.passkeys.PasskeyViewModel
import com.auth0.android.ui_components.presentation.viewmodel.AuthenticatorMethodsViewModel
import com.auth0.android.ui_components.presentation.viewmodel.AuthenticatorUiState
import com.auth0.android.ui_components.presentation.viewmodel.SecondaryAuthenticatorUiData
import com.auth0.android.ui_components.theme.defaultTopbarTitle
import com.auth0.android.ui_components.utils.createCredential


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatorMethodsScreen(
    viewModel: AuthenticatorMethodsViewModel = viewModel(
        factory = MyAccountModule.provideAuthenticatorMethodViewModelFactory()
    ),
    passkeyViewModel: PasskeyViewModel = viewModel(
        factory = MyAccountModule.providePasskeyViewModelFactory()
    ),
    onPasskeyClick: () -> Unit,
    onAuthenticatorItemClick: (SecondaryAuthenticatorUiData) -> Unit,
    onBackPress: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val passkeyUiState by passkeyViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.login_security),
                showBackNavigation = false,
                titleTextStyle = defaultTopbarTitle,
                showSeparator = false,
                onBackClick = onBackPress
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            when (val state = uiState) {
                is AuthenticatorUiState.Error -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                    ) {
                        ErrorHandler(
                            uiError = state.error
                        )
                    }
                }

                AuthenticatorUiState.Loading -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularLoader()
                    }
                }

                is AuthenticatorUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        PrimaryAuthenticatorListScreen(
                            primaryAuthenticatorUiData = state.primaryData,
                            onAddPasskeyClick = {
                                passkeyViewModel.enrollPasskey {
                                    createCredential(context, it)
                                }
                            },
                            onPasskeysClick = {
                                passkeyViewModel.enrollPasskey {
                                    createCredential(context, it)
                                }
                            }
                        )
                        SecondaryAuthenticatorListScreen(
                            secondaryAuthenticatorUiData = state.secondaryData,
                            onAuthenticatorItemClick = onAuthenticatorItemClick
                        )
                    }
                }
            }
        }
    }
}

