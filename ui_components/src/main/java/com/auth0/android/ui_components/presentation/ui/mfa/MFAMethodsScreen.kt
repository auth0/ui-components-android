package com.auth0.android.ui_components.presentation.ui.mfa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auth0.android.ui_components.R
import com.auth0.android.ui_components.di.MyAccountModule
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.model.MFAMethod
import com.auth0.android.ui_components.presentation.ui.UiState
import com.auth0.android.ui_components.presentation.ui.components.CircularLoader
import com.auth0.android.ui_components.presentation.ui.components.ErrorScreen
import com.auth0.android.ui_components.presentation.ui.components.TopBar
import com.auth0.android.ui_components.presentation.viewmodel.MFAMethodViewModel
import com.auth0.android.ui_components.presentation.viewmodel.MFAUiModel
import com.auth0.android.ui_components.theme.Gray


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MFAMethodsScreen(
    modifier: Modifier,
    viewModel: MFAMethodViewModel = viewModel(
        factory = MyAccountModule.provideMFAMethodViewModelFactory()
    ),
    onAuthenticatorClick: (String) -> Unit,
    onBackPress: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.fetchMFAMethods()
    }

    Scaffold(
        topBar = {
            TopBar(
                title = "Login & Security",
                modifier = modifier,
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
                is UiState.Error -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                    ) {
                        ErrorScreen(
                            mainErrorMessage = state.exception?.message ?: "An error occurred",
                            "We are unable to process your request. Please try again in a few minutes. If this problem persists, please ",
                            clickableString = "contact us."
                        )
                    }
                }

                UiState.Loading -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularLoader()
                    }
                }

                is UiState.Success -> {
                    MfaListScreen(state.data, onAuthenticatorClick)
                }
            }
        }
    }
}


@Composable
fun MfaListScreen(
    mfaMethodList: List<MFAUiModel>,
    onAuthenticatorClick: (String) -> Unit
) {

    Text(
        text = "Verification methods",
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp
        ),
        color = Color.Black
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = "Manage your 2FA methods",
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 15.sp
        ),
        color = Gray
    )

    Spacer(modifier = Modifier.height(16.dp))

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(mfaMethodList) { mfaMethod ->
            MFAMethodItem(
                title = mfaMethod.title,
                subtitle = mfaMethod.description,
                leadingIcon = getMFAMethodIcon(mfaMethod.type),
                showDefaultTag = mfaMethod.confirmed,
                onClick = { onAuthenticatorClick(mfaMethod.type.name) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun getMFAMethodIcon(authenticatorType: AuthenticatorType): Painter {
    return painterResource(
        when (authenticatorType) {
            AuthenticatorType.TOTP -> R.drawable.ic_authenticator
            AuthenticatorType.SMS -> R.drawable.ic_sms_otp
            AuthenticatorType.EMAIL -> R.drawable.ic_sms_otp
            AuthenticatorType.PUSH -> R.drawable.ic_authenticator
            AuthenticatorType.RECOVERY_CODE -> R.drawable.ic_recovery_code
        }
    )
}

