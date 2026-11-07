package com.example.util

import android.content.Context

/**
 * Thin adapter to the VideoExportEngine already present in this Clipper repository.
 * Call from a coroutine/ViewModel; startMs/endMs are milliseconds.
 */
object ClipExportHelper {
    suspend fun export(
        context: Context,
        sourceUri: String,
        startMs: Long,
        endMs: Long,
        title: String,
        progress: (Float, String) -> Unit = { _, _ -> }
    ): ExportResult {
        require(startMs >= 0 && endMs > startMs) { "Rentang trim tidak valid" }
        return VideoExportEngine.trimAndExportVideo(
            context = context,
            inputUriString = sourceUri,
            startMs = startMs,
            endMs = endMs,
            baseTitle = title,
            onProgress = progress
        )
    }
}
