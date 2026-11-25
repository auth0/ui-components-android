package com.auth0.universalcomponents.feature.mfachallenge.presentation

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auth0.universalcomponents.utils.findActivity

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

/**
 * Keeps the Activity's window out of screenshots, screen recordings and the recent-apps preview while it is on screen.
 * It is for the screen that shows the new recovery code, which is a live secret. The window is the Activity's, so what
 * the app draws over the login (a screen it replaced, or a headless app's own) is covered too; a Dialog or bottom
 * sheet that holds the login has a window of its own and is not. A window the app already secured stays secured when
 * this leaves.
 */
@Composable
internal fun MfaSecureWindow() {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity) {
        // Only a window this call secured is cleared on the way out.
        val window = activity?.window?.takeIf { it.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE == 0 }
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}

/**
 * True while this engine shows the new recovery code, a live secret: the login secures the window then. Null: no MFA is
 * running.
 */
@Composable
internal fun MfaEngine?.isShowingNewRecoveryCode(): Boolean {
    val state = this?.state?.collectAsStateWithLifecycle()
    // Read through derivedStateOf, so the caller redraws when the answer flips and not on every tick of the resend clock.
    return remember(state) { derivedStateOf { state?.value is MfaState.SaveRecoveryCode } }.value
}
