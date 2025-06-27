package chat.sphinx.common.components

import androidx.compose.animation.VectorConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.sphinx.common.viewmodel.DashboardViewModel
import chat.sphinx.common.viewmodel.FeedViewModel
import chat.sphinx.wrapper.feed.Feed
import theme.place_holder_text

@Composable
fun FeedListUI(
    dashboardViewModel: DashboardViewModel
) {
    val feedViewModel = remember { FeedViewModel(dashboardViewModel) }

    val recentlyReleased by feedViewModel.feedsHolderViewStateFlow.collectAsState()
    val recentlyPlayed by feedViewModel.lastPlayedFeedsHolderViewStateFlow.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.onSurfaceVariant)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        SectionHeader("Recently Played")
        recentlyPlayed.take(3).forEach {
            FeedCard(it)
        }

        Spacer(modifier = Modifier.height(16.dp))
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
            color = Color.White,
            modifier = Modifier.weight(1f)
        )

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
fun FeedCard(feed: Feed) {
    val imageUrl = feed.imageUrlToShow?.value
    val episodeNumber = feed.lastItem?.title?.value?.substringBefore(" ") ?: "#0000"
    val episodeTitle = feed.lastItem?.title?.value?.substringAfter(" ") ?: feed.titleToShow
    val published = feed.lastItem?.datePublished ?: feed.datePublished

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { /* Optional: go to player */ }
            .padding(12.dp)
    ) {
        if (imageUrl != null) {
            PhotoUrlImage(
                photoUrl = feed.imageUrlToShow,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                Modifier
                    .size(80.dp)
                    .background(Color.DarkGray, RoundedCornerShape(8.dp))
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episodeNumber,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = episodeTitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.W600,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = feed.titleToShow,
                fontSize = 12.sp,
                color = place_holder_text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = feed.descriptionToShow,
                fontSize = 11.sp,
                color = Color.Gray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

//            published?.let {
//                Spacer(modifier = Modifier.height(4.dp))
//                Text(
//                    text = it,
//                    fontSize = 10.sp,
//                    color = Color.Gray
//                )
//            }
        }
    }
}
