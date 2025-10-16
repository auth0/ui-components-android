package com.auth0.android.ui_components.presentation.ui.mfa

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auth0.android.ui_components.R
import com.auth0.android.ui_components.di.MyAccountModule
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.model.EnrollmentResult
import com.auth0.android.ui_components.domain.model.EnrollmentType
import com.auth0.android.ui_components.presentation.ui.components.CircularLoader
import com.auth0.android.ui_components.presentation.ui.components.ErrorScreen
import com.auth0.android.ui_components.presentation.ui.components.TopBar
import com.auth0.android.ui_components.presentation.viewmodel.EnrollmentUiState
import com.auth0.android.ui_components.presentation.viewmodel.EnrollmentViewModel
import com.auth0.android.ui_components.theme.ButtonBlack
import com.auth0.android.ui_components.theme.TextGray
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * QR-based Authenticator Enrollment Screen
 * Supports TOTP and Push Notification enrollment
 * Displays QR code and secret for authenticator app enrollment
 */
@Composable
fun QREnrollmentScreen(
    authenticatorType: AuthenticatorType,
    modifier: Modifier = Modifier,
    viewModel: EnrollmentViewModel = viewModel(
        factory = MyAccountModule.provideEnrollmentViewModelFactory()
    ),
    onBackClick: () -> Unit = {},
    onContinueClick: (
        authenticationId: String,
        authSession: String,
    ) -> Unit,
    onEnrollmentSuccess: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    val enrollmentType = when (authenticatorType) {
        AuthenticatorType.TOTP -> EnrollmentType.TOTP
        AuthenticatorType.PUSH -> EnrollmentType.PUSH_NOTIFICATION
        else -> EnrollmentType.TOTP
    }

    // Start enrollment on screen load
    LaunchedEffect(authenticatorType) {
        viewModel.startEnrollment(enrollmentType)
    }

    // Get display title based on authenticator type
    val title = when (authenticatorType) {
        AuthenticatorType.TOTP -> "Authenticator"
        AuthenticatorType.PUSH -> "Push Notification"
        else -> "Authenticator"
    }

    Scaffold(
        topBar = {
            TopBar(
                title = title,
                onBackClick = onBackClick,
                showSeparator = false
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is EnrollmentUiState.Idle -> {
                }

                is EnrollmentUiState.Loading -> {
                    LoadingContent()
                }

                is EnrollmentUiState.EnrollmentInitiated -> {
                    when (val result = state.enrollmentResult) {
                        is EnrollmentResult.TotpEnrollment -> {
                            QREnrollmentContent(
                                authenticatorType = authenticatorType,
                                totpEnrollment = result,
                                onContinueClick = onContinueClick,
                                manualCode = result.challenge.manualInputCode!!,
                                barcodeUri = result.challenge.barcodeUri
                            )
                        }

                        else -> {
                            ErrorContent(
                                message = "Unexpected enrollment type",
                                onRetry = { viewModel.resetState() }
                            )
                        }
                    }
                }

                is EnrollmentUiState.Verifying -> {
                    LoadingContent()
                }

                is EnrollmentUiState.Success -> {
                    LaunchedEffect(Unit) {
                        onEnrollmentSuccess()
                    }
                    SuccessContent()
                }

                is EnrollmentUiState.Error -> {
                    ErrorScreen(
                        mainErrorMessage = state.exception.message ?: "An error occurred",
                        "We are unable to process your request. Please try again in a few minutes. If this problem persists, please ",
                        clickableString = "contact us."
                    )
                }
            }
        }
    }
}

/**
 * Main content showing QR code and enrollment instructions
 */
@Composable
private fun QREnrollmentContent(
    authenticatorType: AuthenticatorType,
    totpEnrollment: EnrollmentResult.TotpEnrollment,
    onContinueClick: (String, String) -> Unit,
    manualCode: String,
    barcodeUri: String
) {
    val clipboardManager = LocalClipboardManager.current
    var showSnackbar by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(showSnackbar) {
        if (showSnackbar) {
            snackbarHostState.showSnackbar("Copied to clipboard")
            showSnackbar = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // QR Code
            QRCodeDisplay(
                data = barcodeUri,
                modifier = Modifier.size(200.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            ManualCodeCard(
                manualCode = manualCode,
                onCopyClick = {
                    clipboardManager.setText(AnnotatedString(manualCode))
                    showSnackbar = true
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(manualCode))
                    showSnackbar = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, Color.Gray),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp,
                    hoveredElevation = 6.dp,
                    focusedElevation = 6.dp
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_copy),
                    contentDescription = "Copy",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Copy as Code",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onContinueClick(
                        totpEnrollment.authenticationMethodId,
                        totpEnrollment.authSession
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonBlack,
                    contentColor = Color.White
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                )
            }



            Spacer(modifier = Modifier.height(36.dp))

            InstructionsText(authenticatorType = authenticatorType)

            Spacer(modifier = Modifier.height(48.dp))

            DownloadLinkText()

            Spacer(modifier = Modifier.height(32.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

/**
 * Instructions text based on authenticator type
 */
@Composable
private fun InstructionsText(authenticatorType: AuthenticatorType) {
    val instructionText = when (authenticatorType) {
        AuthenticatorType.TOTP -> "Use your Authenticator App (like Google Authenticator or Auth0 Guardian) to scan this QR code, or simply copy the code and paste it manually to register your device."
        AuthenticatorType.PUSH -> "Use the Auth0 Guardian App to scan this QR code, or simply copy the code and paste it manually to register your device for push notifications."
        else -> "Scan this QR code with your authenticator app or copy the code manually."
    }

    Text(
        text = instructionText,
        style = MaterialTheme.typography.bodyMedium,
        fontStyle = FontStyle.Normal,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        color = TextGray,
        letterSpacing = 1.2.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
private fun QRCodeDisplay(
    data: String,
    modifier: Modifier = Modifier
) {
    val qrCodeBitmap = remember(data) {
        generateQRCode(data)
    }
    Box(
        modifier = modifier
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (qrCodeBitmap != null) {
            Image(
                bitmap = qrCodeBitmap.asImageBitmap(),
                contentDescription = "QR Code for enrollment",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ManualCodeCard(
    manualCode: String,
    onCopyClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = manualCode,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 1.sp
                ),
                color = Color.Gray
            )

            IconButton(
                onClick = onCopyClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_copy),
                    contentDescription = "Copy secret code",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DownloadLinkText(
    downloadLink: String = "https://play.google.com/store/apps/details?id=com.auth0.guardian&hl=en_IN",
) {
    val annotatedString = buildAnnotatedString {
        withStyle(
            style = SpanStyle(
                color = TextGray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
        ) {
            append("Don't have the Auth0 Guardian App?\n")
        }

        pushStringAnnotation(
            tag = "download",
            annotation = "https://play.google.com/store/apps/details?id=com.auth0.guardian&hl=en_IN"
        )
        withStyle(
            style = SpanStyle(
                color = Color.Black,
                textDecoration = TextDecoration.Underline,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        ) {
            withLink(LinkAnnotation.Url(url = downloadLink)) {
                append("Download it here")
            }
        }
        pop()
    }

    Text(annotatedString, textAlign = TextAlign.Center)
}

/**
 * Loading State
 */
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularLoader()
        }
    }
}

/**
 * Success State
 */
@Composable
private fun SuccessContent() {

}

/**
 * Error State
 */
@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
}

/**
 * Generating QR code using ZXing
 */
private fun generateQRCode(content: String, size: Int = 500): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = createBitmap(width, height)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap[x, y] = if (bitMatrix[x, y]) Color.Black.toArgb()
                else Color.White.toArgb()
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}