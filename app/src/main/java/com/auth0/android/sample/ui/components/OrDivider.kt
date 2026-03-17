package com.auth0.android.sample.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.auth0.universalcomponents.theme.Auth0Theme

/**
 * An "OR" divider separating two sections of a form.
 *
 * Used in the Embedded Login screen between social login buttons
 * and the email/password form.
 *
 * @param modifier Modifier for the divider layout
 * @param text Text shown in the center (defaults to "OR")
 */
@Composable
fun OrDivider(
    modifier: Modifier = Modifier,
    text: String = "OR"
) {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val dimensions = Auth0Theme.dimensions

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = (0.5).dp,
            color = colors.borderDefault
        )

        Text(
            text = text,
            style = typography.helper,
            color = colors.textDefault,
            modifier = Modifier.padding(horizontal = dimensions.spacingMd)
        )

        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = (0.5).dp,
            color = colors.borderDefault
        )
    }
}
