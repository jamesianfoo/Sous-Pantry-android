package com.souspantry.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.souspantry.app.R

// Inter — crisp professional UI font, close to SF Pro on iOS
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular,  FontWeight.Normal),
    Font(R.font.inter_medium,   FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold,     FontWeight.Bold),
)

// DS type scale (Figma · 03 Typography): 34/28/22/17/15/12, section labels
// 12 SemiBold +10% tracking UPPER.
val Typography = Typography(
    headlineLarge  = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold,     fontSize = 34.sp, lineHeight = 41.sp), // Screen Title
    headlineMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold,     fontSize = 28.sp, lineHeight = 34.sp), // Title 1
    headlineSmall  = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold,     fontSize = 22.sp, lineHeight = 28.sp), // Title 2
    titleLarge     = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold,     fontSize = 22.sp, lineHeight = 28.sp), // Title 2
    titleMedium    = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp), // Headline
    titleSmall     = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge      = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal,   fontSize = 17.sp, lineHeight = 24.sp), // Body
    bodyMedium     = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal,   fontSize = 15.sp, lineHeight = 21.sp), // Subheadline
    bodySmall      = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal,   fontSize = 13.sp, lineHeight = 18.sp), // Numbers size
    labelLarge     = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium    = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 1.2.sp), // Section Label
    labelSmall     = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium,   fontSize = 12.sp, lineHeight = 16.sp),
)
