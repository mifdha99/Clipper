package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "VideoMediaAnalyzer"

object VideoMediaAnalyzer {

    /**
     * Extracts sampled keyframe Bitmaps at evenly-spaced intervals across the video
     * to provide genuine visual context for Gemini 4 Argon analysis.
     */
    suspend fun sampleVideoKeyframes(
        context: Context,
        videoUriString: String,
        sampleCount: Int = 4,
        maxDurationMs: Long = 60_000L
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        val retriever = MediaMetadataRetriever()
        try {
            val uri = Uri.parse(videoUriString)
            if (uri.scheme == "content" || uri.scheme == "file") {
                retriever.setDataSource(context, uri)
            } else {
                retriever.setDataSource(videoUriString, HashMap())
            }

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: maxDurationMs
            val effectiveDurationMs = durationMs.coerceAtMost(maxDurationMs).coerceAtLeast(2000L)

            val stepUs = (effectiveDurationMs * 1000L) / (sampleCount + 1)
            for (i in 1..sampleCount) {
                val targetUs = stepUs * i
                val frame = retriever.getFrameAtTime(targetUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (frame != null) {
                    // Scale down for AI tokens and network efficiency
                    val scaled = Bitmap.createScaledBitmap(frame, 480, (480f * frame.height / frame.width).toInt().coerceAtLeast(320), true)
                    bitmaps.add(scaled)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed extracting video keyframes: ${e.message}")
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.w(TAG, "Retriever release error", e)
            }
        }
        bitmaps
    }
}
