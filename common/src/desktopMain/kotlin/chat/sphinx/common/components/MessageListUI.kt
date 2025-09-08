package chat.sphinx.common.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.sphinx.common.chatMesssageUI.ChatMessageUI
import chat.sphinx.common.chatMesssageUI.DateSeparator
import chat.sphinx.common.models.ChatMessage
import chat.sphinx.common.models.DashboardChat
import chat.sphinx.common.models.isIncomingMessage
import chat.sphinx.common.state.ContentState.scope
import chat.sphinx.common.state.MessageListData
import chat.sphinx.common.state.MessageListState
import chat.sphinx.common.viewmodel.DashboardViewModel
import chat.sphinx.common.viewmodel.chat.ChatViewModel
import chat.sphinx.wrapper.util.getHHMMSSString
import chat.sphinx.wrapper.util.getHHMMString
import chat.sphinx.wrapper.util.getInitials
import chat.sphinx.wrapper.util.toFormattedDate
import kotlinx.coroutines.launch
import theme.md_theme_dark_onBackground


@Composable
fun MessageListUI(
    chatViewModel: ChatViewModel,
    dashboardViewModel: DashboardViewModel,
    dashboardChat: DashboardChat?,
    isThreadView: Boolean = false
) {
    chatViewModel.screenInit()
    val isInactiveConversation = dashboardChat is DashboardChat.Inactive.Conversation
    val localScope = rememberCoroutineScope()

    Box {
        if (isThreadView) {
            when (val messageListData = MessageListState.threadScreenState()) {
                is MessageListData.EmptyMessageListData -> {
                    ChatEmptyScreen(isInactiveConversation, dashboardChat)
                }

                is MessageListData.PopulatedMessageListData -> {
                    val listState = remember(messageListData.chatId) { LazyListState() }

                    val chatMessages = messageListData.messages.dropLast(2)
                    val items = mutableStateListOf<ChatMessage>()
                    items.addAll(chatMessages)

                    val isAtBottom by rememberIsAtBottom(listState)
                    var bottomAnchorMessageId by remember(messageListData.chatId) { mutableStateOf<Long?>(null) }
                    var unseenIncomingWhileScrolledUp by remember(messageListData.chatId) { mutableStateOf(0) }

                    LaunchedEffect(isAtBottom, items) {
                        if (isAtBottom) {
                            val latest = items.firstOrNull { !it.isSeparator }
                            bottomAnchorMessageId = latest?.message?.id?.value
                            unseenIncomingWhileScrolledUp = 0
                        }
                    }

                    LaunchedEffect(items) {
                        if (!isAtBottom && bottomAnchorMessageId != null) {
                            var count = 0
                            for (m in items) {
                                if (m.isSeparator) continue
                                // We scan from newest (index 0) until we hit the anchor
                                if (m.message.id.value == bottomAnchorMessageId) break
                                if (m.isIncomingMessage()) count++
                            }
                            unseenIncomingWhileScrolledUp = count
                        }
                    }

                    if (chatMessages.isEmpty()) {
                        ChatEmptyScreen(isInactiveConversation, dashboardChat)
                    } else {
                        ChatMessagesList(
                            items,
                            listState,
                            chatViewModel,
                            dashboardViewModel
                        )
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            ScrollToBottomButton(
                                visible = !isAtBottom,
                                unreadIncomingCount = unseenIncomingWhileScrolledUp,
                                onClick = {
                                    localScope.launch {
                                        listState.animateScrollToItem(0)
                                    }
                                },
                                modifier = Modifier
                                    .padding(end = 16.dp, bottom = 16.dp)
                            )
                        }
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            SuggestedAliasListBar(chatViewModel)
                        }

                        VerticalScrollbar(
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                            reverseLayout = true,
                            adapter = rememberScrollbarAdapter(scrollState = listState)
                        )
                    }
                }
            }
        } else {
            when (val messageListData = MessageListState.screenState()) {
                is MessageListData.EmptyMessageListData -> {
                    ChatEmptyScreen(isInactiveConversation, dashboardChat)
                }

                is MessageListData.PopulatedMessageListData -> {
                    val listState = remember(messageListData.chatId) { LazyListState() }

                    val chatMessages = messageListData.messages
                    val items = mutableStateListOf<ChatMessage>()
                    items.addAll(chatMessages)

                    val isAtBottom by rememberIsAtBottom(listState)
                    var bottomAnchorMessageId by remember(messageListData.chatId) { mutableStateOf<Long?>(null) }
                    var unseenIncomingWhileScrolledUp by remember(messageListData.chatId) { mutableStateOf(0) }

                    LaunchedEffect(isAtBottom, items) {
                        if (isAtBottom) {
                            val latest = items.firstOrNull { !it.isSeparator }
                            bottomAnchorMessageId = latest?.message?.id?.value
                            unseenIncomingWhileScrolledUp = 0
                        }
                    }

                    LaunchedEffect(items) {
                        if (!isAtBottom && bottomAnchorMessageId != null) {
                            var count = 0
                            for (m in items) {
                                if (m.isSeparator) continue
                                if (m.message.id.value == bottomAnchorMessageId) break
                                if (m.isIncomingMessage()) count++
                            }
                            unseenIncomingWhileScrolledUp = count
                        }
                    }

                    if (chatMessages.isEmpty()) {
                        ChatEmptyScreen(isInactiveConversation, dashboardChat)
                    } else {
                        ChatMessagesList(
                            items,
                            listState,
                            chatViewModel,
                            dashboardViewModel
                        )
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            ScrollToBottomButton(
                                visible = !isAtBottom,
                                unreadIncomingCount = unseenIncomingWhileScrolledUp,
                                onClick = {
                                    localScope.launch {
                                        listState.animateScrollToItem(0)
                                    }
                                },
                                modifier = Modifier
                                    .padding(end = 16.dp, bottom = 16.dp)
                            )
                        }
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            SuggestedAliasListBar(chatViewModel)
                        }

                        VerticalScrollbar(
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                            reverseLayout = true,
                            adapter = rememberScrollbarAdapter(scrollState = listState)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatEmptyScreen(isInactiveConversation: Boolean, dashboardChat: DashboardChat?){
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.padding(top = 32.dp)
        ) {
            // Profile Picture with PhotoUrl
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isInactiveConversation) {
                    Canvas(
                        modifier = Modifier.fillMaxSize(),
                        onDraw = {
                            drawCircle(
                                color = md_theme_dark_onBackground,
                                radius = size.minDimension / 2,
                                style = Stroke(
                                    width = 4f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
                                )
                            )
                        }
                    )
                    PhotoUrlImage(
                        dashboardChat?.photoUrl,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        firstNameLetter = (dashboardChat?.chatName ?: "Unknown Chat").getInitials(),
                        color = dashboardChat?.color?.let { Color(it) },
                        fontSize = 16
                    )

                    // Icon beside the profile picture
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-1).dp, y = 50.dp),
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                    )
                } else {
                    PhotoUrlImage(
                        dashboardChat?.photoUrl,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape),
                        firstNameLetter = (dashboardChat?.chatName ?: "Unknown Chat").getInitials(),
                        color = dashboardChat?.color?.let { Color(it) },
                        fontSize = 16
                    )
                }
            }

            Row {
                // Chat Name
                Text(
                    text = dashboardChat?.chatName ?: "Unknown",
                    fontSize = 15.sp,
                    maxLines = 1,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                if (!isInactiveConversation) {

                    Spacer(Modifier.width(8.dp))

                    androidx.compose.material.Icon(
                        Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 2.dp).size(18.dp)
                    )
                }

            }

            val descriptionText = if (isInactiveConversation) {
                "Invited on ${dashboardChat?.sortBy?.toFormattedDate()}"
            } else {
                "Messages and calls are secured\n   with end-to-end encryption"
            }

            // Description Text
            Text(
                text = descriptionText,
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.W400,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }


        if (isInactiveConversation) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Your chat will be enabled\nas soon as your contact goes online",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }

}

@Composable
fun ChatMessagesList(
    items: SnapshotStateList<ChatMessage>,
    listState: LazyListState,
    chatViewModel: ChatViewModel,
    dashboardViewModel: DashboardViewModel
) {
    val scope = rememberCoroutineScope()
    val isLoadingMore by chatViewModel.isLoadingMoreMessages.collectAsState()

    LazyColumn(
        state = listState,
        reverseLayout = true,
        contentPadding = PaddingValues(8.dp)
    ) {
        chatViewModel.onNewMessageCallback = {
            scope.launch {
                if (listState.firstVisibleItemIndex <= 1) {
                    listState.scrollToItem(0)
                }
            }
        }

        itemsIndexed(
            items,
            key = { _, item -> "${item.message.id}-${item.isSeparator}" }
        ) { index, item ->

            LaunchedEffect(index) {
                if (index >= items.size - 10 && !isLoadingMore) {
                    chatViewModel.loadMoreMessages()
                }
            }

            if (item.isSeparator) {
                DateSeparator(item)
            } else {
                ChatMessageUI(
                    item,
                    chatViewModel
                )
            }
        }
    }
}

@Composable
private fun ScrollToBottomButton(
    visible: Boolean,
    unreadIncomingCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    Box(modifier = modifier) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor =MaterialTheme.colorScheme.onBackground
            )
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                contentDescription = "Scroll to bottom",
            )
        }

        if (unreadIncomingCount > 0) {
            Box(
                modifier = Modifier
                    .offset(x = (-8).dp, y = (-6).dp)
            ) {
                MessageCount(unreadIncomingCount.toString())
            }
        }
    }
}

@Composable
private fun rememberIsAtBottom(listState: LazyListState): State<Boolean> {
    // reverseLayout = true -> bottom == index 0 with 0px offset
    return remember(listState) {
        derivedStateOf {
            val atStart = listState.firstVisibleItemIndex == 0
            val atOffset = listState.firstVisibleItemScrollOffset == 0
            atStart && atOffset
        }
    }
}