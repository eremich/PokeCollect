package com.yeromich.pokecollect.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.yeromich.pokecollect.R

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, weight: FontWeight) = Font(
    resId = res,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val DisplayFamily = FontFamily(
    variable(R.font.bricolage_grotesque, FontWeight.Medium),
    variable(R.font.bricolage_grotesque, FontWeight.Bold),
    variable(R.font.bricolage_grotesque, FontWeight.ExtraBold)
)

val TextFamily = FontFamily(
    variable(R.font.inter, FontWeight.Normal),
    variable(R.font.inter, FontWeight.Medium),
    variable(R.font.inter, FontWeight.SemiBold)
)

private fun display(size: Int, line: Int, weight: FontWeight = FontWeight.Bold, tracking: Double = -0.02) = TextStyle(
    fontFamily = DisplayFamily, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.em
)

private fun text(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, tracking: Double = 0.0) = TextStyle(
    fontFamily = TextFamily, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.em
)

val Typography = Typography(
    displayLarge = display(48, 52, FontWeight.ExtraBold, -0.03),
    displayMedium = display(40, 44, FontWeight.ExtraBold, -0.03),
    displaySmall = display(34, 38, FontWeight.ExtraBold, -0.03),
    headlineLarge = display(32, 36),
    headlineMedium = display(28, 32),
    headlineSmall = display(24, 28),
    titleLarge = display(22, 26),
    titleMedium = text(16, 22, FontWeight.SemiBold),
    titleSmall = text(14, 20, FontWeight.SemiBold),
    bodyLarge = text(16, 24),
    bodyMedium = text(14, 20),
    bodySmall = text(12, 16),
    labelLarge = text(14, 20, FontWeight.Medium),
    labelMedium = text(12, 16, FontWeight.Medium, 0.01),
    labelSmall = text(11, 16, FontWeight.Medium, 0.02)
)
