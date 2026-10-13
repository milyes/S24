package com.example.data.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.util.Log
import java.io.File

class AudioPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    var currentPlayingPath: String? = null
        private set

    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying == true

    fun play(filePath: String, speed: Float = 1.0f, onCompletion: () -> Unit = {}): Boolean {
        return try {
            stop()

            val file = File(filePath)
            if (!file.exists()) {
                Log.w("AudioPlayerManager", "File does not exist on disk: $filePath. Falling back to simulated playback.")
                return false
            }

            val player = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    playbackParams = PlaybackParams().apply { this.speed = speed }
                }
                setOnErrorListener { _, what, extra ->
                    Log.w("AudioPlayerManager", "MediaPlayer error caught: what=$what, extra=$extra")
                    stop()
                    true
                }
                setOnCompletionListener {
                    currentPlayingPath = null
                    onCompletion()
                }
                start()
            }

            mediaPlayer = player
            currentPlayingPath = filePath
            Log.d("AudioPlayerManager", "Started playing real audio file: $filePath")
            true
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error playing audio file: $filePath", e)
            stop()
            false
        }
    }

    fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error pausing player", e)
        }
    }

    fun resume(): Boolean {
        return try {
            if (mediaPlayer != null) {
                mediaPlayer?.start()
                true
            } else false
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error resuming player", e)
            false
        }
    }

    fun stop() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error stopping player", e)
        } finally {
            mediaPlayer = null
            currentPlayingPath = null
        }
    }

    fun seekTo(positionSeconds: Int) {
        try {
            mediaPlayer?.seekTo(positionSeconds * 1000)
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error seeking player", e)
        }
    }

    fun setSpeed(speed: Float) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mediaPlayer?.let { player ->
                    val params = player.playbackParams
                    params.speed = speed
                    player.playbackParams = params
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error setting speed", e)
        }
    }

    fun getCurrentPositionSeconds(): Int {
        return try {
            (mediaPlayer?.currentPosition ?: 0) / 1000
        } catch (e: Exception) {
            0
        }
    }
}
