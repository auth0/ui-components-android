package com.auth0.android.ui_components.styles.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auth0.android.ui_components.theme.ActiveLabelBackground
import com.auth0.android.ui_components.theme.ActiveLabelText
import com.auth0.android.ui_components.theme.AuthenticatorItemBorder
import com.auth0.android.ui_components.theme.authenticatorItemSubTitle
import com.auth0.android.ui_components.theme.authenticatorItemTitle
import com.auth0.android.ui_components.theme.sectionHeading1
import com.auth0.android.ui_components.theme.sectionHeading2

/**
 * Style configuration for authenticator list item component.
 *
 * All properties are optional. When null, the component will use values from the theme.
 *
 * Example:
 * ```
 * AuthenticatorItemStyle(
 *     titleStyle = TextStyle(fontWeight = FontWeight.Bold),
 *     borderColor = Color.LightGray,
 *     iconTint = Color(0xFF6200EE)
 * )
 * ```
 */
@Immutable
public data class AuthenticatorItemStyle(

    val sectionTitle: TextStyle = sectionHeading1,
    val sectionSubtitle: TextStyle = sectionHeading2,
    /** Background color of the item */
    val backgroundColor: Color = Color.White,
    /** Border stroke of the item */
    val borderStroke: BorderStroke = BorderStroke(1.dp, AuthenticatorItemBorder),
    /** Shape/corner radius of the item */
    val shape: Shape = RoundedCornerShape(16.dp),
    /** Text style for the title */
    val titleStyle: TextStyle = authenticatorItemTitle,
    /** Text style for the subtitle */
    val subtitleStyle: TextStyle = authenticatorItemSubTitle,
    /** Tint color for the icon */
    val iconTint: Color = Color.Black,
    /** Background color for the active tag */
    val activeTagBackgroundColor: Color = ActiveLabelBackground,
    /** Text color for the active tag */
    val activeTagTextColor: Color = ActiveLabelText,
) {
    public companion object {
        /**
         * Default authenticator item style that uses theme values.
         */
        public val Default: AuthenticatorItemStyle = AuthenticatorItemStyle()
    }
}
