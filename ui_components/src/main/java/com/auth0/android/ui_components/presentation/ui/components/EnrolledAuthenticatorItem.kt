package com.auth0.android.ui_components.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.auth0.android.ui_components.R
import com.auth0.android.ui_components.presentation.ui.menu.MenuAction
import com.auth0.android.ui_components.presentation.ui.menu.MenuItem
import com.auth0.android.ui_components.theme.AuthenticatorItemBorder
import com.auth0.android.ui_components.theme.AuthenticatorItemSubTitle
import com.auth0.android.ui_components.theme.AuthenticatorItemTitle


@Composable
fun EnrolledAuthenticatorItem(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    menuActions: List<MenuItem>? = null,
    onMenuActionClick: ((MenuAction) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, AuthenticatorItemBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = AuthenticatorItemTitle
                )
                Text(
                    text = subtitle,
                    style = AuthenticatorItemSubTitle
                )
            }

            if (!menuActions.isNullOrEmpty() && onMenuActionClick != null) {
                Box {
                    IconButton(
                        onClick = { expanded = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.more_options),
                            tint = Color.Black
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = !expanded },
                        modifier = Modifier.background(Color.White)
                    ) {
                        menuActions.forEach { menuAction ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = menuAction.label,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = {
                                    onMenuActionClick(menuAction.action)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


