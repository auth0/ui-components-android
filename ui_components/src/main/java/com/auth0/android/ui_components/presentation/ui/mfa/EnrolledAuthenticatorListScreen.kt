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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auth0.android.ui_components.R
import com.auth0.android.ui_components.di.MyAccountModule
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.model.EnrolledAuthenticationMethod
import com.auth0.android.ui_components.presentation.ui.components.CircularLoader
import com.auth0.android.ui_components.presentation.ui.components.EmptyAuthenticatorItem
import com.auth0.android.ui_components.presentation.ui.components.ErrorHandler
import com.auth0.android.ui_components.presentation.ui.components.InfoCard
import com.auth0.android.ui_components.presentation.ui.components.TopBar
import com.auth0.android.ui_components.presentation.ui.menu.MenuAction
import com.auth0.android.ui_components.presentation.ui.menu.MenuItem
import com.auth0.android.ui_components.presentation.ui.utils.UiStringFormat
import com.auth0.android.ui_components.presentation.viewmodel.EnrolledAuthenticatorViewModel
import com.auth0.android.ui_components.utils.DateUtil

/**
 * Main screen displaying the list of saved/enrolled authenticators
 * Uses the reusable InfoCard component for displaying authenticator details
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnrolledAuthenticatorListScreen(
    authenticatorType: AuthenticatorType,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    viewModel: EnrolledAuthenticatorViewModel = viewModel(
        factory = MyAccountModule.provideMFAEnrolledItemViewModelFactory(authenticatorType)
    )
) {
    val uiState by viewModel.uiState.collectAsState()


    Scaffold(
        topBar = {
            TopBar(
                title = UiStringFormat.formatTopBarTitleForAuthenticator(authenticatorType.type),
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {

            if (uiState.loading) {
                CircularLoader()
            } else {
                AuthenticatorListContent(
                    authenticatorType.type,
                    authenticators = uiState.authenticators,
                    onDeleteAuthenticator = { authenticator ->
                        viewModel.deleteAuthenticationMethod(authenticator.id)
                    },
                )
            }

            if (uiState.uiError != null) {
                ErrorHandler(
                    uiState.uiError!!
                )
            }
        }
    }
}

/**
 * Content section displaying the list of authenticators
 * Supports both single item and list views
 */
@Composable
fun AuthenticatorListContent(
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
            text = UiStringFormat.formatDescriptionForAuthenticator(authenticatorType),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (authenticators.isEmpty()) {
            EmptyAuthenticatorItem()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(authenticators) { authenticator ->
                    val menuActions = listOf(
                        MenuItem(stringResource(R.string.remove), MenuAction.Remove)
                    )

                    InfoCard(
                        title = authenticator.name
                            ?: UiStringFormat.formatDefaultNameForAuthenticatorItems(authenticator.type),
                        subtitles = listOf(
                            stringResource(R.string.created_on, DateUtil.formatIsoDate(authenticator.createdAt)),
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






