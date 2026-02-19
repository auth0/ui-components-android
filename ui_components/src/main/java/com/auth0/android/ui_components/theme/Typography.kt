package com.auth0.android.ui_components.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
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

val sectionHeading1 = TextStyle(
    fontSize = 20.sp,
    fontWeight = FontWeight.SemiBold,
    fontFamily = FontFamily.Default,
    color = Color.Black,
    letterSpacing = 1.sp
)

val sectionHeading2 = TextStyle(
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal,
    fontFamily = FontFamily.Default,
    color = secondaryTextColor,
    letterSpacing = 1.sp
)

val authenticatorItemTitle = TextStyle(
    fontSize = 16.sp,
    fontWeight = FontWeight.Medium,
    fontFamily = FontFamily.Default,
    color = Color.Black,
    lineHeight = 24.sp,
    letterSpacing = 1.sp
)

val authenticatorItemSubTitle = TextStyle(
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal,
    fontFamily = FontFamily.Default,
    color = AuthenticatorItemSubtitle,
    lineHeight = 20.sp,
    letterSpacing = 1.sp
)

val emptyAuthenticatorText = TextStyle(
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal,
    fontFamily = FontFamily.Default,
    color = EmptyAuthenticatorTextColor,
    letterSpacing = 0.sp
)

val sectionTitle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontStyle = FontStyle.Normal,
    fontWeight = FontWeight.Medium,
    fontSize = 24.sp,
    lineHeight = 1.1499.em,
    letterSpacing = 0.0125.em,
    color = Color.Black,
    textAlign = TextAlign.Center,
)

val contentTextStyle = TextStyle(
    fontFamily = interFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 17.5.sp,
    letterSpacing = 0.084.sp,
    color = SectionDescriptionTextColor,
    textAlign = TextAlign.Center
)

val textInputStyle = TextStyle(
    fontFamily = interFamily,
    fontSize = 20.sp,
    fontWeight = FontWeight.SemiBold,
    lineHeight = 24.sp,
    letterSpacing = 0.em,
)



