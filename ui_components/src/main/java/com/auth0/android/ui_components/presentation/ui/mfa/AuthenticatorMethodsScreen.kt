package com.auth0.android.ui_components.presentation.ui.mfa

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatorMethodsScreen(
    modifier: Modifier = Modifier,
    viewModel: AuthenticatorMethodsViewModel = viewModel(
        factory = MyAccountModule.provideAuthenticatorMethodViewModelFactory()
    ),
    onAuthenticatorItemClick: (AuthenticatorUiData) -> Unit,
    onBackPress: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.login_security),
                modifier = modifier,
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
                    Column {
                        AuthenticatorListScreen(state.data, onAuthenticatorItemClick)
                    }
                }
            }
        }
    }
}

