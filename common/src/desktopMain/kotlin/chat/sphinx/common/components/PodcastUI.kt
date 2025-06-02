package chat.sphinx.common.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.sphinx.common.Res
import chat.sphinx.common.components.media_player.DesktopMediaPlayerHolder
import chat.sphinx.common.components.media_player.MediaPlayerServiceState
import chat.sphinx.common.components.media_player.UserAction
import chat.sphinx.wrapper.dashboard.ChatId
import chat.sphinx.wrapper.feed.FeedItemDuration
import chat.sphinx.wrapper.lightning.toSat
import chat.sphinx.wrapper.podcast.Podcast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import theme.primary_blue
import theme.primary_green

@Composable
fun PodcastSplitScreen(
    podcast: Podcast?,
    chatId: ChatId,
    mediaPlayerHolder: DesktopMediaPlayerHolder
) {
    podcast?.let {
        PodcastMainPlayer(it, chatId, mediaPlayerHolder)
    } ?: run {
        Text("No podcast data available", modifier = Modifier.padding(16.dp))
    }
}

@Composable
fun PodcastMainPlayer(
    podcast: Podcast,
    chatId: ChatId,
    mediaPlayerHolder: DesktopMediaPlayerHolder
) {
    val scope = rememberCoroutineScope()
    val mediaState by mediaPlayerHolder.mediaState.collectAsState()
    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(mediaState) {
        isPlaying = mediaState is MediaPlayerServiceState.ServiceActive.MediaState.Playing
    }

    var currentTime by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    val episode = podcast.getCurrentEpisode()
    val progress = if (duration > 0) (currentTime.toFloat() / duration.toFloat()) else 0f
    var sliderPosition by remember { mutableStateOf(progress * 100f) }
    var isUserSeeking by remember { mutableStateOf(false) }
    var satsSliderPosition by remember { mutableStateOf(podcast.satsPerMinute.toFloat()) }
    var isAdjustingSats by remember { mutableStateOf(false) }

    // Live playback time updater
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            currentTime = mediaPlayerHolder.getCurrentPlaybackPosition()
            duration = mediaPlayerHolder.getTotalDuration()
        }

        while (isPlaying) {
            delay(1000)
            currentTime = mediaPlayerHolder.getCurrentPlaybackPosition()
            duration = mediaPlayerHolder.getTotalDuration()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.onSurfaceVariant)
    ) {
        PhotoUrlImage(
            photoUrl = podcast.imageToShow,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
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

                        // Create a new ContentEpisodeStatus with updated currentTime
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
                            currentTime = newPositionMillis
                        }

                        isUserSeeking = false
                    }
,
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
