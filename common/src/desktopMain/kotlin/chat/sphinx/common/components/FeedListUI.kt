package chat.sphinx.common.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.sphinx.common.viewmodel.FeedViewModel
import chat.sphinx.wrapper.feed.Feed
import chat.sphinx.wrapper.feed.FeedItem
import chat.sphinx.wrapper.podcast.toFeed
import chat.sphinx.wrapper.timeAgo
import theme.primary_blue

@Composable
fun FeedListUI(
    feedViewModel: FeedViewModel,
    isFollowing: Boolean = false,
) {
    val searchResults by feedViewModel.searchResults.collectAsState()
    val recentlyReleased by feedViewModel.feedsHolderViewStateFlow.collectAsState()
    val recentlyPlayed by feedViewModel.recentlyPlayedEpisode.collectAsState()
    val isSearchFocused by feedViewModel.isSearchFocused.collectAsState()
    val searchText = feedViewModel.feedSearchText.value?.text ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.onSurfaceVariant)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (isFollowing) {
            Spacer(modifier = Modifier.height(12.dp))

            if (isSearchFocused && searchText.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 64.dp, bottom = 32.dp, start = 32.dp, end = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Search over\n4 000 000 podcasts\non the Podcast Index",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            } else if (searchResults.isNotEmpty()) {
                // Show "Search Results" header
                SectionHeader("Search Results")

                searchResults.forEach { result ->
                    if (!result.isSectionHeader) {
                        result.feedSearchResult?.let { searchResult ->
                            FollowingFeedItem(feed = searchResult.toFeed() ?: return@let) {
                                feedViewModel.onPodcastSearchResultClicked(searchResult)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            } else {
                // Default Following feeds
                SectionHeader("Following")

                recentlyReleased.forEach { feed ->
                    FollowingFeedItem(feed = feed) {
                        feedViewModel.onPodcastFeedItemClicked(feed.lastItem ?: return@FollowingFeedItem)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        } else {
            // ... existing code for non-following tab ...
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader("Recently Released")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                recentlyReleased.mapNotNull { it.lastPublished }.forEach { episode ->
                    FeedCardSquare(episode, Modifier.padding(end = 12.dp)) {
                        feedViewModel.onPodcastFeedItemClicked(episode)
                    }
                }
            }

            if (recentlyPlayed != null) {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader("Recently Played")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    FeedCardSquare(recentlyPlayed!!, Modifier.padding(end = 12.dp)) {
                        feedViewModel.onPodcastFeedItemClicked(recentlyPlayed!!)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}@Composable
fun FeedCardSquare(
    episode: FeedItem,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val imageUrl = episode.imageUrlToShow?.value
    val episodeTitle = episode.titleToShow
    val showTitle = episode.feed?.titleToShow ?: ""
    val description = episode.descriptionToShow
    val published = episode.datePublished?.timeAgo() ?: ""

    val currentTime = (episode.contentEpisodeStatus?.currentTime?.value ?: 0L) * 1000
    val duration = (episode.contentEpisodeStatus?.duration?.value ?: 0L) * 1000
    val remainingTime = (duration - currentTime).coerceAtLeast(0)

    Column(
        modifier = modifier
            .width(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(8.dp)
    ) {
        if (imageUrl != null) {
            PhotoUrlImage(
                photoUrl = episode.imageUrlToShow,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color.DarkGray, RoundedCornerShape(8.dp))
            )
        }

        Spacer(Modifier.height(2.dp))

        Text(
            text = episodeTitle,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = showTitle,
            fontSize = 12.sp,
            color = Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = description,
            fontSize = 11.sp,
            color = Color.Gray,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = published,
            fontSize = 10.sp,
            color = Color.Gray
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (duration > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (remainingTime > 0) "${formatMillis(remainingTime)} left" else formatMillis(duration),
                        fontSize = 10.sp,
                        color = primary_blue
                    )

                    Column(
                        modifier = Modifier.width(80.dp)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = currentTime.toFloat() / duration.coerceAtLeast(1),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = primary_blue,
                            trackColor = Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun SectionHeader(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = "Go to section",
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}


@Composable
fun FollowingFeedItem(
    feed: Feed,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val imageUrl = feed.imageUrlToShow?.value.orEmpty()
    val title = feed.titleToShow
    val author = feed.author?.value ?: ""

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (imageUrl.isNotBlank()) {
            PhotoUrlImage(
                photoUrl = feed.imageUrlToShow,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                Modifier
                    .size(48.dp)
                    .background(Color.DarkGray, RoundedCornerShape(6.dp))
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = author,
                color = Color.Gray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


