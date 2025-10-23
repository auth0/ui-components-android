package com.auth0.android.ui_components.presentation.ui.mfa

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auth0.android.ui_components.di.MyAccountModule
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.model.EnrollmentInput
import com.auth0.android.ui_components.domain.model.EnrollmentResult
import com.auth0.android.ui_components.presentation.ui.components.CircularLoader
import com.auth0.android.ui_components.presentation.ui.components.ErrorHandler
import com.auth0.android.ui_components.presentation.ui.components.GradientButton
import com.auth0.android.ui_components.presentation.ui.components.TopBar
import com.auth0.android.ui_components.presentation.viewmodel.EnrollmentUiState
import com.auth0.android.ui_components.presentation.viewmodel.EnrollmentViewModel
import com.auth0.android.ui_components.theme.TextGray
import com.auth0.android.ui_components.utils.ValidationUtil

/**
 * Email Enrollment Screen
 *
 * Allows users to enter their email address for MFA enrollment.
 * Validates email format and initiates enrollment via EnrollmentViewModel.
 * On success, navigates to OTP verification screen.
 *
 * @param authenticatorType Type of authenticator (should be EMAIL)
 * @param viewModel EnrollmentViewModel instance for handling enrollment
 * @param onBackClick Callback for back navigation
 * @param onContinueToOTP Callback when enrollment succeeds, passes authenticationId and authSession
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailEnrollmentScreen(
    authenticatorType: AuthenticatorType,
    viewModel: EnrollmentViewModel = viewModel(
        factory = MyAccountModule.provideEnrollmentViewModelFactory(authenticatorType)
    ),
    onBackClick: () -> Unit,
    onContinueToOTP: (authenticationId: String, authSession: String, email: String) -> Unit = { _, _, _ -> }
) {
    var email by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopBar(
                title = "Add Email OTP",
                onBackClick = onBackClick,
                showSeparator = false
            )
        },
        containerColor = Color.White
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {


            when (val state = uiState) {
                is EnrollmentUiState.EnrollmentInitiated -> {
                    when (val result = state.enrollmentResult) {
                        is EnrollmentResult.DefaultEnrollment -> {
                            onContinueToOTP(
                                result.authenticationMethodId,
                                result.authSession,
                                email
                            )
                            viewModel.resetState()
                        }

                        else -> {
                            validationError = true
                            errorMessage = "Unexpected enrollment result"
                        }
                    }
                }

                is EnrollmentUiState.Error -> {
                    validationError = true
                    ErrorHandler(state.uiError)
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

                else -> {
                    // Other states handled in UI
                }
            }







            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EmailEnrollmentHeader()

                EmailFormField(
                    email = email,
                    onEmailChange = { newEmail ->
                        email = newEmail
                        if (validationError) {
                            validationError = false
                            errorMessage = ""
                        }
                    },
                    isValidationError = validationError,
                    errorMessage = errorMessage
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            ContinueButton(
                onClick = {
                    if (!ValidationUtil.isValidEmail(email)) {
                        validationError = true
                        errorMessage = ValidationUtil.getEmailErrorMessage(email)
                    } else {
                        validationError = false
                        errorMessage = ""
                        viewModel.startEnrollment(
                            authenticatorType = AuthenticatorType.EMAIL,
                            input = EnrollmentInput.Email(email)
                        )
                    }
                }
            )
        }
    }
}

/**
 * Email Enrollment Header Component
 *
 * Displays title and description for email enrollment
 */
@Composable
private fun EmailEnrollmentHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Enter your Email address",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            color = Color.Black,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "We will send you a verification code.",
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = TextGray,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EmailFormField(
    email: String,
    onEmailChange: (String) -> Unit,
    isValidationError: Boolean,
    errorMessage: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Label
        Text(
            text = "Email",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1F1F1F),
            lineHeight = 14.sp
        )

        EmailTextField(
            value = email,
            onValueChange = onEmailChange,
            isError = isValidationError,
            errorMessage = errorMessage
        )
    }
}

/**
 * Email Text Field Component
 *
 * Custom styled text input field with error state support
 */
@Composable
private fun EmailTextField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String
) {
    val backgroundColor = if (isError) {
        Color(0xFFB82819).copy(alpha = 0.05f)
    } else {
        Color.White
    }

    val borderColor = if (isError) {
        Color(0xFFB82819).copy(alpha = 0.25f)
    } else {
        Color(0xFFE0E0E0)
    }

    val textColor = if (isError) {
        Color(0xFFCA3B2B)
    } else {
        Color(0xFF1F1F1F)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val shape = RoundedCornerShape(14.dp)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(61.dp)
                .background(
                    color = backgroundColor,
                    shape = shape
                )
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = shape
                ),
            shadowElevation = 8.dp,
            shape = shape,
        )
        {

            OutlinedTextField(

                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .background(
                        color = backgroundColor,
                        shape = shape
                    )
                    .border(
                        width = 0.dp,
                        color = borderColor,
                        shape = shape
                    ),
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 20.sp,
                    color = textColor
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = borderColor,
                    cursorColor = Color.Gray,
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                ),
                singleLine = true,
            )
        }

        if (isError && errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Color(0xFFCA3B2B),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun ContinueButton(
    onClick: () -> Unit,
) {
    GradientButton(
        text = "Continue",
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