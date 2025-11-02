package com.auth0.android.ui_components.presentation.ui.mfa

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.auth0.android.ui_components.presentation.ui.components.ErrorScreen
import com.auth0.android.ui_components.presentation.ui.components.GradientButton
import com.auth0.android.ui_components.presentation.ui.components.TopBar
import com.auth0.android.ui_components.presentation.viewmodel.EnrollmentUiState
import com.auth0.android.ui_components.presentation.viewmodel.EnrollmentViewModel
import com.auth0.android.ui_components.theme.AuthenticatorItemBorder
import com.auth0.android.ui_components.theme.contentTextStyle
import com.auth0.android.ui_components.theme.sectionTitle
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
    var recoveryCode by remember { mutableStateOf("") }
    var enrollmentData by remember { mutableStateOf<Pair<String, String>?>(null) }


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
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is EnrollmentUiState.Idle -> {
                    // Initial state
                }

                is EnrollmentUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularLoader()
                    }
                }

                is EnrollmentUiState.EnrollmentInitiated -> {
                    when (val result = state.enrollmentResult) {
                        is EnrollmentResult.RecoveryCodeEnrollment -> {
                            // Extract recovery code from challenge
                            recoveryCode = result.challenge.recoveryCode
                            enrollmentData = Pair(result.authenticationMethodId, result.authSession)

                            RecoveryCodeContent(
                                recoveryCode = recoveryCode,
                                isVerifying = false,
                                onCopyClick = {
                                    clipboardManager.setText(AnnotatedString(recoveryCode))
                                    kotlinx.coroutines.MainScope().launch {
                                        snackbarHostState.showSnackbar("Copied to clipboard")
                                    }
                                },
                                onContinueClick = {
                                    enrollmentData?.let { (authMethodId, authSession) ->
                                        viewModel.verifyWithoutOtp(
                                            authenticationMethodId = authMethodId,
                                            authSession = authSession
                                        )
                                    }
                                }
                            )
                        }

                        else -> {
                            ErrorScreen(
                                mainErrorMessage = stringResource(R.string.unexpected_enrollment_result),
                                stringResource(R.string.try_again),
                                Modifier
                            )
                        }
                    }
                }

                is EnrollmentUiState.Verifying -> {
                    RecoveryCodeContent(
                        recoveryCode = recoveryCode,
                        isVerifying = true,
                        onCopyClick = {
                            clipboardManager.setText(AnnotatedString(recoveryCode))
                            kotlinx.coroutines.MainScope().launch {
                                snackbarHostState.showSnackbar("Copied to clipboard")
                            }
                        },
                        onContinueClick = {} // Disabled during verification
                    )
                }

                is EnrollmentUiState.Success -> {
                    LaunchedEffect(Unit) {
                        onContinue(state.authenticationMethod.id, state.authenticationMethod.type)

                    }
                }

                is EnrollmentUiState.Error -> {
                    ErrorHandler(state.uiError)
                }
            }
        }
    }
}

/**
 * Main content showing recovery code and instructions
 */
@Composable
private fun RecoveryCodeContent(
    recoveryCode: String,
    isVerifying: Boolean,
    onCopyClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(178.dp))

        Column(
            modifier = Modifier.width(300.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RecoveryCodeHeader()

            Spacer(modifier = Modifier.height(40.dp))

            RecoveryCodeDisplay(recoveryCode, onCopyClick)

            Spacer(modifier = Modifier.height(24.dp))

            ContinueButton(
                isLoading = isVerifying,
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