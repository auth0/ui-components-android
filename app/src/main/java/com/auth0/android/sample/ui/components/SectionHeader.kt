package com.auth0.android.sample.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.auth0.universalcomponents.theme.Auth0Theme

/**
 * A section header with a bold title and optional subtitle.
 *
 * Used across multiple screens (Dashboard, Explore Login, Login & Security, Profile)
 * to separate content sections.
 *
 * @param title Section title text
 * @param subtitle Optional description text below the title
 * @param modifier Modifier for the header layout
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val dimensions = Auth0Theme.dimensions

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = typography.displayMedium,
            color = colors.textBold,
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(dimensions.spacingXs))
            Text(
                text = subtitle,
                style = typography.body,
                color = colors.textDefault
            )
        }
    }
}
