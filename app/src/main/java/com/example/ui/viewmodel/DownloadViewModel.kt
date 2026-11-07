package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.DownloadProgress
import com.example.data.repository.DownloadRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class DownloadUiState(
    val url: String = "",
    val busy: Boolean = false,
    val percent: Int? = null,
    val receivedBytes: Long = 0,
    val totalBytes: Long = 0,
    val message: String? = null,
    val error: String? = null,
    val files: List<File> = emptyList()
)

class DownloadViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DownloadRepository()
    private val directory = File(application.getExternalFilesDir(null), "Downloads")
    private val _state = MutableStateFlow(DownloadUiState(files = listFiles()))
    val state = _state.asStateFlow()
    private var job: Job? = null
    @Volatile private var cancelled = false

    fun setUrl(value: String) { _state.value = _state.value.copy(url = value, error = null) }

    fun startDownload() {
        if (_state.value.busy) return
        val url = _state.value.url.trim()
        cancelled = false
        job = viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, percent = 0, message = "Memulai download…", error = null)
            try {
                val file = repository.downloadDirectVideo(url, directory, ::onProgress) { cancelled }
                _state.value = _state.value.copy(
                    busy = false, percent = 100, message = "Download selesai: ${file.name}",
                    files = listFiles(), url = ""
                )
            } catch (e: CancellationException) {
                _state.value = _state.value.copy(busy = false, message = "Download dibatalkan")
            } catch (e: Exception) {
                _state.value = _state.value.copy(busy = false, error = e.message ?: "Download gagal")
            }
        }
    }

    private fun onProgress(p: DownloadProgress) {
        _state.value = _state.value.copy(
            percent = p.percent, receivedBytes = p.receivedBytes,
            totalBytes = p.totalBytes, message = "Mengunduh…"
        )
    }

    fun cancelDownload() { cancelled = true; job?.cancel(); job = null }
    fun refresh() { _state.value = _state.value.copy(files = listFiles()) }
    fun delete(file: File) { file.delete(); refresh() }

    private fun listFiles(): List<File> =
        directory.listFiles()?.filter { it.isFile && !it.name.endsWith(".part") }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
}
