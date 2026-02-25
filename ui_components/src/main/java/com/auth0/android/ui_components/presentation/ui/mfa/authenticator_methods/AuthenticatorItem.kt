package com.auth0.android.ui_components.presentation.ui.mfa.authenticator_methods

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.auth0.android.ui_components.R
import com.auth0.android.ui_components.theme.Auth0TokenDefaults

@Composable
fun AuthenticatorItem(
    title: String,
    leadingIcon: Painter,
    showActiveTag: Boolean = false,
    onClick: () -> Unit = {}
) {
    val colors = Auth0TokenDefaults.color()
    val typography = Auth0TokenDefaults.typography()
    val shapes = Auth0TokenDefaults.shapes()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clickable(onClick = onClick),
        shape = shapes.large,
        color = colors.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = colors.foreground
            )
            Spacer(modifier = Modifier.width(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = typography.bodyLarge,
                    color = colors.cardForeground
                )

                Spacer(Modifier.weight(1f))
                if (showActiveTag) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_active),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.Unspecified
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.navigate),
                    tint = colors.foreground,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
