package com.auth0.android.ui_components.presentation.ui.mfa

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
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
import kotlinx.coroutines.launch

/**
 * Recovery Code Enrollment Screen
 *
 * Displays recovery codes for backup authentication.
 * Users must save these codes as they are the backup sign-in method
 * if their multifactor device is unavailable.
 *
 * @param authenticatorType Type of authenticator (should be RECOVERY_CODE)
 * @param viewModel EnrollmentViewModel instance for handling enrollment
 * @param onBackClick Callback for back navigation
 * @param onContinue Callback when user continues after saving codes
 */
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

                            RecoveryCodeContent(
                                recoveryCode = recoveryCode,
                                onCopyClick = {
                                    clipboardManager.setText(AnnotatedString(recoveryCode))
                                    kotlinx.coroutines.MainScope().launch {
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularLoader()
                    }
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
            modifier = Modifier.width(286.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Section
            RecoveryCodeHeader()

            Spacer(modifier = Modifier.height(40.dp))
            // Body Section
            RecoveryCodeBody(recoveryCode = recoveryCode)

            Spacer(modifier = Modifier.height(24.dp))

            // Copy Code Button
            CopyCodeButton(onClick = onCopyClick)

            Spacer(modifier = Modifier.height(24.dp))

            // Continue Button
            ContinueButton(onClick = onContinueClick)
        }
    }
}

/**
 * Header with title and description
 */
@Composable
private fun RecoveryCodeHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.save_recovery_code),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            fontSize = 24.sp,
            lineHeight = 27.6.sp,
            letterSpacing = 0.3.sp,
            color = Color(0xFF191919),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = stringResource(R.string.recovery_code_description),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 17.5.sp,
            letterSpacing = (-0.084).sp,
            color = Color(0xFF737373),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Body section with label and recovery code display
 */
@Composable
private fun RecoveryCodeBody(recoveryCode: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Label
        Text(
            text = stringResource(R.string.recovery_code_label),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 14.sp,
            color = Color(0xFF1F1F1F),
            modifier = Modifier.fillMaxWidth()
        )

        // Recovery Code Display (OTP Field format)
        RecoveryCodeDisplay(code = recoveryCode)
    }
}

/**
 * Displays recovery code in OTP field format with separator
 */
@Composable
private fun RecoveryCodeDisplay(code: String) {
    // Format: ABC-DEF (3 characters, separator, 3 characters)
    val characters = code.take(6).toCharArray()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // First 3 characters
        repeat(3) { index ->
            if (index < characters.size) {
                RecoveryCodeCharacter(char = characters[index])
            } else {
                RecoveryCodeCharacter(char = ' ')
            }
        }

        // Separator
        Text(
            text = "-",
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 32.sp,
            color = Color(0xFF828282),
            textAlign = TextAlign.Center
        )

        // Last 3 characters
        repeat(3) { index ->
            val charIndex = index + 3
            if (charIndex < characters.size) {
                RecoveryCodeCharacter(char = characters[charIndex])
            } else {
                RecoveryCodeCharacter(char = ' ')
            }
        }
    }
}

/**
 * Individual character box for recovery code
 */
@Composable
private fun RecoveryCodeCharacter(char: Char) {
    val shape = RoundedCornerShape(14.dp)

    Surface(
        modifier = Modifier
            .width(38.dp)
            .height(52.dp)
            .background(
                color = Color.White,
                shape = shape
            )
            .border(
                width = 3.dp,
                color = Color(0xFFCECECE).copy(alpha = 0.5f),
                shape = shape
            ),
        shadowElevation = 0.dp,
        shape = shape,
        color = Color.White
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = char.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 28.sp,
                color = Color(0xFF1F1F1F),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Copy Code button (Outline variant)
 */
@Composable
private fun CopyCodeButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color(0xFF262420).copy(alpha = 0.35f)
        )
    ) {
        Text(
            text = stringResource(R.string.copy_code),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 24.sp,
            color = Color(0xFF262420)
        )
    }
}

/**
 * Continue button (Primary variant)
 */
@Composable
private fun ContinueButton(onClick: () -> Unit) {
    GradientButton(
        text = stringResource(R.string.continue_button),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        gradient = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.15f),
                Color.Transparent
            )
        ),
    ) {
        onClick()
    }
}