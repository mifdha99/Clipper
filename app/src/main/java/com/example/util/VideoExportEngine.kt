package com.example.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

private const val TAG = "VideoExportEngine"

data class ExportResult(
    val outputFilePath: String,
    val outputContentUri: String,
    val fileSizeBytes: Long,
    val durationMs: Long,
    val resolution: String,
    val fileName: String
)

object VideoExportEngine {

    /**
     * Lossless hardware MP4 trimming using native MediaExtractor and MediaMuxer.
     * Extracts all tracks (Video + Audio) and muxes the exact [startMs, endMs] window.
     * Saves the final video to device Movies/Clipper gallery directory.
     */
    suspend fun trimAndExportVideo(
        context: Context,
        inputUriString: String,
        startMs: Long,
        endMs: Long,
        baseTitle: String,
        onProgress: (Float, String) -> Unit
    ): ExportResult = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val startUs = (startMs * 1000L).coerceAtLeast(0L)
        val endUs = (endMs * 1000L).coerceAtLeast(startUs + 1_000_000L)
        val targetDurationUs = endUs - startUs

        onProgress(0.05f, "Menyiapkan demuxer media...")

        val cleanTitle = baseTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(24).ifEmpty { "clip" }
        val fileName = "Clipper_${cleanTitle}_${System.currentTimeMillis()}.mp4"
        val tempOutputFile = File(appContext.cacheDir, "temp_render_${System.currentTimeMillis()}.mp4")

        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        var videoWidth = 1080
        var videoHeight = 1920

        try {
            val inputUri = Uri.parse(inputUriString)
            if (inputUri.scheme == "content" || inputUri.scheme == "file") {
                extractor.setDataSource(appContext, inputUri, null)
            } else {
                extractor.setDataSource(inputUriString)
            }

            val trackCount = extractor.trackCount
            if (trackCount <= 0) {
                throw IllegalStateException("Tidak dapat membaca track media dari video input ($inputUriString)")
            }

            muxer = MediaMuxer(tempOutputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val indexMap = HashMap<Int, Int>()

            var videoTrackIndex = -1
            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i
                    if (format.containsKey(MediaFormat.KEY_WIDTH)) {
                        videoWidth = format.getInteger(MediaFormat.KEY_WIDTH)
                    }
                    if (format.containsKey(MediaFormat.KEY_HEIGHT)) {
                        videoHeight = format.getInteger(MediaFormat.KEY_HEIGHT)
                    }
                    val muxerTrack = muxer.addTrack(format)
                    indexMap[i] = muxerTrack
                } else if (mime.startsWith("audio/")) {
                    val muxerTrack = muxer.addTrack(format)
                    indexMap[i] = muxerTrack
                }
            }

            if (videoTrackIndex == -1) {
                throw IllegalStateException("Video tidak memiliki track visual yang valid.")
            }

            muxer.start()
            onProgress(0.20f, "Mengekstrak dan memotong keyframe...")

            // Select all tracks
            for (i in 0 until trackCount) {
                if (indexMap.containsKey(i)) {
                    extractor.selectTrack(i)
                }
            }

            // Seek to startUs with previous sync frame
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            val bufferSize = 1024 * 1024 // 1 MB buffer
            val buffer = ByteBuffer.allocateDirect(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            val trackFirstSampleTimeUs = HashMap<Int, Long>()

            var writtenSamples = 0
            while (true) {
                val trackIndex = extractor.sampleTrackIndex
                if (trackIndex < 0) {
                    break
                }

                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs > endUs) {
                    extractor.advance()
                    continue
                }

                if (sampleTimeUs < startUs) {
                    extractor.advance()
                    continue
                }

                val muxerTrack = indexMap[trackIndex]
                if (muxerTrack != null) {
                    buffer.clear()
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize > 0) {
                        val firstUs = trackFirstSampleTimeUs.getOrPut(trackIndex) { sampleTimeUs }
                        val adjustedTimeUs = (sampleTimeUs - firstUs).coerceAtLeast(0L)

                        val flags = extractor.sampleFlags
                        bufferInfo.set(0, sampleSize, adjustedTimeUs, flags)

                        muxer.writeSampleData(muxerTrack, buffer, bufferInfo)
                        writtenSamples++

                        if (trackIndex == videoTrackIndex && targetDurationUs > 0) {
                            val progress = 0.20f + (0.65f * (sampleTimeUs - startUs).toFloat() / targetDurationUs.toFloat())
                            val clampedProgress = progress.coerceIn(0.20f, 0.85f)
                            onProgress(clampedProgress, "Memotong video MP4 (${(clampedProgress * 100).toInt()}%)...")
                        }
                    }
                }

                extractor.advance()
            }

            onProgress(0.88f, "Menyelesaikan muxing container MP4...")
        } finally {
            try {
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Muxer stop/release warning", e)
            }
            try {
                extractor.release()
            } catch (e: Exception) {
                Log.w(TAG, "Extractor release warning", e)
            }
        }

        if (!tempOutputFile.exists() || tempOutputFile.length() == 0L) {
            throw IllegalStateException("Gagal memotong video: file output kosong atau gagal ditulis.")
        }

        onProgress(0.92f, "Menyimpan video ke Galeri HP (Movies/Clipper)...")

        // Save to Android MediaStore in Movies/Clipper
        val savedUri = saveToMediaStore(appContext, tempOutputFile, fileName)
        onProgress(1.0f, "Ekspor Berhasil!")

        val finalDurationMs = (endMs - startMs).coerceAtLeast(1000L)
        val resolutionStr = "${videoWidth}x${videoHeight}"

        ExportResult(
            outputFilePath = tempOutputFile.absolutePath,
            outputContentUri = savedUri.toString(),
            fileSizeBytes = tempOutputFile.length(),
            durationMs = finalDurationMs,
            resolution = resolutionStr,
            fileName = fileName
        )
    }

    private fun saveToMediaStore(context: Context, sourceFile: File, displayName: String): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Clipper")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val itemUri = resolver.insert(collection, values)
            ?: throw IllegalStateException("Tidak dapat membuat entri MediaStore untuk $displayName")

        try {
            resolver.openOutputStream(itemUri)?.use { out ->
                FileInputStream(sourceFile).use { inStream ->
                    inStream.copyTo(out)
                }
            } ?: throw IllegalStateException("Tidak dapat membuka output stream MediaStore.")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
            }
        } catch (e: Exception) {
            resolver.delete(itemUri, null, null)
            throw e
        }

        return itemUri
    }
}
