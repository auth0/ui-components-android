package com.auth0.android.ui_components.presentation.ui.mfa

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.ui_components.di.MyAccountModule
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.presentation.ui.components.CircularLoader
import com.auth0.android.ui_components.presentation.ui.components.GradientButton
import com.auth0.android.ui_components.presentation.ui.components.TopBar
import com.auth0.android.ui_components.presentation.viewmodel.EnrollmentUiState
import com.auth0.android.ui_components.presentation.viewmodel.EnrollmentViewModel
import com.auth0.android.ui_components.theme.ButtonBlack
import com.auth0.android.ui_components.theme.TextGray

/**
 * OTP Verification Screen
 *
 * A reusable screen for verifying OTP codes from various authenticator types.
 * Supports error states, resend functionality, and customizable messaging.
 * Integrates with EnrollmentViewModel for API calls and state management.
 *
 * @param authenticatorType Type of authenticator (TOTP, SMS, EMAIL, etc.)
 * @param authenticationId The authentication method ID
 * @param authSession The authentication session token
 * @param phoneNumberOrEmail Optional phone number or email to display (e.g., "+1(•••)•••1234")
 * @param showResendOption Whether to show the "Resend" link
 * @param viewModel EnrollmentViewModel instance for handling verification
 * @param onBackClick Callback for back navigation
 * @param onVerificationSuccess Callback when verification is successful
 * @param onResend Callback when resend link is clicked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OTPVerificationScreen(
    authenticatorType: AuthenticatorType,
    authenticationId: String,
    authSession: String,
    phoneNumberOrEmail: String? = null,
    showResendOption: Boolean = true,
    viewModel: EnrollmentViewModel = viewModel(
        factory = MyAccountModule.provideEnrollmentViewModelFactory()
    ),
    onBackClick: () -> Unit = {},
    onVerificationSuccess: () -> Unit = {},
    onResend: () -> Unit = {}
) {
    // State management
    var otpValue by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val uiState by viewModel.uiState.collectAsState()

    val screenConfig = remember(authenticatorType, phoneNumberOrEmail) {
        getScreenConfigText(authenticatorType, phoneNumberOrEmail)
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is EnrollmentUiState.Success -> {
                // Verification successful - navigate to success screen
                onVerificationSuccess()
            }

            is EnrollmentUiState.Error -> {
                // Handle error from API
                isError = true
                errorMessage = getErrorMessage(state.uiError.error.cause)
            }

            else -> {
                // Other states handled in UI
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = screenConfig.title,
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
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Main Heading
            Text(
                text = screenConfig.headline,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = Color.Black,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle (if available)
            if (screenConfig.description.isNotEmpty()) {
                Text(
                    text = screenConfig.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    color = TextGray,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Label
            Text(
                text = "One-Time Passcode",
                style = MaterialTheme.typography.labelMedium,
                fontSize = 12.sp,
                color = TextGray,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // OTP Input Fields
            OTPInputField(
                value = otpValue,
                length = 6,
                isError = isError,
                onValueChange = { newValue ->
                    if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                        otpValue = newValue
                        // Clear error when user starts typing
                        if (isError) {
                            isError = false
                            errorMessage = ""
                        }
                    }
                },
                focusRequester = focusRequester
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Error Message
            if (isError && errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Resend Link (if enabled)
            if (showResendOption && !isError) {
                ResendLink(onResend = onResend)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Continue/Try Again Button
            GradientButton(
                text = if (isError) "Try again" else "Continue",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                gradient = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.Transparent
                    )
                ),
                buttonDefaultColor = ButtonDefaults.buttonColors(
                    containerColor = ButtonBlack,
                    contentColor = Color.White,
                    disabledContainerColor = ButtonBlack.copy(alpha = 0.6f),
                    disabledContentColor = Color.White.copy(alpha = 0.6f)
                )
            ) {
                if (otpValue.length == 6) {
                    // Clear any existing errors
                    isError = false
                    errorMessage = ""

                    // Call ViewModel to verify OTP
                    viewModel.verifyWithOtp(
                        authenticationMethodId = authenticationId,
                        otpCode = otpValue,
                        authSession = authSession
                    )
                } else {
                    isError = true
                    errorMessage = "Please enter the complete 6-digit code"
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Show loading overlay when verifying
    if (uiState is EnrollmentUiState.Verifying) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            CircularLoader()
        }
    }

    // Auto-focus on OTP input when screen loads
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

/**
 * OTP Input Field Component
 *
 * Displays 6 individual boxes for OTP digit entry with error state support
 */
@Composable
private fun OTPInputField(
    value: String,
    length: Int = 6,
    isError: Boolean = false,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        decorationBox = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(length) { index ->
                    OTPBox(
                        value = value.getOrNull(index)?.toString() ?: "",
                        isError = isError,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        textStyle = TextStyle(
            fontSize = 0.sp, // Hide the actual text field
            color = Color.Transparent
        )
    )
}

/**
 * Individual OTP Box Component
 *
 * Displays a single digit in a styled box with error state styling
 */
@Composable
private fun OTPBox(
    value: String,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isError -> Color(0xFFFFEBEE) // Light pink for error
        value.isNotEmpty() -> Color(0xFFF5F5F5) // Light gray when filled
        else -> Color.White
    }

    val borderColor = when {
        isError -> Color(0xFFD32F2F) // Red for error
        value.isNotEmpty() -> Color(0xFFE0E0E0) // Gray when filled
        else -> Color(0xFFE0E0E0) // Light gray default
    }

    Box(
        modifier = modifier
            .aspectRatio(0.85f) // Slightly taller than wide
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isError) Color(0xFFD32F2F) else Color.Black,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Resend Link Component
 *
 * Displays "Didn't get a code? Resend it." with clickable link
 */
@Composable
private fun ResendLink(
    onResend: () -> Unit
) {
    val annotatedString = buildAnnotatedString {
        withStyle(
            style = SpanStyle(
                color = TextGray,
                fontSize = 14.sp
            )
        ) {
            append("Didn't get a code? ")
        }
        withLink(
            LinkAnnotation.Clickable(
                tag = "resend",
                linkInteractionListener = { onResend() }
            )
        ) {
            withStyle(
                style = SpanStyle(
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append("Resend it.")
            }
        }
    }

    Text(
        text = annotatedString,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Holds screen text  based on authenticator type
 */
private data class ConfigurationText(
    val title: String,
    val headline: String,
    val description: String
)

/**
 * Get Screen Configuration
 *
 * Returns appropriate text content based on authenticator type
 */
private fun getScreenConfigText(
    authenticatorType: AuthenticatorType,
    phoneNumber: String?
): ConfigurationText {
    return when (authenticatorType) {
        AuthenticatorType.PHONE -> ConfigurationText(
            title = "Verify it's you",
            headline = "Enter the 6-digit code we sent to ${phoneNumber ?: "your phone"}",
            description = ""
        )

        AuthenticatorType.EMAIL -> ConfigurationText(
            title = "Verify it's you",
            headline = "Enter the 6-digit code we sent to your email",
            description = ""
        )

        AuthenticatorType.TOTP -> ConfigurationText(
            title = "Add and Authenticator",
            headline = "Enter the 6-digit code",
            description = "From your Authenticator App"
        )

        else -> ConfigurationText(
            "", "", ""
        )
    }
}

/**
 * Extract user-friendly error message from exception
 */
private fun getErrorMessage(exception: Throwable): String {
    return when (exception) {
        is AuthenticationException -> {
            when {
                exception.getCode() == "invalid_grant" ||
                        exception.getDescription().contains("invalid", ignoreCase = true) ||
                        exception.getDescription().contains("incorrect", ignoreCase = true) -> {
                    "Invalid passcode. Please try again."
                }

                exception.getDescription().contains("expired", ignoreCase = true) -> {
                    "Passcode expired. Please request a new one."
                }

                exception.getDescription().contains("rate", ignoreCase = true) ||
                        exception.getDescription().contains("too many", ignoreCase = true) -> {
                    "Too many attempts. Please try again later."
                }

                else -> {
                    exception.getDescription() ?: "Verification failed. Please try again."
                }
            }
        }

        else -> {
            exception.message ?: "An error occurred. Please try again."
        }
    }
}