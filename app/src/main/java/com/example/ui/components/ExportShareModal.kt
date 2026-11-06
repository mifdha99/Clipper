package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.VideoClipEntity
import com.example.ui.theme.CrimsonRedPrimary
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ReelsPurple
import com.example.ui.theme.TikTokBlack
import com.example.ui.theme.ViralYellow
import com.example.ui.theme.YouTubeRed
import com.example.ui.viewmodel.ExportUiState

@Composable
fun ExportShareModal(
    state: ExportUiState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (state is ExportUiState.Idle) return

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF141824),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B344D)),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (state is ExportUiState.Success) "Ekspor Selesai! 🎉" else "Mengekspor Klip Video",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (state) {
                    is ExportUiState.Exporting -> {
                        // Processing state
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFF1B2133), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { state.progress },
                                color = CrimsonRedPrimary,
                                modifier = Modifier.size(52.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "${(state.progress * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = state.currentTask,
                            color = NeonCyan,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        LinearProgressIndicator(
                            progress = { state.progress },
                            color = CrimsonRedPrimary,
                            trackColor = Color(0xFF1F263B),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }

                    is ExportUiState.Success -> {
                        val clip = state.clip

                        // Celebration card
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFF0F382E), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4ADE80),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = clip.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Surface(
                            color = Color(0xFF1F263B),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Format: ${clip.aspectRatio} | Durasi: ${(clip.durationMs / 1000f)} detik",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "BAGIKAN LANGSUNG KE MEDIA SOSIAL:",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Social Platform Quick Share Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            SocialShareButton(
                                name = "TikTok",
                                badgeColor = TikTokBlack,
                                textColor = Color.White,
                                onClick = {
                                    shareToApp(context, "com.zhiliaoapp.musically", clip)
                                }
                            )

                            SocialShareButton(
                                name = "Reels",
                                badgeColor = ReelsPurple,
                                textColor = Color.White,
                                onClick = {
                                    shareToApp(context, "com.instagram.android", clip)
                                }
                            )

                            SocialShareButton(
                                name = "Shorts",
                                badgeColor = YouTubeRed,
                                textColor = Color.White,
                                onClick = {
                                    shareToApp(context, "com.google.android.youtube", clip)
                                }
                            )

                            SocialShareButton(
                                name = "WhatsApp",
                                badgeColor = Color(0xFF25D366),
                                textColor = Color.White,
                                onClick = {
                                    shareToApp(context, "com.whatsapp", clip)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // General System Share Button
                        Button(
                            onClick = {
                                shareGeneral(context, clip)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrimsonRedPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("share_clip_button")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bagikan Lewat Menu Lainnya", fontWeight = FontWeight.Bold)
                        }
                    }

                    is ExportUiState.Error -> {
                        Text(
                            text = state.message,
                            color = Color(0xFFF87171),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    is ExportUiState.Idle -> {}
                }
            }
        }
    }
}

@Composable
fun SocialShareButton(
    name: String,
    badgeColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = badgeColor,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF3B4561)),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.IosShare,
                    contentDescription = name,
                    tint = textColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun shareToApp(context: Context, packageName: String, clip: VideoClipEntity) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, clip.title)
        putExtra(
            Intent.EXTRA_TEXT,
            "${clip.title}\n\nKlip video dibuat dengan TubeClip (${clip.aspectRatio})\n${clip.sourceVideoUrl}\n#${clip.tags.replace(",", " #")}"
        )
        setPackage(packageName)
    }

    try {
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        // Fallback to general chooser if specific app isn't installed
        shareGeneral(context, clip)
        Toast.makeText(context, "Membuka menu bagikan...", Toast.LENGTH_SHORT).show()
    }
}

private fun shareGeneral(context: Context, clip: VideoClipEntity) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, clip.title)
        putExtra(
            Intent.EXTRA_TEXT,
            "${clip.title}\n\nKlip video dibuat dengan TubeClip (${clip.aspectRatio})\nSumber: ${clip.sourceVideoUrl}\n#${clip.tags.replace(",", " #")}"
        )
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan Klip Video Ke:"))
}
