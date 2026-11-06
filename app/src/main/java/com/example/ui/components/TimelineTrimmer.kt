package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrimsonRedPrimary
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ViralYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineTrimmer(
    totalDurationSeconds: Int,
    inTimestampMs: Long,
    outTimestampMs: Long,
    currentPositionMs: Long,
    onInChanged: (Long) -> Unit,
    onOutChanged: (Long) -> Unit,
    onSeekPosition: (Long) -> Unit,
    onAdjustIn: (Long) -> Unit,
    onAdjustOut: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalMs = (totalDurationSeconds * 1000L).coerceAtLeast(1000L).toFloat()
    val clipDurationSec = (outTimestampMs - inTimestampMs) / 1000f

    Surface(
        color = Color(0xFF13161F),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF242A3B)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Duration and Platform badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ContentCut,
                        contentDescription = null,
                        tint = CrimsonRedPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pemotong Klip Video",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Duration indicator with TikTok/Reels limit status
                val isOptimalForReels = clipDurationSec in 5.0f..60.0f
                Surface(
                    color = if (isOptimalForReels) Color(0xFF0F382E) else Color(0xFF38151B),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Durasi: ${"%.1f".format(clipDurationSec)}s ${if (isOptimalForReels) "✨ Optimal" else "⚠️"}",
                        color = if (isOptimalForReels) Color(0xFF4ADE80) else Color(0xFFF87171),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Time stamps display: [In] -> [Current] -> [Out]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "TITIK AWAL (IN)", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = formatTimeMs(inTimestampMs),
                        color = NeonCyan,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "PLAYHEAD", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = formatTimeMs(currentPositionMs),
                        color = ViralYellow,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "TITIK AKHIR (OUT)", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = formatTimeMs(outTimestampMs),
                        color = CrimsonRedPrimary,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Visual Filmstrip & Range Slider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0B0D13))
                    .border(1.dp, Color(0xFF1E2433), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Background visual filmstrip pattern
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .padding(horizontal = 4.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF1B2030),
                                    Color(0xFF272F47),
                                    Color(0xFF1B2030)
                                )
                            ),
                            RoundedCornerShape(6.dp)
                        )
                )

                // Range slider for [In] and [Out]
                RangeSlider(
                    value = inTimestampMs.toFloat()..outTimestampMs.toFloat(),
                    onValueChange = { range ->
                        onInChanged(range.start.toLong())
                        onOutChanged(range.endInclusive.toLong())
                    },
                    valueRange = 0f..totalMs,
                    colors = SliderDefaults.colors(
                        thumbColor = CrimsonRedPrimary,
                        activeTrackColor = CrimsonRedPrimary.copy(alpha = 0.85f),
                        inactiveTrackColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trimmer_range_slider")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Precision Fine-Tuning Controls (-1s, -0.1s, +0.1s, +1s)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // In Point Controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "In:", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    FineTuneButton(label = "-1s") { onAdjustIn(-1000L) }
                    Spacer(modifier = Modifier.width(3.dp))
                    FineTuneButton(label = "-0.1s") { onAdjustIn(-100L) }
                    Spacer(modifier = Modifier.width(3.dp))
                    FineTuneButton(label = "+0.1s") { onAdjustIn(100L) }
                    Spacer(modifier = Modifier.width(3.dp))
                    FineTuneButton(label = "+1s") { onAdjustIn(1000L) }
                }

                // Out Point Controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Out:", color = CrimsonRedPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    FineTuneButton(label = "-1s") { onAdjustOut(-1000L) }
                    Spacer(modifier = Modifier.width(3.dp))
                    FineTuneButton(label = "-0.1s") { onAdjustOut(-100L) }
                    Spacer(modifier = Modifier.width(3.dp))
                    FineTuneButton(label = "+0.1s") { onAdjustOut(100L) }
                    Spacer(modifier = Modifier.width(3.dp))
                    FineTuneButton(label = "+1s") { onAdjustOut(1000L) }
                }
            }
        }
    }
}

@Composable
private fun FineTuneButton(label: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = Color(0xFF1E2333),
            contentColor = Color(0xFFE2E8F0)
        ),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.height(26.dp)
    ) {
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

fun formatTimeMs(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val tenths = (ms % 1000) / 100
    return "%02d:%02d.%d".format(minutes, seconds, tenths)
}
