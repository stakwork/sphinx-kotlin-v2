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
        println("Controller.initialize() mediaPlayer=${System.identityHashCode(mediaPlayer)} holder? ${System.identityHashCode(holder)}")
        this.player = mediaPlayer

        Platform.runLater {
            mediaPlayer.setOnReady {
                println("Controller: setOnReady; duration=${mediaPlayer.totalDuration.toMillis()}")
                isReady = true
                duration = mediaPlayer.totalDuration.toMillis().toLong()
            }

            mediaPlayer.setOnPlaying { println("Controller: setOnPlaying"); isPlaying = true }
            mediaPlayer.setOnPaused { println("Controller: setOnPaused"); isPlaying = false }
            mediaPlayer.setOnStopped { println("Controller: setOnStopped"); isPlaying = false }

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
        // Don’t touch the Scene/JFXPanel here. Only stop/dispose the player.
        Platform.runLater {
            try { player?.stop() } catch (_: Exception) {}
            try { player?.dispose() } catch (_: Exception) {}
            player = null
            isReady = false
        }
    }
}
