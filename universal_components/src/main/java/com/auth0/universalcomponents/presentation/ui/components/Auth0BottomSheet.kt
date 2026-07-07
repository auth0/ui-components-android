package com.auth0.universalcomponents.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.theme.Auth0Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Auth0BottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    showCloseButton: Boolean = true,
    scrollable: Boolean = true,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Auth0Theme.colors
    val dimensions = Auth0Theme.dimensions

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.backgroundLayerBase,
        shape = RoundedCornerShape(
            topStart = 38.dp,
            topEnd = 38.dp,
        ),
        dragHandle = { Grabber() }
    ) {
        if (showCloseButton) {
            CloseButton(
                onClick = onDismiss,
                modifier = Modifier.padding(
                    start = dimensions.spacingMd,
                    bottom = dimensions.spacingXs,
                ),
            )
        }

        val contentModifier = modifier
            .fillMaxWidth()
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(horizontal = dimensions.spacingLg)
            .padding(bottom = dimensions.spacingXxl)

        Column(
            modifier = contentModifier,
            content = content,
        )
    }
}

@Composable
private fun Grabber() {
    val colors = Auth0Theme.colors
    Box(
        modifier = Modifier
            .padding(top = 4.dp)
            .width(36.dp)
            .height(4.dp)
            .background(
                color = colors.borderBold,
                shape = CircleShape,
            ),
    )
}

@Composable
private fun CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {

    val colors = Auth0Theme.colors

    Box(
        modifier = modifier
            .width(44.dp)
            .height(44.dp)
            .background(
                color = colors.borderDefault,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_cancel),
            contentDescription = stringResource(R.string.close),
            tint = Color.Unspecified,
        )
    }
}
