package com.auth0.universalcomponents.feature.embedded.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * What a field shows of the server's error: [message], or null once the user edited the field ([dismiss]).
 */
internal class DismissibleError(val message: String?, val dismiss: () -> Unit)

/**
 * The server's [message] under a field goes away once the user edits the field, and comes back when the server reports
 * one again. What was dismissed is kept across a rotation: the screen drawn again gets the same message, which stays
 * hidden; another one, or the same one after it went away (each request clears it), shows.
 */
@Composable
internal fun rememberDismissibleError(message: String?): DismissibleError {
    // The message itself, not a flag: the effect below runs again when a rotation draws the screen anew, and it is the
    // same message then that tells it apart from a new one.
    var dismissed by rememberSaveable { mutableStateOf<String?>(null) }
    val current by rememberUpdatedState(message)
    LaunchedEffect(message) { if (message != dismissed) dismissed = null }
    // The same object until what it shows changes, so a lambda that captures it is not rebuilt on every redraw (the
    // password screen redraws each second while its code wait counts down).
    val dismiss = remember { { dismissed = current } }
    return remember(message, dismissed) { DismissibleError(message.takeUnless { it == dismissed }, dismiss) }
}
