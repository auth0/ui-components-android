package com.auth0.android.ui_components.styles.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.auth0.android.ui_components.theme.enrollmentTopbarTitle

/**
 * Style configuration for the TopBar component.
 *
 * All properties are optional. When null, the component will use values from the theme.
 *
 * Example:
 * ```
 * TopBarStyle(
 *     backgroundColor = Color(0xFF1A1A2E),
 *     titleStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
 *     showDivider = true
 * )
 * ```
 */
@Immutable
public data class TopBarStyle(
    /** Background color of the top bar */
    val backgroundColor: Color = Color.White,
    /** Text style for the title */
    val titleStyle: TextStyle = enrollmentTopbarTitle,
    /** Whether to show a divider below the top bar */
    val showDivider: Boolean = false,
)
