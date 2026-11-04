package com.auth0.universalcomponents.feature.mfachallenge

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * How an app customizes MFA, each part on its own: the [messages] (change the words), the [screens] (replace one) and
 * the [presentation] (draw it all, headless).
 */
@Immutable
class MfaConfiguration(
    val messages: MfaMessages = MfaMessages(),
    val screens: MfaScreens = MfaScreens(),
    val presentation: MfaPresentation = MfaPresentation.Standard,
)

/** Who draws MFA. */
sealed interface MfaPresentation {

    /** The library draws every screen, except those the app replaced in [MfaScreens]. */
    data object Standard : MfaPresentation

    /**
     * The app draws every MFA screen itself, from [stream]. Nothing is drawn by the library; its frame (navigation
     * bar and Back) stays, and the app draws over it.
     */
    class Headless(val stream: MfaStepStream) : MfaPresentation
}

/**
 * What a headless app draws from, and how MFA ended. Keep one instance across recompositions (`remember`); it survives
 * a rotation, because the flow lives in the ViewModel. It is not saved: after process death the sign-in starts over
 * from email entry.
 */
class MfaStepStream {

    private val _step = MutableStateFlow<MfaStep?>(null)
    private val _result = MutableStateFlow<MfaResult?>(null)

    /** The step to draw; null when there is nothing to draw (before MFA, after it ended, or the app has taken over). */
    val step: StateFlow<MfaStep?> = _step.asStateFlow()

    /** How MFA ended; null while it runs, and cleared when the next MFA starts. */
    val result: StateFlow<MfaResult?> = _result.asStateFlow()

    internal fun showStep(step: MfaStep?) {
        _step.value = step
    }

    internal fun finish(result: MfaResult?) {
        _result.value = result
    }
}

/** How MFA ended. */
sealed interface MfaResult {

    /** The user is signed in; the credentials went to `onAuthenticated`. */
    data object SignedIn : MfaResult

    /** MFA ended without signing in. [error] is [MfaError.Cancelled] when the user left. */
    class Ended(val error: MfaError) : MfaResult
}
