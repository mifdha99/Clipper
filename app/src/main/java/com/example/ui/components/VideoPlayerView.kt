package com.example.ui.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.ui.theme.CrimsonRedPrimary
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ViralYellow
import com.example.ui.viewmodel.AspectRatioMode
import com.example.ui.viewmodel.CaptionPosition
import com.example.ui.viewmodel.CaptionStyle
import com.example.ui.viewmodel.TransitionEffect
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    videoStreamUrl: String,
    thumbnailUrl: String,
    isPlaying: Boolean,
    currentPositionMs: Long,
    inTimestampMs: Long,
    outTimestampMs: Long,
    aspectRatioMode: AspectRatioMode,
    blurredBackgroundEnabled: Boolean,
    overlayText: String,
    captionStyle: CaptionStyle,
    captionPosition: CaptionPosition,
    captionSize: Float,
    transitionEffect: TransitionEffect,
    speedMultiplier: Float,
    onPositionChanged: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isBuffering by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }

    // Media3 ExoPlayer instance
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // Attach video source when videoStreamUrl changes
    LaunchedEffect(videoStreamUrl) {
        if (videoStreamUrl.isNotBlank()) {
            playbackError = null
            try {
                val mediaItem = MediaItem.fromUri(Uri.parse(videoStreamUrl))
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.seekTo(inTimestampMs)
            } catch (e: Exception) {
                playbackError = "Gagal memuat video: ${e.message}"
            }
        }
    }

    // Speed multiplier synchronization
    LaunchedEffect(speedMultiplier) {
        exoPlayer.playbackParameters = PlaybackParameters(speedMultiplier)
    }

    // Play / Pause synchronization
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            if (exoPlayer.currentPosition >= outTimestampMs) {
                exoPlayer.seekTo(inTimestampMs)
            }
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    // Seek synchronization from external controls
    LaunchedEffect(currentPositionMs) {
        val currentDiff = Math.abs(exoPlayer.currentPosition - currentPositionMs)
        if (currentDiff > 250 && !exoPlayer.isPlaying) {
            exoPlayer.seekTo(currentPositionMs)
        }
    }

    // Boundary checking and position reporting loop
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            val current = exoPlayer.currentPosition
            onPositionChanged(current)

            // Loop back to in-point if out-point is reached
            if (current >= outTimestampMs) {
                exoPlayer.seekTo(inTimestampMs)
                onPositionChanged(inTimestampMs)
            }
            delay(50)
        }
    }

    // Player event listener for buffering and errors
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = (playbackState == Player.STATE_BUFFERING)
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                playbackError = error.localizedMessage ?: "Kesalahan pemutaran video."
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Aspect ratio container
    val containerRatio = when (aspectRatioMode) {
        AspectRatioMode.PORTRAIT_9_16 -> 9f / 16f
        AspectRatioMode.SQUARE_1_1 -> 1f
        AspectRatioMode.LANDSCAPE_16_9 -> 16f / 9f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(containerRatio)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .border(1.5.dp, Color(0xFF262D42), RoundedCornerShape(16.dp))
            .clickable(onClick = onTogglePlayPause),
        contentAlignment = Alignment.Center
    ) {
        // Blurred Background for 9:16 portrait mode
        if (aspectRatioMode == AspectRatioMode.PORTRAIT_9_16 && blurredBackgroundEnabled && thumbnailUrl.isNotBlank()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(28.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            )
        }

        // ExoPlayer AndroidView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering Indicator
        if (isBuffering) {
            CircularProgressIndicator(
                color = CrimsonRedPrimary,
                modifier = Modifier.size(42.dp)
            )
        }

        // Play/Pause Overlay Icon when paused
        if (!isPlaying && !isBuffering && playbackError == null) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        // Error message overlay
        if (playbackError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF38151B), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Gagal memutar video: $playbackError",
                        color = Color(0xFFFCA5A5),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Teks Overlay & Caption Rendering
        if (overlayText.isNotBlank()) {
            val align = when (captionPosition) {
                CaptionPosition.TOP -> Alignment.TopCenter
                CaptionPosition.CENTER -> Alignment.Center
                CaptionPosition.BOTTOM -> Alignment.BottomCenter
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                contentAlignment = align
            ) {
                when (captionStyle) {
                    CaptionStyle.VIRAL_YELLOW -> {
                        Text(
                            text = overlayText,
                            color = ViralYellow,
                            fontSize = captionSize.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                shadow = Shadow(color = Color.Black, blurRadius = 14f)
                            )
                        )
                    }
                    CaptionStyle.MINIMAL_WHITE -> {
                        Text(
                            text = overlayText,
                            color = Color.White,
                            fontSize = captionSize.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                shadow = Shadow(color = Color.Black.copy(alpha = 0.8f), blurRadius = 8f)
                            )
                        )
                    }
                    CaptionStyle.NEON_GLOW -> {
                        Text(
                            text = overlayText,
                            color = NeonCyan,
                            fontSize = captionSize.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                shadow = Shadow(color = CrimsonRedPrimary, blurRadius = 20f)
                            )
                        )
                    }
                    CaptionStyle.SUBTITLE_BOX -> {
                        Surface(
                            color = Color.Black.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = overlayText,
                                color = Color.White,
                                fontSize = (captionSize * 0.9f).sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
