package com.auth0.android.ui_components.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.auth0.android.ui_components.theme.ButtonBlack

@Composable
fun GradientButton(
    modifier: Modifier = Modifier,
    gradient: Brush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.15f),
            Color.Transparent
        )
    ),
    buttonDefaultColor: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = ButtonBlack,
        contentColor = Color.White,
        disabledContainerColor = ButtonBlack,
        disabledContentColor = Color.White
    ),
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    elevation: ButtonElevation = ButtonDefaults.buttonElevation(
        defaultElevation = 0.dp,
        pressedElevation = 2.dp
    ),
    isLoading: Boolean = false,
    enabled: Boolean = true,
    borderStroke: BorderStroke?=null,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {

    Button(
        modifier = modifier,
        colors = buttonDefaultColor,
        shape = shape,
        contentPadding = PaddingValues(),
        elevation = elevation,
        enabled = enabled && !isLoading,
        border = borderStroke,
        onClick = { onClick() },
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .then(modifier),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading) {
                    CircularLoader(
                        modifier = Modifier.size(16.dp),
                        color = Color.White.copy(alpha = 0.75f),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                } else {
                    content()
                }
            }
        }
    }

}