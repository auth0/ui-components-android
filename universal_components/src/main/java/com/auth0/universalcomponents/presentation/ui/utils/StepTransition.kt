package com.auth0.universalcomponents.presentation.ui.utils

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset

private const val TRANSITION_MILLIS = 280

/**
 * The slide between two steps of a flow: a deeper screen pushes in from the end, a shallower one pops back in from the
 * start. Shared by every flow that moves between screens (embedded login and MFA today), so none has to reach into
 * another's package for it.
 */
internal fun stepTransition(isForward: Boolean): ContentTransform {
    val direction = if (isForward) 1 else -1
    val spec = tween<Float>(TRANSITION_MILLIS, easing = EaseInOut)
    val slide = tween<IntOffset>(TRANSITION_MILLIS, easing = EaseInOut)
    return (slideInHorizontally(slide) { direction * it } + fadeIn(spec)) togetherWith
        (slideOutHorizontally(slide) { -direction * it } + fadeOut(spec))
}

/**
 * A screen that slides in over another and back out over it, while the one under it stays where it is. [opening] is
 * the screen on top coming in; the one on top is always the one that moves.
 *
 * Going out, the screen below is put under with a z-index of its own. The one leaving only has the z-index its opening
 * gave it, and a screen drawn again after a rotation or a restore never had that opening, so without this the screen
 * below could be drawn over it and the slide-out would not be seen.
 */
internal fun overlayTransition(opening: Boolean): ContentTransform {
    val spec = tween<Float>(TRANSITION_MILLIS, easing = EaseInOut)
    val slide = tween<IntOffset>(TRANSITION_MILLIS, easing = EaseInOut)
    return if (opening) {
        ((slideInHorizontally(slide) { it } + fadeIn(spec)) togetherWith ExitTransition.None)
            .apply { targetContentZIndex = 1f }
    } else {
        (EnterTransition.None togetherWith (slideOutHorizontally(slide) { it } + fadeOut(spec)))
            .apply { targetContentZIndex = -1f }
    }
}
