package com.auth0.universalcomponents.presentation.ui.utils

import androidx.compose.animation.ContentTransform
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
