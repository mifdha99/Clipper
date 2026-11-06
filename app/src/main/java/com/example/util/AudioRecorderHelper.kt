package com.example.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

class AudioRecorderHelper(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    var isRecording: Boolean = false
        private set

    fun startRecording(): File? {
        try {
            val audioDir = File(context.cacheDir, "audio_records").apply { mkdirs() }
            val file = File(audioDir, "recorded_voice_${System.currentTimeMillis()}.m4a")
            currentOutputFile = file

            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recorder = newRecorder
            isRecording = true
            Log.d("AudioRecorderHelper", "Recording started: ${file.absolutePath}")
            return file
        } catch (e: Exception) {
            Log.e("AudioRecorderHelper", "Failed to start recording", e)
            stopRecording()
            return null
        }
    }

    fun getMaxAmplitude(): Int {
        return try {
            if (isRecording) recorder?.maxAmplitude ?: 0 else 0
        } catch (e: Exception) {
            0
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) return currentOutputFile
        try {
            recorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorderHelper", "Error stopping recorder", e)
        } finally {
            recorder = null
            isRecording = false
        }
        return currentOutputFile
    }

    fun cancelRecording() {
        stopRecording()
        currentOutputFile?.let {
            if (it.exists()) it.delete()
        }
        currentOutputFile = null
    }
}
