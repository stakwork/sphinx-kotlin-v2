package chat.sphinx.common.components.media_player

import chat.sphinx.wrapper.dashboard.ChatId
import chat.sphinx.wrapper.podcast.ContentEpisodeStatus
import chat.sphinx.wrapper.feed.FeedDestination
import chat.sphinx.wrapper.podcast.ContentFeedStatus

sealed class UserAction(val chatId: ChatId) {

    sealed class ServiceAction(chatId: ChatId): UserAction(chatId) {

        class Play(
            chatId: ChatId,
            val episodeUrl: String,
            val contentFeedStatus: ContentFeedStatus,
            val contentEpisodeStatus: ContentEpisodeStatus,
            val destinations: List<FeedDestination>
        ): ServiceAction(chatId)

        class Pause(
            chatId: ChatId,
            val episodeId: String,
        ): ServiceAction(chatId)

        class Seek(
            chatId: ChatId,
            val contentEpisodeStatus: ContentEpisodeStatus
        ): ServiceAction(chatId)
    }

    class AdjustSatsPerMinute(
        chatId: ChatId,
        val contentFeedStatus: ContentFeedStatus
    ): UserAction(chatId)

    class AdjustSpeed(
        chatId: ChatId,
        val contentFeedStatus: ContentFeedStatus
    ): UserAction(chatId)

    class SendBoost(
        chatId: ChatId,
        val podcastId: String,
        val contentFeedStatus: ContentFeedStatus,
        val contentEpisodeStatus: ContentEpisodeStatus,
        val destinations: List<FeedDestination>
    ): UserAction(chatId)

    class SetPaymentsDestinations(
        chatId: ChatId,
        val destinations: List<FeedDestination>
    ): UserAction(chatId)

    class TrackPodcastConsumed(
        chatId: ChatId
    ): UserAction(chatId)
}
