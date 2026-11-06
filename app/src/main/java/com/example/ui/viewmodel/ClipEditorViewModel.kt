package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.TranscriptionResult
import com.example.data.model.VideoAnalysisResult
import com.example.data.model.VideoClipEntity
import com.example.data.model.VideoHighlight
import com.example.data.model.YouTubeVideo
import com.example.data.repository.AiRepository
import com.example.data.repository.VideoRepository
import com.example.util.AudioRecorderHelper
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

sealed interface ExportUiState {
    data object Idle : ExportUiState
    data class Exporting(val progress: Float, val currentTask: String) : ExportUiState
    data class Success(val clip: VideoClipEntity) : ExportUiState
    data class Error(val message: String) : ExportUiState
}

class ClipEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val clipDao = AppDatabase.getInstance(application).clipDao()
    private val videoRepository = VideoRepository()
    private val aiRepository = AiRepository()
    private val audioRecorderHelper = AudioRecorderHelper(application)

    // Curated videos for discovery
    private val _curatedVideos = MutableStateFlow(videoRepository.getSampleVideos())
    val curatedVideos: StateFlow<List<YouTubeVideo>> = _curatedVideos.asStateFlow()

    // Saved clips from Room DB
    val savedClips: StateFlow<List<VideoClipEntity>> = clipDao.getAllClips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently active video in the clipper editor
    private val _currentVideo = MutableStateFlow(videoRepository.getSampleVideos().first())
    val currentVideo: StateFlow<YouTubeVideo> = _currentVideo.asStateFlow()

    // Clipper Editor State
    private val _inTimestampMs = MutableStateFlow(0L)
    val inTimestampMs: StateFlow<Long> = _inTimestampMs.asStateFlow()

    private val _outTimestampMs = MutableStateFlow(30_000L)
    val outTimestampMs: StateFlow<Long> = _outTimestampMs.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // Aspect ratio & framing
    private val _selectedAspectRatio = MutableStateFlow(AspectRatioMode.PORTRAIT_9_16)
    val selectedAspectRatio: StateFlow<AspectRatioMode> = _selectedAspectRatio.asStateFlow()

    private val _blurredBackgroundEnabled = MutableStateFlow(true)
    val blurredBackgroundEnabled: StateFlow<Boolean> = _blurredBackgroundEnabled.asStateFlow()

    // Caption & Text Overlay
    private val _overlayText = MutableStateFlow("POV: Bagian paling gila dari video ini! 😱")
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

    fun selectVideo(video: YouTubeVideo) {
        _currentVideo.value = video
        _inTimestampMs.value = 0L
        val maxDurationMs = video.durationSeconds * 1000L
        _outTimestampMs.value = minOf(30_000L, maxDurationMs)
        _currentPositionMs.value = 0L
        _isPlaying.value = false
        _aiAnalysisState.value = AiAnalysisUiState.Idle
        _transcriptionState.value = TranscriptionUiState.Idle
        _overlayText.value = "POV: Momen terbaik di ${video.title} 🔥"
    }

    fun loadCustomYouTubeUrl(url: String): Boolean {
        val parsed = videoRepository.parseYouTubeUrl(url) ?: return false
        selectVideo(parsed)
        return true
    }

    fun setInTimestamp(ms: Long) {
        val totalMs = _currentVideo.value.durationSeconds * 1000L
        val clamped = ms.coerceIn(0L, totalMs - 1000L)
        if (clamped < _outTimestampMs.value) {
            _inTimestampMs.value = clamped
            if (_currentPositionMs.value < clamped) {
                _currentPositionMs.value = clamped
            }
        }
    }

    fun setOutTimestamp(ms: Long) {
        val totalMs = _currentVideo.value.durationSeconds * 1000L
        val clamped = ms.coerceIn(1000L, totalMs)
        if (clamped > _inTimestampMs.value) {
            _outTimestampMs.value = clamped
            if (_currentPositionMs.value > clamped) {
                _currentPositionMs.value = clamped
            }
        }
    }

    fun adjustInTimestamp(deltaMs: Long) {
        setInTimestamp(_inTimestampMs.value + deltaMs)
    }

    fun adjustOutTimestamp(deltaMs: Long) {
        setOutTimestamp(_outTimestampMs.value + deltaMs)
    }

    fun setCurrentPosition(ms: Long) {
        val totalMs = _currentVideo.value.durationSeconds * 1000L
        _currentPositionMs.value = ms.coerceIn(0L, totalMs)
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

    // Apply an AI Highlight to timeline
    fun applyAiHighlight(highlight: VideoHighlight) {
        val startMs = highlight.startSeconds * 1000L
        val endMs = highlight.endSeconds * 1000L
        _inTimestampMs.value = startMs
        _outTimestampMs.value = endMs
        _currentPositionMs.value = startMs
        _overlayText.value = highlight.suggestedCaption
    }

    // 1. Feature: "Analyze video content" with Gemini 4 Argon
    fun runGemini4ArgonAnalysis() {
        val video = _currentVideo.value
        viewModelScope.launch {
            _aiAnalysisState.value = AiAnalysisUiState.Loading
            try {
                val result = aiRepository.analyzeVideoContent(
                    videoTitle = video.title,
                    channelName = video.channel,
                    videoDurationSeconds = video.durationSeconds,
                    descriptionOrContext = video.description
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

    // 2. Feature: "Transcribe audio" with Gemini 3.5 Transcribe
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
                    contextPrompt = "YouTube clipper voiceover for ${_currentVideo.value.title}"
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

    // Export & Save Clip
    fun exportCurrentClip(customTitle: String? = null) {
        viewModelScope.launch {
            val video = _currentVideo.value
            val inMs = _inTimestampMs.value
            val outMs = _outTimestampMs.value
            val durationMs = (outMs - inMs).coerceAtLeast(1000L)
            val title = customTitle ?: "Clip: ${video.title.take(30)}..."

            _exportState.value = ExportUiState.Exporting(0.1f, "Menyiapkan timeline video...")
            delay(400)
            _exportState.value = ExportUiState.Exporting(0.35f, "Memproses format portrait 9:16 untuk TikTok/Reels...")
            delay(500)
            _exportState.value = ExportUiState.Exporting(0.65f, "Menerapkan teks overlay & transisi...")
            delay(500)
            _exportState.value = ExportUiState.Exporting(0.9f, "Menyimpan ke memori perangkat...")
            delay(400)

            val newClip = VideoClipEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                sourceVideoTitle = video.title,
                sourceVideoUrl = "https://youtube.com/watch?v=${video.id}",
                sourceThumbnailUrl = video.thumbnailUrl,
                inTimestampMs = inMs,
                outTimestampMs = outMs,
                durationMs = durationMs,
                aspectRatio = _selectedAspectRatio.value.label,
                overlayText = _overlayText.value,
                overlayStyle = _captionStyle.value.label,
                transitionEffect = _selectedTransition.value.label,
                speedMultiplier = _speedMultiplier.value,
                createdAt = System.currentTimeMillis(),
                tags = "tiktok,reels,shorts,viral"
            )

            clipDao.insertClip(newClip)
            _exportState.value = ExportUiState.Success(newClip)
        }
    }

    fun resetExportState() {
        _exportState.value = ExportUiState.Idle
    }

    fun deleteSavedClip(clip: VideoClipEntity) {
        viewModelScope.launch {
            clipDao.deleteClip(clip)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorderHelper.cancelRecording()
    }
}
