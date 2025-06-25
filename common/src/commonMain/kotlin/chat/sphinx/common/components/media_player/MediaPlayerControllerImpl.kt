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
            val nativeDir = File(
                MediaPlayerControllerImpl::class.java.getResource("/natives/windows/x86_64")!!.toURI()
            )
            System.setProperty("jna.library.path", nativeDir.absolutePath)
            System.setProperty("VLC_PLUGIN_PATH", File(nativeDir, "plugins").absolutePath)
        } catch (e: Exception) {
            println("VLC path setup failed: ${e.message}")
        }

        mediaPlayerFactory = MediaPlayerFactory()
        mediaPlayer = mediaPlayerFactory.mediaPlayers().newMediaPlayer()

        mediaPlayer.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun finished(mediaPlayer: MediaPlayer?) {
                playbackListener?.onPlaybackCompleted()
            }
        })
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
