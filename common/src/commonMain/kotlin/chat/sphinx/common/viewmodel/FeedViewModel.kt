package chat.sphinx.common.viewmodel

import chat.sphinx.di.container.SphinxContainer
import chat.sphinx.utils.notifications.createSphinxNotificationManager
import chat.sphinx.wrapper.chat.ChatHost
import chat.sphinx.wrapper.chat.ChatUUID
import chat.sphinx.wrapper.chat.toChatHost
import chat.sphinx.wrapper.dashboard.ChatId
import chat.sphinx.wrapper.feed.*
import chat.sphinx.wrapper.time
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FeedViewModel(
    private val dashboardViewModel: DashboardViewModel
) {

    val scope = SphinxContainer.appModule.applicationScope
    val dispatchers = SphinxContainer.appModule.dispatchers
    private val coreDB = SphinxContainer.appModule.coreDBImpl
    private val sphinxNotificationManager = createSphinxNotificationManager()
    private val repositoryDashboard = SphinxContainer.repositoryModule(sphinxNotificationManager).repositoryDashboard
    private val feedRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).feedRepository

    private val _feedsHolderViewStateFlow = MutableStateFlow<List<Feed>>(emptyList())
    val feedsHolderViewStateFlow: StateFlow<List<Feed>> = _feedsHolderViewStateFlow

//    private val _lastPlayedFeedsHolderViewStateFlow = MutableStateFlow<List<Feed>>(emptyList())
//    val lastPlayedFeedsHolderViewStateFlow: StateFlow<List<Feed>> = _lastPlayedFeedsHolderViewStateFlow

    init {
        scope.launch(dispatchers.mainImmediate) {
            repositoryDashboard.getAllFeeds().collect { feeds ->
                val filteredFeeds = feeds.toList()

                _feedsHolderViewStateFlow.value = filteredFeeds
                    .filter { it.subscribed.isTrue() || it.chatId.value.toInt() != ChatId.NULL_CHAT_ID }
                    .sortedByDescending { it.lastPublished?.datePublished?.time ?: 0 }

//                _lastPlayedFeedsHolderViewStateFlow.value = filteredFeeds
//                    .filter { it.lastPlayed != null }
//                    .sortedWith(compareByDescending<Feed> { it.lastPlayed?.time }
//                        .thenByDescending { it.lastPublished?.datePublished?.time ?: 0 })
            }
        }
    }
    fun onPodcastFeedItemClicked(item: FeedItem) {
        val feed = item.feed ?: return
        val chatId = feed.chatId
        val feedUrl = feed.feedUrl
        val chatUUID = feed.chat?.uuid ?: return
        val host = ChatHost(Feed.TRIBES_DEFAULT_SERVER_URL) // not used for networkQueryChat.getFeedContent
        val feedType = feed.feedType

        if (!feedType.isPodcast()) return

        scope.launch(dispatchers.mainImmediate) {
            feedRepository.updateFeedContent(
                chatId = chatId,
                host = host,
                feedUrl = feedUrl,
                chatUUID = chatUUID,
                subscribed = false.toSubscribed(),
                currentEpisodeId = null
            )

            delay(300L)

            dashboardViewModel.toggleSplitScreen(
                isOpen = true,
                type = DashboardViewModel.SplitContentType.Podcast(chatId)
            )
        }
    }
}
