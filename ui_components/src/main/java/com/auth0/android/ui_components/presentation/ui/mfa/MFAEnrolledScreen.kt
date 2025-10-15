package com.auth0.android.ui_components.presentation.ui.mfa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auth0.android.ui_components.di.MyAccountModule
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.model.EnrolledAuthenticationMethod
import com.auth0.android.ui_components.presentation.ui.UiState
import com.auth0.android.ui_components.presentation.ui.UiUtils
import com.auth0.android.ui_components.presentation.ui.components.CircularLoader
import com.auth0.android.ui_components.presentation.ui.components.ErrorScreen
import com.auth0.android.ui_components.presentation.ui.components.InfoCard
import com.auth0.android.ui_components.presentation.ui.components.TopBar
import com.auth0.android.ui_components.presentation.ui.menu.MenuAction
import com.auth0.android.ui_components.presentation.ui.menu.MenuItem
import com.auth0.android.ui_components.presentation.viewmodel.MFAEnrolledItemViewModel
import com.auth0.android.ui_components.utils.DateUtil

/**
 * Main screen displaying the list of saved/enrolled authenticators
 * Uses the reusable InfoCard component for displaying authenticator details
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MFAEnrolledScreen(
    authenticatorType: AuthenticatorType,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    viewModel: MFAEnrolledItemViewModel = viewModel(
        factory = MyAccountModule.provideMFAEnrolledItemViewModelFactory()
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchEnrolledAuthenticators(authenticatorType)
    }

    Scaffold(
        topBar = {
            TopBar(
                title = UiUtils.formatTopBarTitleForAuthenticator(authenticatorType.type),
                topBarColor = Color.White,
                showSeparator = false,
                trailingIcon = rememberVectorPainter(Icons.Default.Add),
                trailingIconClick = onAddClick,
                onBackClick = onBackClick
            )
        },
        containerColor = Color.White,
        modifier = modifier
    ) { paddingValues ->
        when (val state = uiState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularLoader()
                }
            }

            is UiState.Success -> {
                AuthenticatorListContent(
                    authenticatorType.type,
                    authenticators = state.data,
                    onDeleteAuthenticator = { authenticator ->
                        viewModel.deleteAuthenticationMethod(authenticator.id)
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    ErrorScreen(
                        mainErrorMessage = state.exception.message ?: "An error occurred",
                        description = "We are unable to process your request. Please try again in a few minutes. If this problem persists, please",
                        modifier = modifier,
                        clickableString = "contact us.",
                    )
                }
            }
        }
    }
}

/**
 * Content section displaying the list of authenticators
 * Supports both single item and list views
 */
@Composable
private fun AuthenticatorListContent(
    authenticatorType: String,
    authenticators: List<EnrolledAuthenticationMethod>,
    onDeleteAuthenticator: (EnrolledAuthenticationMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = UiUtils.formatDescriptionForAuthenticator(authenticatorType),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (authenticators.isEmpty()) {
            EmptyStateMessage()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(authenticators) { authenticator ->
                    val menuActions = listOf(
                        MenuItem("Remove", MenuAction.Remove)
                    )

                    InfoCard(
                        title = UiUtils.formatDefaultNameForAuthenticatorItems(authenticator.type),
                        subtitles = listOf(
                            "Created on ${DateUtil.formatIsoDate(authenticator.createdAt)}",
                            "Last used on ${DateUtil.formatIsoDate(authenticator.createdAt)}"
                        ),
                        menuActions = menuActions,
                        onMenuActionClick = { action ->
                            when (action) {
                                MenuAction.Remove -> onDeleteAuthenticator(authenticator)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Empty state message when no authenticators are available
 */
@Composable
private fun EmptyStateMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No authenticators found",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}



