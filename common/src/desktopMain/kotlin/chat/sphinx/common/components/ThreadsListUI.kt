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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.sphinx.common.models.ChatMessage
import chat.sphinx.common.models.ThreadItem
import chat.sphinx.common.viewmodel.DashboardViewModel
import chat.sphinx.common.viewmodel.ThreadsViewModel
import chat.sphinx.common.viewmodel.chat.ChatViewModel
import chat.sphinx.utils.toAnnotatedString
import chat.sphinx.wrapper.chatTimeFormat
import chat.sphinx.wrapper.message.media.*
import chat.sphinx.wrapper.message.retrieveTextToShow
import chat.sphinx.wrapper.thumbnailUrl
import chat.sphinx.wrapper.util.getInitials
import theme.md_theme_dark_background


@Composable
fun ThreadsListUI(
    threadsViewModel: ThreadsViewModel,
    dashboardViewModel: DashboardViewModel,
    chatViewModel: ChatViewModel?
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
                    ThreadItemUI(thread = thread, chatViewModel = chatViewModel)
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
        Text(
            "No threads found.",
            fontWeight = FontWeight.W500,
            fontFamily = Roboto,
            color = MaterialTheme.colorScheme.tertiary,
            fontSize = 14.sp,
            maxLines = 1
        )
    }
}

@Composable
fun ThreadItemUI(
    thread: ThreadItem,
    chatViewModel: ChatViewModel?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                chatViewModel?.navigateToThreadChat(thread.uuid, true)
            },
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

                    Spacer(modifier = Modifier.height(2.dp))

                    if (chatViewModel != null) {
                        // --- MEDIA ATTACHMENT ---
                        thread.messageMedia?.message?.messageMedia?.let { media ->
                            when {
                                media.mediaType.isImage -> {
                                    MessageMediaImage(
                                        chatMessage = thread.messageMedia,
                                        chatViewModel = chatViewModel,
                                        modifier = Modifier
                                            .wrapContentHeight()
                                            .fillMaxWidth()
                                    )
                                }

                                media.mediaType.isUnknown || media.mediaType.isPdf -> {
                                    MessageFile(
                                        chatMessage = thread.messageMedia,
                                        chatViewModel = chatViewModel,
                                    )
                                }

                                media.mediaType.isVideo -> {
                                    MessageVideo(
                                        chatMessage = thread.messageMedia,
                                        chatViewModel = chatViewModel,
                                        modifier = Modifier
                                            .wrapContentHeight()
                                            .fillMaxWidth()
                                    )
                                }

                                media.mediaType.isAudio -> {
                                    MessageAudio(
                                        chatMessage = thread.messageMedia,
                                        chatViewModel = chatViewModel,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    // --- END MEDIA ATTACHMENT ---

                    Spacer(modifier = Modifier.height(6.dp))

                    // The original message text
                    Text(
                        text = thread.message.toAnnotatedString(),
                        fontWeight = FontWeight.W400,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(16.dp))

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

    Box(modifier = modifier) {
        // Each avatar is offset by an increasing X
        displayedUsers.forEachIndexed { index, user ->
            Box(
                modifier = Modifier
                    .offset(x = (index * 16).dp)
            ) {
                PhotoUrlImage(
                    photoUrl = user.photoUrl,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape),
                    color = user.colorKey?.let { Color(it) },
                    firstNameLetter = user.alias?.value?.getInitials(),
                    fontSize = 11
                )
            }
        }
    }
}
@Composable
fun ThreadHeaderUI(
    chatMessage: ChatMessage,
    chatViewModel: ChatViewModel? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(md_theme_dark_background)
            .padding(horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            PhotoUrlImage(
                photoUrl = chatMessage.contact?.photoUrl?.thumbnailUrl ?: chatMessage.message.senderPic?.thumbnailUrl,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                color = chatMessage.replyToMessageColor?.let { Color(it) },
                firstNameLetter = chatMessage.replyToMessageSenderAliasPreview.getInitials(),
                fontSize = 12
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = chatMessage.replyToMessageSenderAliasPreview,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 12.sp,
                        fontFamily = Roboto,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = chatMessage.message.date.chatTimeFormat(),
                        fontWeight = FontWeight.W400,
                        fontFamily = Roboto,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            if (chatViewModel != null) {
                chatMessage.message.messageMedia?.let { media ->
                    when {
                        media.mediaType.isImage -> {
                            Column(
                                modifier = Modifier.fillMaxWidth().height(160.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                MessageMediaImage(
                                    chatMessage = chatMessage,
                                    chatViewModel = chatViewModel,
                                    modifier = Modifier
                                        .wrapContentHeight()
                                        .widthIn(max = 300.dp) // Adjust the max width as needed
                                )
                            }
                        }
                        media.mediaType.isUnknown || media.mediaType.isPdf -> {
                            MessageFile(
                                chatMessage = chatMessage,
                                chatViewModel = chatViewModel
                            )
                        }
                        media.mediaType.isVideo -> {
                            MessageVideo(
                                chatMessage = chatMessage,
                                chatViewModel = chatViewModel,
                                modifier = Modifier
                                    .wrapContentHeight()
                                    .fillMaxWidth()
                            )
                        }
                        media.mediaType.isAudio -> {
                            MessageAudio(
                                chatMessage = chatMessage,
                                chatViewModel = chatViewModel
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = chatMessage.message.retrieveTextToShow()?.trim()?.toAnnotatedString() ?: AnnotatedString(""),
                fontWeight = FontWeight.W400,
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = 14.sp,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Divider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
