package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Base Palette from User
val Chartreuse = Color(0xFFDDDE68)
val Tan = Color(0xFFD0BEA3)
val Olive = Color(0xFF8F917C)
val DarkGrey = Color(0xFF1F1F1F)
val SteelBlue = Color(0xFFBAC8E0)
val Peach = Color(0xFFEBDBD3)
val OffWhite = Color(0xFFF5F4F7)

// Light Colors
val LightPrimary = Olive
val LightOnPrimary = OffWhite
val LightPrimaryContainer = SteelBlue
val LightOnPrimaryContainer = DarkGrey

val LightSecondary = Tan
val LightOnSecondary = DarkGrey
val LightSecondaryContainer = Peach
val LightOnSecondaryContainer = DarkGrey

val LightTertiary = Chartreuse
val LightOnTertiary = DarkGrey
val LightTertiaryContainer = Chartreuse.copy(alpha = 0.5f)
val LightOnTertiaryContainer = DarkGrey

val LightBackground = OffWhite
val LightSurface = OffWhite
val LightSurfaceVariant = Peach
val LightOnBackground = DarkGrey
val LightOnSurface = DarkGrey
val LightOnSurfaceVariant = DarkGrey
val LightOutline = Olive

// Dark Colors
val DarkPrimary = Chartreuse
val DarkOnPrimary = DarkGrey
val DarkPrimaryContainer = Olive
val DarkOnPrimaryContainer = OffWhite

val DarkSecondary = SteelBlue
val DarkOnSecondary = DarkGrey
val DarkSecondaryContainer = Tan
val DarkOnSecondaryContainer = DarkGrey

val DarkTertiary = Peach
val DarkOnTertiary = DarkGrey
val DarkTertiaryContainer = Peach.copy(alpha = 0.5f)
val DarkOnTertiaryContainer = DarkGrey

val DarkBackground = DarkGrey
val DarkSurface = DarkGrey
val DarkSurfaceVariant = Olive.copy(alpha = 0.3f)
val DarkOnBackground = OffWhite
val DarkOnSurface = OffWhite
val DarkOnSurfaceVariant = OffWhite
val DarkOutline = Tan
