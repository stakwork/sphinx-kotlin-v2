package chat.sphinx.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.sphinx.common.viewmodel.DashboardViewModel
import chat.sphinx.wrapper.podcast.PodcastEpisode

@Composable
fun EpisodeDetailsScreen(
    episode: PodcastEpisode,
    podcastTitle: String,
    dashboardViewModel: DashboardViewModel,
    preferredSize: DpSize
) {
    Box(
        modifier = Modifier
            .size(preferredSize)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopHeaderContainer(
                title = "Episode Details and Chapters",
                onClose = { dashboardViewModel.closeFullScreenView() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // 1. Centered square image
                    PhotoUrlImage(
                        photoUrl = episode.imageUrlToShow,
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .align(Alignment.CenterHorizontally),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Podcast title (gray)
                    Text(
                        text = podcastTitle,
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 3. Episode title (Title)
                    Text(
                        text = episode.titleToShow,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4. Meta info row
                    Row(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
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

                        Text(text = "Podcast", color = Color.White, fontSize = 14.sp)

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(Color.White, CircleShape)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(text = episode.dateString, color = Color.White, fontSize = 14.sp)

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(Color.White, CircleShape)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        val remaining = episode.durationMilliseconds?.let {
                            formatMillis(it - (episode.currentTimeMilliseconds ?: 0))
                        } ?: ""

                        Text(text = "$remaining left", color = Color.White, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Divider(color = Color.Gray.copy(alpha = 0.4f))

                    // 6. Share Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = "Share", color = Color.White, fontSize = 16.sp)
                    }

                    Divider(color = Color.Gray.copy(alpha = 0.4f))

                    // 8. Mark As Played Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Mark As Played",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = "Mark As Played", color = Color.White, fontSize = 16.sp)
                    }

                    Divider(color = Color.Gray.copy(alpha = 0.4f))

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
