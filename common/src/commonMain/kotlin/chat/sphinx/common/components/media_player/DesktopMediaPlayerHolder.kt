package chat.sphinx.common.components.media_player

import chat.sphinx.concepts.repository.feed.FeedRepository
import chat.sphinx.di.container.SphinxContainer
import chat.sphinx.utils.notifications.createSphinxNotificationManager
import chat.sphinx.wrapper.ItemId
import chat.sphinx.wrapper.feed.FeedId
import chat.sphinx.wrapper.feed.FeedItemDuration
import chat.sphinx.wrapper.lightning.Sat
import kotlinx.coroutines.flow.MutableStateFlow

class DesktopMediaPlayerHolder() {
    val scope = SphinxContainer.appModule.applicationScope
    val dispatchers = SphinxContainer.appModule.dispatchers
    private val sphinxNotificationManager = createSphinxNotificationManager()
    private val feedRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).feedRepository

    private val mediaPlayerController = MediaPlayerControllerImpl()
    var currentState: MediaPlayerServiceState = MediaPlayerServiceState.ServiceInactive
        private set

    private var currentData: PodcastDataHolder? = null

    val mediaState = MutableStateFlow<MediaPlayerServiceState>(MediaPlayerServiceState.ServiceInactive)

    fun getPlayingContent(): Triple<String, String, Boolean>? {
        return currentData?.takeIf { mediaPlayerController.isPlaying() }?.let {
            Triple(it.podcastId, it.episodeId, true)
        }
    }

    suspend fun processUserAction(userAction: UserAction) {
        when (userAction) {
            is UserAction.ServiceAction.Play -> {
                val isNewEpisode = currentData?.episodeId != userAction.contentEpisodeStatus.itemId.value

                val resumeTime = if (
                    currentData?.episodeId == userAction.contentEpisodeStatus.itemId.value
                ) {
                    mediaPlayerController.getCurrentPosition()
                } else {
                    userAction.contentEpisodeStatus.currentTime.value * 1000L
                }

                if (isNewEpisode) {
                    mediaPlayerController.stop()
                    currentData = PodcastDataHolder.instantiate(
                        userAction.chatId,
                        userAction.contentFeedStatus.feedId.value,
                        userAction.contentEpisodeStatus.itemId.value,
                        userAction.contentFeedStatus.satsPerMinute ?: Sat(0),
                        userAction.contentFeedStatus.playerSpeed?.value ?: 1.0,
                        userAction.contentFeedStatus.feedUrl,
                        userAction.contentFeedStatus.subscriptionStatus
                    )
                }

                currentData?.let { mediaPlayerController.setPlaybackSpeed(it.speed) }
                mediaPlayerController.play(
                    userAction.episodeUrl,
                    resumeTime
                )

                feedRepository.updateContentFeedStatus(
                    userAction.contentFeedStatus.feedId,
                    userAction.contentFeedStatus.feedUrl,
                    userAction.contentFeedStatus.subscriptionStatus,
                    userAction.chatId,
                    userAction.contentFeedStatus.itemId,
                    userAction.contentFeedStatus.satsPerMinute,
                    userAction.contentFeedStatus.playerSpeed
                )

                currentState = MediaPlayerServiceState.ServiceActive.MediaState.Playing(
                    userAction.chatId,
                    userAction.contentFeedStatus.feedId.value,
                    userAction.contentEpisodeStatus.itemId.value,
                    mediaPlayerController.getCurrentPosition(),
                    mediaPlayerController.getDuration(),
                    currentData!!.speed
                )
                mediaState.value = currentState
            }

            is UserAction.ServiceAction.Pause -> {
                mediaPlayerController.pause()
                currentState = MediaPlayerServiceState.ServiceActive.MediaState.Paused(
                    userAction.chatId,
                    currentData?.podcastId ?: "",
                    userAction.episodeId,
                    mediaPlayerController.getCurrentPosition(),
                    mediaPlayerController.getDuration(),
                    currentData?.speed ?: 1.0
                )
                mediaState.value = currentState

                feedRepository.updateContentEpisodeStatus(
                    FeedId(currentData?.podcastId ?: ""),
                    FeedId(currentData?.episodeId!!),
                    FeedItemDuration(mediaPlayerController.getDuration() / 1000),
                    FeedItemDuration(value = mediaPlayerController.getCurrentPosition() / 1000)
                )
            }

            is UserAction.ServiceAction.Seek -> {
                mediaPlayerController.seekTo(userAction.contentEpisodeStatus.currentTime.value * 1000L)

                feedRepository.updateContentEpisodeStatus(
                    userAction.contentEpisodeStatus.feedId,
                    userAction.contentEpisodeStatus.itemId,
                    userAction.contentEpisodeStatus.duration,
                    userAction.contentEpisodeStatus.currentTime
                )
            }

            is UserAction.AdjustSpeed -> {
                currentData?.setSpeed(userAction.contentFeedStatus.playerSpeed?.value ?: 1.0)?.also {
                    mediaPlayerController.setPlaybackSpeed(it)
                }

                feedRepository.updateContentFeedStatus(
                    userAction.contentFeedStatus.feedId,
                    userAction.contentFeedStatus.feedUrl,
                    userAction.contentFeedStatus.subscriptionStatus,
                    userAction.chatId,
                    userAction.contentFeedStatus.itemId,
                    userAction.contentFeedStatus.satsPerMinute,
                    userAction.contentFeedStatus.playerSpeed
                )
            }

            is UserAction.AdjustSatsPerMinute -> {
                currentData?.setSatsPerMinute(userAction.contentFeedStatus.satsPerMinute ?: Sat(0))

                feedRepository.updateContentFeedStatus(
                    userAction.contentFeedStatus.feedId,
                    userAction.contentFeedStatus.feedUrl,
                    userAction.contentFeedStatus.subscriptionStatus,
                    userAction.chatId,
                    userAction.contentFeedStatus.itemId,
                    userAction.contentFeedStatus.satsPerMinute,
                    userAction.contentFeedStatus.playerSpeed
                )
            }

            is UserAction.SetPaymentsDestinations -> {
                currentData?.setDestinations(userAction.destinations)
            }

            is UserAction.SendBoost,
            is UserAction.TrackPodcastConsumed -> {
                // NO-OP: Ignored as per instruction
            }
        }
    }

    fun clear() {
        currentState = MediaPlayerServiceState.ServiceInactive
        mediaPlayerController.stop()
        currentData = null
    }

    fun getCurrentPlaybackPosition(): Long {
        return mediaPlayerController.getCurrentPosition()
    }

    fun getTotalDuration(): Long {
        return mediaPlayerController.getDuration()
    }
}
