package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Spacing System
object Spacing {
    val none = 0.dp
    val extraSmall = 4.dp
    val small = 8.dp
    val extraMedium = 12.dp
    val medium = 16.dp
    val large = 24.dp
    val extraLarge = 32.dp
    val huge = 48.dp
}

// Elevation System
object Elevation {
    val flat = 0.dp
    val level1 = 1.dp
    val level2 = 3.dp
    val level3 = 6.dp
    val level4 = 8.dp
    val level5 = 12.dp
}

// Corner Radius System
object CornerRadius {
    val none = RoundedCornerShape(0.dp)
    val extraSmall = RoundedCornerShape(4.dp)
    val small = RoundedCornerShape(8.dp)
    val medium = RoundedCornerShape(12.dp)
    val large = RoundedCornerShape(16.dp)
    val extraLarge = RoundedCornerShape(24.dp)
    val full = RoundedCornerShape(50) // Pill shape
}

// Icon Sizes System
object IconSizes {
    val extraSmall = 16.dp
    val small = 20.dp
    val medium = 24.dp
    val large = 32.dp
    val extraLarge = 48.dp
}

// Animation Durations
object Duration {
    const val short = 150
    const val medium = 300
    const val long = 500
}

// Specialized Custom Theme Colors (Custom Palette Extensions)
data class CustomColors(
    val success: Color = Color(0xFF4CAF50),
    val onSuccess: Color = Color(0xFFFFFFFF),
    val successContainer: Color = Color(0xFFE8F5E9),
    val warning: Color = Color(0xFFFF9800),
    val onWarning: Color = Color(0xFFFFFFFF),
    val warningContainer: Color = Color(0xFFFFF3E0),
    val info: Color = Color(0xFF2196F3),
    val bookmark: Color = Color(0xFFFFC107), // Golden Accent
    val searchHighlight: Color = Color(0xFFFFEB3B), // Highlight Yellow
    val searchHighlightText: Color = Color(0xFF3E2723), // Highlight Text Dark Brown
    
    // Stats Colors for visual charts & metrics
    val statPolity: Color = Color(0xFFE91E63),
    val statHistory: Color = Color(0xFF3F51B5),
    val statEconomy: Color = Color(0xFF009688),
    val statGeography: Color = Color(0xFF9C27B0),
    val statIR: Color = Color(0xFFFF5722),
    val statGeneral: Color = Color(0xFF607D8B)
)

val LocalCustomColors = compositionLocalOf { CustomColors() }

val MaterialTheme.customColors: CustomColors
    @Composable
    @ReadOnlyComposable
    get() = LocalCustomColors.current
