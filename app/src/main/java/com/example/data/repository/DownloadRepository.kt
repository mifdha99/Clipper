package com.example.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

data class DownloadProgress(
    val receivedBytes: Long,
    val totalBytes: Long,
    val percent: Int?
)

class DownloadRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.SECONDS) // allow long videos; cancellation closes the call
        .build()
) {
    /**
     * Downloads only a direct video URL. It rejects HTML pages and does not extract
     * video from platform watch pages.
     */
    suspend fun downloadDirectVideo(
        url: String,
        downloadsDir: File,
        onProgress: (DownloadProgress) -> Unit = {},
        isCancelled: () -> Boolean = { false }
    ): File = withContext(Dispatchers.IO) {
        require(url.startsWith("https://") || url.startsWith("http://")) {
            "URL harus diawali http:// atau https://"
        }
        if (url.contains("youtube.com/watch", true) ||
            url.contains("youtu.be/", true) ||
            url.contains("youtube.com/shorts/", true)
        ) {
            throw IOException("YouTube watch URL bukan direct MP4. Impor file video yang sah atau gunakan direct URL.")
        }
        downloadsDir.mkdirs()
        val temp = File(downloadsDir, ".${UUID.randomUUID()}.part")
        val request = Request.Builder()
            .url(url.trim())
            .header("User-Agent", "ClipperAndroid/1.0")
            .build()
        val call = client.newCall(request)
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) throw IOException("Server merespons HTTP ${response.code}")
                val body = response.body ?: throw IOException("Respons tidak memiliki body")
                val type = body.contentType()?.toString()?.lowercase().orEmpty()
                if (!(type.startsWith("video/") || type == "application/octet-stream")) {
                    throw IOException("Link bukan file video langsung (Content-Type: ${type.ifBlank { "unknown" }})")
                }
                val total = body.contentLength()
                var readTotal = 0L
                body.byteStream().use { input ->
                    temp.outputStream().buffered().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            if (isCancelled()) {
                                call.cancel()
                                throw CancellationException("Download dibatalkan")
                            }
                            val n = input.read(buffer)
                            if (n < 0) break
                            output.write(buffer, 0, n)
                            readTotal += n
                            onProgress(
                                DownloadProgress(
                                    readTotal,
                                    total,
                                    if (total > 0) ((readTotal * 100) / total).toInt().coerceIn(0, 100) else null
                                )
                            )
                        }
                    }
                }
                if (temp.length() <= 0) throw IOException("File hasil download kosong")
                // Basic MP4 signature check when the server says video/mp4.
                if (type.contains("mp4")) {
                    temp.inputStream().use { stream ->
                        val header = ByteArray(12)
                        val count = stream.read(header)
                        if (count < 8 || String(header, 4, 4, Charsets.US_ASCII) != "ftyp") {
                            throw IOException("File tidak memiliki signature MP4 yang valid")
                        }
                    }
                }
                val safeName = response.header("Content-Disposition")
                    ?.substringAfter("filename=", "")
                    ?.trim('"', '\'')
                    ?.takeIf { it.isNotBlank() && !it.contains("/") && !it.contains("\\") }
                    ?: "Clipper_${System.currentTimeMillis()}.mp4"
                val finalName = if (safeName.endsWith(".mp4", true)) safeName else "$safeName.mp4"
                var target = File(downloadsDir, finalName)
                if (target.exists()) target = File(downloadsDir, "Clipper_${System.currentTimeMillis()}.mp4")
                if (!temp.renameTo(target)) {
                    temp.copyTo(target, overwrite = false)
                    temp.delete()
                }
                target
            }
        } catch (t: Throwable) {
            temp.delete()
            throw t
        }
    }
}
