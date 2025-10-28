package com.auth0.android.ui_components.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp


val defaultTopbarTitle = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontFamily = FontFamily.SansSerif,
    fontSize = 30.sp,
    color = TopBarTitle,
    letterSpacing = (-0.25).sp,
)

val enrollmentTopbarTitle = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontFamily = FontFamily.Default,
    fontSize = 18.sp,
    color = TopBarTitle,
    lineHeight = 22.sp,
    letterSpacing = (-0.43).sp
)

val enrollmentSubTitle = TextStyle(
    fontWeight = FontWeight.Normal,
    fontFamily = FontFamily.Default,
    fontSize = 14.sp,
    color = subTitleGray,
    letterSpacing = 0.sp
)

val sectionTitle = TextStyle(
    fontSize = 20.sp,
    fontWeight = FontWeight.SemiBold,
    fontFamily = FontFamily.Default,
    color = Color.Black,
    letterSpacing = 1.sp
)

val sectionSubtitle = TextStyle(
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal,
    fontFamily = FontFamily.Default,
    color = SectionSubtitle,
    letterSpacing = 1.sp
)

val AuthenticatorItemTitle = TextStyle(
    fontSize = 16.sp,
    fontWeight = FontWeight.Medium,
    fontFamily = FontFamily.Default,
    color = Color.Black,
    lineHeight = 24.sp,
    letterSpacing = 1.sp
)

val AuthenticatorItemSubTitle = TextStyle(
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal,
    fontFamily = FontFamily.Default,
    color = AuthenticatorItemSubtitle,
    lineHeight = 20.sp,
    letterSpacing = 1.sp
)

val EmptyAuthenticatorText = TextStyle(
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal,
    fontFamily = FontFamily.Default,
    color = EmptyAuthenticatorTextColor,
    letterSpacing = 0.sp
)



