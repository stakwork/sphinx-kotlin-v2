package chat.sphinx.common.components.media_player

import chat.sphinx.concepts.repository.feed.FeedRepository
import chat.sphinx.wrapper.lightning.Sat

class DesktopMediaPlayerHolder(
    private val feedRepository: FeedRepository
) {
    private val mediaPlayerController = MediaPlayerControllerImpl()
    var currentState: MediaPlayerServiceState = MediaPlayerServiceState.ServiceInactive
        private set

    private var currentData: PodcastDataHolder? = null

    fun getPlayingContent(): Triple<String, String, Boolean>? {
        return currentData?.takeIf { mediaPlayerController.isPlaying() }?.let {
            Triple(it.podcastId, it.episodeId, true)
        }
    }

    suspend fun processUserAction(userAction: UserAction) {
        when (userAction) {
            is UserAction.ServiceAction.Play -> {
                currentData = PodcastDataHolder.instantiate(
                    userAction.chatId,
                    userAction.contentFeedStatus.feedId.value,
                    userAction.contentEpisodeStatus.itemId.value,
                    userAction.contentFeedStatus.satsPerMinute ?: Sat(0),
                    userAction.contentFeedStatus.playerSpeed?.value ?: 1.0,
                    userAction.contentFeedStatus.feedUrl,
                    userAction.contentFeedStatus.subscriptionStatus
                )

                mediaPlayerController.setPlaybackSpeed(currentData!!.speed)
                mediaPlayerController.play(
                    userAction.episodeUrl,
                    userAction.contentEpisodeStatus.currentTime.value * 1000L
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
}
