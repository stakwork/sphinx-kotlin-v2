package chat.sphinx.common.viewmodel

import chat.sphinx.common.state.ConfirmationType
import chat.sphinx.di.container.SphinxContainer
import chat.sphinx.utils.notifications.createSphinxNotificationManager
import chat.sphinx.wrapper.dashboard.ChatId
import chat.sphinx.wrapper.feed.FeedId
import chat.sphinx.wrapper.feed.FeedTitle
import chat.sphinx.wrapper.feed.generateFeedItemLink
import chat.sphinx.wrapper.podcast.Podcast
import chat.sphinx.wrapper.podcast.PodcastEpisode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
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

    suspend fun buildPodcastShareConfirmation(episodeId: FeedId): ConfirmationType.PodcastShare? {
        val podcast = podcastState.value ?: return null
        val episode = podcast.episodes.find { it.id == episodeId } ?: return null
        val feed = feedRepository.getFeedById(podcast.id).firstOrNull() ?: return null

        val fromBeginning = generateFeedItemLink(
            feedUrl = feed.feedUrl,
            feedId = feed.id,
            itemId = episode.id,
            atTime = null
        )

        val atTimeSeconds = episode.contentEpisodeStatus?.currentTime?.value
        val fromCurrentTime = generateFeedItemLink(
            feedUrl = feed.feedUrl,
            feedId = feed.id,
            itemId = episode.id,
            atTime = atTimeSeconds
        )

        return ConfirmationType.PodcastShare(
            fromBeginningLink = fromBeginning,
            fromCurrentTimeLink = fromCurrentTime
        )
    }

    fun getChapters(podcastEpisode: PodcastEpisode, title: FeedTitle) {
        scope.launch(dispatchers.mainImmediate) {
            val workflowId = 37159
            val token = "690d8f037df0fdb002836edfddf4b626"

            if (false) {
                feedRepository.getChaptersData(
                    podcastEpisode,
                    title,
                    podcastEpisode.referenceId!!,
                    podcastEpisode.id,
                    workflowId,
                    token
                )
            } else {
                feedRepository.checkIfEpisodeNodeExists(
                    podcastEpisode,
                    title,
                    workflowId,
                    token
                )
            }
        }
    }
}
