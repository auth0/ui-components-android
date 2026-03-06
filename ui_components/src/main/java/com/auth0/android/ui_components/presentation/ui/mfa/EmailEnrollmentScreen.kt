package com.auth0.android.ui_components.presentation.ui.mfa

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auth0.android.ui_components.R
import com.auth0.android.ui_components.di.MyAccountModule
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.model.EnrollmentInput
import com.auth0.android.ui_components.presentation.ui.components.CircularLoader
import com.auth0.android.ui_components.presentation.ui.components.ErrorHandler
import com.auth0.android.ui_components.presentation.ui.components.GradientButton
import com.auth0.android.ui_components.presentation.ui.components.TopBar
import com.auth0.android.ui_components.presentation.ui.utils.ObserveAsEvents
import com.auth0.android.ui_components.presentation.viewmodel.EnrollmentEvent
import com.auth0.android.ui_components.presentation.viewmodel.EnrollmentUiState
import com.auth0.android.ui_components.presentation.viewmodel.EnrollmentViewModel
import com.auth0.android.ui_components.theme.Auth0Theme
import com.auth0.android.ui_components.utils.ValidationUtil

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
    val colors = Auth0Theme.colors
    val dimensions = Auth0Theme.dimensions

    var email by remember { mutableStateOf("") }
    var validationErrorMessage by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is EnrollmentEvent.EnrollmentChallengeSuccess -> {
                onContinueToOTP(
                    event.authenticationMethodId,
                    event.authSession,
                    email
                )
            }

            is EnrollmentEvent.VerificationSuccess -> {
                Log.d("EmailEnrollmentScreen", "$event not handled ")
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.add_email_otp),
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
                .padding(horizontal = dimensions.spacingMd, vertical = 38.dp)
        ) {

            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                EmailEnrollmentHeader()

                Spacer(modifier = Modifier.height(dimensions.spacingLg))
                EmailFormField(
                    email = email,
                    onEmailChange = { newEmail ->
                        email = newEmail
                        if (validationErrorMessage.isNotEmpty()) {
                            validationErrorMessage = ""
                        }
                    },
                    errorMessage = validationErrorMessage
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            ContinueButton(
                onClick = {
                    if (!ValidationUtil.isValidEmail(email)) {
                        validationErrorMessage = ValidationUtil.getEmailErrorMessage(email)
                    } else {
                        validationErrorMessage = ""
                        viewModel.startEnrollment(
                            authenticatorType = AuthenticatorType.EMAIL,
                            input = EnrollmentInput.Email(email)
                        )
                    }
                }
            )
        }
        LoadingScreen(state = uiState)
        ErrorScreen(uiState)
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
private fun EmailEnrollmentHeader() {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val dimensions = Auth0Theme.dimensions

    Text(
        text = stringResource(R.string.enter_email_address),
        style = typography.titleLarge,
        color = colors.textBold,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(dimensions.spacingXs))

    Text(
        text = stringResource(R.string.email_verification_code_text),
        style = typography.body,
        color = colors.textDefault,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun EmailFormField(
    email: String,
    onEmailChange: (String) -> Unit,
    errorMessage: String
) {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val dimensions = Auth0Theme.dimensions

    Text(
        text = stringResource(R.string.email_label),
        style = typography.body,
        color = colors.textBold,
        textAlign = TextAlign.Start
    )

    Spacer(modifier = Modifier.height(dimensions.spacingXs))

    EmailTextField(
        email = email,
        onEmailChange = onEmailChange,
        isError = errorMessage.isNotEmpty(),
        errorMessage = errorMessage
    )
}

@Composable
private fun EmailTextField(
    email: String,
    onEmailChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String
) {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val shapes = Auth0Theme.shapes
    val sizes = Auth0Theme.sizes
    val dimensions = Auth0Theme.dimensions

    val backgroundColor = if (isError) {
        colors.backgroundError
    } else {
        colors.backgroundLayerMedium
    }

    val borderColor = if (isError) {
        colors.backgroundError.copy(alpha = 0.5f)
    } else {
        colors.borderDefault
    }

    val textColor = if (isError) {
        colors.textOnError
    } else {
        colors.textBold
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(sizes.otpFieldHeight)
            .background(
                color = backgroundColor,
                shape = shapes.medium
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shapes.medium
            ),
        shadowElevation = 10.dp,
        shape = shapes.medium,
    )
    {
        BasicTextField(
            value = email,
            onValueChange = onEmailChange,
            textStyle = typography.title.copy(color = textColor, textAlign = TextAlign.Start),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(sizes.padding),
            decorationBox = { innerTextField ->
                if (email.isEmpty()) {
                    Text(
                        text = stringResource(R.string.email_placeholder),
                        style = typography.title.copy(
                            color = colors.textBold.copy(alpha = 0.54f),
                            textAlign = TextAlign.Start
                        )
                    )
                }
                innerTextField()
            }
        )
    }

    if (isError) {
        Spacer(modifier = Modifier.height(dimensions.spacingXs))
        Text(
            text = errorMessage,
            color = colors.textOnError,
            style = typography.body,
            modifier = Modifier.padding(start = dimensions.spacingXxs, top = dimensions.spacingXs)
        )
    }
}

@Composable
private fun ContinueButton(
    onClick: () -> Unit,
) {
    val typography = Auth0Theme.typography
    val sizes = Auth0Theme.sizes

    GradientButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(sizes.buttonHeight),
        onClick = onClick
    ) {
        Text(
            text = stringResource(R.string.continue_button),
            style = typography.label
        )
    }
}