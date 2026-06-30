package com.auth0.android.sample.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * Wraps [onClick] so it fires at most once per [windowMs]. Compose can dispatch several queued taps
 * within the same frame, before recomposition removes the composable; for back navigation that
 * over-pops past the start destination and leaves an empty NavHost (blank screen). Letting only the
 * first tap of a burst through avoids that.
 */
@Composable
internal fun rememberDebouncedOnClick(
    windowMs: Long = 500L,
    onClick: () -> Unit
): () -> Unit {
    val latestOnClick by rememberUpdatedState(onClick)
    return remember(windowMs) {
        var lastClickMs = 0L
        {
            val now = System.currentTimeMillis()
            if (now - lastClickMs >= windowMs) {
                lastClickMs = now
                latestOnClick()
            }
        }
    }
}
