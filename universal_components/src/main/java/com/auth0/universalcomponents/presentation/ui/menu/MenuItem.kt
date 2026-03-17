package com.auth0.universalcomponents.presentation.ui.menu

//TODO: Move this to the correct package based on usage

sealed interface MenuAction {
    object Remove : MenuAction
}

data class MenuItem(
    val label: String,
    val action: MenuAction
)