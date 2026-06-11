package com.auth0.universalcomponents.presentation.ui.menu

sealed interface MenuAction {
    object Remove : MenuAction
}

data class MenuItem(
    val label: String,
    val action: MenuAction
)
