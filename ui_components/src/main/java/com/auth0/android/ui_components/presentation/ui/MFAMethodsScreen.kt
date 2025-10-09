package com.auth0.android.ui_components.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.auth0.android.ui_components.presentation.viewmodel.MFAMethodUiState
import com.auth0.android.ui_components.presentation.viewmodel.MFAMethodViewModel


@Composable
fun MFAMethodsScreen(
    viewModel: MFAMethodViewModel,
    onAuthenticatorClick: (String) -> Unit
) {

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchMFAMethods()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is MFAMethodUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is MFAMethodUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.mfaMethods) { mfaMethods ->
                        MFAMethodItem(
                            title = mfaMethods.name,
                            subtitle = "Last used: ${mfaMethods.lastUsed ?: "Never"}",
                            leadingIcon = Icons.Default.AccountBox,
                            trailingIcon = Icons.Default.Check,
                            tag = if (mfaMethods.isActive) "Active" else null,
                            onClick = { onAuthenticatorClick(mfaMethods.id) }
                        )
                    }
                }
            }

            is MFAMethodUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error: ${state.message}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            MFAMethodUiState.Idle -> {
                // Initial state
            }
        }
    }
}
