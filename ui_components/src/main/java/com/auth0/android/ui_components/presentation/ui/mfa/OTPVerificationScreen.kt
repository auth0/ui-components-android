package com.auth0.android.ui_components.presentation.ui.mfa

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.em
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
import com.auth0.android.ui_components.theme.ErrorRed
import com.auth0.android.ui_components.theme.contentTextStyle
import com.auth0.android.ui_components.theme.secondaryTextColor
import interFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OTPVerificationScreen(
    authenticatorType: AuthenticatorType,
    authenticationId: String,
    authSession: String,
    phoneNumberOrEmail: String? = null,
    showResendOption: Boolean = true,
    viewModel: EnrollmentViewModel = viewModel(
        factory = MyAccountModule.provideEnrollmentViewModelFactory(authenticatorType)
    ),
    onBackClick: () -> Unit = {},
    onVerificationSuccess: () -> Unit = {},
    onResend: () -> Unit = {}
) {
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
                onVerificationSuccess()
            }

            is EnrollmentUiState.Error -> {
                isError = true
                errorMessage = getErrorMessage(state.uiError.error.cause)
            }

            else -> {
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = screenConfig.topBarTitle,
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
                .padding(horizontal = 16.dp, vertical = 38.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = screenConfig.primaryText,
                style = contentTextStyle,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = Color.Black,
                lineHeight = 1.em,
                letterSpacing = 0.em,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (!screenConfig.description.isNullOrEmpty()) {
                Text(
                    text = screenConfig.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    color = secondaryTextColor,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                Spacer(modifier = Modifier.height(32.dp))
            }

            Text(
                text = "One-Time Passcode",
                style = contentTextStyle,
                fontSize = 14.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.em,
                color = Color.Black,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OTPInputField(
                value = otpValue,
                isError = isError,
                onValueChange = { newValue ->
                    if (newValue.length <= 6) {
                        otpValue = newValue
                        if (isError) {
                            isError = false
                            errorMessage = ""
                        }
                    }
                },
                focusRequester = focusRequester
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isError && errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    style = contentTextStyle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = ErrorRed,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (showResendOption && !isError) {
                ResendLink(onResend = onResend)
            }

            Spacer(modifier = Modifier.weight(1f))

            val text = if (isError) "Try again" else "Continue"
            val click = {
                if (otpValue.length == 6) {
                    isError = false
                    errorMessage = ""
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

            GradientButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                buttonDefaultColor = ButtonDefaults.buttonColors(
                    containerColor = ButtonBlack,
                    contentColor = Color.White,
                    disabledContainerColor = ButtonBlack.copy(alpha = 0.6f),
                    disabledContentColor = Color.White.copy(alpha = 0.6f)
                ),
                onClick = click
            ) {
                Text(text)
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

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

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
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                        isFocused = value.length == index,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        textStyle = TextStyle(
            fontSize = 0.sp,
            color = Color.Transparent
        )
    )
}


@Composable
private fun OTPBox(
    modifier: Modifier = Modifier,
    value: String,
    isError: Boolean,
    isFocused: Boolean = false
) {
    val backgroundColor = when {
        isError -> Color(0xFFFFEBEE)
        else -> Color.White
    }

    val borderColor = when {
        isError -> Color(0xFFD32F2F)
        value.isNotEmpty() -> Color(0xFFE0E0E0)
        else -> Color(0xFFE0E0E0)
    }

    val borderWidth = if (isFocused) 3.dp else 1.dp

    Box(
        modifier = modifier
            .aspectRatio(0.85f)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            style = contentTextStyle,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.em,
            color = if (isError) Color(0xFFD32F2F) else Color.Black,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ResendLink(
    onResend: () -> Unit
) {
    val annotatedString = buildAnnotatedString {
        withStyle(
            style = SpanStyle(
                fontFamily = interFamily,
                fontWeight = FontWeight.Normal,
                color = secondaryTextColor,
                letterSpacing = 0.em,
                fontSize = 16.sp
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
                    fontFamily = interFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    letterSpacing = 0.em,
                    fontSize = 16.sp,
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
    val topBarTitle: String,
    val primaryText: String,
    val description: String? = null
)

/**
 * Get Screen Configuration
 *
 * Returns appropriate text content based on authenticator type
 */
private fun getScreenConfigText(
    authenticatorType: AuthenticatorType,
    phoneNumberOrEmail: String?
): ConfigurationText {
    return when (authenticatorType) {
        AuthenticatorType.PHONE -> ConfigurationText(
            topBarTitle = "Verify it's you",
            primaryText = "Enter the 6-digit code we sent to ${phoneNumberOrEmail ?: "your phone"}",
        )

        AuthenticatorType.EMAIL -> ConfigurationText(
            topBarTitle = "Verify it's you",
            primaryText = "Enter the 6-digit code we sent to $phoneNumberOrEmail",
        )

        else -> ConfigurationText(
            topBarTitle = "Add and Authenticator",
            primaryText = "Enter the 6-digit code",
            description = "From your Authenticator App"
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