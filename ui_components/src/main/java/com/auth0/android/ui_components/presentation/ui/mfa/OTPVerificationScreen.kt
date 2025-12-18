package com.auth0.android.ui_components.presentation.ui.mfa

import android.util.Log
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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
import com.auth0.android.ui_components.R
import com.auth0.android.ui_components.di.MyAccountModule
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.presentation.ui.components.CircularLoader
import com.auth0.android.ui_components.presentation.ui.components.ErrorHandler
import com.auth0.android.ui_components.presentation.ui.components.GradientButton
import com.auth0.android.ui_components.presentation.ui.components.TopBar
import com.auth0.android.ui_components.presentation.ui.utils.ObserveAsEvents
import com.auth0.android.ui_components.presentation.ui.utils.UiUtils
import com.auth0.android.ui_components.presentation.viewmodel.EnrollmentEvent
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
        factory = MyAccountModule.provideEnrollmentViewModelFactory(
            authenticatorType,
            startDefaultEnrollment = false
        )
    ),
    onBackClick: () -> Unit = {},
    onVerificationSuccess: () -> Unit = {},
    onResend: () -> Unit = {}
) {
    var otpValue by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val screenText = remember(authenticatorType, phoneNumberOrEmail) {
        UiUtils.getOTPVerificationScreenText(authenticatorType, phoneNumberOrEmail)
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is EnrollmentEvent.EnrollmentChallengeSuccess -> {
                Log.d("OTPVerificationScreen", "EnrollmentChallengeSuccess not handled ")
            }

            is EnrollmentEvent.VerificationSuccess -> {
                onVerificationSuccess()
            }
        }
    }


    Scaffold(
        topBar = {
            TopBar(
                title = screenText.topBarTitle,
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
                text = screenText.primaryText,
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

            if (!screenText.description.isNullOrEmpty()) {
                Text(
                    text = screenText.description,
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

            OTPFieldError(uiState, isError, errorMessage)

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

        LoadingScreen(uiState)
        ErrorScreen(uiState)

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

@Composable
private fun OTPFieldError(
    state: EnrollmentUiState,
    isError: Boolean,
    errorMessage: String
) {

    val error = state.otpError || isError
    val errorString =
        if (state.otpError) stringResource(R.string.invalid_passcode) else errorMessage
    if (error && errorString.isNotEmpty()) {
        Text(
            text = errorString,
            style = contentTextStyle,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = ErrorRed,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.height(16.dp))
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
