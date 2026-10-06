package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Typography defined per Waves design system — 100% Solid Black Text
val HeaderTitleStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    color = OnPrimary
)

val ScreenTitleStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    color = Color(0xFF000000)
)

val SectionTitleStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    color = Color(0xFF000000)
)

val BodyStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    color = Color(0xFF000000)
)

val CaptionStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    color = Color(0xFF000000)
)

val ButtonTextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp
)

val Typography = Typography(
    headlineMedium = ScreenTitleStyle,
    titleLarge = HeaderTitleStyle,
    titleMedium = SectionTitleStyle,
    bodyMedium = BodyStyle,
    bodySmall = CaptionStyle,
    labelLarge = ButtonTextStyle
)
