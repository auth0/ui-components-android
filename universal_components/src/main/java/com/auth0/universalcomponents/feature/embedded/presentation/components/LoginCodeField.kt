package com.auth0.universalcomponents.feature.embedded.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import com.auth0.universalcomponents.presentation.ui.components.OTPInputField

internal const val CODE_LENGTH = 6

/** The library's boxed code input; keeps digits only and takes focus when it first appears. */
@Composable
internal fun LoginCodeField(
    code: String,
    onCodeChange: (String) -> Unit,
    isError: Boolean = false,
    length: Int = CODE_LENGTH,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    OTPInputField(
        value = code,
        length = length,
        isError = isError,
        onValueChange = { onCodeChange(it.filter(Char::isDigit).take(length)) },
        focusRequester = focusRequester,
    )
}
