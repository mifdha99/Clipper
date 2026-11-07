package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.VideoClipEntity
import com.example.ui.theme.CrimsonRedPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ViralYellow
import com.example.ui.viewmodel.ClipEditorViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: ClipEditorViewModel,
    modifier: Modifier = Modifier
) {
    val savedClips by viewModel.savedClips.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Koleksi Klip Saya (${savedClips.size})",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (savedClips.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFF1E2333), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = CrimsonRedPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Belum Ada Klip Tersimpan",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Pilih video dari galeri HP, potong segmen viral, dan ekspor ke galeri HP (Movies/Clipper).",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(savedClips, key = { it.id }) { clip ->
                    SavedClipCard(
                        clip = clip,
                        onPlay = { playClip(context, clip) },
                        onShare = { shareClip(context, clip) },
                        onDelete = { viewModel.deleteSavedClip(clip) }
                    )
                }
            }
        }
    }
}

@Composable
fun SavedClipCard(
    clip: VideoClipEntity,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val fileExists = clip.outputFilePath.isNotBlank() && File(clip.outputFilePath).exists()

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF141824)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF262E44), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail with aspect ratio tag
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F121B))
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = clip.outputContentUri.ifBlank { clip.sourceThumbnailUrl },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.65f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Putar",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text = "${clip.durationMs / 1000}s",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = clip.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${clip.resolution} • ${clip.aspectRatio}",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (clip.fileSizeBytes > 0L) {
                    val mb = clip.fileSizeBytes / (1024f * 1024f)
                    Text(
                        text = "Ukuran: ${"%.1f".format(mb)} MB",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (fileExists) Color(0xFF0F382E) else Color(0xFF38151B),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (fileExists) "Movies/Clipper" else "File Asli Dihapus",
                            color = if (fileExists) Color(0xFF4ADE80) else Color(0xFFF87171),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(clip.createdAt)),
                        color = Color(0xFF64748B),
                        fontSize = 10.sp
                    )
                }
            }

            // Actions
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Bagikan",
                    tint = NeonCyan
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus",
                    tint = Color(0xFFEF4444)
                )
            }
        }
    }
}

private fun playClip(context: Context, clip: VideoClipEntity) {
    try {
        val targetUri = if (clip.outputContentUri.isNotBlank()) {
            Uri.parse(clip.outputContentUri)
        } else if (clip.outputFilePath.isNotBlank()) {
            Uri.fromFile(File(clip.outputFilePath))
        } else {
            Uri.parse(clip.sourceVideoUrl)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(targetUri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Tidak ada aplikasi video player: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun shareClip(context: Context, clip: VideoClipEntity) {
    try {
        val targetUri = if (clip.outputContentUri.isNotBlank()) {
            Uri.parse(clip.outputContentUri)
        } else if (clip.outputFilePath.isNotBlank()) {
            Uri.fromFile(File(clip.outputFilePath))
        } else null

        val intent = if (targetUri != null) {
            Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, targetUri)
                putExtra(Intent.EXTRA_SUBJECT, clip.title)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "${clip.title}\n\nKlip video dibuat dengan Clipper AI (${clip.aspectRatio}) #Clipper #Viral #Reels #TikTok"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, clip.title)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "${clip.title}\n\nKlip video dibuat dengan Clipper AI (${clip.aspectRatio})"
                )
            }
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Klip Video Ke:"))
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal membagikan klip: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
