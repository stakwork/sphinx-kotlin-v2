package chat.sphinx.common.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimary
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.sphinx.common.Res
import chat.sphinx.common.components.media_player.DesktopMediaPlayerHolder
import chat.sphinx.common.components.media_player.MediaPlayerServiceState
import chat.sphinx.common.components.media_player.UserAction
import chat.sphinx.common.state.ContentState.scope
import chat.sphinx.common.viewmodel.DashboardViewModel
import chat.sphinx.common.viewmodel.PodcastViewModel
import chat.sphinx.wrapper.dashboard.ChatId
import chat.sphinx.wrapper.feed.FeedItemDuration
import chat.sphinx.wrapper.feed.isTrue
import chat.sphinx.wrapper.lightning.toSat
import chat.sphinx.wrapper.podcast.NodeDto
import chat.sphinx.wrapper.podcast.PodcastEpisode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import theme.primary_blue
import theme.primary_green
import kotlin.math.roundToInt

@Composable
fun PodcastMainPlayer(
    chatId: ChatId,
    mediaPlayerHolder: DesktopMediaPlayerHolder,
    dashboardViewModel: DashboardViewModel,
    podcastViewModel: PodcastViewModel

) {
    val podcastState by podcastViewModel.podcastState.collectAsState()

    val podcast = podcastState ?: run {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = primary_blue)
        }
        return
    }

    val episode = podcast.getCurrentEpisode()

    val scope = rememberCoroutineScope()
    val mediaState by mediaPlayerHolder.mediaState.collectAsState()
    var isPlaying by remember { mutableStateOf(false) }

    val isPlayingThisPodcast = remember(mediaState, podcast) {
        val currentPodcastId = podcast.id.value
        when (val state = mediaState) {
            is MediaPlayerServiceState.ServiceActive.MediaState.Playing -> state.podcastId == currentPodcastId
            is MediaPlayerServiceState.ServiceActive.MediaState.Paused -> state.podcastId == currentPodcastId
            is MediaPlayerServiceState.ServiceActive.MediaState.Ended  -> state.podcastId == currentPodcastId
            is MediaPlayerServiceState.ServiceActive.MediaState.Failed -> state.podcastId == currentPodcastId
            else -> false
        }
    }

    LaunchedEffect(mediaState) {
        isPlaying = mediaState is MediaPlayerServiceState.ServiceActive.MediaState.Playing
    }

    var currentTime by remember {
        mutableStateOf((episode.contentEpisodeStatus?.currentTime?.value ?: 0L) * 1000)
    }
    var duration by remember {
        mutableStateOf((episode.contentEpisodeStatus?.duration?.value ?: 0L) * 1000)
    }

    val progress = if (isPlayingThisPodcast && duration > 0) {
        (currentTime.toFloat() / duration.toFloat())
    } else {
        0f
    }

    var sliderPosition by remember { mutableStateOf(progress * 100f) }
    var isUserSeeking by remember { mutableStateOf(false) }
    var satsSliderPosition by remember { mutableStateOf(podcast.satsPerMinute.toFloat()) }
    var isAdjustingSats by remember { mutableStateOf(false) }
    var expandedEpisodeId by remember { mutableStateOf<String?>(null) }
    val isSkipAdsEnabled by podcastViewModel.isSkipAdsEnabled.collectAsState()
    val chaptersMap by podcastViewModel.episodeChapters.collectAsState()

    // Live playback time updater
    LaunchedEffect(episode) {
        val savedTime = (episode.contentEpisodeStatus?.currentTime?.value ?: 0L) * 1000
        val savedDuration = (episode.contentEpisodeStatus?.duration?.value ?: 0L) * 1000

        currentTime = savedTime
        duration = savedDuration

        if (!isUserSeeking && savedDuration > 0) {
            sliderPosition = (savedTime.toFloat() / savedDuration.toFloat()) * 100f
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {

            currentTime = mediaPlayerHolder.getCurrentPlaybackPosition()
            duration = mediaPlayerHolder.getTotalDuration()

            podcastViewModel.setPlayingTime(currentTime)

            while (isPlaying) {
                delay(1000)

                currentTime = mediaPlayerHolder.getCurrentPlaybackPosition()
                duration = mediaPlayerHolder.getTotalDuration()

                podcastViewModel.setPlayingTime(currentTime)

                if (!isUserSeeking && duration > 0) {
                    sliderPosition = (currentTime.toFloat() / duration.toFloat()) * 100f
                }
            }
        }
    }

    LaunchedEffect(currentTime) {
        if (!isSkipAdsEnabled || !isPlaying) return@LaunchedEffect

        val chapters = chaptersMap[episode.id.value]?.nodes
            ?.mapNotNull { it.properties }
            ?.filter { !it.timestamp.isNullOrBlank() }
            ?.sortedBy { parseTimestampToMillis(it.timestamp!!) }
            ?: return@LaunchedEffect

        for (i in chapters.indices) {
            val chapterStart = parseTimestampToMillis(chapters[i].timestamp!!)
            val chapterEnd = if (i + 1 < chapters.size)
                parseTimestampToMillis(chapters[i + 1].timestamp!!)
            else
                duration

            if (chapters[i].isAdBoolean && currentTime in chapterStart until chapterEnd) {

                val nextNonAd = chapters.drop(i + 1).firstOrNull { !it.isAdBoolean }
                val skipTo = nextNonAd?.timestamp?.let { parseTimestampToMillis(it) } ?: chapterEnd

                val updatedStatus = podcast.getUpdatedContentEpisodeStatus().copy(
                    currentTime = FeedItemDuration(skipTo / 1000L)
                )

                mediaPlayerHolder.processUserAction(
                    UserAction.ServiceAction.Seek(chatId, updatedStatus)
                )

                break
            }
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.onSurfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
        ) {
            PhotoUrlImage(
                photoUrl = podcast.imageToShow,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                // Subscribe Button (Only if not subscribed)
                if (!podcast.subscribed.isTrue()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Button(
                            onClick = {
                                podcastViewModel.toggleSubscribeState(podcast.id, podcast.subscribed)
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                "SUBSCRIBE",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

            }

            if (podcast.hasDestinations) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sats per minute", fontSize = 12.sp, color = Color.White)
                        Text(satsSliderPosition.toInt().toString(), fontSize = 12.sp, color = Color.White)
                    }

                    Slider(
                        value = satsSliderPosition,
                        onValueChange = {
                            isAdjustingSats = true
                            satsSliderPosition = it
                        },
                        onValueChangeFinished = {
                            isAdjustingSats = false
                            val newSats = satsSliderPosition.toLong()
                            if (newSats != podcast.satsPerMinute) {
                                podcast.didChangeSatsPerMinute(newSats)
                                scope.launch {
                                    mediaPlayerHolder.processUserAction(
                                        UserAction.AdjustSatsPerMinute(
                                            chatId,
                                            podcast.getUpdatedContentFeedStatus(
                                                customAmount = newSats.toSat()
                                            )
                                        )
                                    )
                                }
                            }
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = primary_blue,
                            activeTrackColor = primary_blue,
                            inactiveTrackColor = Color.Gray
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = episode.title.value,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            var sliderWidthPx by remember { mutableStateOf(1) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .padding(horizontal = 16.dp)
                    .onGloballyPositioned { coords -> sliderWidthPx = coords.size.width }
            ) {
                // Playback Slider
                Slider(
                    value = sliderPosition,
                    onValueChange = { isUserSeeking = true; sliderPosition = it },
                    onValueChangeFinished = {
                        val newPositionMillis = (duration * (sliderPosition / 100f)).toLong()
                        val updatedStatus = podcast.getUpdatedContentEpisodeStatus().copy(
                            currentTime = FeedItemDuration(newPositionMillis / 1000L)
                        )
                        scope.launch {
                            mediaPlayerHolder.processUserAction(UserAction.ServiceAction.Seek(chatId, updatedStatus))
                            currentTime = newPositionMillis
                        }
                        isUserSeeking = false
                    },
                    valueRange = 0f..100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterStart),
                    colors = SliderDefaults.colors(
                        thumbColor = primary_blue,
                        activeTrackColor = primary_blue,
                        inactiveTrackColor = Color.Gray
                    )
                )

                // Chapter Markers
                val chapterMarkers = chaptersMap[episode.id.value]?.nodes
                    ?.mapNotNull { it.properties }
                    ?.filter { !it.timestamp.isNullOrBlank() }
                    ?.map {
                        val positionMs = parseTimestampToMillis(it.timestamp!!)
                        val ratio = positionMs.toFloat() / duration.coerceAtLeast(1L)
                        Triple(ratio, it.isAdBoolean, it.timestamp!!)
                    } ?: emptyList()

                val density = LocalDensity.current
                val trackPaddingPx = with(density) { 16.dp.roundToPx() }
                val trackWidthPx   = sliderWidthPx - trackPaddingPx * 2

                chapterMarkers.forEach { (ratio, isAd, _) ->
                    val dotRadiusPx = with(density) { (6.dp).roundToPx() }
                    val positionPx = (trackWidthPx * ratio).roundToInt() + trackPaddingPx
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(positionPx - dotRadiusPx, 0) }
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isAd) Color.Gray else Color.White)
                            .align(Alignment.CenterStart)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isPlayingThisPodcast) formatMillis(currentTime) else "0:00",
                    fontSize = 12.sp,
                    color = primary_blue
                )
                Text(
                    text = formatMillis(duration),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val currentSpeed = podcast.getUpdatedContentFeedStatus().playerSpeed?.value?.toFloat() ?: 1f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            PlaybackSpeedSelector(
                currentSpeed = currentSpeed,
                onSpeedChange = { speed ->
                    scope.launch {
                        podcast.updateSpeed(speed.toDouble())
                        mediaPlayerHolder.processUserAction(
                            UserAction.AdjustSpeed(
                                chatId,
                                podcast.getUpdatedContentFeedStatus()
                            )
                        )
                    }
                }
            )
        }


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { /* Disabled */ },
                enabled = false
            ) {
                Icon(
                    Icons.Filled.FormatQuote,
                    contentDescription = "Quote",
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(onClick = {
                scope.launch {
                    val newTime = (currentTime - 10_000).coerceAtLeast(0L)
                    val updatedEpisodeStatus = podcast.getUpdatedContentEpisodeStatus().copy(
                        currentTime = FeedItemDuration(newTime / 1000L)
                    )
                    mediaPlayerHolder.processUserAction(
                        UserAction.ServiceAction.Seek(
                            chatId,
                            updatedEpisodeStatus
                        )
                    )
                    currentTime = newTime
                }
            }) {
                Icon(Icons.Filled.Replay10, "Rewind 10s", tint = Color.Gray, modifier = Modifier.size(28.dp))
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(primary_blue, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val isPausedThisEpisode = mediaState is MediaPlayerServiceState.ServiceActive.MediaState.Paused &&
                        (mediaState as MediaPlayerServiceState.ServiceActive.MediaState.Paused).podcastId == podcast.id.value

                IconButton(onClick = {
                    scope.launch {
                        val shouldResume = mediaState is MediaPlayerServiceState.ServiceActive.MediaState.Paused &&
                                (mediaState as MediaPlayerServiceState.ServiceActive.MediaState.Paused).podcastId == podcast.id.value

                        val shouldRestart = mediaState is MediaPlayerServiceState.ServiceActive.MediaState.Ended ||
                                mediaState is MediaPlayerServiceState.ServiceActive.MediaState.Failed

                        if (isPlayingThisPodcast && !shouldResume && !shouldRestart) {
                            // Pause only if currently playing and not in Paused or Ended state
                            mediaPlayerHolder.processUserAction(
                                UserAction.ServiceAction.Pause(chatId, episode.id.value)
                            )
                        } else {
                            // Always allow play/resume from paused or ended
                            val resumeFromTime = currentTime

                            podcast.willStartPlayingEpisode(
                                episodeId = episode.id.value,
                                time = resumeFromTime.toInt(),
                                duration = duration
                            )

                            podcastViewModel.getChapters(episode, podcast.title)

                            mediaPlayerHolder.processUserAction(
                                UserAction.ServiceAction.Play(
                                    chatId = chatId,
                                    episodeUrl = episode.episodeUrl,
                                    contentFeedStatus = podcast.getUpdatedContentFeedStatus(),
                                    contentEpisodeStatus = episode.getUpdatedContentEpisodeStatus().copy(
                                        currentTime = FeedItemDuration(resumeFromTime / 1000L)
                                    ),
                                    destinations = podcast.getFeedDestinations()
                                )
                            )
                        }
                    }
                })
                {
                    val isPlayingThisEpisode = mediaState is MediaPlayerServiceState.ServiceActive.MediaState.Playing &&
                            (mediaState as MediaPlayerServiceState.ServiceActive.MediaState.Playing).podcastId == podcast.id.value

                    Icon(
                        imageVector = if (isPlayingThisEpisode) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlayingThisEpisode) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            IconButton(onClick = {
                scope.launch {
                    val newTime = (currentTime + 30_000).coerceAtMost(duration)
                    val updatedEpisodeStatus = podcast.getUpdatedContentEpisodeStatus().copy(
                        currentTime = FeedItemDuration(newTime / 1000L)
                    )
                    mediaPlayerHolder.processUserAction(
                        UserAction.ServiceAction.Seek(
                            chatId,
                            updatedEpisodeStatus
                        )
                    )
                    currentTime = newTime
                }
            }) {
                Icon(Icons.Filled.Forward30, "Forward 30s", tint = Color.Gray, modifier = Modifier.size(28.dp))
            }
            val boostEnabled = podcast.hasDestinations

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (boostEnabled) primary_green.copy(alpha = 0.5f)
                        else Color.Gray.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                    .then(
                        if (boostEnabled)
                            Modifier.clickable {
                                scope.launch {
                                    mediaPlayerHolder.processUserAction(
                                        UserAction.SendBoost(
                                            chatId,
                                            podcast.id.value,
                                            podcast.getUpdatedContentFeedStatus(),
                                            podcast.getUpdatedContentEpisodeStatus(),
                                            podcast.getFeedDestinations()
                                        )
                                    )
                                }
                            }
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_boost),
                    contentDescription = "Boost",
                    modifier = Modifier
                        .size(28.dp)
                        .padding(2.dp),
                    contentScale = ContentScale.Inside,
                    colorFilter = ColorFilter.tint(Color.White.copy(alpha = if (boostEnabled) 1f else 0.4f))
                )
            }
        }
        val hasChapters = episode.chapters?.nodes?.isNotEmpty() == true

        PodcastEpisodesHeader(
            episodesCount = podcast.episodes.size,
            isSkipAdsEnabled = isSkipAdsEnabled,
            onSkipAdsClick = { podcastViewModel.toggleSkipAds() },
            showSkipButton = hasChapters
        )

        val playingTime by podcastViewModel.playingEpisodeTime.collectAsState()

        podcast.episodes.forEach { episode ->
            val isPlayingThisEpisode = mediaState is MediaPlayerServiceState.ServiceActive.MediaState.Playing &&
                    podcast.getCurrentEpisode().id.value == episode.id.value

            val episodeStatus = episode.contentEpisodeStatus

            val episodeDuration = if (isPlayingThisEpisode) duration
            else (episodeStatus?.duration?.value ?: 0L) * 1000

            val episodeCurrentTime = if (isPlayingThisEpisode) playingTime
            else (episodeStatus?.currentTime?.value ?: 0L) * 1000

            PodcastEpisodeItem(
                episode = episode,
                isPlaying = isPlayingThisEpisode,
                isDownloaded = episode.downloaded,
                isPlayed = episode.played,
                isExpanded = expandedEpisodeId == episode.id.value,
                currentTime = episodeCurrentTime,
                duration = episodeDuration,
                onPlayPauseClick = {
                    scope.launch {
                        val isCurrentlyPlayingThisEpisode = mediaState is MediaPlayerServiceState.ServiceActive.MediaState.Playing &&
                                podcast.getCurrentEpisode()?.id?.value == episode.id.value

                        if (isCurrentlyPlayingThisEpisode) {
                            mediaPlayerHolder.processUserAction(
                                UserAction.ServiceAction.Pause(chatId, episode.id.value)
                            )
                        } else {
                            podcast.willStartPlayingEpisode(
                                episodeId = episode.id.value,
                                time = 0,
                                duration = duration
                            )

                            podcastViewModel.getChapters(episode, podcast.title)

                            mediaPlayerHolder.processUserAction(
                                UserAction.ServiceAction.Play(
                                    chatId = chatId,
                                    episodeUrl = episode.episodeUrl,
                                    contentFeedStatus = podcast.getUpdatedContentFeedStatus(),
                                    contentEpisodeStatus = episode.getUpdatedContentEpisodeStatus(),
                                    destinations = podcast.getFeedDestinations()
                                )
                            )
                        }
                    }
                },
                onDownloadClick = { /* implement as needed */ },
                onShareClick = {
                    scope.launch {
                        podcastViewModel.buildPodcastShareConfirmation(episode.id)?.let { confirmation ->
                            dashboardViewModel.toggleConfirmationWindow(true, confirmation)
                        }
                    }
                },
                onMoreOptionsClick = {
                    scope.launch {
                        val episodeShare = podcastViewModel.buildPodcastShareConfirmation(episode.id)

                        dashboardViewModel.showFullScreenView(
                            DashboardViewModel.FullScreenView.EpisodeDetails(
                                episode,
                                podcast.title.value,
                                episodeShare
                            )
                        )
                    }
                },
                onToggleChaptersClick = {
                    expandedEpisodeId = if (expandedEpisodeId == episode.id.value) null else episode.id.value
                },
                mediaPlayerHolder = mediaPlayerHolder,
                chatId = chatId,
                chapterNodes = chaptersMap[episode.id.value]?.nodes
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun PlaybackSpeedSelector(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit
) {
    val speeds = listOf(0.5f, 0.8f, 1f, 1.2f, 1.5f, 2.1f)
    var expanded by remember { mutableStateOf(false) }

    fun formatSpeed(speed: Float): String =
        if (speed == 1f) "1x" else "${speed}x"

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = formatSpeed(currentSpeed),
            color = Color.White,
            modifier = Modifier
                .clickable { expanded = true }
                .padding(bottom = 4.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.DarkGray)
        ) {
            speeds.forEach { speed ->
                DropdownMenuItem(
                    onClick = {
                        onSpeedChange(speed)
                        expanded = false
                    },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = formatSpeed(speed), color = Color.White)
                            if (speed == currentSpeed) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PodcastEpisodesHeader(
    episodesCount: Int,
    isSkipAdsEnabled: Boolean,
    onSkipAdsClick: () -> Unit,
    showSkipButton: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EPISODES",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = episodesCount.toString(),
                style = MaterialTheme.typography.labelLarge.copy(
                    color = primary_blue,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        if (showSkipButton) {
            Button(
                onClick = { onSkipAdsClick() },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSkipAdsEnabled) primary_green else Color.Gray
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isSkipAdsEnabled) "SKIP ADS ENABLED" else "SKIP ADS DISABLED",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun PodcastEpisodeItem(
    episode: PodcastEpisode,
    isPlaying: Boolean,
    isDownloaded: Boolean,
    isPlayed: Boolean,
    isExpanded: Boolean,
    currentTime: Long,
    duration: Long,
    onPlayPauseClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onShareClick: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    onToggleChaptersClick: () -> Unit,
    modifier: Modifier = Modifier,
    mediaPlayerHolder: DesktopMediaPlayerHolder,
    chatId: ChatId,
    chapterNodes: List<NodeDto>?
    ) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isPlaying) primary_blue.copy(alpha = 0.08f) else Color.Transparent)
            .padding(16.dp)
    ) {
        // 1. Thumbnail + Title Row
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Episode image
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                PhotoUrlImage(
                    photoUrl = episode.imageUrlToShow,
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(if (isPlaying) 0.4f else 1f),
                    contentScale = ContentScale.Crop
                )

                if (isPlaying) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Playing",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))

            // Title centered with image
            Text(
                text = episode.titleToShow,
                color = if (isPlaying) primary_blue else Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 2. Description
        Text(
            text = episode.descriptionToShow,
            maxLines = 2,
            fontSize = 14.sp,
            color = Color.LightGray
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 3. Date + Duration Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding()
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(Color(0xFFAF52DE), shape = RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Podcasts,
                    contentDescription = "Podcast",
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = episode.dateString,
                fontSize = 12.sp,
                color = Color.White,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(Color.White, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))

            if (episode.played) {
                // Show blue mark icon + "Played" label
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(primary_blue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Played",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "Played",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            } else if (duration > 0) {
                Text(
                    text = "${formatMillis(duration - currentTime)} left",
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            LinearProgressIndicator(
                progress = currentTime.toFloat() / duration.coerceAtLeast(1),
                modifier = Modifier
                    .width(80.dp)
                    .height(4.dp),
                color = if (isPlaying) primary_blue else Color.Gray,
                trackColor = Color.DarkGray
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Bottom Buttons Row + Equalizer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically

        ) {
            Row {
                IconButton(onClick = onDownloadClick) {
                    Icon(
                        if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                        contentDescription = "Download",
                        tint = if (isDownloaded) Color.Green else Color.Gray
                    )
                }

                IconButton(onClick = onShareClick) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.Gray)
                }

                IconButton(onClick = onMoreOptionsClick) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.Gray)
                }


                if (!chapterNodes.isNullOrEmpty()) {
                    IconButton(onClick = onToggleChaptersClick) {
                        Icon(Icons.Default.List, contentDescription = "Chapters", tint = Color.Gray)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White, CircleShape)
                    .clickable(onClick = onPlayPauseClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (isExpanded && !chapterNodes.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp)
            ) {
                chapterNodes
                    .mapNotNull { it.properties }
                    .filter { !it.name.isNullOrBlank() && !it.timestamp.isNullOrBlank() }
                    .sortedBy { parseTimestampToMillis(it.timestamp!!) }
                    .forEachIndexed { index, chapter ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val timeMillis = parseTimestampToMillis(chapter.timestamp!!)
                                    scope.launch {
                                        mediaPlayerHolder.processUserAction(
                                            UserAction.ServiceAction.Seek(
                                                chatId,
                                                episode.getUpdatedContentEpisodeStatus().copy(
                                                    currentTime = FeedItemDuration(timeMillis / 1000L)
                                                )
                                            )
                                        )
                                    }
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = chapter.name.orEmpty(),
                                color = Color.White,
                                fontSize = 14.sp,
                                maxLines = 2,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = chapter.timestamp.orEmpty(),
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                        }

                        if (index != chapterNodes.lastIndex) {
                            Divider(
                                modifier = Modifier.fillMaxWidth(),
                                thickness = 0.5.dp,
                                color = Color.DarkGray
                            )
                        }
                    }
            }
        }

        Divider(
            modifier = Modifier.padding(top = 12.dp),
            thickness = 1.dp,
            color = Color.DarkGray
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FloatingPodcastPlayer(
    chatId: ChatId,
    mediaPlayerHolder: DesktopMediaPlayerHolder,
    dashboardViewModel: DashboardViewModel,
    podcastViewModel: PodcastViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val podcastState by podcastViewModel.podcastState.collectAsState()
    val podcast = podcastState ?: return

    val episode = podcast.getCurrentEpisode()
    val scope = rememberCoroutineScope()
    val mediaState by mediaPlayerHolder.mediaState.collectAsState()
    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(mediaState) {
        isPlaying = mediaState is MediaPlayerServiceState.ServiceActive.MediaState.Playing
    }

    var currentTime by remember {
        mutableStateOf((episode.contentEpisodeStatus?.currentTime?.value ?: 0L) * 1000)
    }
    var duration by remember {
        mutableStateOf((episode.contentEpisodeStatus?.duration?.value ?: 0L) * 1000)
    }

    val progress = if (duration > 0) (currentTime.toFloat() / duration.toFloat()) else 0f

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                currentTime = mediaPlayerHolder.getCurrentPlaybackPosition()
                duration = mediaPlayerHolder.getTotalDuration()
                delay(1000)
            }
        }
    }

    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .width(320.dp)
            .height(140.dp)
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .onPointerEvent(PointerEventType.Press) { if (it.button.isPrimary) isDragging = true }
            .onPointerEvent(PointerEventType.Release) { if (it.button.isPrimary) isDragging = false }
            .onPointerEvent(PointerEventType.Move) { event ->
                if (isDragging) {
                    val change = event.changes.first()
                    offsetX += change.position.x - change.previousPosition.x
                    offsetY += change.position.y - change.previousPosition.y
                }
            }
            .shadow(8.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PhotoUrlImage(
                        photoUrl = podcast.imageToShow,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = podcast.title.value,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = episode.title.value,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                SeekableProgressBar(
                    currentTime = currentTime,
                    duration = duration,
                    onSeek = { newTime ->
                        val updatedEpisodeStatus = podcast.getUpdatedContentEpisodeStatus().copy(
                            currentTime = FeedItemDuration(newTime / 1000L)
                        )
                        scope.launch {
                            mediaPlayerHolder.processUserAction(
                                UserAction.ServiceAction.Seek(chatId, updatedEpisodeStatus)
                            )
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                val newTime = (currentTime - 15_000).coerceAtLeast(0L)
                                val updatedEpisodeStatus = podcast.getUpdatedContentEpisodeStatus().copy(
                                    currentTime = FeedItemDuration(newTime / 1000L)
                                )
                                mediaPlayerHolder.processUserAction(
                                    UserAction.ServiceAction.Seek(chatId, updatedEpisodeStatus)
                                )
                            }
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            Icons.Filled.Replay10,
                            contentDescription = "Rewind",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            scope.launch {
                                if (isPlaying) {
                                    mediaPlayerHolder.processUserAction(
                                        UserAction.ServiceAction.Pause(chatId, episode.id.value)
                                    )
                                } else {
                                    mediaPlayerHolder.processUserAction(
                                        UserAction.ServiceAction.Play(
                                            chatId,
                                            episode.episodeUrl,
                                            podcast.getUpdatedContentFeedStatus(),
                                            podcast.getUpdatedContentEpisodeStatus(),
                                            podcast.getFeedDestinations()
                                        )
                                    )
                                }
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(primary_blue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            scope.launch {
                                val newTime = (currentTime + 30_000).coerceAtMost(duration)
                                val updatedEpisodeStatus = podcast.getUpdatedContentEpisodeStatus().copy(
                                    currentTime = FeedItemDuration(newTime / 1000L)
                                )
                                mediaPlayerHolder.processUserAction(
                                    UserAction.ServiceAction.Seek(chatId, updatedEpisodeStatus)
                                )
                            }
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            Icons.Filled.Forward30,
                            contentDescription = "Forward",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun SeekableProgressBar(
    currentTime: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = primary_blue
) {
    var sliderPosition by remember { mutableStateOf(0f) }
    var isUserSeeking by remember { mutableStateOf(false) }

    val progress = if (duration > 0) currentTime.toFloat() / duration else 0f

    LaunchedEffect(currentTime, duration) {
        if (!isUserSeeking) {
            sliderPosition = progress
        }
    }

    Slider(
        value = sliderPosition,
        onValueChange = {
            isUserSeeking = true
            sliderPosition = it
        },
        onValueChangeFinished = {
            val newTime = (sliderPosition * duration).toLong()
            onSeek(newTime)
            isUserSeeking = false
        },
        valueRange = 0f..1f,
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp),
        colors = SliderDefaults.colors(
            thumbColor = Color.Transparent, // hides the thumb
            activeTrackColor = color,
            inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
        )
    )
}


fun formatMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0)
        "%d:%02d:%02d".format(hours, minutes, seconds)
    else
        "%02d:%02d".format(minutes, seconds)
}

fun parseTimestampToMillis(timestamp: String): Long {
    return try {
        val parts = timestamp.split(":").map { it.toLong() }
        when (parts.size) {
            3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000
            2 -> (parts[0] * 60 + parts[1]) * 1000
            else -> 0L
        }
    } catch (e: Exception) {
        0L
    }
}
