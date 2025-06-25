package chat.sphinx.common.components.media_player

import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import java.io.File

class MediaPlayerControllerImpl : MediaPlayerController() {

    private val mediaPlayerFactory: MediaPlayerFactory
    private val mediaPlayer: MediaPlayer

    private var playbackListener: MediaPlayerController.PlaybackListener? = null

    init {
        try {
            val jvmArch = System.getProperty("os.arch") // e.g., "x86", "amd64"
            val nativePathUrl = MediaPlayerControllerImpl::class.java.getResource("/natives/windows/x86_64")
                ?: throw IllegalStateException("VLC native path not found")

            val nativeDir = File(nativePathUrl.toURI())

            // Check if we're on 64-bit JVM and using 64-bit VLC
            if (jvmArch.contains("64") && nativeDir.absolutePath.contains("x86_64").not()) {
                throw IllegalStateException("64-bit JVM but 32-bit VLC library found.")
            }

            // (Optional) Check for 32-bit JVM
            if (jvmArch.contains("86") && jvmArch.contains("64").not() && nativeDir.absolutePath.contains("x86_64")) {
                throw IllegalStateException("32-bit JVM but 64-bit VLC library found.")
            }

            System.setProperty("jna.library.path", nativeDir.absolutePath)
            System.setProperty("VLC_PLUGIN_PATH", File(nativeDir, "plugins").absolutePath)

            mediaPlayerFactory = MediaPlayerFactory()
            mediaPlayer = mediaPlayerFactory.mediaPlayers().newMediaPlayer()

            mediaPlayer.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
                override fun finished(mediaPlayer: MediaPlayer?) {
                    playbackListener?.onPlaybackCompleted()
                }
            })

        } catch (e: Exception) {
            println("⚠️ VLC player could not be initialized: ${e.message}")
            // Fallback: maybe disable media player feature or use a dummy player
            throw IllegalStateException("VLC not supported on this system", e)
        }
    }

    override fun setPlaybackListener(listener: MediaPlayerController.PlaybackListener?) {
        playbackListener = listener
    }

    override fun play(url: String, startTimeMillis: Long) {
        mediaPlayer.media().play(url)
        if (startTimeMillis > 0) {
            mediaPlayer.controls().setTime(startTimeMillis)
        }
    }

    override fun pause() {
        mediaPlayer.controls().pause()
    }

    override fun stop() {
        mediaPlayer.controls().stop()
    }

    override fun seekTo(millis: Long) {
        mediaPlayer.controls().setTime(millis)
    }

    override fun setPlaybackSpeed(speed: Double) {
        mediaPlayer.controls().setRate(speed.toFloat())
    }

    override fun isPlaying(): Boolean {
        return mediaPlayer.status().isPlaying
    }

    override fun getCurrentPosition(): Long {
        return mediaPlayer.status().time()
    }

    override fun getDuration(): Long {
        return mediaPlayer.media().info()?.duration() ?: 0
    }
}
