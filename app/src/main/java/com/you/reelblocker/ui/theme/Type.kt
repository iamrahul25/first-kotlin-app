package com.you.reelblocker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.you.reelblocker.R

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

private val Base = Typography()

val Typography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = Inter),
    displayMedium = Base.displayMedium.copy(fontFamily = Inter),
    displaySmall = Base.displaySmall.copy(fontFamily = Inter),
    headlineLarge = Base.headlineLarge.copy(fontFamily = Inter),
    headlineMedium = Base.headlineMedium.copy(fontFamily = Inter),
    headlineSmall = Base.headlineSmall.copy(fontFamily = Inter),
    titleLarge = Base.titleLarge.copy(fontFamily = Inter),
    titleMedium = Base.titleMedium.copy(fontFamily = Inter),
    titleSmall = Base.titleSmall.copy(fontFamily = Inter),
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.sp,
        color = Ink
    ),
    bodyMedium = Base.bodyMedium.copy(fontFamily = Inter, letterSpacing = 0.sp),
    bodySmall = Base.bodySmall.copy(fontFamily = Inter, letterSpacing = 0.sp),
    labelLarge = Base.labelLarge.copy(fontFamily = Inter, letterSpacing = 0.sp),
    labelMedium = Base.labelMedium.copy(fontFamily = Inter, letterSpacing = 0.sp),
    labelSmall = Base.labelSmall.copy(fontFamily = Inter, letterSpacing = 0.sp)
)
