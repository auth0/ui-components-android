package com.auth0.android.sample.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.auth0.android.sample.R
import com.auth0.android.sample.ui.components.NavigationGridCard
import com.auth0.android.sample.ui.components.SectionHeader
import com.auth0.universalcomponents.theme.Auth0Theme

enum class DashboardDestination(val label: String, val icon: Int) {
    Profile("Profile", R.drawable.ic_person),
    LoginSecurity("Login & Security", R.drawable.ic_login_security),
    Tokens("Tokens", R.drawable.ic_tokens),
    Sessions("Sessions", R.drawable.ic_sessions),
    Docs("Docs", R.drawable.ic_docs),
    Favorites("Favorites", R.drawable.ic_favorites)
}

/**
 * Post-login dashboard/home screen with a greeting and a 2x3 navigation card grid.
 *
 *
 * @param userName Display name of the authenticated user
 * @param onNavigate Callback with the selected dashboard destination
 * @param onLogout Callback when log out is tapped
 */
@Composable
fun DashboardScreen(
    userName: String = "User",
    onNavigate: (DashboardDestination) -> Unit,
    onLogout: () -> Unit
) {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val dimensions = Auth0Theme.dimensions

    Box(
        modifier = Modifier.background(colors.backgroundLayerBase)
    ) {
        Column(
            modifier = Modifier
                .padding(dimensions.spacingLg)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(dimensions.spacingXxl * 2))

            SectionHeader(
                title = "Hi, $userName",
                subtitle = "Discover how to utilize auth0's powerful native SDK and account API in this app."
            )

            Spacer(modifier = Modifier.height(dimensions.spacingLg))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(0.dp),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                verticalArrangement = Arrangement.spacedBy(dimensions.spacingMd),
                modifier = Modifier.weight(1f)
            ) {
                items(DashboardDestination.entries.toList()) { destination ->
                    NavigationGridCard(
                        label = destination.label,
                        icon = destination.icon,
                        onClick = { onNavigate(destination) }
                    )
                }
            }

            TextButton(onClick = onLogout) {
                Text(
                    text = "Log out",
                    style = typography.label,
                    color = colors.textBold
                )
            }
        }
    }
}
