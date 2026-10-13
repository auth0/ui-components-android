package com.auth0.universalcomponents.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auth0.universalcomponents.theme.Auth0Theme

private const val ERROR_FILL_ALPHA = 0.5f
private const val ERROR_EDGE_ALPHA = 0.18f

@Composable
internal fun OTPInputField(
    value: String,
    length: Int = 6,
    isError: Boolean = false,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester
) {
    val dimensions = Auth0Theme.dimensions

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        decorationBox = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensions.spacingXs)
            ) {
                repeat(length) { index ->
                    OTPBox(
                        value = value.getOrNull(index)?.toString() ?: "",
                        isError = isError,
                        isFocused = value.length == index,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        textStyle = TextStyle(
            fontSize = 0.sp,
            color = Color.Transparent
        )
    )
}

@Composable
private fun OTPBox(
    modifier: Modifier = Modifier,
    value: String,
    isError: Boolean,
    isFocused: Boolean = false
) {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val shapes = Auth0Theme.shapes

    val errorEdge = colors.textOnError.copy(alpha = ERROR_EDGE_ALPHA)

    val backgroundColor = when {
        isError -> colors.backgroundError.copy(alpha = ERROR_FILL_ALPHA).compositeOver(colors.backgroundLayerTop)
        else -> colors.backgroundLayerMedium
    }

    val borderColor = when {
        isError -> errorEdge
        else -> colors.borderDefault
    }

    val borderWidth = if (isFocused) 3.dp else 1.dp

    Box(
        modifier = modifier
            .aspectRatio(0.85f)
            .shadow(
                elevation = if (isError) 1.dp else 0.dp,
                shape = shapes.medium,
                ambientColor = errorEdge,
                spotColor = errorEdge
            )
            .background(
                color = backgroundColor,
                shape = shapes.medium
            )
            .border(
                width = borderWidth,
                color = borderColor,
                shape = shapes.medium
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            style = typography.titleLarge,
            color = if (isError) colors.textOnError else colors.textBold,
            textAlign = TextAlign.Center
        )
    }
}
