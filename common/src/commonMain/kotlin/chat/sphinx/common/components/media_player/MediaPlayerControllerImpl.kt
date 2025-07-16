import chat.sphinx.common.components.media_player.MediaPlayerController
import javafx.application.Platform
import javafx.embed.swing.JFXPanel // Needed to init JavaFX runtime
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import java.io.File

class MediaPlayerControllerImpl : MediaPlayerController() {

    private var mediaPlayer: MediaPlayer? = null
    private var playbackListener: MediaPlayerController.PlaybackListener? = null

    init {
        // Initializes JavaFX runtime
        JFXPanel()
    }

    override fun setPlaybackListener(listener: MediaPlayerController.PlaybackListener?) {
        playbackListener = listener
    }

    override fun play(url: String, startTimeMillis: Long) {
        Platform.runLater {
            try {
                mediaPlayer?.dispose()

                val uri = if (url.startsWith("http")) url else File(url).toURI().toString()
                val media = Media(uri)

                media.setOnError {
                    println("Media error: ${media.error}")
                }

                mediaPlayer = MediaPlayer(media).apply {
                    setOnError {
                        println("MediaPlayer error: $error")
                    }

                    setOnEndOfMedia {
                        playbackListener?.onPlaybackCompleted()
                    }

                    setOnReady {
                        if (startTimeMillis > 0) {
                            seek(javafx.util.Duration.millis(startTimeMillis.toDouble()))
                        }
                        play()
                    }

                    setOnPlaying {
                        playbackListener?.onPlaybackStarted()
                    }
                }

                // Notify UI that we're preparing
                playbackListener?.onPreparing()

            } catch (e: Exception) {
                println("Exception while trying to play media: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    override fun pause() {
        Platform.runLater {
            mediaPlayer?.pause()
        }
    }

    override fun stop() {
        Platform.runLater {
            mediaPlayer?.stop()
        }
    }

    override fun seekTo(millis: Long) {
        Platform.runLater {
            mediaPlayer?.seek(javafx.util.Duration.millis(millis.toDouble()))
        }
    }

    override fun setPlaybackSpeed(speed: Double) {
        Platform.runLater {
            mediaPlayer?.rate = speed
        }
    }

    override fun isPlaying(): Boolean {
        return mediaPlayer?.status == MediaPlayer.Status.PLAYING
    }

    override fun getCurrentPosition(): Long {
        return mediaPlayer?.currentTime?.toMillis()?.toLong() ?: 0
    }

    override fun getDuration(): Long {
        return mediaPlayer?.totalDuration?.toMillis()?.toLong() ?: 0
    }
}
