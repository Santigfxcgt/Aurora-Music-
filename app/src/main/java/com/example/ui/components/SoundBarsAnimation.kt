package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SoundBarsAnimation(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF00E5FF),
    barWidth: Dp = 3.dp,
    maxHeight: Dp = 18.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "soundbars")

    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (isPlaying) 0.95f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )

    val scale2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isPlaying) 1.0f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, delayMillis = 100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )

    val scale3 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = if (isPlaying) 0.85f else 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, delayMillis = 200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    Row(
        modifier = modifier.height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val scales = listOf(scale1, scale2, scale3)
        scales.forEach { scale ->
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .fillMaxHeight(if (isPlaying) scale else 0.2f)
                    .clip(RoundedCornerShape(barWidth / 2))
                    .background(barColor)
            )
        }
    }
}
