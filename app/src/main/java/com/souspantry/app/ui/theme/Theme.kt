package com.souspantry.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Brand colours (mirrors iOS Theme.swift) ───────────────────────────────────
val Navy       = Color(0xFF162437)
val Green      = Color(0xFF2D5A3D)
val Cream      = Color(0xFFF7F3EC)
val SoftMint   = Color(0xFFE3EDE6)
val Gold       = Color(0xFFC4965A)
val Slate      = Color(0xFF647080)
val White      = Color(0xFFFFFFFF)

private val LightColorScheme = lightColorScheme(
    primary          = Green,
    onPrimary        = White,
    primaryContainer = SoftMint,
    secondary        = Navy,
    onSecondary      = White,
    background       = Cream,
    onBackground     = Navy,
    surface          = White,
    onSurface        = Navy,
    surfaceVariant   = SoftMint,
    outline          = Slate,
)

@Composable
fun SousPantryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = Typography,
        content     = content,
    )
}
