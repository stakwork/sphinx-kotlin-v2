package chat.sphinx.common.state

import chat.sphinx.wrapper.dashboard.ChatId
import chat.sphinx.wrapper.feed.FeedId

data class FloatingPlayerState(
    val chatId: ChatId,
    val feedId: FeedId,
    val isVisible: Boolean = true
)