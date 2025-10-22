package com.auth0.android.ui_components.presentation.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
public fun SnackBar(
    message: String,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .padding(16.dp)
    )

    LaunchedEffect(Unit) {
        snackbarHostState.showSnackbar("Copied to clipboard")
    }
}