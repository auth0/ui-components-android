package com.auth0.universalcomponents.presentation.ui.mfa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.di.UniversalComponentsModule
import com.auth0.universalcomponents.presentation.ui.components.CircularLoader
import com.auth0.universalcomponents.presentation.ui.components.ErrorHandler
import com.auth0.universalcomponents.presentation.ui.components.TopBar
import com.auth0.universalcomponents.presentation.ui.components.skeleton.AuthMethodCardSkeleton
import com.auth0.universalcomponents.presentation.ui.components.skeleton.SkeletonLine
import com.auth0.universalcomponents.presentation.ui.components.skeleton.SkeletonList
import com.auth0.universalcomponents.presentation.ui.components.skeleton.shimmer
import com.auth0.universalcomponents.presentation.ui.mfa.authenticatormethods.PrimaryAuthenticatorListScreen
import com.auth0.universalcomponents.presentation.ui.mfa.authenticatormethods.SecondaryAuthenticatorListScreen
import com.auth0.universalcomponents.presentation.ui.passkeys.PasskeyEvent
import com.auth0.universalcomponents.presentation.ui.passkeys.PasskeyUiState
import com.auth0.universalcomponents.presentation.ui.passkeys.PasskeyViewModel
import com.auth0.universalcomponents.presentation.ui.utils.ObserveAsEvents
import com.auth0.universalcomponents.presentation.viewmodel.AuthenticatorMethodsViewModel
import com.auth0.universalcomponents.presentation.viewmodel.AuthenticatorUiState
import com.auth0.universalcomponents.presentation.viewmodel.SecondaryAuthenticatorUiData
import com.auth0.universalcomponents.theme.Auth0Theme
import com.auth0.universalcomponents.utils.createCredential

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AuthenticatorMethodsScreen(
    authenticatorMethodViewModel: AuthenticatorMethodsViewModel = viewModel(
        factory = UniversalComponentsModule.provideAuthenticatorMethodViewModelFactory()
    ),
    passkeyViewModel: PasskeyViewModel = viewModel(
        factory = UniversalComponentsModule.providePasskeyViewModelFactory()
    ),
    onPasskeyClick: () -> Unit,
    onAuthenticatorItemClick: (SecondaryAuthenticatorUiData) -> Unit,
    onBackPress: () -> Unit
) {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography

    val uiState by authenticatorMethodViewModel.uiState.collectAsStateWithLifecycle()
    val passkeyUiState by passkeyViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ObserveAsEvents(passkeyViewModel.events) { event ->
        when (event) {
            is PasskeyEvent.EnrollmentSuccess -> {
                onPasskeyClick()
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.login_security),
                showBackNavigation = passkeyUiState is PasskeyUiState.Error,
                showSeparator = false,
                titleTextStyle = typography.displayMedium,
                onBackClick = {
                    if (passkeyUiState is PasskeyUiState.Error) {
                        passkeyViewModel.resetState()
                    } else {
                        onBackPress()
                    }
                }
            )
        },
        containerColor = colors.backgroundLayerBase
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            val sizes = Auth0Theme.sizes
            val dimensions = Auth0Theme.dimensions

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = sizes.padding)
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
                        // Skeleton mirrors the loaded layout → no layout shift when data arrives.
                        // One shimmer on the container = one synchronized sweep over header + cards.
                        val loadingDescription = stringResource(R.string.loading)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = dimensions.spacingMd)
                                .shimmer()
                                .semantics { contentDescription = loadingDescription },
                        ) {
                            SkeletonLine(width = 180.dp, modifier = Modifier.height(22.dp))
                            Spacer(Modifier.height(dimensions.spacingMd))
                            SkeletonList(count = 5) { AuthMethodCardSkeleton() }
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
                                    if (state.primaryData.isEmpty()) {
                                        passkeyViewModel.enrollPasskey {
                                            createCredential(context, it)
                                        }
                                    } else {
                                        onPasskeyClick()
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

            when (val state = passkeyUiState) {
                is PasskeyUiState.RequestingChallenge,
                is PasskeyUiState.EnrollingPasskey -> {
                    Box(
                        modifier = Modifier
                            .background(colors.backgroundLayerBase)
                            .fillMaxSize()
                            .padding(horizontal = dimensions.spacingMd),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularLoader()
                    }
                }

                is PasskeyUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = dimensions.spacingMd)
                    ) {
                        ErrorHandler(
                            uiError = state.error,
                            shouldRetry = state.shouldRetry
                        )
                    }
                }

                else -> {}
            }
        }
    }
}
