package com.auth0.universalcomponents.feature.embedded.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.presentation.ui.flow.flowSurface
import com.auth0.universalcomponents.theme.Auth0Theme

private const val DISABLED_ALPHA = 0.4f

/** The smallest a tap target should be. */
private val EDIT_TARGET = 48.dp

/**
 * Who is signing in, shaped like the field it was typed in but read-only: the email, number or username from the screen
 * before, under the label that field had, with "Edit" at the end to go back and change it, which a screen reader calls
 * [editDescription]. A null [onEdit] hides Edit, and Edit is off while [isEditEnabled] is false.
 */
@Composable
internal fun LoginIdentifierRow(
    label: String,
    editDescription: String,
    value: String,
    isEditEnabled: Boolean,
    onEdit: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = Auth0Theme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        LoginFieldLabel(label)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Auth0Theme.sizes.codeContainerHeight)
                .flowSurface(Auth0Theme.shapes.medium, isFocused = false, isError = false)
                .padding(start = Auth0Theme.sizes.padding, end = Auth0Theme.dimensions.spacingSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value,
                style = Auth0Theme.typography.body,
                color = colors.textBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = Auth0Theme.dimensions.spacingSm),
            )
            if (onEdit != null) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = EDIT_TARGET, minHeight = EDIT_TARGET)
                        .alpha(if (isEditEnabled) 1f else DISABLED_ALPHA)
                        .clickable(enabled = isEditEnabled, role = Role.Button, onClick = onEdit)
                        .semantics { contentDescription = editDescription }
                        .padding(horizontal = Auth0Theme.dimensions.spacingSm),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.embedded_login_code_edit),
                        style = Auth0Theme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textBold,
                    )
                }
            }
        }
    }
}
