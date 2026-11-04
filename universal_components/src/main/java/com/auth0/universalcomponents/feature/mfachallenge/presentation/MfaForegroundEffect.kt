package com.auth0.universalcomponents.feature.mfachallenge.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * Tells [engine] when the app leaves or returns to the foreground, so a network failure while the user approves in
 * another app doesn't count against the push. It lives with MFA, apart from the screens, because a headless app draws
 * none, and the rule must hold for it too. Whoever hosts MFA places it with the engine that is running (null: none).
 *
 * A rotation stops and starts the Activity too, but the app never left, so that stop is not passed on: otherwise every
 * rotation would send an extra check ahead of the server's interval, and the server may answer "slow down".
 */
@Composable
internal fun MfaForegroundEffect(engine: MfaEngine?) {
    val current by rememberUpdatedState(engine)
    val activity = LocalContext.current.findActivity()
    LifecycleEventEffect(Lifecycle.Event.ON_START) { current?.onAppForegrounded() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (activity?.isChangingConfigurations != true) current?.onAppBackgrounded()
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
