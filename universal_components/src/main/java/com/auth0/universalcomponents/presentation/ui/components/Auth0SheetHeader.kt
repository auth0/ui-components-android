package com.auth0.universalcomponents.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.auth0.universalcomponents.theme.Auth0Theme

/**
 * Header block used at the top of a bottom sheet or dialog: a bold title with a
 * supporting description below it. Left-aligned, with 8dp between the two lines.
 *
 * @param title Bold title line, clamped to one line
 * @param description Supporting body text shown below the title
 * @param modifier Modifier applied to the header column
 * @param titleMaxLines Maximum lines for the title before ellipsize
 * @param descriptionMaxLines Maximum lines for the description before ellipsize
 */
@Composable
internal fun Auth0SheetHeader(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    titleMaxLines: Int = 1,
    descriptionMaxLines: Int = 5,
) {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val dimensions = Auth0Theme.dimensions

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingXs),
    ) {
        Text(
            text = title,
            style = typography.titleLarge,
            color = colors.textBold,
            maxLines = titleMaxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = description,
            style = typography.body,
            color = colors.textDefault,
            maxLines = descriptionMaxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
