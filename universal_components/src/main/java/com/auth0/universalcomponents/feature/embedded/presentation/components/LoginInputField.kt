package com.auth0.universalcomponents.feature.embedded.presentation.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.auth0.universalcomponents.theme.Auth0Theme

/** A single-line text field for the identifier; takes focus when it first appears. */
@Composable
internal fun LoginInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isInvalid: Boolean = false,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Email,
    onDone: () -> Unit = {},
) {
    val colors = Auth0Theme.colors
    val shape = Auth0Theme.shapes.medium
    val textColor = if (isInvalid) colors.textOnError else colors.textBold
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val focusManager = LocalFocusManager.current

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        interactionSource = interactionSource,
        textStyle = Auth0Theme.typography.body.copy(color = textColor),
        cursorBrush = SolidColor(colors.textBold),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = keyboardType,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = {
            focusManager.clearFocus()
            onDone()
        }),
        modifier = modifier
            .fillMaxWidth()
            .height(Auth0Theme.sizes.codeContainerHeight)
            .focusRequester(focusRequester)
            .loginSurface(shape, isFocused = isFocused, isError = isInvalid),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.padding(horizontal = Auth0Theme.sizes.padding),
                contentAlignment = Alignment.CenterStart,
            ) {
                innerTextField()
            }
        },
    )
}
