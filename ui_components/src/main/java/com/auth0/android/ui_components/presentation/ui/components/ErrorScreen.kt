package com.auth0.android.ui_components.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import com.auth0.android.ui_components.R
import com.auth0.android.ui_components.theme.Auth0Theme

@Composable
fun ErrorScreen(
    modifier: Modifier = Modifier,
    mainErrorMessage: String,
    description: String = stringResource(R.string.unable_to_process_contact),
    clickableString: String? = null,
    shouldRetry: Boolean = true,
    onRetryClick: () -> Unit = {}
) {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val shapes = Auth0Theme.shapes
    val sizes = Auth0Theme.sizes
    val dimensions = Auth0Theme.dimensions

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundLayerBase)
            .padding(horizontal = dimensions.spacingXl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = mainErrorMessage,
                style = typography.displayMedium,
                color = colors.textBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            MessageWithLink(
                message = description,
                clickableLinkText = clickableString
            )

            Spacer(modifier = Modifier.height(dimensions.spacingLg))

            if (shouldRetry) {
                Button(
                    onClick = onRetryClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(sizes.buttonHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.backgroundPrimary,
                        contentColor = colors.textOnPrimary
                    ),
                    shape = shapes.medium
                ) {
                    Text(
                        text = stringResource(R.string.try_again),
                        style = typography.label
                    )
                }
            }
        }
    }
}


@Composable
fun MessageWithLink(
    message: String,
    clickableLinkText: String? = null,
    clickableLink: String = "https://auth0.com/contact-us"
) {
    val colors = Auth0Theme.colors

    val annotatedString = buildAnnotatedString {
        withStyle(
            style = SpanStyle(
                fontFamily = Auth0Theme.typography.bodySmall.fontFamily,
                fontWeight = Auth0Theme.typography.bodySmall.fontWeight,
                color = colors.textDefault,
                fontSize = Auth0Theme.typography.bodySmall.fontSize,
                letterSpacing = Auth0Theme.typography.bodySmall.letterSpacing,
            )
        ) {
            append(message)
        }
        clickableLinkText?.let {
            pushStringAnnotation(
                tag = "URL",
                annotation = "contact_us"
            )
            withStyle(
                style = SpanStyle(
                    fontFamily = Auth0Theme.typography.bodySmall.fontFamily,
                    fontWeight = Auth0Theme.typography.bodySmall.fontWeight,
                    color = colors.textDefault,
                    fontSize = Auth0Theme.typography.bodySmall.fontSize,
                    letterSpacing = Auth0Theme.typography.bodySmall.letterSpacing,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                withLink(LinkAnnotation.Url(url = clickableLink)) {
                    append(it)
                }
            }
            pop()
        }
    }

    Text(annotatedString, textAlign = TextAlign.Center)
}