package com.auth0.android.sample.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import com.auth0.universalcomponents.theme.Auth0Theme

/**
 * Returns true when the active Auth0Theme is in dark mode.
 *
 * Centralises the dark-mode check so it only needs updating in one place
 * if the underlying token changes.
 */
@Composable
fun isAuth0DarkTheme(): Boolean = Auth0Theme.colors.backgroundLayerBase.red < 0.1f

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
    val isDark = isAuth0DarkTheme()
    val brush = if (isDark) DarkBackGroundColor else BackGroundColor
    return this
        .background(brush)
        .run {
            if (!isDark) background(BottomWarmOverlay).background(BottomCoolOverlay)
            else this
        }
}
