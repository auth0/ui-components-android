package com.auth0.android.ui_components.presentation.ui.components

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ErrorScreen(
    mainErrorMessage: String,
    description: String,
    modifier: Modifier = Modifier,
    clickableString: String? = null,
    onRetryClick: () -> Unit = {}
) {

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = mainErrorMessage,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.sp
                ),
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            MessageWithLink(
                message = description,
                clickableLinkText = clickableString
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRetryClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2D2D2D),
                    contentColor = Color.White
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "Try again",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                )
            }
        }
    }
}

/**
 * Composable that displays the error message with an embedded clickable link.
 * The "contact us" text is underlined and clickable.
 */
@Composable
fun MessageWithLink(
    message: String,
    modifier: Modifier = Modifier,
    clickableLinkText: String? = null,
    clickableLink: String = "https://auth0.com/contact-us"
) {

    val annotatedString = buildAnnotatedString {
        withStyle(
            style = SpanStyle(
                color = Color(0xFF666666),
                fontSize = 16.sp
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
                    color = Color(0xFF666666),
                    fontSize = 16.sp,
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

    Text(annotatedString)
}