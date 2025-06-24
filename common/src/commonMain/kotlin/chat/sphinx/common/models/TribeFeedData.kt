package chat.sphinx.common.models

import chat.sphinx.wrapper.chat.AppUrl
import chat.sphinx.wrapper.chat.ChatHost
import chat.sphinx.wrapper.chat.ChatUUID
import chat.sphinx.wrapper.feed.FeedType
import chat.sphinx.wrapper.feed.FeedUrl


sealed class TribeFeedData {
    object Loading: TribeFeedData()

    sealed class Result: TribeFeedData() {
        object NoFeed : Result()

        data class FeedData(
            val host: ChatHost,
            val feedUrl: FeedUrl?,
            val chatUUID: ChatUUID,
            val feedType: FeedType,
            val appUrl: AppUrl?,
            val badges: Array<String>,
        ) : Result()
    }
}
