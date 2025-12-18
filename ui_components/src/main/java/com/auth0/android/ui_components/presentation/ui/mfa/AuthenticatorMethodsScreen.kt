package com.auth0.android.ui_components.presentation.ui.mfa

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auth0.android.ui_components.R
import com.auth0.android.ui_components.di.MyAccountModule
import com.auth0.android.ui_components.presentation.ui.components.CircularLoader
import com.auth0.android.ui_components.presentation.ui.components.ErrorHandler
import com.auth0.android.ui_components.presentation.ui.components.TopBar
import com.auth0.android.ui_components.presentation.ui.mfa.authenticator_methods.AuthenticatorListScreen
import com.auth0.android.ui_components.presentation.viewmodel.AuthenticatorMethodsViewModel
import com.auth0.android.ui_components.presentation.viewmodel.AuthenticatorUiData
import com.auth0.android.ui_components.presentation.viewmodel.AuthenticatorUiState
import com.auth0.android.ui_components.theme.Auth0Theme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AuthenticatorMethodsScreen(
    viewModel: AuthenticatorMethodsViewModel = viewModel(
        factory = MyAccountModule.provideAuthenticatorMethodViewModelFactory()
    ),
    onAuthenticatorItemClick: (AuthenticatorUiData) -> Unit,
    onBackPress: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val screenStyle = Auth0Theme.screenStyles.authenticatorMethods
    val horizontalPadding = screenStyle.horizontalPadding

    // Merge screen-level topBar style with display typography for this screen
    val topBarStyle = screenStyle.topBarStyle
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.login_security),
                showBackNavigation = false,
                style = topBarStyle,
                onBackClick = onBackPress
            )
        },
        containerColor = screenStyle.backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = horizontalPadding)
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
                    Column {
                        AuthenticatorListScreen(
                            state.data, onAuthenticatorItemClick,
                            screenStyle.listItemStyle
                        )
                    }
                }
            }
        }
    }
}

