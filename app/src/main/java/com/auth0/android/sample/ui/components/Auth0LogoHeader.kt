package com.auth0.android.sample.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.auth0.android.sample.R
import com.auth0.android.sample.ui.theme.isAuth0DarkTheme
import com.auth0.universalcomponents.theme.Auth0Theme

/**
 * Auth0 logo header displayed at the top of screens.
 *
 * @param modifier Modifier for the header layout
 */
@Composable
fun Auth0LogoHeader(
    modifier: Modifier = Modifier,
) {
    val dimensions = Auth0Theme.dimensions
    val colors = Auth0Theme.colors
    val logoTint = if (isAuth0DarkTheme()) colors.textBold else Color.Unspecified

    Spacer(modifier = Modifier.height(dimensions.spacingXl))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = dimensions.spacingLg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(R.drawable.ic_auth0), contentDescription = "Auth0 logo",
            tint = logoTint
        )
    }
}
