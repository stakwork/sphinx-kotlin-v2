package chat.sphinx.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.sphinx.common.viewmodel.TribeFeedViewModel
import chat.sphinx.wrapper.podcast.Podcast

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
            .background(Color.Black)
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
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatMillis(currentTime), fontSize = 12.sp, color = Color.Gray)
                Text(formatMillis(duration), fontSize = 12.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Media controls with 1x above play button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* Mute */ }) {
                Icon(Icons.Filled.VolumeOff, contentDescription = "Mute", tint = Color.White)
            }

            IconButton(onClick = { /* Rewind */ }) {
                Icon(Icons.Filled.Replay10, contentDescription = "Rewind 10s", tint = Color.White)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("1x", color = Color.White)
                IconButton(onClick = { /* Play/Pause */ }) {
                    Icon(
                        imageVector = if (podcast.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(48.dp),
                        tint = Color.White
                    )
                }
            }

            IconButton(onClick = { /* Forward */ }) {
                Icon(Icons.Filled.Forward30, contentDescription = "Forward 30s", tint = Color.White)
            }

            IconButton(onClick = { /* Mute */ }) {
                Icon(Icons.Filled.VolumeOff, contentDescription = "Mute", tint = Color.White)
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
