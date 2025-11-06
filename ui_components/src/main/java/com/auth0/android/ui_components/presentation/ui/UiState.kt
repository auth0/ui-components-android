package com.auth0.android.ui_components.presentation.ui

import com.auth0.android.ui_components.domain.error.Auth0Error


data class UiError(val error: Auth0Error, val onRetry: () -> Unit)