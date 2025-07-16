package chat.sphinx.common.components.media_player

import chat.sphinx.wrapper.dashboard.ChatId

sealed class MediaPlayerServiceState {
    object ServiceInactive : MediaPlayerServiceState()

    sealed class ServiceActive : MediaPlayerServiceState() {

        object ServiceLoading : ServiceActive()
        object ServiceConnected : ServiceActive()


        sealed class MediaState : ServiceActive() {

            data class Preparing(
                val chatId: ChatId,
                val podcastId: String,
                val episodeId: String,
                val currentTimeMillis: Long,
                val durationMillis: Long,
                val playbackSpeed: Double
            ) : ServiceActive()

            data class Playing(
                val chatId: ChatId,
                val podcastId: String,
                val episodeId: String,
                val currentTimeMillis: Long,
                val durationMillis: Long,
                val playbackSpeed: Double
            ) : MediaState()

            data class Paused(
                val chatId: ChatId,
                val podcastId: String,
                val episodeId: String,
                val currentTimeMillis: Long,
                val durationMillis: Long,
                val playbackSpeed: Double
            ) : MediaState()

            data class Ended(
                val chatId: ChatId,
                val podcastId: String,
                val episodeId: String,
                val currentTimeMillis: Long,
                val durationMillis: Long,
                val playbackSpeed: Double
            ) : MediaState()

            data class Failed(
                val chatId: ChatId,
                val podcastId: String,
                val episodeId: String,
                val currentTimeMillis: Int,
                val durationMillis: Int,
                val playbackSpeed: Double
            ) : MediaState()
        }
    }
}
