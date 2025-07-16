package chat.sphinx.common.viewmodel

import chat.sphinx.common.state.ConfirmationType
import chat.sphinx.di.container.SphinxContainer
import chat.sphinx.generated.ApiConfig
import chat.sphinx.utils.notifications.createSphinxNotificationManager
import chat.sphinx.wrapper.dashboard.ChatId
import chat.sphinx.wrapper.feed.FeedId
import chat.sphinx.wrapper.feed.FeedTitle
import chat.sphinx.wrapper.feed.Subscribed
import chat.sphinx.wrapper.feed.generateFeedItemLink
import chat.sphinx.wrapper.podcast.ChapterResponseDto
import chat.sphinx.wrapper.podcast.Podcast
import chat.sphinx.wrapper.podcast.PodcastEpisode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PodcastViewModel(
    private val chatId: ChatId?,
    private val feedId: FeedId?
) {
    private val scope = SphinxContainer.appModule.applicationScope
    private val dispatchers = SphinxContainer.appModule.dispatchers
    private val sphinxNotificationManager = createSphinxNotificationManager()
    private val feedRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).feedRepository

    private val _podcastState = MutableStateFlow<Podcast?>(null)
    val podcastState: StateFlow<Podcast?> = _podcastState.asStateFlow()

    private val _playingEpisodeTime = MutableStateFlow(0L)
    val playingEpisodeTime: StateFlow<Long> get() = _playingEpisodeTime

    private val _isSkipAdsEnabled = MutableStateFlow(true)
    val isSkipAdsEnabled: StateFlow<Boolean> = _isSkipAdsEnabled

    private val _episodeChapters = MutableStateFlow<Map<String, ChapterResponseDto>>(emptyMap())
    val episodeChapters: StateFlow<Map<String, ChapterResponseDto>> = _episodeChapters.asStateFlow()

    init {
        observePodcast()
    }

    fun toggleSkipAds() {
        _isSkipAdsEnabled.value = !_isSkipAdsEnabled.value
    }

    private fun observePodcast() {
        scope.launch(dispatchers.mainImmediate) {
            val podcastFlow = when {
                chatId != null && chatId.value.toInt() != ChatId.NULL_CHAT_ID -> feedRepository.getPodcastByChatId(chatId)
                feedId != null -> feedRepository.getPodcastById(feedId)
                else -> flowOf(null)
            }

            podcastFlow.collect { updatedPodcast ->
                _podcastState.value = updatedPodcast

                updatedPodcast?.episodes?.forEach { episode ->
                    episode.chapters?.takeIf { it.nodes.isNotEmpty() }?.let { chapterData ->
                        _episodeChapters.value += (episode.id.value to chapterData)
                    }
                }
            }
        }
    }

    fun setPlayingTime(timeMs: Long) {
        _playingEpisodeTime.value = timeMs
    }

    fun clearPlayingState() {
        _playingEpisodeTime.value = 0L
    }

    fun refreshPodcast(podcastEpisode: PodcastEpisode) {
        scope.launch(dispatchers.mainImmediate) {
            val podcastFlow = when {
                chatId != null && chatId.value.toInt() != ChatId.NULL_CHAT_ID -> feedRepository.getPodcastByChatId(chatId)
                feedId != null -> feedRepository.getPodcastById(feedId)
                else -> flowOf(null)
            }

            podcastFlow.collect { updatedPodcast ->
                _podcastState.value = updatedPodcast

                val updatedEpisode = updatedPodcast?.episodes?.find { it.id == podcastEpisode.id }
                updatedEpisode?.chapters?.let { chapterData ->
                    _episodeChapters.value += (podcastEpisode.id.value to chapterData)
                }
            }
        }
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
            val workflowId = ApiConfig.WORKFLOW_ID
            val token = ApiConfig.CHAPTERS_TOKEN

            if (podcastEpisode.referenceId != null) {
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

            delay(3000L)
            refreshPodcast(podcastEpisode)
        }
    }

    fun toggleSubscribeState(feedId: FeedId?, subscribed: Subscribed?) {
        if (feedId == null || subscribed == null) return
        scope.launch(dispatchers.mainImmediate) {
            feedRepository.toggleFeedSubscribeState(
                feedId,
                subscribed
            )
        }
    }
}
