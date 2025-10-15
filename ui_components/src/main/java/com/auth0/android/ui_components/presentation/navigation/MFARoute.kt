package com.auth0.android.ui_components.presentation.navigation

import com.auth0.android.ui_components.domain.model.AuthenticatorType
import kotlinx.serialization.Serializable


@Serializable
object MFAMethodList

@Serializable
data class MFAEnrolledItem(val authenticatorType: AuthenticatorType)

