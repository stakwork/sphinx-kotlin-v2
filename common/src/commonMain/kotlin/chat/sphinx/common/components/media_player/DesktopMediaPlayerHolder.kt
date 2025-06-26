package chat.sphinx.common.components.media_player

import MediaPlayerControllerImpl
import chat.sphinx.di.container.SphinxContainer
import chat.sphinx.utils.notifications.createSphinxNotificationManager
import chat.sphinx.wrapper.contact.Contact
import chat.sphinx.wrapper.dashboard.ChatId
import chat.sphinx.wrapper.feed.FeedId
import chat.sphinx.wrapper.feed.FeedItemDuration
import chat.sphinx.wrapper.feed.toFeedPlayerSpeed
import chat.sphinx.wrapper.lightning.Sat
import chat.sphinx.wrapper.message.FeedBoost
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DesktopMediaPlayerHolder {
    val scope = SphinxContainer.appModule.applicationScope
    val dispatchers = SphinxContainer.appModule.dispatchers
    private val sphinxNotificationManager = createSphinxNotificationManager()
    private val feedRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).feedRepository
    private val contactRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).contactRepository
    private val messageRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).messageRepository

    private val mediaPlayerController = MediaPlayerControllerImpl()
    private var currentData: PodcastDataHolder? = null

    private val _mediaState = MutableStateFlow<MediaPlayerServiceState>(MediaPlayerServiceState.ServiceInactive)
    val mediaState: StateFlow<MediaPlayerServiceState> get() = _mediaState

    init {
        mediaPlayerController.setPlaybackListener(object : MediaPlayerController.PlaybackListener {
            override fun onPlaybackCompleted() {
                currentData?.let { data ->
                    scope.launch(dispatchers.io) {
                        feedRepository.updatePlayedMark(FeedId(data.episodeId), played = true)
                    }
                }
            }
        })
    }

    fun getPlayingContent(): Triple<String, String, Boolean>? {
        return currentData?.takeIf { mediaPlayerController.isPlaying() }?.let {
            Triple(it.podcastId, it.episodeId, true)
        }
    }

    private suspend fun getOwner(): Contact {
        return contactRepository.accountOwner.value.let { contact ->
            if (contact != null) {
                contact
            } else {
                var resolvedOwner: Contact? = null
                try {
                    contactRepository.accountOwner.collect { ownerContact ->
                        if (ownerContact != null) {
                            resolvedOwner = ownerContact
                            throw Exception()
                        }
                    }
                } catch (e: Exception) {
                }
                delay(25L)

                resolvedOwner!!
            }
        }
    }


    suspend fun processUserAction(userAction: UserAction) {
        when (userAction) {
            is UserAction.ServiceAction.Play -> handlePlay(userAction)
            is UserAction.ServiceAction.Pause -> handlePause(userAction)
            is UserAction.ServiceAction.Seek -> handleSeek(userAction)
            is UserAction.AdjustSpeed -> handleSpeedChange(userAction)
            is UserAction.AdjustSatsPerMinute -> handleSatsChange(userAction)
            is UserAction.SetPaymentsDestinations -> currentData?.setDestinations(userAction.destinations)
            is UserAction.SendBoost -> handleSendBoost(userAction)
            is UserAction.TrackPodcastConsumed -> Unit
        }
    }

    private var episodeProgressJob: Job? = null

    private fun startProgressSync(chatId: ChatId, feedId: FeedId, episodeId: FeedId) {
        episodeProgressJob?.cancel()
        episodeProgressJob = scope.launch(dispatchers.io) {
            while (_mediaState.value is MediaPlayerServiceState.ServiceActive.MediaState.Playing) {
                val duration = mediaPlayerController.getDuration()
                val position = mediaPlayerController.getCurrentPosition()

                feedRepository.updateContentEpisodeStatus(
                    feedId,
                    episodeId,
                    FeedItemDuration(duration / 1000),
                    FeedItemDuration(position / 1000)
                )

                delay(5000)
            }
        }
    }

    private suspend fun handlePlay(action: UserAction.ServiceAction.Play) {
        val isNewEpisode = currentData?.episodeId != action.contentEpisodeStatus.itemId.value

        val resumeTime = if (!isNewEpisode) {
            mediaPlayerController.getCurrentPosition()
        } else {
            action.contentEpisodeStatus.currentTime.value * 1000L
        }

        if (isNewEpisode) {
            mediaPlayerController.stop()
            currentData = PodcastDataHolder.instantiate(
                action.chatId,
                action.contentFeedStatus.feedId.value,
                action.contentEpisodeStatus.itemId.value,
                action.contentFeedStatus.satsPerMinute ?: Sat(0),
                action.contentFeedStatus.playerSpeed?.value ?: 1.0,
                action.contentFeedStatus.feedUrl,
                action.contentFeedStatus.subscriptionStatus
            ).also {
                it.setDestinations(action.destinations)
            }
        }

        mediaPlayerController.setPlaybackSpeed(currentData!!.speed)
        mediaPlayerController.play(action.episodeUrl, resumeTime)

        startProgressSync(
            action.chatId,
            action.contentFeedStatus.feedId,
            action.contentEpisodeStatus.itemId
        )

        feedRepository.updateContentFeedStatus(
            action.contentFeedStatus.feedId,
            action.contentFeedStatus.feedUrl,
            action.contentFeedStatus.subscriptionStatus,
            action.chatId,
            action.contentFeedStatus.itemId,
            action.contentFeedStatus.satsPerMinute,
            action.contentFeedStatus.playerSpeed
        )

        feedRepository.updateContentEpisodeStatus(
            action.contentEpisodeStatus.feedId,
            action.contentEpisodeStatus.itemId,
            action.contentEpisodeStatus.duration,
            action.contentEpisodeStatus.currentTime
        )

        _mediaState.value = MediaPlayerServiceState.ServiceActive.MediaState.Playing(
            action.chatId,
            action.contentFeedStatus.feedId.value,
            action.contentEpisodeStatus.itemId.value,
            mediaPlayerController.getCurrentPosition(),
            mediaPlayerController.getDuration(),
            currentData!!.speed
        )
        startStreamingPayments()
    }

    private suspend fun handlePause(action: UserAction.ServiceAction.Pause) {
        mediaPlayerController.pause()
        episodeProgressJob?.cancel()
        episodeProgressJob = null
        val episodeId = currentData?.episodeId ?: return

        _mediaState.value = MediaPlayerServiceState.ServiceActive.MediaState.Paused(
            action.chatId,
            currentData?.podcastId ?: "",
            action.episodeId,
            mediaPlayerController.getCurrentPosition(),
            mediaPlayerController.getDuration(),
            currentData?.speed ?: 1.0
        )

        feedRepository.updateContentEpisodeStatus(
            FeedId(currentData?.podcastId ?: ""),
            FeedId(episodeId),
            FeedItemDuration(mediaPlayerController.getDuration() / 1000),
            FeedItemDuration(value = mediaPlayerController.getCurrentPosition() / 1000)
        )
        stopStreamingPayments()
    }

    private suspend fun handleSeek(action: UserAction.ServiceAction.Seek) {
        mediaPlayerController.seekTo(action.contentEpisodeStatus.currentTime.value * 1000L)

        feedRepository.updateContentEpisodeStatus(
            action.contentEpisodeStatus.feedId,
            action.contentEpisodeStatus.itemId,
            action.contentEpisodeStatus.duration,
            action.contentEpisodeStatus.currentTime
        )
    }

    private suspend fun handleSpeedChange(action: UserAction.AdjustSpeed) {
        currentData?.setSpeed(action.contentFeedStatus.playerSpeed?.value ?: 1.0)?.also {
            mediaPlayerController.setPlaybackSpeed(it)
        }

        feedRepository.updateContentFeedStatus(
            action.contentFeedStatus.feedId,
            action.contentFeedStatus.feedUrl,
            action.contentFeedStatus.subscriptionStatus,
            action.chatId,
            action.contentFeedStatus.itemId,
            action.contentFeedStatus.satsPerMinute,
            action.contentFeedStatus.playerSpeed
        )
    }

    private suspend fun handleSatsChange(action: UserAction.AdjustSatsPerMinute) {
        currentData?.setSatsPerMinute(action.contentFeedStatus.satsPerMinute ?: Sat(0))

        feedRepository.updateContentFeedStatus(
            action.contentFeedStatus.feedId,
            action.contentFeedStatus.feedUrl,
            action.contentFeedStatus.subscriptionStatus,
            action.chatId,
            action.contentFeedStatus.itemId,
            action.contentFeedStatus.satsPerMinute,
            action.contentFeedStatus.playerSpeed
        )
    }

    private suspend fun handleSendBoost(action: UserAction.SendBoost) {
        val owner = getOwner()

        // Use tipAmount from owner to determine if we should send a boost
        owner.tipAmount?.let { tip ->
            if (tip.value > 0) {
                val podcastId = action.podcastId
                val contentFeedStatus = action.contentFeedStatus ?: return
                val itemId = contentFeedStatus.itemId ?: FeedId(currentData?.episodeId ?: return)
                val currentTime = action.contentEpisodeStatus.currentTime.value

                val feedBoost = FeedBoost(
                    FeedId(podcastId),
                    itemId,
                    currentTime.toInt(),
                    tip
                )

                messageRepository.sendBoost(
                    action.chatId,
                    feedBoost
                )

                // Stream payment to destinations
                contentFeedStatus.itemId?.value?.let { itemIdValue ->
                    feedRepository.streamFeedPayments(
                        action.chatId,
                        podcastId,
                        itemIdValue,
                        currentTime,
                        tip,
                        contentFeedStatus.playerSpeed,
                        action.destinations
                    )
                }
            }
        }
    }

    private var streamingPaymentJob: Job? = null

    private fun startStreamingPayments() {
        streamingPaymentJob?.cancel()
        streamingPaymentJob = scope.launch(dispatchers.io) {
            val data = currentData ?: return@launch

            // Wait for player to start (max 2 seconds total)
            repeat(20) {
                if (mediaPlayerController.isPlaying()) return@repeat
                delay(100)
            }

            if (!mediaPlayerController.isPlaying()) {
                // Playback never started
                return@launch
            }

            val speed = data.speed
            val intervalMs = (60_000.0 / speed).toLong()
            var lastStreamTime = System.currentTimeMillis()

            while (isActive && mediaPlayerController.isPlaying()) {
                val now = System.currentTimeMillis()

                if (now - lastStreamTime >= intervalMs) {
                    val currentTimeSeconds = mediaPlayerController.getCurrentPosition() / 1000

                    feedRepository.streamFeedPayments(
                        data.chatId,
                        data.podcastId,
                        data.episodeId,
                        currentTimeSeconds,
                        data.satsPerMinute,
                        data.speed.toFeedPlayerSpeed(),
                        data.destinations
                    )

                    lastStreamTime = now
                }

                delay(1000L)
            }
        }
    }


    private fun stopStreamingPayments() {
        streamingPaymentJob?.cancel()
        streamingPaymentJob = null
    }


    fun clear() {
        mediaPlayerController.stop()
        stopStreamingPayments()
        currentData = null
        _mediaState.value = MediaPlayerServiceState.ServiceInactive
    }

    fun getCurrentPlaybackPosition(): Long = mediaPlayerController.getCurrentPosition()

    fun getTotalDuration(): Long = mediaPlayerController.getDuration()
}
