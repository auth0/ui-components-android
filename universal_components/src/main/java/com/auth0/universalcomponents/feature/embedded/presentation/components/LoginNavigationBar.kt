package com.auth0.universalcomponents.feature.embedded.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.theme.Auth0Theme

/** The top bar with the back arrow and the "Log in" title. */
@Composable
internal fun LoginNavigationBar(
    onBack: () -> Unit,
    title: String = stringResource(R.string.embedded_login_title),
) {
    val colors = Auth0Theme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Auth0Theme.dimensions.spacingXxl)
            .padding(horizontal = Auth0Theme.dimensions.spacingXxs),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.embedded_login_back),
                tint = colors.textBold,
                modifier = Modifier.size(Auth0Theme.sizes.iconMedium),
            )
        }
        Text(
            text = title,
            style = Auth0Theme.typography.title,
            color = colors.textBold,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/** The frame every login screen sits in: glowing background, navigation bar on top, content below. */
@Composable
internal fun LoginScreenFrame(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .loginBackground()
            .statusBarsPadding(),
    ) {
        LoginNavigationBar(onBack = onBack)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .navigationBarsPadding(),
        ) {
            content()
        }
    }
}
