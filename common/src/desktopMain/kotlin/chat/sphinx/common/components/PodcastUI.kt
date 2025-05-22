package chat.sphinx.common.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.sphinx.common.Res
import chat.sphinx.common.viewmodel.TribeFeedViewModel
import chat.sphinx.wrapper.podcast.Podcast
import theme.primary_blue
import theme.primary_green

@Composable
fun PodcastSplitScreen(podcast: Podcast?) {
    podcast?.let {
        PodcastMainPlayer(it)
    } ?: run {
        Text("No podcast data available", modifier = Modifier.padding(16.dp))
    }
}

@Composable
fun PodcastMainPlayer(podcast: Podcast) {
    val episode = podcast.getCurrentEpisode()
    val duration = podcast.episodeDuration ?: 0L
    val currentTime = podcast.currentTime.toLong()
    val progress = if (duration > 0) (currentTime.toFloat() / duration.toFloat()) else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.onSurfaceVariant)
    ) {
        // Full-width image
        PhotoUrlImage(
            photoUrl = podcast.imageToShow,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Sats per minute label and slider
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Podcast: sats per minute", fontSize = 14.sp, color = Color.White)
                Text(podcast.satsPerMinute.toString(), color = Color.White)
            }
            Slider(
                value = podcast.satsPerMinute.toFloat(),
                onValueChange = {},
                valueRange = 0f..500f,
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = "#${episode.id.value} - ${episode.title.value}",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Seek bar
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Slider(
                value = progress * 100f,
                onValueChange = {},
                valueRange = 0f..100f,
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = primary_blue,
                    activeTrackColor = primary_blue,
                    inactiveTrackColor = Color.Gray
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatMillis(currentTime), fontSize = 12.sp, color = primary_blue)
                Text(formatMillis(duration), fontSize = 12.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Media controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quote Icon
            IconButton(onClick = { /* Quote */ }) {
                Icon(
                    imageVector = Icons.Filled.FormatQuote,
                    contentDescription = "Quote",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp) // Increased size
                )
            }

            // Rewind Icon
            IconButton(onClick = { /* Rewind */ }) {
                Icon(
                    imageVector = Icons.Filled.Replay10,
                    contentDescription = "Rewind 10s",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp) // Increased size
                )
            }

            // Play Button with Speed Selector
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PlaybackSpeedSelector(
                    currentSpeed = podcast.speed.toFloat(),
                    onSpeedChange = { }
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(color = primary_blue, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = { /* Play/Pause */ }) {
                        Icon(
                            imageVector = if (podcast.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp) // KEEP play button size
                        )
                    }
                }
            }

            // Forward Icon
            IconButton(onClick = { /* Forward */ }) {
                Icon(
                    imageVector = Icons.Filled.Forward30,
                    contentDescription = "Forward 30s",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp) // Increased size
                )
            }

            // Boost Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(primary_green.copy(alpha = 0.5f), shape = CircleShape)
                    .clickable { /* Handle boost click */ },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_boost),
                    contentDescription = "Boost",
                    modifier = Modifier
                        .size(28.dp) // Increased size
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
