package com.example.ui.components

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.net.Uri
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun SongArtwork(
    song: Song?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    allowVideoLoop: Boolean = true,
    isPlaying: Boolean = true
) {
    val context = LocalContext.current
    val customCovers by CustomCoverManager.customCovers.collectAsState()
    val customCover = song?.let { customCovers[it.id] }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF161824)),
        contentAlignment = Alignment.Center
    ) {
        when {
            customCover != null -> {
                when (customCover.type) {
                    CoverType.VIDEO -> {
                        if (allowVideoLoop) {
                            VideoCoverPlayer(
                                videoUri = customCover.fileUri,
                                isPlaying = isPlaying,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (!customCover.thumbnailUri.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(customCover.thumbnailUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Portada personalizada",
                                contentScale = contentScale,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            VideoCoverPlayer(
                                videoUri = customCover.fileUri,
                                isPlaying = isPlaying,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    CoverType.GIF -> {
                        val gifLoader = remember(context) { CustomCoverManager.getGifImageLoader(context) }
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(customCover.fileUri)
                                .crossfade(true)
                                .build(),
                            imageLoader = gifLoader,
                            contentDescription = "Portada GIF",
                            contentScale = contentScale,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    CoverType.IMAGE -> {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(customCover.fileUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Portada personalizada",
                            contentScale = contentScale,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            song?.albumArtUri != null -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(song.albumArtUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = song.title,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
            }
            song?.albumArtResId != null -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(song.albumArtResId)
                        .crossfade(true)
                        .build(),
                    contentDescription = song.title,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                GenericArtworkPlaceholder(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun GenericArtworkPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF7C4DFF).copy(alpha = 0.5f),
                        Color(0xFF161824)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(64.dp)
        )
    }
}

@Composable
fun VideoCoverPlayer(
    videoUri: String,
    isPlaying: Boolean = true,
    allowColorExtraction: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }

    val exoPlayer = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f // Muted
            repeatMode = Player.REPEAT_MODE_ALL
            setMediaItem(MediaItem.fromUri(Uri.parse(videoUri)))
            prepare()
        }
    }

    DisposableEffect(isPlaying) {
        if (isPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
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
                            applyCenterCropTransform(this@apply, videoWidth, videoHeight)
                        }

                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                            applyCenterCropTransform(this@apply, videoWidth, videoHeight)
                        }

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            exoPlayer.setVideoSurface(null)
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                            if (videoWidth > 0 && videoHeight > 0) {
                                applyCenterCropTransform(this@apply, videoWidth, videoHeight)
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
                applyCenterCropTransform(textureView, videoWidth, videoHeight)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

private fun applyCenterCropTransform(textureView: TextureView, videoWidth: Int, videoHeight: Int) {
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
        scaleY = viewAspect / videoAspect
    }

    val matrix = Matrix()
    matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
    textureView.setTransform(matrix)
}
