package chat.sphinx.common.viewmodel

import chat.sphinx.common.viewmodel.chat.ChatTribeViewModel
import chat.sphinx.common.viewmodel.chat.ChatViewModel
import chat.sphinx.di.container.SphinxContainer
import chat.sphinx.utils.notifications.createSphinxNotificationManager
import chat.sphinx.wrapper.chat.TribeData
import chat.sphinx.wrapper.dashboard.ChatId
import chat.sphinx.wrapper.feed.isPodcast
import chat.sphinx.wrapper.feed.toSubscribed
import chat.sphinx.wrapper.podcast.PodcastEpisode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TribeFeedViewModel(
    val chatViewModel: ChatViewModel,
    val dashboardViewModel: DashboardViewModel
) {

    val scope = SphinxContainer.appModule.applicationScope
    val dispatchers = SphinxContainer.appModule.dispatchers
    private val sphinxNotificationManager = createSphinxNotificationManager()
    private val chatRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).chatRepository
    private val feedRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).feedRepository

    fun handleTribeData(data: TribeData) {
        scope.launch(dispatchers.mainImmediate) {
            val feedUrl = data.feedUrl
            val feedType = data.feedType

            if (feedUrl != null && chatViewModel.chatId != null) {
                // Update feed content
                feedRepository.updateFeedContent(
                    chatId = chatViewModel.chatId,
                    host = data.host,
                    feedUrl = feedUrl,
                    chatUUID = data.chatUUID,
                    subscribed = false.toSubscribed(),
                    currentEpisodeId = null
                )

                // If it's a podcast, fetch and load the podcast
                if (feedType.isPodcast()) {
                    scope.launch(dispatchers.mainImmediate) {
                        delay(500L)

                        feedRepository.getPodcastByChatId(chatViewModel.chatId).collect { podcast ->
                            podcast?.let { nnPodcast ->
                                dashboardViewModel.toggleSplitScreen(
                                    true,
                                    DashboardViewModel.SplitContentType.Podcast(chatViewModel.chatId)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

//    private fun retrieveEpisodeDuration(
//        episode: PodcastEpisode
//    ): Long {
//        val duration = episode.localFile?.let {
//            Uri.fromFile(it).getMediaDuration(true)
//        } ?: Uri.parse(episode.episodeUrl).getMediaDuration(false)
//
//        viewModelScope.launch(io) {
//            feedRepository.updateContentEpisodeStatus(
//                feedId = episode.podcastId,
//                itemId = episode.id,
//                FeedItemDuration(duration / 1000),
//                FeedItemDuration(episode.currentTimeSeconds)
//            )
//        }
//
//        return duration
//    }


}