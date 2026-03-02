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
    val sizes = Auth0TokenDefaults.sizes()
    val dimensions = Auth0TokenDefaults.dimensions()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clickable(onClick = onClick),
        shape = shapes.large,
        color = colors.backgroundLayerMedium,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, colors.borderDefault)
    ) {
        Row(
            modifier = Modifier
                .padding(sizes.padding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(sizes.iconMedium),
                tint = colors.textBold
            )
            Spacer(modifier = Modifier.width(dimensions.spacingMd))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = typography.body,
                    color = colors.textBold
                )

                Spacer(Modifier.weight(1f))
                if (showActiveTag) {
                    Spacer(modifier = Modifier.width(dimensions.spacingXs))
                    Icon(
                        painter = painterResource(R.drawable.ic_active),
                        contentDescription = stringResource(R.string.active_label),
                        modifier = Modifier.size(sizes.iconMedium),
                        tint = Color.Unspecified
                    )
                }
                Spacer(modifier = Modifier.width(dimensions.spacingMd))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.navigate),
                    tint = colors.textBold,
                    modifier = Modifier.size(sizes.iconMedium)
                )
            }
        }
    }
}
