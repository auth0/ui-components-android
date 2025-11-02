package com.auth0.android.ui_components.presentation.ui.mfa

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auth0.android.ui_components.R
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
import com.auth0.android.ui_components.theme.ErrorRed
import com.auth0.android.ui_components.theme.ErrorTextRed
import com.auth0.android.ui_components.theme.TextInputBlack
import com.auth0.android.ui_components.theme.contentTextStyle
import com.auth0.android.ui_components.theme.secondaryTextColor
import com.auth0.android.ui_components.theme.textInputStyle
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
    var email by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.add_email_otp),
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
                .padding(horizontal = 16.dp, vertical = 38.dp)
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
                            errorMessage = stringResource(R.string.unexpected_enrollment_result)
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
            ) {
                EmailEnrollmentHeader()

                Spacer(modifier = Modifier.height(24.dp))
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

    Text(
        text = stringResource(R.string.enter_email_address),
        style = contentTextStyle,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 1.em,
        letterSpacing = 0.em,
        color = Color.Black,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = stringResource(R.string.email_verification_code_text),
        style = contentTextStyle,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 1.5.em,
        letterSpacing = 0.01.em,
        color = secondaryTextColor,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun EmailFormField(
    email: String,
    onEmailChange: (String) -> Unit,
    isValidationError: Boolean,
    errorMessage: String
) {
    Text(
        text = stringResource(R.string.email_label),
        style = contentTextStyle,
        fontSize = 14.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.em,
        color = Color.Black,
        textAlign = TextAlign.Start
    )

    Spacer(modifier = Modifier.height(8.dp))

    EmailTextField(
        email = email,
        onEmailChange = onEmailChange,
        isError = isValidationError,
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
    val backgroundColor = if (isError) {
        ErrorRed.copy(alpha = 0.05f)
    } else {
        Color.White
    }

    val borderColor = if (isError) {
        ErrorRed.copy(alpha = 0.25f)
    } else {
        Color(0xFFE0E0E0)
    }

    val textColor = if (isError) {
        ErrorTextRed
    } else {
        TextInputBlack
    }

    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(
                color = backgroundColor,
                shape = shape
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape
            ),
        shadowElevation = 10.dp,
        shape = shape,
    )
    {
        BasicTextField(
            value = email,
            onValueChange = onEmailChange,
            textStyle = textInputStyle.copy(color = textColor, textAlign = TextAlign.Start),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(16.dp),
            decorationBox = { innerTextField ->
                if (email.isEmpty()) {
                    Text(
                        text = stringResource(R.string.email_placeholder),
                        style = textInputStyle.copy(
                            color = TextInputBlack.copy(alpha = 0.54f),
                            textAlign = TextAlign.Start
                        )
                    )
                }
                innerTextField()
            },
//            modifier = Modifier
//                .fillMaxWidth()
//                .background(
//                    color = backgroundColor,
//                    shape = shape
//                )
//                .border(
//                    width = 0.dp,
//                    color = borderColor,
//                    shape = shape
//                ),
//            colors = OutlinedTextFieldDefaults.colors(
//                focusedBorderColor = Color.Transparent,
//                unfocusedBorderColor = borderColor,
//                cursorColor = Color.Gray,
//            ),
        )
    }

    if (isError && errorMessage.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage,
            color = ErrorTextRed,
            style = contentTextStyle,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 0.15.em,
            letterSpacing = 0.01.em,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
        )
    }
}

@Composable
private fun ContinueButton(
    onClick: () -> Unit,
) {
    GradientButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        onClick = onClick
    ) {
        Text(text = stringResource(R.string.continue_button))
    }
}