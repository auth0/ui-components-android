package com.auth0.android.ui_components.domain.model

/**
 * Represents a confirmed authentication method item for display
 * Contains only confirmed authentication methods of a specific type
 */
data class EnrolledAuthenticationMethod(
    val id: String,
    val type: String,
    val confirmed: Boolean,
    val createdAt: String,
    val name: String? = null,
)
