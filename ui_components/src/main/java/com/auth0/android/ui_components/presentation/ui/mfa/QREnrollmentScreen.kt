package com.auth0.android.ui_components.presentation.ui.mfa

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auth0.android.ui_components.R
import com.auth0.android.ui_components.di.MyAccountModule
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.model.EnrollmentResult
import com.auth0.android.ui_components.presentation.ui.components.CircularLoader
import com.auth0.android.ui_components.presentation.ui.components.ErrorHandler
import com.auth0.android.ui_components.presentation.ui.components.GradientButton
import com.auth0.android.ui_components.presentation.ui.components.TopBar
import com.auth0.android.ui_components.presentation.ui.utils.ObserveAsEvents
import com.auth0.android.ui_components.presentation.viewmodel.EnrollmentEvent
import com.auth0.android.ui_components.presentation.viewmodel.EnrollmentUiState
import com.auth0.android.ui_components.presentation.viewmodel.EnrollmentViewModel
import com.auth0.android.ui_components.theme.Auth0Theme
import com.auth0.android.ui_components.theme.interFamily
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun QREnrollmentScreen(
    authenticatorType: AuthenticatorType,
    modifier: Modifier = Modifier,
    viewModel: EnrollmentViewModel = viewModel(
        factory = MyAccountModule.provideEnrollmentViewModelFactory(authenticatorType)
    ),
    onBackClick: () -> Unit = {},
    onContinueClick: (
        authenticationId: String,
        authSession: String,
    ) -> Unit,
) {
    val colors = Auth0Theme.colors

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var enrollmentChallengeResult by rememberSaveable {
        mutableStateOf<EnrollmentResult?>(null)
    }

    val title = when (authenticatorType) {
        AuthenticatorType.PUSH -> "Push Notification"
        else -> "Authenticator"
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is EnrollmentEvent.EnrollmentChallengeSuccess -> {
                enrollmentChallengeResult = event.enrollmentResult
            }

            is EnrollmentEvent.VerificationSuccess -> {
                onContinueClick(
                    event.authenticationMethod.id,
                    event.authenticationMethod.type
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = title,
                onBackClick = onBackClick,
                showSeparator = false
            )
        },
        containerColor = colors.backgroundLayerBase
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            enrollmentChallengeResult?.let {
                QREnrollmentContent(
                    authenticatorType = authenticatorType,
                    enrollmentResult = it,
                    viewModel = viewModel,
                    onContinueClick = onContinueClick
                )
            }
            LoadingScreen(uiState)
            ErrorScreen(uiState)
        }
    }
}

/**
 * Main content showing QR code and enrollment instructions
 */
@Composable
private fun QREnrollmentContent(
    authenticatorType: AuthenticatorType,
    enrollmentResult: EnrollmentResult,
    viewModel: EnrollmentViewModel,
    onContinueClick: (String, String) -> Unit
) {
    val sizes = Auth0Theme.sizes
    val dimensions = Auth0Theme.dimensions
    val totpEnrollment = enrollmentResult as EnrollmentResult.TotpEnrollment
    val manualCode = totpEnrollment.challenge.manualInputCode
    val barcodeUri = totpEnrollment.challenge.barcodeUri
    val hasManualCode = !manualCode.isNullOrEmpty()
    val isPushNotification = authenticatorType == AuthenticatorType.PUSH

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
                .padding(horizontal = sizes.paddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (!hasManualCode) Arrangement.Center else Arrangement.Top
        ) {
            if (hasManualCode) {
                Spacer(modifier = Modifier.height(90.dp))
            }

            QRCodeSection(
                barcodeUri = barcodeUri,
                modifier = Modifier.size(170.dp)
            )

            Spacer(modifier = Modifier.height(dimensions.spacingXl))

            InstructionsText()

            Spacer(modifier = Modifier.height(dimensions.spacingXl))

            if (hasManualCode) {
                ManualCodeSection(
                    manualCode = manualCode,
                    onCopyClick = {
                        clipboardManager.setText(AnnotatedString(manualCode))
                        showSnackbar = true
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            ContinueButtonSection(
                onContinueClick = {
                    if (isPushNotification) {
                        viewModel.verifyWithoutOtp(
                            authenticationMethodId = enrollmentResult.authenticationMethodId,
                            authSession = enrollmentResult.authSession
                        )
                    } else {
                        onContinueClick(
                            enrollmentResult.authenticationMethodId,
                            enrollmentResult.authSession
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(64.dp))

            DownloadLinkText()
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(sizes.padding)
        )
    }
}

/**
 * QR Code display section
 */
@Composable
private fun QRCodeSection(
    barcodeUri: String,
    modifier: Modifier = Modifier
) {
    QRCodeDisplay(
        data = barcodeUri,
        modifier = modifier
    )
}

/**
 * Manual code section with card and copy button
 * Only shown for TOTP enrollment where manual code is available
 */
@Composable
private fun ManualCodeSection(
    manualCode: String,
    onCopyClick: () -> Unit
) {
    val dimensions = Auth0Theme.dimensions

    ManualCodeCard(
        manualCode = manualCode
    )

    Spacer(modifier = Modifier.height(dimensions.spacingMd))

    CopyCodeButton(
        onCopyClick = onCopyClick
    )
}


@Composable
private fun CopyCodeButton(
    onCopyClick: () -> Unit
) {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val shapes = Auth0Theme.shapes
    val sizes = Auth0Theme.sizes
    val dimensions = Auth0Theme.dimensions

    GradientButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(sizes.buttonHeight),
        gradient = Brush.verticalGradient(
            colors = listOf(
                colors.backgroundPrimary.copy(alpha = 0f),
                colors.backgroundPrimary.copy(alpha = 0.05f)
            )
        ),
        buttonDefaultColor = ButtonDefaults.buttonColors(
            containerColor = colors.backgroundLayerMedium,
            contentColor = colors.backgroundPrimary,
            disabledContainerColor = colors.backgroundLayerMedium.copy(alpha = 0.6f),
            disabledContentColor = colors.backgroundPrimary.copy(alpha = 0.4f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 2.dp,
            disabledElevation = 2.dp
        ),
        borderStroke = BorderStroke(
            width = 1.dp,
            color = colors.backgroundPrimary.copy(alpha = 0.35f)
        ),
        shape = shapes.large,
        onClick = onCopyClick
    ) {

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_copy),
                contentDescription = "Copy",
                modifier = Modifier.size(sizes.iconMedium),
                tint = colors.textBold
            )

            Spacer(modifier = Modifier.width(dimensions.spacingXs))

            Text(
                text = stringResource(R.string.copy_as_code),
                style = typography.label,
                color = colors.textBold
            )
        }
    }
}

/**
 * Continue button section
 */
@Composable
private fun ContinueButtonSection(
    onContinueClick: () -> Unit
) {
    val sizes = Auth0Theme.sizes

    GradientButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(sizes.buttonHeight),
        gradient = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.15f),
                Color.Transparent
            )
        ),
        onClick = onContinueClick
    ) {
        Text(
            stringResource(R.string.continue_button),
            style = Auth0Theme.typography.label
        )
    }
}


@Composable
private fun InstructionsText() {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography

    val instructionText =
        "Use your Authenticator App (like Google Authenticator or Auth0 Guardian) to scan this QR code."


    Text(
        modifier = Modifier.fillMaxWidth(0.9f),
        text = instructionText,
        textAlign = TextAlign.Center,
        style = typography.body,
        color = colors.textDefault,
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
) {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val shapes = Auth0Theme.shapes
    val sizes = Auth0Theme.sizes

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(sizes.buttonHeight),
        shape = shapes.medium,
        color = colors.backgroundLayerMedium,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(sizes.padding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = manualCode,
                style = typography.label,
                color = colors.textDefault,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DownloadLinkText(
    downloadLink: String = "https://play.google.com/store/apps/details?id=com.auth0.guardian&hl=en_IN",
) {
    val colors = Auth0Theme.colors

    val annotatedString = buildAnnotatedString {
        withStyle(
            style = SpanStyle(
                fontFamily = interFamily,
                color = colors.textDefault,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = (0.011).em
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
                color = colors.textBold,
                fontFamily = interFamily,
                textDecoration = TextDecoration.Underline,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                letterSpacing = 0.em
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


@Composable
private fun LoadingScreen(state: EnrollmentUiState) {
    if (state.enrollingAuthenticator || state.verifyingAuthenticator)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Auth0Theme.colors.backgroundLayerBase),
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