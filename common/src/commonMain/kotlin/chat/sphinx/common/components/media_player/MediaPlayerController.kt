package chat.sphinx.common.components.media_player

abstract class MediaPlayerController {
    interface PlaybackListener {
        fun onPlaybackCompleted()
    }
    abstract fun play(url: String, startTimeMillis: Long = 0)
    abstract fun pause()
    abstract fun stop()
    abstract fun seekTo(millis: Long)
    abstract fun setPlaybackSpeed(speed: Double)
    abstract fun isPlaying(): Boolean
    abstract fun getCurrentPosition(): Long
    abstract fun getDuration(): Long
    abstract fun setPlaybackListener(listener: PlaybackListener?)
}
