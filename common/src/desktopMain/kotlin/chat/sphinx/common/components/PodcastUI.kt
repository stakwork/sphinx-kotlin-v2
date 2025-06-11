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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.sphinx.common.Res
import chat.sphinx.common.components.media_player.DesktopMediaPlayerHolder
import chat.sphinx.common.components.media_player.MediaPlayerServiceState
import chat.sphinx.common.components.media_player.UserAction
import chat.sphinx.common.viewmodel.DashboardViewModel
import chat.sphinx.common.viewmodel.PodcastViewModel
import chat.sphinx.wrapper.dashboard.ChatId
import chat.sphinx.wrapper.feed.FeedItemDuration
import chat.sphinx.wrapper.lightning.toSat
import chat.sphinx.wrapper.podcast.PodcastEpisode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import theme.primary_blue
import theme.primary_green

@Composable
fun PodcastSplitScreen(
    chatId: ChatId,
    mediaPlayerHolder: DesktopMediaPlayerHolder,
    dashboardViewModel: DashboardViewModel
) {
    PodcastMainPlayer(chatId, mediaPlayerHolder, dashboardViewModel)
}

@Composable
fun PodcastMainPlayer(
    chatId: ChatId,
    mediaPlayerHolder: DesktopMediaPlayerHolder,
    dashboardViewModel: DashboardViewModel
) {
    val viewModel = remember(chatId) { PodcastViewModel(chatId) }
    val podcastState by viewModel.podcastState.collectAsState()

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
    var sliderPosition by remember { mutableStateOf(progress * 100f) }
    var isUserSeeking by remember { mutableStateOf(false) }
    var satsSliderPosition by remember { mutableStateOf(podcast.satsPerMinute.toFloat()) }
    var isAdjustingSats by remember { mutableStateOf(false) }

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

            viewModel.setPlayingTime(currentTime)

            while (isPlaying) {
                delay(1000)

                currentTime = mediaPlayerHolder.getCurrentPlaybackPosition()
                duration = mediaPlayerHolder.getTotalDuration()

                viewModel.setPlayingTime(currentTime)

                if (!isUserSeeking && duration > 0) {
                    sliderPosition = (currentTime.toFloat() / duration.toFloat()) * 100f
                }
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
        PhotoUrlImage(
            photoUrl = podcast.imageToShow,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sats per minute", fontSize = 14.sp, color = Color.White)
                Text(satsSliderPosition.toInt().toString(), color = Color.White)
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
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = primary_blue,
                    activeTrackColor = primary_blue,
                    inactiveTrackColor = Color.Gray
                )
            )
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                // Floating current time label over slider thumb
                Slider(
                    value = sliderPosition,
                    onValueChange = {
                        isUserSeeking = true
                        sliderPosition = it
                    },
                    onValueChangeFinished = {
                        val newPositionMillis = (duration * (sliderPosition / 100f)).toLong()

                        val updatedEpisodeStatus = podcast.getUpdatedContentEpisodeStatus().copy(
                            currentTime = FeedItemDuration(newPositionMillis / 1000L)
                        )

                        scope.launch {
                            mediaPlayerHolder.processUserAction(
                                UserAction.ServiceAction.Seek(
                                    chatId,
                                    updatedEpisodeStatus
                                )
                            )

                            val playingEpisode = podcast.getCurrentEpisode()
                            playingEpisode.contentEpisodeStatus = updatedEpisodeStatus

                            currentTime = newPositionMillis
                            duration = mediaPlayerHolder.getTotalDuration()
                            if (duration > 0) {
                                sliderPosition = (currentTime.toFloat() / duration.toFloat()) * 100f
                            }
                        }

                        isUserSeeking = false
                    },
                    valueRange = 0f..100f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = primary_blue,
                        activeTrackColor = primary_blue,
                        inactiveTrackColor = Color.Gray
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatMillis(currentTime), fontSize = 12.sp, color = primary_blue)
                Text(formatMillis(duration), fontSize = 12.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* Optional: Quote handler */ }) {
                Icon(Icons.Filled.FormatQuote, "Quote", tint = Color.White, modifier = Modifier.size(28.dp))
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
                Icon(Icons.Filled.Replay10, "Rewind 10s", tint = Color.White, modifier = Modifier.size(28.dp))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PlaybackSpeedSelector(
                    currentSpeed = podcast.speed.toFloat(),
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

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(primary_blue, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = {
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
                                        podcast.getUpdatedContentEpisodeStatus()
                                    )
                                )
                            }
                        }
                    }) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
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
                Icon(Icons.Filled.Forward30, "Forward 30s", tint = Color.White, modifier = Modifier.size(28.dp))
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(primary_green.copy(alpha = 0.5f), shape = CircleShape)
                    .clickable {
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
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_boost),
                    contentDescription = "Boost",
                    modifier = Modifier
                        .size(28.dp)
                        .padding(2.dp),
                    contentScale = ContentScale.Inside,
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
        }
        PodcastEpisodesHeader(podcast.episodes.size)

        val playingTime by viewModel.playingEpisodeTime.collectAsState()

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
                isExpanded = false,
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

                            mediaPlayerHolder.processUserAction(
                                UserAction.ServiceAction.Play(
                                    chatId = chatId,
                                    episodeUrl = episode.episodeUrl,
                                    contentFeedStatus = podcast.getUpdatedContentFeedStatus(),
                                    contentEpisodeStatus = episode.getUpdatedContentEpisodeStatus()
                                )
                            )
                        }
                    }
                },
                onDownloadClick = { /* implement as needed */ },
                onShareClick = {
                    scope.launch {
                        viewModel.buildPodcastShareConfirmation(episode.id)?.let { confirmation ->
                            dashboardViewModel.toggleConfirmationWindow(true, confirmation)
                        }
                    }
                },
                onMoreOptionsClick = {
                    dashboardViewModel.showFullScreenView(DashboardViewModel.FullScreenView.EpisodeDetails(episode, podcast.title.value))
                },
                onToggleChaptersClick = {
//                    expandedEpisodeId = if (expandedEpisodeId == episode.id.value) null else episode.id.value
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp)) // padding at the bottom
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
fun PodcastEpisodesHeader(episodesCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
    modifier: Modifier = Modifier
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

        // 2. Description starts at content start (not offset to image)
        Text(
            text = episode.descriptionToShow,
            maxLines = 2,
            fontSize = 14.sp,
            color = Color.LightGray
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 3. Date + Duration Row (with small icon and progress bar)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
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
                color = Color.White
            )

            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(Color.White, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))

            if (duration > 0) {
                Text(
                    text = "${formatMillis(duration - currentTime)} left",
                    fontSize = 12.sp,
                    color = Color.White
                )
            } else if (duration > 0) {
                Text(
                    text = formatMillis(duration),
                    fontSize = 12.sp,
                    color = Color.White
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

                if (episode.chapters != null) {
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

        if (isExpanded && episode.chapters?.nodes?.isNotEmpty() == true) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp)
            ) {
//                episode.chapters.nodes.forEach { chapter ->
//                    val name = chapter.properties?.name
//                    val time = chapter.properties?.timestamp
//                    if (!name.isNullOrBlank() && !time.isNullOrBlank()) {
//                        Text(
//                            text = "- $name ($time)",
//                            fontSize = 12.sp,
//                            color = Color.LightGray,
//                            modifier = Modifier.padding(vertical = 2.dp)
//                        )
//                    }
//                }
            }
        }

        Divider(
            modifier = Modifier.padding(top = 12.dp),
            thickness = 1.dp,
            color = Color.DarkGray
        )
    }
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
