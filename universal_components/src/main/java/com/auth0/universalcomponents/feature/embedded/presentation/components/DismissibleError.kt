package com.auth0.universalcomponents.feature.embedded.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * What a field shows of the server's error: [message], or null once the user edited the field ([dismiss]).
 */
internal class DismissibleError(val message: String?, val dismiss: () -> Unit)

/**
 * The server's [message] under a field goes away once the user edits the field, and comes back when the server reports
 * one again. What was dismissed is kept across a rotation.
 */
@Composable
internal fun rememberDismissibleError(message: String?): DismissibleError {
    var isDismissed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(message) { if (message != null) isDismissed = false }
    // The same object until what it shows changes, so a lambda that captures it is not rebuilt on every redraw (the
    // password screen redraws each second while its code wait counts down).
    val dismiss = remember { { isDismissed = true } }
    return remember(message, isDismissed) { DismissibleError(message.takeUnless { isDismissed }, dismiss) }
}
