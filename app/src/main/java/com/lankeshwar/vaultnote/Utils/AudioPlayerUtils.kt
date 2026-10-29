package com.lankeshwar.vaultnote.Utils

import android.media.MediaPlayer

class AudioPlayerUtils {
    private var mediaPlayer: MediaPlayer? = null
    var currentFilePath: String? = null
        private set

    var isPlaying: Boolean = false
        private set

    fun play(
        filePath: String,
        onPlayingChanged: (Boolean) -> Unit,
        onError: () -> Unit
    ) {
        stop(onPlayingChanged)

        try {
            val player = MediaPlayer()
            mediaPlayer = player
            currentFilePath = filePath

            player.setDataSource(filePath)

            player.setOnPreparedListener { preparedPlayer ->
                preparedPlayer.start()
                isPlaying = true
                onPlayingChanged(true)
            }

            player.setOnCompletionListener {
                releasePlayer()
                onPlayingChanged(false)
            }

            player.setOnErrorListener { _, _, _ ->
                releasePlayer()
                onPlayingChanged(false)
                onError()
                true
            }

            player.prepareAsync()

        } catch (e: Exception) {
            releasePlayer()
            onPlayingChanged(false)
            onError()
        }
    }

    fun pause(onPlayingChanged: (Boolean) -> Unit) {
        val player = mediaPlayer ?: return

        if (player.isPlaying) {
            player.pause()
            isPlaying = false
            onPlayingChanged(false)
        }
    }

    fun resume(onPlayingChanged: (Boolean) -> Unit) {
        val player = mediaPlayer ?: return

        try {
            player.start()
            isPlaying = true
            onPlayingChanged(true)
        } catch (e: IllegalStateException) {
            releasePlayer()
            onPlayingChanged(false)
        }
    }

    fun stop(onPlayingChanged: (Boolean) -> Unit) {
        releasePlayer()
        onPlayingChanged(false)
    }

    fun release() {
        releasePlayer()
    }

    private fun releasePlayer() {
        mediaPlayer?.let { player ->
            runCatching { player.reset() }
            player.release()
        }

        mediaPlayer = null
        currentFilePath = null
        isPlaying = false
    }
}