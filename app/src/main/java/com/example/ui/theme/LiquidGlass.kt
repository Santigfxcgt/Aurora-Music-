package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.Song
import com.example.service.CustomCoverManager
import com.example.ui.components.AmbientArtworkBackground

@Composable
fun rememberLiquidPalette(song: Song?): LiquidPalette {
    val context = LocalContext.current
    var currentPalette by remember { mutableStateOf(LiquidThemeColors.Default) }

    LaunchedEffect(song?.id) {
        if (song == null) {
            currentPalette = LiquidThemeColors.Default
            return@LaunchedEffect
        }

        val customCover = CustomCoverManager.customCovers.value[song.id]
        currentPalette = when {
            customCover != null -> {
                AmbientColorExtractor.extractFromUri(context, customCover.fileUri)
            }
            song.albumArtUri != null -> {
                AmbientColorExtractor.extractFromUri(context, song.albumArtUri)
            }
            song.albumArtResId != null -> {
                AmbientColorExtractor.extractFromResId(context, song.albumArtResId)
            }
            else -> {
                LiquidThemeColors.Default
            }
        }
    }

    val primary by animateColorAsState(currentPalette.primary, animationSpec = tween(1000), label = "primaryColor")
    val secondary by animateColorAsState(currentPalette.secondary, animationSpec = tween(1000), label = "secondaryColor")
    val darkBackground by animateColorAsState(currentPalette.darkBackground, animationSpec = tween(1000), label = "darkColor")
    val glowingAccent by animateColorAsState(currentPalette.glowingAccent, animationSpec = tween(1000), label = "glowColor")

    return LiquidPalette(
        primary = primary,
        secondary = secondary,
        darkBackground = darkBackground,
        glowingAccent = glowingAccent
    )
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = Color(0x1AFFFFFF),
    borderColor: Color = Color(0x33FFFFFF),
    borderWidth: Dp = 1.dp,
    elevation: Dp = 8.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(elevation = elevation, shape = shape, clip = false)
            .clip(shape)
            .background(backgroundColor)
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(
                        borderColor,
                        borderColor.copy(alpha = 0.05f)
                    )
                ),
                shape = shape
            )
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        content = content
    )
}

@Composable
fun LiquidAmbientBackground(
    currentSong: Song?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = rememberLiquidPalette(currentSong)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AuroraBackgroundDark)
    ) {
        AmbientArtworkBackground(
            song = currentSong,
            isPlaying = true,
            modifier = Modifier.fillMaxSize(),
            blurRadius = 32.dp
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            palette.primary.copy(alpha = 0.35f),
                            palette.glowingAccent.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        radius = 1400f
                    )
                )
        )

        content()
    }
}

fun Modifier.liquidGlass(
    cornerRadius: Dp = 24.dp,
    backgroundColor: Color = Color(0x1AFFFFFF),
    borderColor: Color = Color(0x33FFFFFF),
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(backgroundColor)
    .border(
        width = borderWidth,
        brush = Brush.linearGradient(
            colors = listOf(
                borderColor,
                borderColor.copy(alpha = 0.05f)
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )

fun Modifier.liquidGlassGlow(
    cornerRadius: Dp = 24.dp,
    glowColor: Color = Color(0xFF7C4DFF)
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        Brush.radialGradient(
            colors = listOf(
                glowColor.copy(alpha = 0.25f),
                Color.Transparent
            )
        )
    )
