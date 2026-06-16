package com.auth0.android.sample.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.auth0.android.sample.ui.components.SampleTopBar
import com.auth0.universalcomponents.theme.Auth0Theme

@Composable
fun DocsScreen(onBack: () -> Unit) {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography

    Scaffold(
        topBar = {
            SampleTopBar(title = "Docs", onBackClick = onBack)
        },
        containerColor = colors.backgroundLayerBase
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Coming Soon",
                style = typography.display,
                color = colors.textDefault
            )
        }
    }
}
