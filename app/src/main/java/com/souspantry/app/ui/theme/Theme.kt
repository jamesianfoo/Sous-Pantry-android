package com.souspantry.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Brand colours (mirrors Figma "Sous Pantry — Design System" · 02 Colour) ──

// Foundation
val Beige      = Color(0xFFEDE9DF)  // app background — the default canvas
val Cream      = Color(0xFFF7F3EC)  // section / chip backgrounds
val SoftMint   = Color(0xFFE3EDE6)  // subtle highlights, mint accents
val Parchment  = Color(0xFFFAF6EE)  // input fields, subtle wells
val Paper      = Color(0xFFFFFDF8)  // cards, sheets, modals — the lift

// Ink
val Navy       = Color(0xFF162437)  // primary text, primary buttons
val NavyDeep   = Color(0xFF0F1A26)  // pressed states, paywall gradient
val Slate      = Color(0xFF647080)  // body copy, supporting text
val Ink3       = Color(0xFF7B8794)  // captions, metadata, placeholders

// Action
val Green      = Color(0xFF2D5A3D)  // Forest — pro features, success, "fresh"
val ForestDeep = Color(0xFF1E4029)  // pro pressed, gradient terminus
val Sage       = Color(0xFF8FA68E)  // subtle highlights, illustration

// Spice
val Terracotta = Color(0xFFC76F3F)  // warmth, alerts, recipe heat
val Gold       = Color(0xFFC4965A)  // premium tier, premium accents
val Clay       = Color(0xFFB85C38)  // pressed terracotta, deep accents

// Semantic
val Danger     = Color(0xFFB23A48)  // expired, destructive actions
val Info       = Color(0xFF2C5F7C)  // tips, neutral notifications

val White      = Color(0xFFFFFFFF)

private val LightColorScheme = lightColorScheme(
    primary          = Green,
    onPrimary        = White,
    primaryContainer = SoftMint,
    secondary        = Navy,
    onSecondary      = White,
    background       = Beige,
    onBackground     = Navy,
    surface          = Paper,
    onSurface        = Navy,
    surfaceVariant   = SoftMint,
    outline          = Slate,
    error            = Danger,
)

@Composable
fun SousPantryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = Typography,
        content     = content,
    )
}
