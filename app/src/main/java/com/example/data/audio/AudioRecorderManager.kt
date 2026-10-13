package com.example.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class AudioRecorderManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    var isRecording: Boolean = false
        private set
    var isPaused: Boolean = false
        private set

    private var currentOutputFile: File? = null

    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    fun start(outputFile: File): Boolean {
        return try {
            stopAndRelease()

            currentOutputFile = outputFile
            val parentDir = outputFile.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }

            val recorder = createMediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            isRecording = true
            isPaused = false
            Log.d("AudioRecorderManager", "MediaRecorder started successfully on file: ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Failed to start MediaRecorder", e)
            stopAndRelease()
            false
        }
    }

    fun pause() {
        if (isRecording && !isPaused) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    mediaRecorder?.pause()
                    isPaused = true
                    Log.d("AudioRecorderManager", "MediaRecorder paused")
                } catch (e: Exception) {
                    Log.e("AudioRecorderManager", "Error pausing MediaRecorder", e)
                }
            }
        }
    }

    fun resume() {
        if (isRecording && isPaused) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    mediaRecorder?.resume()
                    isPaused = false
                    Log.d("AudioRecorderManager", "MediaRecorder resumed")
                } catch (e: Exception) {
                    Log.e("AudioRecorderManager", "Error resuming MediaRecorder", e)
                }
            }
        }
    }

    fun getMaxAmplitude(): Int {
        return try {
            if (isRecording) {
                mediaRecorder?.maxAmplitude ?: 0
            } else 0
        } catch (e: Exception) {
            0
        }
    }

    fun stop(): File? {
        val file = currentOutputFile
        try {
            if (isRecording) {
                mediaRecorder?.stop()
                Log.d("AudioRecorderManager", "MediaRecorder stopped")
            }
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Error stopping MediaRecorder", e)
        } finally {
            stopAndRelease()
        }
        return file
    }

    fun cancel() {
        try {
            currentOutputFile?.let {
                if (it.exists()) it.delete()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Error deleting canceled recording file", e)
        } finally {
            stopAndRelease()
        }
    }

    private fun stopAndRelease() {
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Error releasing MediaRecorder", e)
        } finally {
            mediaRecorder = null
            isRecording = false
            isPaused = false
            currentOutputFile = null
        }
    }
}
