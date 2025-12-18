package com.auth0.android.ui_components.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dimension tokens for Auth0 UI components.
 *
 * Provides consistent spacing and sizing values across all SDK components.
 * All parameters have default values, so you only need to specify what you want to customize.
 *
 * Example:
 * ```
 * Auth0Dimensions(
 *     buttonHeight = 56.dp,
 *     horizontalPadding = 24.dp
 * )
 * ```
 */
@Immutable
public data class Auth0Dimensions(

    val spacingXxs: Dp = 4.dp,

    val spacingXs: Dp = 8.dp,

    val spacingSm: Dp = 12.dp,

    val spacingMd: Dp = 16.dp,

    val spacingLg: Dp = 24.dp,

    val spacingXl: Dp = 32.dp,

    val spacingXxl: Dp = 48.dp,

    // Component sizing

    val buttonHeight: Dp = 52.dp,

    val inputHeight: Dp = 56.dp,

    val topBarHeight: Dp = 64.dp,

    // Icon sizes

    val iconSizeSmall: Dp = 20.dp,

    val iconSizeMedium: Dp = 24.dp,

    val iconSizeLarge: Dp = 32.dp,

    // Screen padding
    val horizontalPadding: Dp = 16.dp,

    val verticalPadding: Dp = 16.dp,

    // Border

    val borderWidth: Dp = 1.dp,

    val dividerThickness: Dp = 0.3.dp
)
