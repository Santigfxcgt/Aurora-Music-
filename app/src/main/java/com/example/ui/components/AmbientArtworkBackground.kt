package com.example.ui.components

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.net.Uri
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.Song
import com.example.service.CoverType
import com.example.service.CustomCoverManager
import com.example.ui.theme.AmbientColorExtractor
import com.example.ui.theme.LiquidPalette
import com.example.ui.theme.LiquidThemeColors

@Composable
fun AmbientArtworkBackground(
    song: Song?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    blurRadius: Dp = 60.dp,
    blurScale: Float = 1.3f
) {
    val context = LocalContext.current
    val customCovers by CustomCoverManager.customCovers.collectAsState()
    val customCover = song?.let { customCovers[it.id] }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            customCover != null -> {
                when (customCover.type) {
                    CoverType.VIDEO -> {
                        AmbientVideoBackground(
                            videoUri = customCover.fileUri,
                            isPlaying = isPlaying,
                            scale = blurScale,
                            blurRadius = blurRadius
                        )
                    }
                    CoverType.GIF -> {
                        val gifLoader = remember(context) { CustomCoverManager.getGifImageLoader(context) }
                        AmbientGifBackground(
                            gifUri = customCover.fileUri,
                            gifImageLoader = gifLoader,
                            scale = blurScale,
                            blurRadius = blurRadius
                        )
                    }
                    CoverType.IMAGE -> {
                        AmbientImageBackground(
                            imageUri = customCover.fileUri,
                            scale = blurScale,
                            blurRadius = blurRadius
                        )
                    }
                }
            }
            song?.albumArtUri != null -> {
                AmbientImageBackground(
                    imageUri = song.albumArtUri,
                    scale = blurScale,
                    blurRadius = blurRadius
                )
            }
            song?.albumArtResId != null -> {
                AmbientResImageBackground(
                    resId = song.albumArtResId,
                    scale = blurScale,
                    blurRadius = blurRadius
                )
            }
            else -> {
                var palette by remember { mutableStateOf(LiquidThemeColors.Default) }
                LaunchedEffect(song) {
                    if (song != null) {
                        palette = AmbientColorExtractor.extractFromResId(context, com.example.R.drawable.aurora_app_icon)
                    }
                }
                AmbientDefaultAtmosphere(
                    animatedPrimary = palette.primary,
                    animatedGlow = palette.glowingAccent,
                    animatedDark = palette.darkBackground
                )
            }
        }

        // Overlay ambient dark vignette gradient for Liquid Glass contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.55f),
                            Color(0xFF0D0E15).copy(alpha = 0.85f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun AmbientVideoBackground(
    videoUri: String,
    isPlaying: Boolean,
    scale: Float,
    blurRadius: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }

    val exoPlayer = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f
            repeatMode = Player.REPEAT_MODE_ALL
            setMediaItem(MediaItem.fromUri(Uri.parse(videoUri)))
            prepare()
        }
    }

    DisposableEffect(isPlaying) {
        if (isPlaying) exoPlayer.play() else exoPlayer.pause()
        onDispose { }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoWidth = videoSize.width
                    videoHeight = videoSize.height
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            FrameLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                val textureView = TextureView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                            exoPlayer.setVideoSurface(android.view.Surface(surface))
                            applyBackgroundCenterCropTransform(this@apply, videoWidth, videoHeight)
                        }

                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                            applyBackgroundCenterCropTransform(this@apply, videoWidth, videoHeight)
                        }

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            exoPlayer.setVideoSurface(null)
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                            if (videoWidth > 0 && videoHeight > 0) {
                                applyBackgroundCenterCropTransform(this@apply, videoWidth, videoHeight)
                            }
                        }
                    }
                }
                addView(textureView)
            }
        },
        update = { frameLayout ->
            val textureView = frameLayout.getChildAt(0) as? TextureView
            if (textureView != null && videoWidth > 0 && videoHeight > 0) {
                applyBackgroundCenterCropTransform(textureView, videoWidth, videoHeight)
            }
        },
        modifier = modifier
            .fillMaxSize()
            .scale(scale)
            .blur(blurRadius)
    )
}

@Composable
private fun AmbientGifBackground(
    gifUri: String,
    gifImageLoader: coil.ImageLoader,
    scale: Float,
    blurRadius: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(gifUri)
            .crossfade(true)
            .build(),
        imageLoader = gifImageLoader,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxSize()
            .scale(scale)
            .blur(blurRadius)
    )
}

@Composable
private fun AmbientImageBackground(
    imageUri: String,
    scale: Float,
    blurRadius: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageUri)
            .crossfade(true)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxSize()
            .scale(scale)
            .blur(blurRadius)
    )
}

@Composable
private fun AmbientResImageBackground(
    resId: Int,
    scale: Float,
    blurRadius: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(resId)
            .crossfade(true)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxSize()
            .scale(scale)
            .blur(blurRadius)
    )
}

@Composable
private fun AmbientDefaultAtmosphere(
    animatedPrimary: Color,
    animatedGlow: Color,
    animatedDark: Color,
    modifier: Modifier = Modifier
) {
    val color1 by animateColorAsState(animatedPrimary, tween(1000), label = "c1")
    val color2 by animateColorAsState(animatedGlow, tween(1000), label = "c2")
    val color3 by animateColorAsState(animatedDark, tween(1000), label = "c3")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        color1.copy(alpha = 0.6f),
                        color2.copy(alpha = 0.4f),
                        color3
                    )
                )
            )
    )
}

private fun applyBackgroundCenterCropTransform(textureView: TextureView, videoWidth: Int, videoHeight: Int) {
    val viewWidth = textureView.width
    val viewHeight = textureView.height

    if (viewWidth <= 0 || viewHeight <= 0 || videoWidth <= 0 || videoHeight <= 0) return

    val viewAspect = viewWidth.toFloat() / viewHeight.toFloat()
    val videoAspect = videoWidth.toFloat() / videoHeight.toFloat()

    var scaleX = 1f
    var scaleY = 1f

    if (videoAspect > viewAspect) {
        scaleX = videoAspect / viewAspect
    } else {
        scaleY = viewAspect / viewAspect
    }

    val matrix = Matrix()
    matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
    textureView.setTransform(matrix)
}
