package com.auth0.android.sample.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

/**
 * Light theme background — vertical base gradient.
 *
 */
val BackGroundColor = Brush.linearGradient(
    colorStops = arrayOf(
        0.00f to Color(0xFFF4F4F5),
        0.75f to Color(0xFFEDEBF1),
        1.00f to Color(0xFFEBE1E4)
    ),
    start = Offset(0f, 0f),
    end = Offset(0f, Float.POSITIVE_INFINITY)
)

/**
 * Blended on top of [BackGroundColor] to reproduce the #E9D7D4 bottom-left sample.
 */
val BottomWarmOverlay = Brush.radialGradient(
    colors = listOf(Color(0x99E9D7D4), Color(0x00E9D7D4)),
    center = Offset(0f, Float.POSITIVE_INFINITY),
    radius = 900f
)

/**
 * Blended on top of [BackGroundColor] to reproduce the #EDEBF4 bottom-right sample.
 */
val BottomCoolOverlay = Brush.radialGradient(
    colors = listOf(Color(0x99EDEBF4), Color(0x00EDEBF4)),
    center = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
    radius = 900f
)

/**
 * Dark theme background — flat #09090B (Auth0 backgroundLayerBase dark).
 *
 */
val DarkBackGroundColor = Brush.linearGradient(
    colors = listOf(Color(0xFF09090B), Color(0xFF09090B))
)
