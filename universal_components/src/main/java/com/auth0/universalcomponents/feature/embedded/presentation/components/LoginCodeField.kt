package com.auth0.universalcomponents.feature.embedded.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.auth0.universalcomponents.presentation.ui.components.OTPInputField
import com.auth0.universalcomponents.theme.Auth0Theme

internal const val CODE_LENGTH = 6

/**
 * The library's boxed code input; keeps digits only and takes focus when it first appears.
 * Its boxes grow with the width, so it's held to [length] phone-sized boxes; otherwise they balloon in landscape.
 */
@Composable
internal fun LoginCodeField(
    code: String,
    onCodeChange: (String) -> Unit,
    isError: Boolean = false,
    length: Int = CODE_LENGTH,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val maxWidth = Auth0Theme.sizes.otpFieldWidth * length + Auth0Theme.dimensions.spacingXs * (length - 1)

    Box(modifier = Modifier.widthIn(max = maxWidth)) {
        OTPInputField(
            value = code,
            length = length,
            isError = isError,
            onValueChange = { onCodeChange(it.filter(Char::isDigit).take(length)) },
            focusRequester = focusRequester,
        )
    }
}
