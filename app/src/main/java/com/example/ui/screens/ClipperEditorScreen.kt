package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AiAnalysisSheet
import com.example.ui.components.ExportShareModal
import com.example.ui.components.TimelineTrimmer
import com.example.ui.components.VideoPlayerView
import com.example.ui.components.VoiceTranscribeSheet
import com.example.ui.theme.CrimsonRedPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ViralYellow
import com.example.ui.viewmodel.AspectRatioMode
import com.example.ui.viewmodel.CaptionPosition
import com.example.ui.viewmodel.CaptionStyle
import com.example.ui.viewmodel.ClipEditorViewModel
import com.example.ui.viewmodel.TransitionEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipperEditorScreen(
    viewModel: ClipEditorViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentVideo by viewModel.currentVideo.collectAsState()
    val inTimestampMs by viewModel.inTimestampMs.collectAsState()
    val outTimestampMs by viewModel.outTimestampMs.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val selectedPreset by viewModel.selectedPreset.collectAsState()

    val selectedAspectRatio by viewModel.selectedAspectRatio.collectAsState()
    val blurredBgEnabled by viewModel.blurredBackgroundEnabled.collectAsState()
    val overlayText by viewModel.overlayText.collectAsState()
    val captionStyle by viewModel.captionStyle.collectAsState()
    val captionPosition by viewModel.captionPosition.collectAsState()
    val captionSize by viewModel.captionSize.collectAsState()

    val selectedTransition by viewModel.selectedTransition.collectAsState()
    val speedMultiplier by viewModel.speedMultiplier.collectAsState()

    val aiAnalysisState by viewModel.aiAnalysisState.collectAsState()
    val transcriptionState by viewModel.transcriptionState.collectAsState()
    val micAmplitude by viewModel.micAmplitude.collectAsState()
    val exportState by viewModel.exportState.collectAsState()

    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("AI Highlights", "Mic Subtitle", "Teks & Caption", "Format & Transisi")

    val scrollState = rememberScrollState()

    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.importVideoFromUri(
                context = context,
                uri = uri,
                onSuccess = {
                    Toast.makeText(context, "Video baru berhasil dimuat!", Toast.LENGTH_SHORT).show()
                },
                onError = { err ->
                    Toast.makeText(context, "Gagal memuat video: $err", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentVideo?.title ?: "Pilih Video",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${currentVideo?.sourceDescription ?: "Lokal"} • ${currentVideo?.resolution ?: ""}",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            pickVideoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Ganti Video",
                            tint = NeonCyan
                        )
                    }

                    Button(
                        onClick = { viewModel.exportCurrentClip() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrimsonRedPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .testTag("export_clip_button")
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ekspor (9:16)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (currentVideo == null) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Belum Ada Video yang Dipilih",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            pickVideoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonRedPrimary)
                    ) {
                        Text("Pilih Video dari HP")
                    }
                }
            }
        } else {
            val video = currentVideo!!
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Interactive Video Canvas with overlay text & transitions
                VideoPlayerView(
                    videoStreamUrl = video.uriString,
                    thumbnailUrl = video.uriString,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    inTimestampMs = inTimestampMs,
                    outTimestampMs = outTimestampMs,
                    aspectRatioMode = selectedAspectRatio,
                    blurredBackgroundEnabled = blurredBgEnabled,
                    overlayText = overlayText,
                    captionStyle = captionStyle,
                    captionPosition = captionPosition,
                    captionSize = captionSize,
                    transitionEffect = selectedTransition,
                    speedMultiplier = speedMultiplier,
                    onPositionChanged = { viewModel.setCurrentPosition(it) },
                    onTogglePlayPause = { viewModel.togglePlayPause() }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Precision Timeline Trimmer with 15s/30s/45s/60s/Custom presets
                TimelineTrimmer(
                    totalDurationSeconds = video.durationSeconds,
                    inTimestampMs = inTimestampMs,
                    outTimestampMs = outTimestampMs,
                    currentPositionMs = currentPositionMs,
                    selectedPreset = selectedPreset,
                    onPresetSelected = { viewModel.applyPreset(it) },
                    onInChanged = { viewModel.setInTimestamp(it) },
                    onOutChanged = { viewModel.setOutTimestamp(it) },
                    onSeekPosition = { viewModel.setCurrentPosition(it) },
                    onAdjustIn = { viewModel.adjustInTimestamp(it) },
                    onAdjustOut = { viewModel.adjustOutTimestamp(it) },
                    onPreviewClip = { viewModel.previewClip() },
                    onSetInFromPlayhead = { viewModel.setInFromPlayhead() },
                    onSetOutFromPlayhead = { viewModel.setOutFromPlayhead() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Navigation for Tools
                ScrollableTabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color(0xFF141824),
                    contentColor = Color.White,
                    edgePadding = 8.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                            color = CrimsonRedPrimary
                        )
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .fillMaxWidth()
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = activeTab == index,
                            onClick = { activeTab = index },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    when (index) {
                                        0 -> Icon(Icons.Default.AutoAwesome, null, tint = if (activeTab == index) CrimsonRedPrimary else Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                                        1 -> Icon(Icons.Default.Mic, null, tint = if (activeTab == index) NeonCyan else Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                                        2 -> Icon(Icons.Default.TextFields, null, tint = if (activeTab == index) ViralYellow else Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                                        3 -> Icon(Icons.Default.AspectRatio, null, tint = if (activeTab == index) CrimsonRedPrimary else Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = title,
                                        fontSize = 12.sp,
                                        fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Active Tab Content
                when (activeTab) {
                    0 -> {
                        // Real Gemini 4 Argon Video Analysis with Sampled Keyframes
                        AiAnalysisSheet(
                            state = aiAnalysisState,
                            videoTitle = video.title,
                            onRunAnalysis = { viewModel.runGemini4ArgonAnalysis() },
                            onApplyHighlight = { viewModel.applyAiHighlight(it) }
                        )
                    }
                    1 -> {
                        // Speech Transcription with Gemini 3.5 Transcribe
                        VoiceTranscribeSheet(
                            state = transcriptionState,
                            micAmplitude = micAmplitude,
                            onStartRecording = { viewModel.startMicrophoneRecording() },
                            onStopAndTranscribe = { viewModel.stopMicrophoneAndTranscribe() },
                            onCancelRecording = { viewModel.cancelMicrophoneRecording() },
                            onApplyCaption = { viewModel.applyTranscribedTextAsCaption(it) }
                        )
                    }
                    2 -> {
                        // Captions & Text Editing
                        CaptionEditorControls(
                            overlayText = overlayText,
                            onOverlayTextChanged = { viewModel.setOverlayText(it) },
                            captionStyle = captionStyle,
                            onStyleChanged = { viewModel.setCaptionStyle(it) },
                            captionPosition = captionPosition,
                            onPositionChanged = { viewModel.setCaptionPosition(it) },
                            captionSize = captionSize,
                            onSizeChanged = { viewModel.setCaptionSize(it) }
                        )
                    }
                    3 -> {
                        // Format (Portrait 9:16 for TikTok/Reels) & Transitions
                        FormatAndEffectsControls(
                            selectedAspectRatio = selectedAspectRatio,
                            onAspectRatioChanged = { viewModel.setAspectRatio(it) },
                            blurredBackgroundEnabled = blurredBgEnabled,
                            onToggleBlurredBackground = { viewModel.toggleBlurredBackground() },
                            selectedTransition = selectedTransition,
                            onTransitionChanged = { viewModel.setTransition(it) },
                            speedMultiplier = speedMultiplier,
                            onSpeedChanged = { viewModel.setSpeed(it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Export Modal with real file statistics and 4 actions
        ExportShareModal(
            state = exportState,
            onDismiss = { viewModel.resetExportState() }
        )
    }
}

@Composable
fun CaptionEditorControls(
    overlayText: String,
    onOverlayTextChanged: (String) -> Unit,
    captionStyle: CaptionStyle,
    onStyleChanged: (CaptionStyle) -> Unit,
    captionPosition: CaptionPosition,
    onPositionChanged: (CaptionPosition) -> Unit,
    captionSize: Float,
    onSizeChanged: (Float) -> Unit
) {
    Surface(
        color = Color(0xFF131722),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262D42)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Teks Overlay & Gaya Subtitle",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = overlayText,
                onValueChange = onOverlayTextChanged,
                placeholder = { Text("Tulis hook atau judul klip...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CrimsonRedPrimary,
                    unfocusedBorderColor = Color(0xFF2E3850),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("caption_input_field")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Caption Styles
            Text(text = "Gaya Teks Viral:", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CaptionStyle.entries.forEach { style ->
                    FilterChip(
                        selected = captionStyle == style,
                        onClick = { onStyleChanged(style) },
                        label = { Text(style.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CrimsonRedPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1B2030),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Caption Position
            Text(text = "Posisi Teks:", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CaptionPosition.entries.forEach { pos ->
                    FilterChip(
                        selected = captionPosition == pos,
                        onClick = { onPositionChanged(pos) },
                        label = { Text(pos.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan,
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF1B2030),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Size Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Ukuran Teks:", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(text = "${captionSize.toInt()} sp", color = ViralYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = captionSize,
                onValueChange = onSizeChanged,
                valueRange = 14f..36f,
                colors = SliderDefaults.colors(
                    thumbColor = ViralYellow,
                    activeTrackColor = ViralYellow
                )
            )
        }
    }
}

@Composable
fun FormatAndEffectsControls(
    selectedAspectRatio: AspectRatioMode,
    onAspectRatioChanged: (AspectRatioMode) -> Unit,
    blurredBackgroundEnabled: Boolean,
    onToggleBlurredBackground: () -> Unit,
    selectedTransition: TransitionEffect,
    onTransitionChanged: (TransitionEffect) -> Unit,
    speedMultiplier: Float,
    onSpeedChanged: (Float) -> Unit
) {
    Surface(
        color = Color(0xFF131722),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262D42)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Aspect Ratio selection
            Text(
                text = "Format Rasio Video (TikTok / Reels / Shorts):",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AspectRatioMode.entries.forEach { mode ->
                    val isSelected = selectedAspectRatio == mode
                    Surface(
                        color = if (isSelected) CrimsonRedPrimary.copy(alpha = 0.2f) else Color(0xFF1B2030),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (isSelected) CrimsonRedPrimary else Color(0xFF2E3850)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onAspectRatioChanged(mode) }
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = mode.label,
                                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = mode.platformBadge,
                                color = if (isSelected) CrimsonRedPrimary else Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Blurred Background Toggle for 9:16
            if (selectedAspectRatio == AspectRatioMode.PORTRAIT_9_16) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F121B))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Latar Belakang Blur (Estetika Viral)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Mengisi ruang kosong 9:16 dengan efek blur video",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = blurredBackgroundEnabled,
                        onCheckedChange = { onToggleBlurredBackground() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CrimsonRedPrimary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Auto-Transitions
            Text(
                text = "Transisi Otomatis:",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TransitionEffect.entries.take(3).forEach { trans ->
                    FilterChip(
                        selected = selectedTransition == trans,
                        onClick = { onTransitionChanged(trans) },
                        label = { Text(trans.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan,
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF1B2030),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TransitionEffect.entries.drop(3).forEach { trans ->
                    FilterChip(
                        selected = selectedTransition == trans,
                        onClick = { onTransitionChanged(trans) },
                        label = { Text(trans.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan,
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF1B2030),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Speed multiplier
            Text(
                text = "Kecepatan Putar Video:",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                    FilterChip(
                        selected = speedMultiplier == speed,
                        onClick = { onSpeedChanged(speed) },
                        label = { Text("${speed}x", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ViralYellow,
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF1B2030),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }
        }
    }
}
