package com.auth0.universalcomponents.feature.mfachallenge.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * Tells [engine] when the app leaves or returns to the foreground, so a network failure while the user approves in
 * another app doesn't count against the push. It lives with MFA, apart from the screens, because a headless app draws
 * none, and the rule must hold for it too. Whoever hosts MFA places it with the engine that is running (null: none).
 */
@Composable
internal fun MfaLifecycle(engine: MfaEngine?) {
    val current by rememberUpdatedState(engine)
    LifecycleEventEffect(Lifecycle.Event.ON_START) { current?.onAppForeground(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { current?.onAppForeground(false) }
}
