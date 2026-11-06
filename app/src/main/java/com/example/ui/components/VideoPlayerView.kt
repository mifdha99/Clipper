package com.example.ui.components

import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.ui.theme.CrimsonRedPrimary
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ViralYellow
import com.example.ui.viewmodel.AspectRatioMode
import com.example.ui.viewmodel.CaptionPosition
import com.example.ui.viewmodel.CaptionStyle
import com.example.ui.viewmodel.TransitionEffect
import kotlinx.coroutines.delay

private const val TAG = "VideoPlayerView"

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
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isMediaPrepared by remember { mutableStateOf(false) }
    var isVideoFailed by remember { mutableStateOf(false) }

    // Synchronize media playback speed safely
    LaunchedEffect(speedMultiplier, mediaPlayer, isMediaPrepared, isVideoFailed) {
        if (isMediaPrepared && !isVideoFailed) {
            try {
                mediaPlayer?.let { player ->
                    player.playbackParams = player.playbackParams.setSpeed(speedMultiplier)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Speed adjustment skipped: ${e.message}")
            }
        }
    }

    // Synchronize play/pause safely
    LaunchedEffect(isPlaying, isMediaPrepared, isVideoFailed) {
        if (isMediaPrepared && !isVideoFailed) {
            try {
                mediaPlayer?.let { player ->
                    if (isPlaying) {
                        if (!player.isPlaying) player.start()
                    } else {
                        if (player.isPlaying) player.pause()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Playback toggle handled: ${e.message}")
            }
        }
    }

    // Robust playback loop with automatic simulation fallback
    LaunchedEffect(isPlaying, isMediaPrepared, isVideoFailed, inTimestampMs, outTimestampMs, speedMultiplier) {
        if (isPlaying) {
            val stepMs = 100L
            val delayMs = ((stepMs / speedMultiplier).toLong()).coerceIn(20L, 500L)

            while (isPlaying) {
                if (isMediaPrepared && !isVideoFailed && mediaPlayer != null) {
                    try {
                        val currentPos = mediaPlayer!!.currentPosition.toLong()
                        onPositionChanged(currentPos)

                        // Loop back to inTimestamp if reaching clip boundary
                        if (currentPos >= outTimestampMs || currentPos < inTimestampMs) {
                            mediaPlayer!!.seekTo(inTimestampMs.toInt())
                            onPositionChanged(inTimestampMs)
                        }
                    } catch (e: Exception) {
                        // Fall back to software timer tracking
                        val next = (currentPositionMs + (stepMs * speedMultiplier).toLong())
                        if (next >= outTimestampMs) {
                            onPositionChanged(inTimestampMs)
                        } else {
                            onPositionChanged(next)
                        }
                    }
                } else {
                    // Smooth, simulated playback progression for preview & trimming
                    val advance = (stepMs * speedMultiplier).toLong()
                    val next = currentPositionMs + advance
                    if (next >= outTimestampMs || next < inTimestampMs) {
                        onPositionChanged(inTimestampMs)
                    } else {
                        onPositionChanged(next)
                    }
                }
                delay(delayMs)
            }
        }
    }

    // Subtle scanline animation during active playback in simulation mode
    val infiniteTransition = rememberInfiniteTransition(label = "playbackScan")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanProgress"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF090A0E))
            .border(1.dp, Color(0xFF262B3A), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = maxWidth
        val targetRatio = aspectRatioMode.ratio

        // Outer container maintaining chosen aspect ratio (e.g. 9:16 Portrait for TikTok/Reels)
        Box(
            modifier = Modifier
                .width(containerWidth)
                .aspectRatio(targetRatio)
                .clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Blurred background option for 9:16 portrait export (classic viral clip style)
            if (aspectRatioMode == AspectRatioMode.PORTRAIT_9_16 && blurredBackgroundEnabled) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(24.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0B0D13))
                )
            }

            // Central Video Display Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                // High-resolution video frame backdrop
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = "Video preview frame",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Android SurfaceView for hardware video playback when stream is available
                if (!isVideoFailed) {
                    AndroidView(
                        factory = { ctx ->
                            val frameLayout = FrameLayout(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }

                            val surfaceView = SurfaceView(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                                )
                            }

                            surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
                                override fun surfaceCreated(holder: SurfaceHolder) {
                                    try {
                                        val player = MediaPlayer().apply {
                                            setDisplay(holder)

                                            // CRITICAL: Consume errors gracefully to prevent native crashes/aborts
                                            setOnErrorListener { _, what, extra ->
                                                Log.w(TAG, "MediaPlayer native code ($what, $extra) handled gracefully.")
                                                isMediaPrepared = false
                                                isVideoFailed = true
                                                true // Returns true indicating the error is consumed
                                            }

                                            setOnInfoListener { _, _, _ -> true }

                                            setDataSource(ctx, Uri.parse(videoStreamUrl))
                                            isLooping = false

                                            setOnPreparedListener { mp ->
                                                try {
                                                    isMediaPrepared = true
                                                    isVideoFailed = false
                                                    mp.seekTo(inTimestampMs.toInt())
                                                    if (isPlaying) mp.start()
                                                } catch (e: Exception) {
                                                    Log.w(TAG, "Prepared listener warning: ${e.message}")
                                                }
                                            }
                                            prepareAsync()
                                        }
                                        mediaPlayer = player
                                    } catch (e: Exception) {
                                        Log.w(TAG, "Surface creation initialization fallback: ${e.message}")
                                        isVideoFailed = true
                                    }
                                }

                                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

                                override fun surfaceDestroyed(holder: SurfaceHolder) {
                                    try {
                                        mediaPlayer?.apply {
                                            if (isPlaying) stop()
                                            reset()
                                            release()
                                        }
                                    } catch (e: Exception) {
                                        Log.w(TAG, "Surface destroy release cleanup: ${e.message}")
                                    } finally {
                                        mediaPlayer = null
                                        isMediaPrepared = false
                                    }
                                }
                            })

                            frameLayout.addView(surfaceView)
                            frameLayout
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Cinematic subtle scanline when playing
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = (scanProgress * 180).dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        CrimsonRedPrimary.copy(alpha = 0.6f),
                                        NeonCyan.copy(alpha = 0.8f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }

            // Clickable layer to toggle play/pause
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onTogglePlayPause
                    )
            )

            // Live Caption / Subtitle Overlay
            if (overlayText.isNotBlank()) {
                val alignment = when (captionPosition) {
                    CaptionPosition.TOP -> Alignment.TopCenter
                    CaptionPosition.CENTER -> Alignment.Center
                    CaptionPosition.BOTTOM -> Alignment.BottomCenter
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    contentAlignment = alignment
                ) {
                    RenderStyledCaption(
                        text = overlayText,
                        style = captionStyle,
                        fontSize = captionSize
                    )
                }
            }

            // Transition effect badge / indicator
            if (transitionEffect != TransitionEffect.NONE) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(NeonCyan, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = transitionEffect.label,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Speed multiplier badge
            if (speedMultiplier != 1.0f) {
                Surface(
                    color = CrimsonRedPrimary.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${speedMultiplier}x",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Big Play/Pause Button Overlay (fades out when playing)
            AnimatedVisibility(
                visible = !isPlaying,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(2.dp, CrimsonRedPrimary),
                    modifier = Modifier
                        .size(56.dp)
                        .clickable(onClick = onTogglePlayPause)
                        .testTag("play_pause_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.apply {
                    if (isPlaying) stop()
                    reset()
                    release()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cleanup exception: ${e.message}")
            } finally {
                mediaPlayer = null
                isMediaPrepared = false
            }
        }
    }
}

@Composable
fun RenderStyledCaption(
    text: String,
    style: CaptionStyle,
    fontSize: Float
) {
    when (style) {
        CaptionStyle.VIRAL_YELLOW -> {
            // Bold TikTok / YouTube Shorts style: Bright Yellow with heavy drop shadow
            Text(
                text = text.uppercase(),
                style = TextStyle(
                    color = ViralYellow,
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    shadow = Shadow(
                        color = Color.Black,
                        blurRadius = 12f
                    )
                ),
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        CaptionStyle.MINIMAL_WHITE -> {
            Text(
                text = text,
                style = TextStyle(
                    color = Color.White,
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.8f),
                        blurRadius = 8f
                    )
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        CaptionStyle.NEON_GLOW -> {
            Text(
                text = text,
                style = TextStyle(
                    color = Color.White,
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    shadow = Shadow(
                        color = CrimsonRedPrimary,
                        blurRadius = 20f
                    )
                ),
                modifier = Modifier
                    .border(1.5.dp, CrimsonRedPrimary, RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
        CaptionStyle.SUBTITLE_BOX -> {
            Text(
                text = text,
                style = TextStyle(
                    color = Color.White,
                    fontSize = (fontSize - 2).sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .background(Color(0xDD000000), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}
