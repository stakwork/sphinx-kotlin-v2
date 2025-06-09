package chat.sphinx.common.viewmodel

import chat.sphinx.di.container.SphinxContainer
import chat.sphinx.utils.notifications.createSphinxNotificationManager
import chat.sphinx.wrapper.dashboard.ChatId
import chat.sphinx.wrapper.feed.FeedId
import chat.sphinx.wrapper.podcast.Podcast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PodcastViewModel(
    private val chatId: ChatId
) {
    val scope = SphinxContainer.appModule.applicationScope
    val dispatchers = SphinxContainer.appModule.dispatchers
    private val sphinxNotificationManager = createSphinxNotificationManager()
    private val feedRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).feedRepository

    private val _podcastState = MutableStateFlow<Podcast?>(null)
    val podcastState: StateFlow<Podcast?> = _podcastState.asStateFlow()

    private val _playingEpisodeTime = MutableStateFlow(0L)
    val playingEpisodeTime: StateFlow<Long> get() = _playingEpisodeTime

    init {
        observePodcast()
    }

    private fun observePodcast() {
        scope.launch(dispatchers.mainImmediate) {
            feedRepository.getPodcastByChatId(chatId).collect { updatedPodcast ->
                _podcastState.value = updatedPodcast
            }
        }
    }

    fun setPlayingTime(timeMs: Long) {
        _playingEpisodeTime.value = timeMs
    }

    fun refreshPodcast() {
        // Optional: add explicit refresh logic
    }
}
