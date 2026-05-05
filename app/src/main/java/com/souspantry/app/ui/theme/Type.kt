package com.souspantry.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    headlineLarge  = TextStyle(fontWeight = FontWeight.Bold,    fontSize = 30.sp, color = Navy),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold,    fontSize = 24.sp, color = Navy),
    titleLarge     = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Navy),
    titleMedium    = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Navy),
    bodyLarge      = TextStyle(fontWeight = FontWeight.Normal,  fontSize = 16.sp, color = Navy),
    bodyMedium     = TextStyle(fontWeight = FontWeight.Normal,  fontSize = 14.sp, color = Slate),
    labelSmall     = TextStyle(fontWeight = FontWeight.Medium,  fontSize = 11.sp, color = Slate),
)
