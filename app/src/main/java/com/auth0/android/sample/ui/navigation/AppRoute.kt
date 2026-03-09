package com.auth0.android.sample.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute {

    @Serializable
    data object ChooseSignIn : AppRoute

    @Serializable
    data object EmbeddedLogin : AppRoute

    @Serializable
    data object ExploreLogin : AppRoute

    @Serializable
    data object GoogleLogin : AppRoute

    @Serializable
    data object Dashboard : AppRoute

    @Serializable
    data object Profile : AppRoute

    @Serializable
    data object UpdateFullName : AppRoute

    @Serializable
    data object EnablePasskey : AppRoute

    @Serializable
    data object LoginSecurity : AppRoute

    @Serializable
    data object Appearance : AppRoute

    @Serializable
    data object Tokens : AppRoute

    @Serializable
    data object Sessions : AppRoute

    @Serializable
    data object Docs : AppRoute

    @Serializable
    data object Favorites : AppRoute

    @Serializable
    data object Settings : AppRoute
}
