package com.auth0.android.ui_components.presentation.ui.mfa

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auth0.android.ui_components.R
import com.auth0.android.ui_components.di.MyAccountModule
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.model.EnrollmentResult
import com.auth0.android.ui_components.presentation.ui.components.CircularLoader
import com.auth0.android.ui_components.presentation.ui.components.ErrorHandler
import com.auth0.android.ui_components.presentation.ui.components.GradientButton
import com.auth0.android.ui_components.presentation.ui.components.TopBar
import com.auth0.android.ui_components.presentation.ui.utils.ObserveAsEvents
import com.auth0.android.ui_components.presentation.viewmodel.EnrollmentEvent
import com.auth0.android.ui_components.presentation.viewmodel.EnrollmentUiState
import com.auth0.android.ui_components.presentation.viewmodel.EnrollmentViewModel
import com.auth0.android.ui_components.theme.AuthenticatorItemBorder
import com.auth0.android.ui_components.theme.contentTextStyle
import com.auth0.android.ui_components.theme.sectionTitle
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryCodeEnrollmentScreen(
    authenticatorType: AuthenticatorType = AuthenticatorType.RECOVERY_CODE,
    viewModel: EnrollmentViewModel = viewModel(
        factory = MyAccountModule.provideEnrollmentViewModelFactory(authenticatorType)
    ),
    onBackClick: () -> Unit,
    onContinue: (
        String, String
    ) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    var enrollmentResult by rememberSaveable {
        mutableStateOf<EnrollmentResult?>(null)
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is EnrollmentEvent.EnrollmentChallengeSuccess -> {
                enrollmentResult = event.enrollmentResult
            }

            is EnrollmentEvent.VerificationSuccess -> {
                onContinue(
                    event.authenticationMethod.id,
                    event.authenticationMethod.type
                )
                Log.d("EmailEnrollmentScreen", "$event not handled ")
            }
        }
    }


    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.recovery_code),
                onBackClick = onBackClick,
                showSeparator = false
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.White
    ) { _ ->
        enrollmentResult?.let {
            val result = it as EnrollmentResult.RecoveryCodeEnrollment
            RecoveryCodeContent(
                recoveryCode = result.challenge.recoveryCode,
                uiState,
                onCopyClick = {
                    clipboardManager.setText(AnnotatedString(result.challenge.recoveryCode))
                    MainScope().launch {
                        snackbarHostState.showSnackbar("Copied to clipboard")
                    }
                },
                onContinueClick = {
                    viewModel.verifyWithoutOtp(
                        authenticationMethodId = result.authenticationMethodId,
                        authSession = result.authSession
                    )
                }
            )
        }

        LoadingScreen(uiState)

        ErrorScreen(uiState)
    }
}

/**
 * Main content showing recovery code and instructions
 */
@Composable
private fun RecoveryCodeContent(
    recoveryCode: String,
    state: EnrollmentUiState,
    onCopyClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.width(300.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RecoveryCodeHeader()

            Spacer(modifier = Modifier.height(40.dp))

            RecoveryCodeDisplay(recoveryCode, onCopyClick)

            Spacer(modifier = Modifier.height(24.dp))

            ContinueButton(
                isLoading = state.verifyingAuthenticator,
                onClick = onContinueClick
            )
        }
    }
}


@Composable
private fun RecoveryCodeHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.save_recovery_code),
            style = sectionTitle,
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
        )

        Text(
            text = stringResource(R.string.recovery_code_description),
            style = contentTextStyle,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RecoveryCodeDisplay(code: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = shape,
        color = Color.White,
        shadowElevation = 1.dp,
        border = BorderStroke(
            width = 1.dp,
            color = AuthenticatorItemBorder
        )
    ) {
        Row(
            modifier = Modifier
                .height(35.dp)
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                style = contentTextStyle,
                text = code,
                lineHeight = 20.sp,
                letterSpacing = 0.2.sp,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onClick) {

                Icon(
                    painter = painterResource(id = R.drawable.ic_copy),
                    contentDescription = "Copy",
                    modifier = Modifier
                        .size(16.dp)
                        .padding(vertical = 2.dp),
                    tint = Color.Black
                )
            }
        }
    }
}

/**
 * Continue button (Primary variant)
 */
@Composable
private fun ContinueButton(
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    GradientButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        gradient = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.15f),
                Color.Transparent
            )
        ),
        isLoading = isLoading,
        enabled = !isLoading,
        onClick = onClick
    ) {
        Text(stringResource(R.string.continue_button))
    }
}

@Composable
private fun LoadingScreen(state: EnrollmentUiState) {
    if (state.enrollingAuthenticator)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            CircularLoader()
        }
}

@Composable
private fun ErrorScreen(state: EnrollmentUiState) {
    state.uiError?.let {
        ErrorHandler(it)
    }
}