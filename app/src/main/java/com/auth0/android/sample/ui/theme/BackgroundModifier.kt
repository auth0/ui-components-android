package com.auth0.android.sample.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import com.auth0.universalcomponents.theme.Auth0Theme

/**
 * Applies the Auth0 screen background to this modifier.
 *
 * Light theme: vertical gradient [BackGroundColor]
 * Dark theme: flat [DarkBackGroundColor] (#09090B)
 *
 * Theme mode is detected from [Auth0Theme.colors] so it correctly follows the in-app
 * theme switcher, not just the OS dark mode setting.
 */
@Composable
fun Modifier.auth0ScreenBackground(): Modifier {
    val colors = Auth0Theme.colors
    // backgroundLayerBase is #09090B (red≈0.035) in dark, #F4F4F5 (red≈0.957) in light.
    val isDark = colors.backgroundLayerBase.red < 0.1f
    val brush = if (isDark) DarkBackGroundColor else BackGroundColor
    return this
        .background(brush)
        .run {
            if (!isDark) background(BottomWarmOverlay).background(BottomCoolOverlay)
            else this
        }
}
