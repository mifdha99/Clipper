package com.example.data.model

data class VideoHighlight(
    val title: String,
    val startSeconds: Int,
    val endSeconds: Int,
    val hookReason: String,
    val viralScore: Int,
    val suggestedCaption: String
)

data class VideoAnalysisResult(
    val summary: String,
    val topics: List<String>,
    val suggestedTitles: List<String>,
    val hashtags: List<String>,
    val highlights: List<VideoHighlight>,
    val idealAspectRatio: String,
    val targetAudienceTip: String
)

data class TranscriptionResult(
    val transcriptText: String,
    val languageDetected: String,
    val confidenceScore: Float,
    val formattedCaptions: List<String>
)
