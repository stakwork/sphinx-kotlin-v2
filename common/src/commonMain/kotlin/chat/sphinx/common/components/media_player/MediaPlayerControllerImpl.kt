package chat.sphinx.common.components.media_player

import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer

class MediaPlayerControllerImpl : MediaPlayerController() {

    private val mediaPlayerFactory = MediaPlayerFactory()
    private val mediaPlayer: MediaPlayer = mediaPlayerFactory.mediaPlayers().newMediaPlayer()

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
