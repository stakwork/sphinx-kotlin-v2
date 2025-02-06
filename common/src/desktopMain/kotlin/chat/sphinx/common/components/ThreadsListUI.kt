package chat.sphinx.common.components

import Roboto
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.sphinx.common.models.ChatMessage
import chat.sphinx.common.models.ThreadItem
import chat.sphinx.common.viewmodel.DashboardViewModel
import chat.sphinx.common.viewmodel.ThreadsViewModel
import chat.sphinx.wrapper.thumbnailUrl
import chat.sphinx.wrapper.util.getInitials
import theme.md_theme_dark_background


@Composable
fun ThreadsListUI(
    threadsViewModel: ThreadsViewModel,
    dashboardViewModel: DashboardViewModel
) {
    val threadItems by threadsViewModel.threadItems.collectAsState(initial = emptyList())
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize().background(md_theme_dark_background)) {
        if (threadItems.isEmpty()) {
            ThreadsEmptyScreen()
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(8.dp)
            ) {
                itemsIndexed(
                    items = threadItems,
                    key = { index, thread -> thread.uuid }
                ) { index, thread ->
                    ThreadItemUI(thread = thread)
                }
            }

            VerticalScrollbar(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState = listState)
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ThreadsEmptyScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("No threads available.", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun ThreadItemUI(
    thread: ThreadItem,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = md_theme_dark_background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Avatar

                PhotoUrlImage(
                    photoUrl = thread.photoUrl?.thumbnailUrl,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    color = thread.aliasAndColorKey.second?.let { Color(it) },
                    firstNameLetter = thread.aliasAndColorKey.first?.value?.getInitials(),
                    fontSize = 12
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    // Row with Alias & Date
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = thread.aliasAndColorKey.first?.value ?: "",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 12.sp,
                            fontFamily = Roboto,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = thread.date,
                            fontWeight = FontWeight.W400,
                            fontFamily = Roboto,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }

                    // Message
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = thread.message,
                        fontWeight = FontWeight.Normal,
                        fontFamily = Roboto,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OverlappedAvatars(
                            users = thread.usersReplies.orEmpty(),
                            modifier = Modifier
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (thread.repliesAmount.isNotEmpty()) {
                                Text(
                                    text = "${thread.repliesAmount} replies",
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }

                            thread.lastReplyDate?.let { lastReply ->
                                Text(
                                    text = lastReply,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom divider
            Divider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OverlappedAvatars(
    users: List<ChatMessage.ReplyUserHolder>,
    modifier: Modifier = Modifier,
) {
    // Maximum avatars to show before we display a +N overlay
    val maxAvatars = 6

    // Always take at most 6 from the list
    val displayedUsers = users.take(maxAvatars)
    val totalUserCount = users.size

    Box(modifier = modifier) {
        // Each avatar is offset by an increasing X
        displayedUsers.forEachIndexed { index, user ->
            Box(
                modifier = Modifier
                    // For example, each subsequent avatar moves 16.dp to the right
                    .offset(x = (index * 16).dp)
            ) {
                // Our normal avatar
                PhotoUrlImage(
                    photoUrl = user.photoUrl,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape),
                    color = user.colorKey?.let { Color(it) },
                    firstNameLetter = user.alias?.value?.getInitials(),
                    fontSize = 11
                )

                // If we're on the last avatar (index == 5) AND
                // there are more actual users than 6, show overlay
                if (index == maxAvatars - 1 && totalUserCount > maxAvatars) {
                    // Dark overlay
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.6f))
                    )
                    // “+N” text, typically centered
                    Text(
                        text = "+${totalUserCount - maxAvatars}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}
