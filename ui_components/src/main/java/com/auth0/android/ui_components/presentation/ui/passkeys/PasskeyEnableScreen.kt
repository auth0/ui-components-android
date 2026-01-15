package com.auth0.android.ui_components.presentation.ui.passkeys

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auth0.android.ui_components.R
import com.auth0.android.ui_components.presentation.ui.components.GradientButton
import com.auth0.android.ui_components.presentation.ui.components.TopBar
import com.auth0.android.ui_components.theme.ButtonBlack
import com.auth0.android.ui_components.theme.TextInputBlack
import com.auth0.android.ui_components.theme.textInputStyle

/**
 * Passkey Enable Screen
 *
 * Displays information about passkeys and allows users to enable passkey authentication.
 */
@Composable
fun PasskeyEnableScreen(
    onBackClick: () -> Unit = {},
    onEnableClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.passkeys_title),
                onBackClick = onBackClick
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 36.dp),
            verticalArrangement = Arrangement.Center
        ) {
            PasskeyIcon()

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = stringResource(R.string.enable_passkey),
                style = textInputStyle.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 36.sp
                ),
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "\u2022  ${stringResource(R.string.what_are_passkeys)}",
                style = textInputStyle.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 26.sp,
                    letterSpacing = (-0.34).sp
                ),
                color = Color.Black
            )
            Text(
                modifier = Modifier.padding(vertical = 6.dp),
                text = stringResource(R.string.passkeys_description),
                style = textInputStyle.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 24.sp,
                    letterSpacing = (-0.176).sp
                ),
                color = Color(0xFF606060)
            )

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "\u2022  ${stringResource(R.string.where_are_passkeys_saved)}",
                style = textInputStyle.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 26.sp,
                    letterSpacing = (-0.34).sp
                ),
                color = Color.Black
            )
            Text(
                modifier = Modifier.padding(vertical = 6.dp),
                text = stringResource(R.string.passkeys_saved_description),
                style = textInputStyle.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 24.sp,
                    letterSpacing = (-0.176).sp
                ),
                color = Color(0xFF606060)
            )

            Spacer(modifier = Modifier.height(40.dp))

            GradientButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                onClick = onEnableClick,
                shape = RoundedCornerShape(16.dp),
                buttonDefaultColor = ButtonDefaults.buttonColors(
                    containerColor = ButtonBlack,
                    contentColor = Color.White
                ),
                gradient = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.Transparent
                    )
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 2.dp
                ),
                isLoading = false
            ) {
                Text(
                    text = stringResource(R.string.enable),
                    style = textInputStyle.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 24.sp
                    ),
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Passkey Icon Component
 *
 * Displays the concentric circles passkey icon
 */
@Composable
private fun PasskeyIcon() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        repeat(5) { index ->
            val sizes = listOf(165.dp, 139.62.dp, 114.23.dp, 88.85.dp, 63.46.dp)
            Box(
                modifier = Modifier
                    .size(sizes[index])
                    .clip(CircleShape)
                    .background(
                        color = Color(0xFFB1E4DE).copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            )
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_passkey),
            contentDescription = stringResource(R.string.passkeys_title),
            modifier = Modifier.size(48.dp),
            tint = TextInputBlack
        )
    }
}
