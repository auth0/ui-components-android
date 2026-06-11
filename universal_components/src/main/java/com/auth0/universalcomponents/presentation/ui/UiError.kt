package com.auth0.universalcomponents.presentation.ui

import com.auth0.universalcomponents.domain.error.Auth0Error

data class UiError(val error: Auth0Error, val onRetry: () -> Unit)
