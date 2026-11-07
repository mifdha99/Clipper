package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.TranscriptionResult
import com.example.data.model.VideoAnalysisResult
import com.example.data.model.VideoClipEntity
import com.example.data.model.VideoHighlight
import com.example.data.model.VideoItem
import com.example.data.repository.AiRepository
import com.example.data.repository.VideoRepository
import com.example.data.repository.YouTubeParseResult
import com.example.ui.components.ClipDurationPreset
import com.example.util.AudioRecorderHelper
import com.example.util.VideoExportEngine
import com.example.util.VideoMediaAnalyzer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

private const val TAG = "ClipEditorViewModel"

enum class AspectRatioMode(val label: String, val ratio: Float, val platformBadge: String) {
    PORTRAIT_9_16("9:16 Portrait", 9f / 16f, "TikTok & Reels"),
    SQUARE_1_1("1:1 Square", 1f, "Instagram Feed"),
    LANDSCAPE_16_9("16:9 Landscape", 16f / 9f, "YouTube Full")
}

enum class CaptionStyle(val label: String) {
    VIRAL_YELLOW("Viral Yellow"),
    MINIMAL_WHITE("Minimal White"),
    NEON_GLOW("Neon Red Glow"),
    SUBTITLE_BOX("Subtitle Box")
}

enum class CaptionPosition(val label: String) {
    TOP("Top Header"),
    CENTER("Center Viral"),
    BOTTOM("Bottom Subtitle")
}

enum class TransitionEffect(val label: String) {
    NONE("No Transition"),
    FADE("Fade In / Out"),
    ZOOM_PUNCH("Zoom Punch"),
    GLITCH("Glitch Flash"),
    SLIDE("Dynamic Slide")
}

sealed interface AiAnalysisUiState {
    data object Idle : AiAnalysisUiState
    data object Loading : AiAnalysisUiState
    data class Success(val result: VideoAnalysisResult) : AiAnalysisUiState
    data class Error(val message: String) : AiAnalysisUiState
}

sealed interface TranscriptionUiState {
    data object Idle : TranscriptionUiState
    data object Recording : TranscriptionUiState
    data object Transcribing : TranscriptionUiState
    data class Success(val result: TranscriptionResult) : TranscriptionUiState
    data class Error(val message: String) : TranscriptionUiState
}

data class ExportSuccessInfo(
    val fileName: String,
    val durationMs: Long,
    val resolution: String,
    val fileSizeBytes: Long,
    val filePath: String,
    val contentUri: String
)

sealed interface ExportUiState {
    data object Idle : ExportUiState
    data class Exporting(val progress: Float, val currentTask: String) : ExportUiState
    data class Success(val info: ExportSuccessInfo) : ExportUiState
    data class Error(val message: String) : ExportUiState
}

class ClipEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val clipDao = AppDatabase.getInstance(application).clipDao()
    private val videoRepository = VideoRepository()
    private val aiRepository = AiRepository()
    private val audioRecorderHelper = AudioRecorderHelper(application)

    // Saved clips from Room DB
    val savedClips: StateFlow<List<VideoClipEntity>> = clipDao.getAllClips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Device local videos
    private val _deviceVideos = MutableStateFlow<List<VideoItem>>(emptyList())
    val deviceVideos: StateFlow<List<VideoItem>> = _deviceVideos.asStateFlow()

    // Currently active video in editor
    private val _currentVideo = MutableStateFlow<VideoItem?>(null)
    val currentVideo: StateFlow<VideoItem?> = _currentVideo.asStateFlow()

    // Clipper Editor State
    private val _inTimestampMs = MutableStateFlow(0L)
    val inTimestampMs: StateFlow<Long> = _inTimestampMs.asStateFlow()

    private val _outTimestampMs = MutableStateFlow(30_000L)
    val outTimestampMs: StateFlow<Long> = _outTimestampMs.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // Duration preset
    private val _selectedPreset = MutableStateFlow(ClipDurationPreset.SEC_30)
    val selectedPreset: StateFlow<ClipDurationPreset> = _selectedPreset.asStateFlow()

    // Aspect ratio & framing
    private val _selectedAspectRatio = MutableStateFlow(AspectRatioMode.PORTRAIT_9_16)
    val selectedAspectRatio: StateFlow<AspectRatioMode> = _selectedAspectRatio.asStateFlow()

    private val _blurredBackgroundEnabled = MutableStateFlow(true)
    val blurredBackgroundEnabled: StateFlow<Boolean> = _blurredBackgroundEnabled.asStateFlow()

    // Caption & Text Overlay
    private val _overlayText = MutableStateFlow("POV: Bagian paling viral dari video ini! 😱")
    val overlayText: StateFlow<String> = _overlayText.asStateFlow()

    private val _captionStyle = MutableStateFlow(CaptionStyle.VIRAL_YELLOW)
    val captionStyle: StateFlow<CaptionStyle> = _captionStyle.asStateFlow()

    private val _captionPosition = MutableStateFlow(CaptionPosition.CENTER)
    val captionPosition: StateFlow<CaptionPosition> = _captionPosition.asStateFlow()

    private val _captionSize = MutableStateFlow(22f)
    val captionSize: StateFlow<Float> = _captionSize.asStateFlow()

    // Transitions & Speed
    private val _selectedTransition = MutableStateFlow(TransitionEffect.ZOOM_PUNCH)
    val selectedTransition: StateFlow<TransitionEffect> = _selectedTransition.asStateFlow()

    private val _speedMultiplier = MutableStateFlow(1.0f)
    val speedMultiplier: StateFlow<Float> = _speedMultiplier.asStateFlow()

    // AI States
    private val _aiAnalysisState = MutableStateFlow<AiAnalysisUiState>(AiAnalysisUiState.Idle)
    val aiAnalysisState: StateFlow<AiAnalysisUiState> = _aiAnalysisState.asStateFlow()

    private val _transcriptionState = MutableStateFlow<TranscriptionUiState>(TranscriptionUiState.Idle)
    val transcriptionState: StateFlow<TranscriptionUiState> = _transcriptionState.asStateFlow()

    private val _micAmplitude = MutableStateFlow(0)
    val micAmplitude: StateFlow<Int> = _micAmplitude.asStateFlow()

    // Export State
    private val _exportState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val exportState: StateFlow<ExportUiState> = _exportState.asStateFlow()

    private var currentRecordedAudioFile: File? = null
    private var amplitudePollJob: Job? = null

    init {
        loadDeviceVideos()
    }

    fun loadDeviceVideos() {
        viewModelScope.launch {
            val list = videoRepository.queryDeviceVideos(getApplication())
            _deviceVideos.value = list
            if (_currentVideo.value == null && list.isNotEmpty()) {
                selectVideo(list.first())
            }
        }
    }

    fun importVideoFromUri(context: Context, uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val item = videoRepository.createVideoItemFromUri(context, uri)
                selectVideo(item)
                // Add to head of deviceVideos if not present
                val current = _deviceVideos.value.toMutableList()
                if (current.none { it.uriString == item.uriString }) {
                    current.add(0, item)
                    _deviceVideos.value = current
                }
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import video from uri: $uri", e)
                onError(e.localizedMessage ?: "Gagal mengimpor file video.")
            }
        }
    }

    fun selectVideo(video: VideoItem) {
        _currentVideo.value = video
        _inTimestampMs.value = 0L
        val maxDurationMs = video.durationMs
        _outTimestampMs.value = minOf(30_000L, maxDurationMs)
        _currentPositionMs.value = 0L
        _isPlaying.value = false
        _selectedPreset.value = ClipDurationPreset.SEC_30
        _aiAnalysisState.value = AiAnalysisUiState.Idle
        _transcriptionState.value = TranscriptionUiState.Idle
        _overlayText.value = "POV: Momen terbaik di ${video.title.take(25)} 🔥"
    }

    fun applyPreset(preset: ClipDurationPreset) {
        _selectedPreset.value = preset
        if (preset == ClipDurationPreset.CUSTOM) return
        val currentIn = _inTimestampMs.value
        val totalMs = _currentVideo.value?.durationMs ?: (preset.seconds * 1000L)
        val targetDurationMs = preset.seconds * 1000L
        val newOut = (currentIn + targetDurationMs).coerceAtMost(totalMs)
        _outTimestampMs.value = newOut
    }

    fun setInTimestamp(ms: Long) {
        val totalMs = _currentVideo.value?.durationMs ?: 60_000L
        val clamped = ms.coerceIn(0L, totalMs - 1000L)
        if (clamped < _outTimestampMs.value) {
            _inTimestampMs.value = clamped
            if (_currentPositionMs.value < clamped) {
                _currentPositionMs.value = clamped
            }
            _selectedPreset.value = ClipDurationPreset.CUSTOM
        }
    }

    fun setOutTimestamp(ms: Long) {
        val totalMs = _currentVideo.value?.durationMs ?: 60_000L
        val clamped = ms.coerceIn(1000L, totalMs)
        if (clamped > _inTimestampMs.value) {
            _outTimestampMs.value = clamped
            if (_currentPositionMs.value > clamped) {
                _currentPositionMs.value = clamped
            }
            _selectedPreset.value = ClipDurationPreset.CUSTOM
        }
    }

    fun setInFromPlayhead() {
        setInTimestamp(_currentPositionMs.value)
    }

    fun setOutFromPlayhead() {
        setOutTimestamp(_currentPositionMs.value)
    }

    fun adjustInTimestamp(deltaMs: Long) {
        setInTimestamp(_inTimestampMs.value + deltaMs)
    }

    fun adjustOutTimestamp(deltaMs: Long) {
        setOutTimestamp(_outTimestampMs.value + deltaMs)
    }

    fun setCurrentPosition(ms: Long) {
        val totalMs = _currentVideo.value?.durationMs ?: 60_000L
        _currentPositionMs.value = ms.coerceIn(0L, totalMs)
    }

    fun previewClip() {
        _currentPositionMs.value = _inTimestampMs.value
        _isPlaying.value = true
    }

    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        _selectedAspectRatio.value = mode
    }

    fun toggleBlurredBackground() {
        _blurredBackgroundEnabled.value = !_blurredBackgroundEnabled.value
    }

    fun setOverlayText(text: String) {
        _overlayText.value = text
    }

    fun setCaptionStyle(style: CaptionStyle) {
        _captionStyle.value = style
    }

    fun setCaptionPosition(position: CaptionPosition) {
        _captionPosition.value = position
    }

    fun setCaptionSize(size: Float) {
        _captionSize.value = size
    }

    fun setTransition(transition: TransitionEffect) {
        _selectedTransition.value = transition
    }

    fun setSpeed(speed: Float) {
        _speedMultiplier.value = speed
    }

    fun applyAiHighlight(highlight: VideoHighlight) {
        val startMs = highlight.startSeconds * 1000L
        val endMs = highlight.endSeconds * 1000L
        _inTimestampMs.value = startMs
        _outTimestampMs.value = endMs
        _currentPositionMs.value = startMs
        _overlayText.value = highlight.suggestedCaption
        _selectedPreset.value = ClipDurationPreset.CUSTOM
    }

    // Real AI Video Analysis with sampled keyframes passed to Gemini 4 Argon
    fun runGemini4ArgonAnalysis() {
        val video = _currentVideo.value ?: return
        viewModelScope.launch {
            _aiAnalysisState.value = AiAnalysisUiState.Loading
            try {
                // Sample 4 genuine keyframes from video
                val keyframes = VideoMediaAnalyzer.sampleVideoKeyframes(
                    context = getApplication(),
                    videoUriString = video.uriString,
                    sampleCount = 4,
                    maxDurationMs = video.durationMs
                )

                val result = aiRepository.analyzeVideoContent(
                    videoTitle = video.title,
                    channelName = video.sourceDescription,
                    videoDurationSeconds = video.durationSeconds,
                    descriptionOrContext = "Video durasi ${video.durationSeconds}s, resolusi ${video.resolution}",
                    keyframes = keyframes
                )
                _aiAnalysisState.value = AiAnalysisUiState.Success(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini 4 Argon video analysis failed", e)
                _aiAnalysisState.value = AiAnalysisUiState.Error(
                    e.localizedMessage ?: "Gagal menganalisis video dengan Gemini 4 Argon."
                )
            }
        }
    }

    // Audio Transcription with Gemini 3.5 Transcribe
    fun startMicrophoneRecording() {
        val file = audioRecorderHelper.startRecording()
        if (file != null) {
            currentRecordedAudioFile = file
            _transcriptionState.value = TranscriptionUiState.Recording
            startAmplitudePolling()
        } else {
            _transcriptionState.value = TranscriptionUiState.Error("Tidak dapat memulai perekaman mikrofon.")
        }
    }

    fun stopMicrophoneAndTranscribe() {
        stopAmplitudePolling()
        val file = audioRecorderHelper.stopRecording() ?: currentRecordedAudioFile
        if (file == null || !file.exists()) {
            _transcriptionState.value = TranscriptionUiState.Error("File rekaman audio tidak ditemukan.")
            return
        }

        viewModelScope.launch {
            _transcriptionState.value = TranscriptionUiState.Transcribing
            try {
                val result = aiRepository.transcribeAudio(
                    audioFile = file,
                    contextPrompt = "Voiceover komentar video untuk ${_currentVideo.value?.title ?: "Klip"}"
                )
                _transcriptionState.value = TranscriptionUiState.Success(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "gemini-3.5-transcribe failed", e)
                _transcriptionState.value = TranscriptionUiState.Error(
                    e.localizedMessage ?: "Gagal mentranskripsi suara dengan Gemini 3.5 Transcribe."
                )
            }
        }
    }

    fun cancelMicrophoneRecording() {
        stopAmplitudePolling()
        audioRecorderHelper.cancelRecording()
        currentRecordedAudioFile = null
        _transcriptionState.value = TranscriptionUiState.Idle
    }

    fun applyTranscribedTextAsCaption(text: String) {
        _overlayText.value = text
    }

    private fun startAmplitudePolling() {
        amplitudePollJob?.cancel()
        amplitudePollJob = viewModelScope.launch {
            while (audioRecorderHelper.isRecording) {
                _micAmplitude.value = audioRecorderHelper.getMaxAmplitude()
                delay(100)
            }
            _micAmplitude.value = 0
        }
    }

    private fun stopAmplitudePolling() {
        amplitudePollJob?.cancel()
        amplitudePollJob = null
        _micAmplitude.value = 0
    }

    // REAL Hardware MP4 Trimming & MediaStore Export (Movies/Clipper)
    fun exportCurrentClip(customTitle: String? = null) {
        val video = _currentVideo.value
        if (video == null) {
            _exportState.value = ExportUiState.Error("Tidak ada video yang dipilih.")
            return
        }

        viewModelScope.launch {
            val inMs = _inTimestampMs.value
            val outMs = _outTimestampMs.value
            val durationMs = (outMs - inMs).coerceAtLeast(1000L)
            val title = customTitle ?: "Clip: ${video.title.take(24)}"

            _exportState.value = ExportUiState.Exporting(0.05f, "Menyiapkan pipeline trim...")

            try {
                val exportResult = VideoExportEngine.trimAndExportVideo(
                    context = getApplication(),
                    inputUriString = video.uriString,
                    startMs = inMs,
                    endMs = outMs,
                    baseTitle = video.title,
                    onProgress = { progress, task ->
                        _exportState.value = ExportUiState.Exporting(progress, task)
                    }
                )

                val newClip = VideoClipEntity(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    sourceVideoTitle = video.title,
                    sourceVideoUrl = video.uriString,
                    sourceThumbnailUrl = video.uriString,
                    inTimestampMs = inMs,
                    outTimestampMs = outMs,
                    durationMs = durationMs,
                    aspectRatio = _selectedAspectRatio.value.label,
                    overlayText = _overlayText.value,
                    overlayStyle = _captionStyle.value.label,
                    transitionEffect = _selectedTransition.value.label,
                    speedMultiplier = _speedMultiplier.value,
                    createdAt = System.currentTimeMillis(),
                    tags = "tiktok,reels,shorts,viral",
                    outputFilePath = exportResult.outputFilePath,
                    outputContentUri = exportResult.outputContentUri,
                    fileSizeBytes = exportResult.fileSizeBytes,
                    resolution = exportResult.resolution
                )

                clipDao.insertClip(newClip)

                val successInfo = ExportSuccessInfo(
                    fileName = exportResult.fileName,
                    durationMs = exportResult.durationMs,
                    resolution = exportResult.resolution,
                    fileSizeBytes = exportResult.fileSizeBytes,
                    filePath = exportResult.outputFilePath,
                    contentUri = exportResult.outputContentUri
                )

                _exportState.value = ExportUiState.Success(successInfo)
            } catch (e: Exception) {
                Log.e(TAG, "Video export failed", e)
                _exportState.value = ExportUiState.Error(
                    "Gagal mengekspor video: ${e.localizedMessage ?: "Format tidak didukung atau memori tidak mencukupi"}"
                )
            }
        }
    }

    fun resetExportState() {
        _exportState.value = ExportUiState.Idle
    }

    fun deleteSavedClip(clip: VideoClipEntity) {
        viewModelScope.launch {
            clipDao.deleteClip(clip)
            // Attempt deleting disk file if path exists
            if (clip.outputFilePath.isNotBlank()) {
                try {
                    val file = File(clip.outputFilePath)
                    if (file.exists()) file.delete()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not delete cached clip file", e)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorderHelper.cancelRecording()
    }
}
