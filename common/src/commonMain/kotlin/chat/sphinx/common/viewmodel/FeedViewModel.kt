package chat.sphinx.common.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import chat.sphinx.di.container.SphinxContainer
import chat.sphinx.response.Response
import chat.sphinx.utils.notifications.createSphinxNotificationManager
import chat.sphinx.wrapper.chat.ChatHost
import chat.sphinx.wrapper.dashboard.ChatId
import chat.sphinx.wrapper.feed.*
import chat.sphinx.wrapper.podcast.FeedSearchResult
import chat.sphinx.wrapper.podcast.FeedSearchResultRow
import chat.sphinx.wrapper.time
import chat.sphinx.wrapper.toPhotoUrl
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
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

    private val _recentlyPlayedEpisode = MutableStateFlow<FeedItem?>(null)
    val recentlyPlayedEpisode: StateFlow<FeedItem?> = _recentlyPlayedEpisode

    private val _searchResults = MutableStateFlow<List<FeedSearchResultRow>>(emptyList())
    val searchResults: StateFlow<List<FeedSearchResultRow>> = _searchResults

    var feedSearchText: MutableState<TextFieldValue?> = mutableStateOf(null)

    init {
        scope.launch(dispatchers.mainImmediate) {
            repositoryDashboard.getAllFeeds().collect { feeds ->
                val filteredFeeds = feeds.toList()

                _feedsHolderViewStateFlow.value = filteredFeeds
                    .filter { it.subscribed.isTrue() || it.chatId.value.toInt() != ChatId.NULL_CHAT_ID }
                    .sortedByDescending { it.lastPublished?.datePublished?.time ?: 0 }

                val episodes = feeds.flatMap { feed ->
                    feed.items.mapNotNull { item ->
                        item.contentEpisodeStatus?.let { status ->
                            status.lastPlayed?.let { lastPlayed ->
                                item to lastPlayed
                            }
                        }
                    }
                }

                val mostRecent = episodes.maxByOrNull { it.second.value }?.first
                _recentlyPlayedEpisode.value = mostRecent
            }
        }
        observeContentEpisodeStatus()
    }

    fun searchFeeds(searchTerm: TextFieldValue) {
        feedSearchText.value = searchTerm
        if (searchTerm.text.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        scope.launch(dispatchers.io) {
            feedRepository.searchFeedsBy(searchTerm.text, FeedType.Podcast).collect {
                _searchResults.value = it
            }
        }
    }

    fun clearFeedSearch() {
        feedSearchText.value = TextFieldValue("")
        _searchResults.value = emptyList()
    }

    fun onPodcastSearchResultClicked(
        searchResult: FeedSearchResult,
        onComplete: (() -> Unit)? = null
    ) {
        if (!searchResult.feedType.toInt().toFeedType().isPodcast()) {
            onComplete?.invoke()
            return
        }

        scope.launch(dispatchers.mainImmediate) {
            val feedUrl = searchResult.url.toFeedUrl()
            if (feedUrl == null) {
                onComplete?.invoke()
                return@launch
            }

            val response = feedRepository.updateFeedContent(
                chatId = ChatId(ChatId.NULL_CHAT_ID.toLong()),
                host = ChatHost(Feed.TRIBES_DEFAULT_SERVER_URL),
                feedUrl = feedUrl,
                searchResultDescription = searchResult.description?.toFeedDescription(),
                searchResultImageUrl = searchResult.imageUrl?.toPhotoUrl(),
                chatUUID = null,
                subscribed = false.toSubscribed(),
                currentEpisodeId = null
            )

            when (response) {
                is Response.Success -> {
                    val feed = feedRepository.getFeedById(response.value).firstOrNull()
                    if (feed != null && feed.isPodcast) {
                        delay(300L)
                        // DashboardViewModel.SplitContentType.Podcast needs a new implementation because
                        // now we will handle null chatId in Feed
//                        dashboardViewModel.toggleSplitScreen(
//                            isOpen = true,
//                            type = DashboardViewModel.SplitContentType.Podcast(feed.chatId)
//                        )
                    } else { }
                }

                is Response.Error -> {}
            }

            onComplete?.invoke()
        }
    }


    private fun observeContentEpisodeStatus() {
        scope.launch(dispatchers.io) {
            feedRepository.getLastPlayedEpisode().collect { statuses ->

                _recentlyPlayedEpisode.value = feedsHolderViewStateFlow.value.find { feed ->
                    feed.items.any { item ->
                        feed.id == statuses?.feedId && item.id == statuses.itemId
                    }
                }?.items?.find { item ->
                    item.id == statuses?.itemId
                }
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
