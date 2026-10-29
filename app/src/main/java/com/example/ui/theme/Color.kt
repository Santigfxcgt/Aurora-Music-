package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val AuroraPrimary = Color(0xFF7C4DFF)
val AuroraSecondary = Color(0xFF00E5FF)
val AuroraAccent = Color(0xFFFF4081)
val AuroraBackgroundDark = Color(0xFF0D0E15)
val AuroraSurfaceDark = Color(0xFF161824)
val AuroraGlassBorder = Color(0x33FFFFFF)
val AuroraGlassHighlight = Color(0x1AFFFFFF)

val AuroraGolden = Color(0xFFFFD700)
val AuroraTextPrimary = Color(0xFFFFFFFF)
val AuroraTextSecondary = Color(0xB3FFFFFF)

data class LiquidPalette(
    val primary: Color,
    val secondary: Color,
    val darkBackground: Color,
    val glowingAccent: Color
) {
    val primaryAccent: Color get() = primary
    val ambientGlow: Color get() = glowingAccent

    companion object {
        fun fromSong(song: com.example.model.Song?): LiquidPalette = LiquidThemeColors.Default

        @androidx.compose.runtime.Composable
        fun rememberLiquidPalette(song: com.example.model.Song?): LiquidPalette =
            com.example.ui.theme.rememberLiquidPalette(song)

        fun getSpecularHighlightBrush(): androidx.compose.ui.graphics.Brush {
            return androidx.compose.ui.graphics.Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.25f),
                    Color.White.copy(alpha = 0.05f),
                    Color.Transparent
                )
            )
        }

        fun getGlassBorderBrush(topAlpha: Float = 0.35f, bottomAlpha: Float = 0.08f): androidx.compose.ui.graphics.Brush {
            return androidx.compose.ui.graphics.Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = topAlpha),
                    Color.White.copy(alpha = bottomAlpha)
                )
            )
        }
    }
}

object LiquidThemeColors {
    val Default = LiquidPalette(
        primary = Color(0xFF7C4DFF),
        secondary = Color(0xFF00E5FF),
        darkBackground = Color(0xFF0D0E15),
        glowingAccent = Color(0xFFFF4081)
    )
}
