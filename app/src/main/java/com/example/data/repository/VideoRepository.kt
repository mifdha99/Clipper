package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.regex.Pattern

private const val TAG = "VideoRepository"

class VideoRepository {

    /**
     * Query real device videos from MediaStore (Gallery / Storage).
     */
    suspend fun queryDeviceVideos(context: Context): List<VideoItem> = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<VideoItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
        val queryUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        try {
            context.contentResolver.query(
                queryUri,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Video $id"
                    val duration = cursor.getLong(durationColumn)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateModified = cursor.getLong(dateColumn)

                    val contentUri = ContentUris.withAppendedId(queryUri, id)

                    // Only add videos with valid duration
                    if (duration > 500L) {
                        videoList.add(
                            VideoItem(
                                id = id.toString(),
                                title = name,
                                uriString = contentUri.toString(),
                                durationMs = duration,
                                width = if (width > 0) width else 1080,
                                height = if (height > 0) height else 1920,
                                sizeBytes = size,
                                dateModified = dateModified,
                                isLocal = true,
                                sourceDescription = "Galeri HP"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed querying device videos", e)
        }

        videoList
    }

    /**
     * Inspects a newly selected URI (from Photo Picker / SAF) and extracts real metadata.
     */
    suspend fun createVideoItemFromUri(context: Context, uri: Uri): VideoItem = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var durationMs = 0L
        var width = 1080
        var height = 1920
        var displayName = "Imported_Video_${System.currentTimeMillis()}"
        var sizeBytes = 0L

        try {
            retriever.setDataSource(context, uri)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationMs = durStr?.toLongOrNull() ?: 0L

            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            width = widthStr?.toIntOrNull() ?: 1080
            height = heightStr?.toIntOrNull() ?: 1920
        } catch (e: Exception) {
            Log.w(TAG, "Metadata extraction partial fail", e)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // ignore
            }
        }

        // Try getting display name and file size from content resolver
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) displayName = cursor.getString(nameIndex) ?: displayName
                    if (sizeIndex != -1) sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Content resolver query error", e)
        }

        if (durationMs <= 0L) {
            throw IllegalArgumentException("Tidak dapat membaca durasi video dari file yang dipilih. Pastikan file adalah video yang valid.")
        }

        VideoItem(
            id = uri.toString().hashCode().toString(),
            title = displayName,
            uriString = uri.toString(),
            durationMs = durationMs,
            width = width,
            height = height,
            sizeBytes = sizeBytes,
            isLocal = true,
            sourceDescription = "Impor Galeri HP"
        )
    }

    /**
     * Parse YouTube link info without claiming illegal video downloading.
     */
    fun parseYouTubeUrl(url: String): YouTubeParseResult {
        val trimmed = url.trim()
        val videoId = extractYouTubeId(trimmed)
            ?: return YouTubeParseResult.Invalid("URL YouTube tidak valid. Harap gunakan format seperti https://www.youtube.com/watch?v=... atau https://youtu.be/...")

        return YouTubeParseResult.Valid(
            videoId = videoId,
            webUrl = "https://www.youtube.com/watch?v=$videoId",
            thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
            title = "YouTube Video ($videoId)"
        )
    }

    private fun extractYouTubeId(url: String): String? {
        val pattern = "(?<=watch\\?v=|/videos/|embed/|youtu.be/|/v/|/e/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%2F|youtu.be%2F|/shorts/)[^#&?\\n]*"
        val compiled = Pattern.compile(pattern)
        val matcher = compiled.matcher(url)
        return if (matcher.find()) {
            val id = matcher.group()
            if (id.length in 8..15) id else null
        } else null
    }
}

sealed interface YouTubeParseResult {
    data class Valid(
        val videoId: String,
        val webUrl: String,
        val thumbnailUrl: String,
        val title: String
    ) : YouTubeParseResult

    data class Invalid(val message: String) : YouTubeParseResult
}
