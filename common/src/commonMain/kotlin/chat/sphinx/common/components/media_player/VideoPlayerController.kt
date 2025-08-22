package chat.sphinx.common.components.media_player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.jvm.JvmName


class VideoPlayerController(private val holder: EnhancedFxPlayerHolder) {
    private var player: MediaPlayer? = null

    private fun orientationOf(width: Int, height: Int): String = when {
        width <= 0 || height <= 0 -> "unknown"
        width > height -> "landscape"
        width < height -> "portrait"
        else -> "square"
    }

    private fun detectRequiredRotation(media: javafx.scene.media.Media): Double {
        val w = media.width
        val h = media.height

        // Check for rotation metadata first
        val rotationFromMeta = media.metadata["rotate"]?.toString()?.toDoubleOrNull() ?: 0.0


        // If there's explicit rotation metadata, use it
        if (rotationFromMeta != 0.0) {
            return -rotationFromMeta // Negative to counter-rotate
        }

        // Heuristic: if dimensions suggest landscape but video appears rotated 90° to the left
        // we need to rotate 90° to the right (+90°) to correct it
        if (w > h) {
            // Large landscape videos from mobile devices often need rotation
            if (w >= 1920 && h >= 1080) {
                return 90.0  // Changed from -90.0 to +90.0
            }
        }

        return 0.0
    }

    var isPlaying by mutableStateOf(false)
        private set
    var currentTime by mutableStateOf(0L)
        private set
    var duration by mutableStateOf(0L)
        private set
    var volume by mutableStateOf(1.0)
        private set
    var playbackSpeed by mutableStateOf(1.0)
        private set
    var isReady by mutableStateOf(false)
        private set

    fun initialize(mediaPlayer: MediaPlayer) {
        this.player = mediaPlayer

        Platform.runLater {
            mediaPlayer.setOnReady {
                val durMs = mediaPlayer.totalDuration.toMillis().toLong()
                isReady = true
                duration = durMs

                // Get natural dimensions and detect rotation
                val media = mediaPlayer.media
                val w = media.width
                val h = media.height
                val requiredRotation = detectRequiredRotation(media)


                // Apply rotation to MediaView if needed
                holder.mediaView?.let { mv ->
                    if (requiredRotation != 0.0) {
                        mv.rotate = requiredRotation
                    } else {
                        mv.rotate = 0.0
                    }
                }

                // Optional peek at metadata keys/values (handy for debugging device-recorded files)
                if (media.metadata.isNotEmpty()) {
                    media.metadata.forEach { (k, v) ->
                        // Log rotation-related metadata
                        if (k.contains("rotat", ignoreCase = true) ||
                            k.contains("orient", ignoreCase = true) ||
                            k.contains("transform", ignoreCase = true)) {
                        }
                    }
                }
            }


            mediaPlayer.currentTimeProperty().addListener { _, _, newTime ->
                currentTime = newTime.toMillis().toLong()
            }

            mediaPlayer.volumeProperty().addListener { _, _, newVolume ->
                volume = newVolume.toDouble()
            }
        }

        startTimeUpdater()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun startTimeUpdater() {
        GlobalScope.launch {
            while (true) {
                delay(1000)
                player?.let { p ->
                    Platform.runLater {
                        if (!p.currentTime.isUnknown) {
                            currentTime = p.currentTime.toMillis().toLong()
                        }
                        if (!p.totalDuration.isUnknown) {
                            duration = p.totalDuration.toMillis().toLong()
                        }
                    }
                }
            }
        }
    }

    fun play() { Platform.runLater { player?.play() } }
    fun pause() { Platform.runLater { player?.pause() } }
    fun stop() { Platform.runLater { player?.stop() } }

    fun seek(timeMillis: Long) {
        Platform.runLater {
            player?.seek(javafx.util.Duration.millis(timeMillis.toDouble()))
        }
    }

    fun rewind(seconds: Int = 10) = seek((currentTime - seconds * 1000).coerceAtLeast(0L))
    fun forward(seconds: Int = 10) = seek((currentTime + seconds * 1000).coerceAtMost(duration))

    @JvmName("updatePlayerVolume")   // <-- give function a distinct JVM name
    fun setVolume(volume: Double) {
        Platform.runLater {
            val clampedVolume = volume.coerceIn(0.0, 1.0)
            player?.volume = clampedVolume
            this.volume = clampedVolume
        }
    }

    @JvmName("updatePlaybackRate")   // <-- and here too
    fun setPlaybackSpeed(speed: Double) {
        Platform.runLater {
            val clampedSpeed = speed.coerceIn(0.25, 4.0)
            player?.rate = clampedSpeed
            this.playbackSpeed = clampedSpeed
        }
    }
}

class EnhancedFxPlayerHolder(
    var jfxPanel: JFXPanel? = null,
    var player: MediaPlayer? = null,
    var isReady: Boolean = false,
    var currentPath: String? = null,
    var mediaView: MediaView? = null
) {
    fun cleanup() {
        // Reset MediaView rotation when cleaning up
        Platform.runLater {
            mediaView?.rotate = 0.0
            try { player?.stop() } catch (_: Exception) {}
            try { player?.dispose() } catch (_: Exception) {}
            player = null
            isReady = false
        }
    }
}