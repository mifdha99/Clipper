package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.TranscriptionResult
import com.example.data.model.VideoAnalysisResult
import com.example.data.model.VideoHighlight
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.Part
import com.google.firebase.ai.type.TextPart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private const val TAG = "AiRepository"

class AiRepository {
    // 1. Mandatory model gemini-4-argon for deep video understanding and keyframe multimodal analysis
    private val argonModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-argon"
        )
    }

    // 2. Mandatory model gemini-3.5-transcribe for audio microphone speech transcription
    private val transcribeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-transcribe"
        )
    }

    /**
     * Analyzes video content, key timestamps, viral hooks, and social metadata
     * using Gemini 4 Argon with genuine sampled keyframes whenever available.
     * Throws real exceptions on failure without fabricating mock responses.
     */
    suspend fun analyzeVideoContent(
        videoTitle: String,
        channelName: String,
        videoDurationSeconds: Int,
        descriptionOrContext: String,
        keyframes: List<Bitmap> = emptyList()
    ): VideoAnalysisResult = withContext(Dispatchers.IO) {
        val promptText = """
            You are an expert social media video editor and viral content strategist analyzing a video for clipping into portrait TikTok / Instagram Reels / YouTube Shorts.
            
            Video Title: "$videoTitle"
            Source / Context: "$channelName - $descriptionOrContext"
            Total Video Duration: $videoDurationSeconds seconds
            Sampled visual keyframe frames attached: ${keyframes.size} frames.
            
            INSTRUCTIONS:
            1. Thoroughly evaluate the visual frames and content context.
            2. Identify genuine compelling moments with timestamps STRICTLY between 0 and $videoDurationSeconds seconds.
            3. Highlight points with high viral potential (humor, surprise, emotional shift, key dialogue, climax).
            4. Do NOT fabricate timestamps beyond $videoDurationSeconds seconds.
            
            Return ONLY raw valid JSON (no markdown formatting, no code blocks):
            {
              "summary": "Compelling 2-sentence summary based on real visual and topic content",
              "topics": ["topic1", "topic2", "topic3"],
              "suggestedTitles": ["Viral Title 1", "Catchy Hook Title 2", "Punchy Title 3"],
              "hashtags": ["#viral", "#reels", "#fyp", "#shorts", "#clipper"],
              "idealAspectRatio": "9:16",
              "targetAudienceTip": "Strategic advice for viewer retention",
              "highlights": [
                {
                  "title": "Highlight 1: Catchy Opening Hook",
                  "startSeconds": 0,
                  "endSeconds": 30,
                  "hookReason": "Why this moment retains viewers in first 3 seconds",
                  "viralScore": 95,
                  "suggestedCaption": "WAIT FOR THIS! 😱"
                }
              ]
            }
        """.trimIndent()

        val parts = mutableListOf<Part>()
        for (bitmap in keyframes) {
            parts.add(ImagePart(bitmap))
        }
        parts.add(TextPart(promptText))

        val content = Content(role = "user", parts = parts)

        try {
            val response = withTimeout(60_000L) {
                argonModel.generateContent(content)
            }
            val responseText = response.text ?: throw IllegalStateException("Gemini 4 Argon mengembalikan respon kosong.")
            Log.d(TAG, "Argon video analysis response: $responseText")
            parseVideoAnalysisJson(responseText, videoDurationSeconds)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed video analysis with gemini-4-argon", e)
            throw IllegalStateException("Analisis AI dengan Gemini 4 Argon gagal: ${e.message ?: "Koneksi atau model tidak tersedia."}", e)
        }
    }

    /**
     * Transcribes recorded audio from the user's microphone or video track using gemini-3.5-transcribe.
     */
    suspend fun transcribeAudio(
        audioFile: File,
        contextPrompt: String? = null
    ): TranscriptionResult = withContext(Dispatchers.IO) {
        if (!audioFile.exists() || audioFile.length() == 0L) {
            throw IllegalArgumentException("File audio tidak ditemukan atau kosong (${audioFile.absolutePath})")
        }

        try {
            val audioBytes = audioFile.readBytes()
            Log.d(TAG, "Audio file size: ${audioBytes.size} bytes from ${audioFile.name}")

            val parts = mutableListOf<Part>()
            parts.add(InlineDataPart(audioBytes, "audio/mp4"))
            parts.add(
                TextPart(
                    """
                    You are an ultra-fast speech-to-text transcriber for social video clips.
                    Transcribe all spoken words in this audio recording cleanly and accurately.
                    Context: "${contextPrompt ?: "Video voiceover commentary"}"
                    
                    Return ONLY raw valid JSON:
                    {
                      "transcriptText": "the full transcribed text",
                      "languageDetected": "id",
                      "confidenceScore": 0.98,
                      "formattedCaptions": [
                        "First short subtitle line",
                        "Second short subtitle line"
                      ]
                    }
                    """.trimIndent()
                )
            )
            val requestContent = Content(role = "user", parts = parts)

            val response = withTimeout(60_000L) {
                transcribeModel.generateContent(requestContent)
            }

            val responseText = response.text ?: throw IllegalStateException("Gemini 3.5 Transcribe mengembalikan respon kosong.")
            Log.d(TAG, "gemini-3.5-transcribe response: $responseText")
            parseTranscriptionJson(responseText)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed audio transcription with gemini-3.5-transcribe", e)
            throw IllegalStateException("Transkripsi suara dengan Gemini 3.5 Transcribe gagal: ${e.message ?: "Model tidak merespon."}", e)
        }
    }

    private fun parseVideoAnalysisJson(rawJson: String, totalDuration: Int): VideoAnalysisResult {
        val cleanJson = cleanJsonString(rawJson)
        val json = JSONObject(cleanJson)

        val summary = json.optString("summary", "Analisis video selesai.")
        val idealAspectRatio = json.optString("idealAspectRatio", "9:16")
        val targetAudienceTip = json.optString("targetAudienceTip", "Gunakan teks tebal berwarna kontras dan transisi dinamis.")

        val topicsList = mutableListOf<String>()
        val topicsArr = json.optJSONArray("topics")
        if (topicsArr != null) {
            for (i in 0 until topicsArr.length()) {
                topicsList.add(topicsArr.getString(i))
            }
        }

        val titlesList = mutableListOf<String>()
        val titlesArr = json.optJSONArray("suggestedTitles")
        if (titlesArr != null) {
            for (i in 0 until titlesArr.length()) {
                titlesList.add(titlesArr.getString(i))
            }
        }

        val hashtagsList = mutableListOf<String>()
        val hashtagsArr = json.optJSONArray("hashtags")
        if (hashtagsArr != null) {
            for (i in 0 until hashtagsArr.length()) {
                hashtagsList.add(hashtagsArr.getString(i))
            }
        }

        val highlightsList = mutableListOf<VideoHighlight>()
        val highlightsArr = json.optJSONArray("highlights")
        if (highlightsArr != null) {
            for (i in 0 until highlightsArr.length()) {
                val hObj = highlightsArr.getJSONObject(i)
                val start = hObj.optInt("startSeconds", 0).coerceIn(0, totalDuration.coerceAtLeast(1))
                val rawEnd = hObj.optInt("endSeconds", start + 30)
                val end = if (totalDuration > 0) rawEnd.coerceIn(start + 1, totalDuration) else rawEnd
                highlightsList.add(
                    VideoHighlight(
                        title = hObj.optString("title", "Highlight ${i + 1}"),
                        startSeconds = start,
                        endSeconds = if (end > start) end else (start + 15).coerceAtMost(totalDuration),
                        hookReason = hObj.optString("hookReason", "Momen penting dalam video"),
                        viralScore = hObj.optInt("viralScore", 90),
                        suggestedCaption = hObj.optString("suggestedCaption", "Tonton sampai habis!")
                    )
                )
            }
        }

        if (highlightsList.isEmpty()) {
            throw IllegalStateException("Model tidak menemukan highlight yang valid.")
        }

        // Rank candidates using ViralMomentRanking
        val candidates = highlightsList.map {
            com.example.data.model.ViralMomentCandidate(
                startMs = it.startSeconds * 1000L,
                endMs = it.endSeconds * 1000L,
                title = it.title,
                hook = it.suggestedCaption,
                reason = it.hookReason,
                score = it.viralScore.coerceIn(0, 100)
            )
        }
        val rankedCandidates = com.example.data.model.ViralMomentRanking.rank(candidates)
        val finalHighlights = rankedCandidates.map { rc ->
            VideoHighlight(
                title = rc.title,
                startSeconds = (rc.startMs / 1000).toInt(),
                endSeconds = (rc.endMs / 1000).toInt(),
                hookReason = rc.reason,
                viralScore = rc.score,
                suggestedCaption = rc.hook
            )
        }

        return VideoAnalysisResult(
            summary = summary,
            topics = if (topicsList.isNotEmpty()) topicsList else listOf("Video", "Highlight"),
            suggestedTitles = if (titlesList.isNotEmpty()) titlesList else listOf("Momen Menarik"),
            hashtags = if (hashtagsList.isNotEmpty()) hashtagsList else listOf("#reels", "#tiktok", "#viral"),
            highlights = finalHighlights,
            idealAspectRatio = idealAspectRatio,
            targetAudienceTip = targetAudienceTip
        )
    }

    private fun parseTranscriptionJson(rawJson: String): TranscriptionResult {
        val clean = cleanJsonString(rawJson)
        val json = JSONObject(clean)
        val transcript = json.optString("transcriptText", clean)
        val lang = json.optString("languageDetected", "id")
        val conf = json.optDouble("confidenceScore", 0.98).toFloat()

        val captions = mutableListOf<String>()
        val capArr = json.optJSONArray("formattedCaptions")
        if (capArr != null) {
            for (i in 0 until capArr.length()) {
                captions.add(capArr.getString(i))
            }
        } else {
            transcript.split(". ").forEach {
                if (it.isNotBlank()) captions.add(it.trim())
            }
        }

        return TranscriptionResult(
            transcriptText = transcript,
            languageDetected = lang,
            confidenceScore = conf,
            formattedCaptions = if (captions.isNotEmpty()) captions else listOf(transcript)
        )
    }

    private fun cleanJsonString(str: String): String {
        var clean = str.trim()
        if (clean.startsWith("```json")) {
            clean = clean.substring(7)
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3)
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length - 3)
        }
        return clean.trim()
    }
}
