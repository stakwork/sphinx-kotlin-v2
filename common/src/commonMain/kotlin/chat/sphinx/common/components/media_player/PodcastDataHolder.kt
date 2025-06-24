package chat.sphinx.common.components.media_player

import chat.sphinx.wrapper.dashboard.ChatId
import chat.sphinx.wrapper.feed.FeedDestination
import chat.sphinx.wrapper.feed.FeedUrl
import chat.sphinx.wrapper.feed.Subscribed
import chat.sphinx.wrapper.lightning.Sat

class PodcastDataHolder private constructor(
    val chatId: ChatId,
    val podcastId: String,
    val episodeId: String,
    val feedUrl: FeedUrl,
    val subscriptionStatus: Subscribed
) {

    var speed: Double = 1.0
        private set

    var satsPerMinute: Sat = Sat(0)
        private set

    var destinations: List<FeedDestination> = emptyList()
        private set

    companion object {
        fun instantiate(
            chatId: ChatId,
            podcastId: String,
            episodeId: String,
            satsPerMinute: Sat,
            speed: Double,
            feedUrl: FeedUrl,
            subscriptionStatus: Subscribed
        ): PodcastDataHolder {
            return PodcastDataHolder(chatId, podcastId, episodeId, feedUrl, subscriptionStatus).apply {
                setSpeed(speed)
                setSatsPerMinute(satsPerMinute)
            }
        }
    }

    fun setSpeed(speed: Double): Double {
        this.speed = speed.coerceIn(0.5, 2.1)
        return this.speed
    }

    fun setSatsPerMinute(sats: Sat): Sat {
        this.satsPerMinute = sats
        return this.satsPerMinute
    }

    fun setDestinations(destinations: List<FeedDestination>) {
        this.destinations = destinations
    }
}
