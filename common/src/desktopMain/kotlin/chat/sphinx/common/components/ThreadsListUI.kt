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
                    color = thread.aliasAndColorKey.second?.let { colorString ->
                        Color.Black
                    },
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val replyUsers = thread.usersReplies.orEmpty().take(3)
                            replyUsers.forEachIndexed { index, userHolder ->
                                if (index > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                PhotoUrlImage(
                                    photoUrl = userHolder.photoUrl?.thumbnailUrl,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape),
                                    color = userHolder.colorKey?.let { Color(it) },
                                    firstNameLetter = userHolder.alias?.value?.getInitials(),
                                    fontSize = 10
                                )
                            }

                            val extraUsers = (thread.usersReplies?.size ?: 0) - replyUsers.size
                            if (extraUsers > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+$extraUsers",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

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
