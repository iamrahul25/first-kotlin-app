package com.you.reelblocker.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Brand palette
val DarkBg = Color(0xFF0A0E17)
val DarkSurface = Color(0xFF121826)
val DarkCard = Color(0xFF1A2234)
val DarkCardBorder = Color(0xFF26334D)
val DarkCardHover = Color(0xFF222C42)

val CyanPrimary = Color(0xFF00E5FF)
val CyanSecondary = Color(0xFF06B6D4)
val VioletAccent = Color(0xFF8B5CF6)
val PurpleAccent = Color(0xFFA855F7)

val EmeraldActive = Color(0xFF10B981)
val EmeraldGlow = Color(0x3310B981)
val EmeraldBg = Color(0x1A10B981)

val AmberWarning = Color(0xFFF59E0B)
val AmberGlow = Color(0x33F59E0B)
val AmberBg = Color(0x1AF59E0B)

val RedAlert = Color(0xFFEF4444)
val RedBg = Color(0x1AEF4444)

val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Gradients
val PrimaryGradient = Brush.horizontalGradient(
    listOf(CyanPrimary, VioletAccent)
)

val ActiveShieldGradient = Brush.linearGradient(
    listOf(Color(0xFF00E5FF), Color(0xFF8B5CF6), Color(0xFF3B82F6))
)

val InactiveShieldGradient = Brush.linearGradient(
    listOf(Color(0xFF64748B), Color(0xFF334155))
)

val CardGradient = Brush.verticalGradient(
    listOf(Color(0xFF1E283E), Color(0xFF131A29))
)
