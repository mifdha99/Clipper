package com.example.data.model

import android.net.Uri

data class VideoItem(
    val id: String,
    val title: String,
    val uriString: String,
    val durationMs: Long,
    val width: Int = 1080,
    val height: Int = 1920,
    val sizeBytes: Long = 0L,
    val dateModified: Long = System.currentTimeMillis(),
    val isLocal: Boolean = true,
    val sourceDescription: String = "Lokal HP"
) {
    val durationSeconds: Int
        get() = (durationMs / 1000).toInt().coerceAtLeast(1)

    val resolution: String
        get() = "${width}x${height}"
}
