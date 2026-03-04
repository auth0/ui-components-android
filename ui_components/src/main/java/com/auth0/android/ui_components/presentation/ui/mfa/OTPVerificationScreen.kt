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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.auth0.android.ui_components.theme.Auth0Theme
import com.auth0.android.ui_components.theme.interFamily

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
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val dimensions = Auth0Theme.dimensions

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
        containerColor = colors.backgroundLayerBase
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = dimensions.spacingMd, vertical = 38.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = screenText.primaryText,
                style = typography.title,
                color = colors.textBold,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(dimensions.spacingXs))

            if (!screenText.description.isNullOrEmpty()) {
                Text(
                    text = screenText.description,
                    style = typography.body,
                    color = colors.textDefault,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(dimensions.spacingLg))
            } else {
                Spacer(modifier = Modifier.height(dimensions.spacingXl))
            }

            Text(
                text = "One-Time Passcode",
                style = typography.body,
                color = colors.textBold,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(dimensions.spacingMd))

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

            Spacer(modifier = Modifier.height(dimensions.spacingMd))

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

            val sizes = Auth0Theme.sizes

            GradientButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sizes.buttonHeight),
                onClick = click
            ) {
                Text(text, style = typography.label)
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
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val dimensions = Auth0Theme.dimensions

    val error = state.otpError || isError
    val errorString =
        if (state.otpError) stringResource(R.string.invalid_passcode) else errorMessage
    if (error && errorString.isNotEmpty()) {
        Text(
            text = errorString,
            style = typography.title,
            color = colors.textOnError,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.height(dimensions.spacingMd))
    }
}

@Composable
private fun LoadingScreen(state: EnrollmentUiState) {
    val colors = Auth0Theme.colors

    if (state.enrollingAuthenticator)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.backgroundLayerBase),
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
    val dimensions = Auth0Theme.dimensions

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
                horizontalArrangement = Arrangement.spacedBy(dimensions.spacingXs)
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
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val shapes = Auth0Theme.shapes

    val backgroundColor = when {
        isError -> colors.backgroundError.copy(alpha = 0.1f)
        else -> colors.backgroundLayerMedium
    }

    val borderColor = when {
        isError -> colors.backgroundError
        else -> colors.borderDefault
    }

    val borderWidth = if (isFocused) 3.dp else 1.dp

    Box(
        modifier = modifier
            .aspectRatio(0.85f)
            .background(
                color = backgroundColor,
                shape = shapes.medium
            )
            .border(
                width = borderWidth,
                color = borderColor,
                shape = shapes.medium
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            style = typography.titleLarge,
            color = if (isError) colors.textOnError else colors.textBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ResendLink(
    onResend: () -> Unit
) {
    val colors = Auth0Theme.colors

    val annotatedString = buildAnnotatedString {
        withStyle(
            style = SpanStyle(
                fontFamily = interFamily,
                fontWeight = FontWeight.Normal,
                color = colors.textDefault,
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
                    color = colors.textBold,
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
