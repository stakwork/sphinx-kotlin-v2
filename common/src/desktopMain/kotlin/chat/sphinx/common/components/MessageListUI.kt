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
import chat.sphinx.common.chatMesssageUI.UnseenSeparator
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

    val shouldShowUnseenSeparator by chatViewModel.shouldShowUnseenSeparator.collectAsState()
    val firstUnseenMessageId by chatViewModel.firstUnseenMessageId.collectAsState()

    Box {
        if (isThreadView) {
            // Thread view logic remains the same...
            when (val messageListData = MessageListState.threadScreenState()) {
                is MessageListData.EmptyMessageListData -> {
                    ChatEmptyScreen(isInactiveConversation, dashboardChat)
                }

                is MessageListData.PopulatedMessageListData -> {
                    val listState = remember(messageListData.chatId) { LazyListState() }

                    val chatMessages = messageListData.messages.dropLast(2)
                    val items = remember(messageListData.chatId) { mutableStateListOf<ChatMessage>() }

                    var previousItemsSize by remember(messageListData.chatId) { mutableStateOf(0) }
                    var previousLastMessageId by remember(messageListData.chatId) { mutableStateOf<Long?>(null) }
                    var wasAtBottom by remember(messageListData.chatId) { mutableStateOf(true) }

                    val isAtBottom by rememberIsAtBottom(listState)
                    var bottomAnchorMessageId by remember(messageListData.chatId) { mutableStateOf<Long?>(null) }
                    var unseenIncomingWhileScrolledUp by remember(messageListData.chatId) { mutableStateOf(0) }

                    // Update items and handle scrolling
                    LaunchedEffect(chatMessages, shouldShowUnseenSeparator, firstUnseenMessageId) {
                        val newSize = chatMessages.size
                        val currentLastMessageId = chatMessages.firstOrNull()?.message?.id?.value

                        val hasNewMessages = newSize > previousItemsSize && previousItemsSize > 0
                        val hasNewMessage = currentLastMessageId != previousLastMessageId && previousLastMessageId != null

                        updateItemsEfficiently(items, chatMessages)

                        // Handle initial scroll positioning
                        if (shouldShowUnseenSeparator && firstUnseenMessageId != null && previousItemsSize == 0) {
                            // First time loading with unseen messages - scroll to unseen separator
                            val unseenSeparatorIndex = items.indexOfFirst {
                                it.isUnseenSeparator || (it.message.id.value == firstUnseenMessageId)
                            }
                            if (unseenSeparatorIndex >= 0) {
                                kotlinx.coroutines.delay(50) // Allow UI to settle
                                listState.scrollToItem(unseenSeparatorIndex, 0)
                                wasAtBottom = false
                            }
                        } else if ((hasNewMessages || hasNewMessage) && wasAtBottom) {
                            // Normal new message behavior - scroll to bottom if user was at bottom
                            kotlinx.coroutines.delay(10)
                            listState.scrollToItem(0, 0)
                        }

                        previousItemsSize = newSize
                        previousLastMessageId = currentLastMessageId
                    }

                    // Track if user is at bottom
                    LaunchedEffect(isAtBottom) {
                        wasAtBottom = isAtBottom
                        if (isAtBottom && shouldShowUnseenSeparator) {
                            chatViewModel.readMessages()
                        }
                    }

                    LaunchedEffect(messageListData.chatId) {
                        chatViewModel.onNewMessageCallback = {
                            localScope.launch {
                                // Force scroll to bottom for any new message when at bottom
                                if (wasAtBottom) {
                                    kotlinx.coroutines.delay(50)
                                    listState.animateScrollToItem(0, 0)
                                    wasAtBottom = true
                                }
                            }
                        }
                    }

                    LaunchedEffect(isAtBottom, items.size) {
                        if (isAtBottom) {
                            val latest = items.firstOrNull { !it.isSeparator }
                            bottomAnchorMessageId = latest?.message?.id?.value
                            unseenIncomingWhileScrolledUp = 0
                        }
                    }

                    LaunchedEffect(items.size, bottomAnchorMessageId) {
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
                            dashboardViewModel,
                            shouldSetCallback = false
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
                                        listState.animateScrollToItem(0, 0)
                                        wasAtBottom = true
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
            // MAIN CHAT VIEW - This is where the fix is needed
            when (val messageListData = MessageListState.screenState()) {
                is MessageListData.EmptyMessageListData -> {
                    ChatEmptyScreen(isInactiveConversation, dashboardChat)
                }

                is MessageListData.PopulatedMessageListData -> {
                    val listState = remember(messageListData.chatId) { LazyListState() }

                    val chatMessages = messageListData.messages
                    val items = remember(messageListData.chatId) { mutableStateListOf<ChatMessage>() }

                    // Track previous size and messages to detect new messages
                    var previousItemsSize by remember(messageListData.chatId) { mutableStateOf(0) }
                    var previousLastMessageId by remember(messageListData.chatId) { mutableStateOf<Long?>(null) }
                    var wasAtBottom by remember(messageListData.chatId) { mutableStateOf(true) }
                    var hasScrolledToUnseen by remember(messageListData.chatId) { mutableStateOf(false) }

                    val isAtBottom by rememberIsAtBottom(listState)
                    var bottomAnchorMessageId by remember(messageListData.chatId) { mutableStateOf<Long?>(null) }
                    var unseenIncomingWhileScrolledUp by remember(messageListData.chatId) { mutableStateOf(0) }

                    // Update items and handle scrolling - THIS IS THE KEY FIX
                    LaunchedEffect(chatMessages, shouldShowUnseenSeparator, firstUnseenMessageId) {
                        val newSize = chatMessages.size
                        val currentLastMessageId = chatMessages.firstOrNull()?.message?.id?.value

                        val hasNewMessages = newSize > previousItemsSize && previousItemsSize > 0
                        val hasNewMessage = currentLastMessageId != previousLastMessageId && previousLastMessageId != null
                        val isInitialLoad = previousItemsSize == 0

                        updateItemsEfficiently(items, chatMessages)

                        // Handle initial scroll positioning for main chat view
                        if (shouldShowUnseenSeparator && firstUnseenMessageId != null && isInitialLoad && !hasScrolledToUnseen) {
                            // First time loading with unseen messages - scroll to unseen separator
                            val unseenSeparatorIndex = items.indexOfFirst { it.isUnseenSeparator }
                            val firstUnseenMessageIndex = items.indexOfFirst {
                                !it.isSeparator && !it.isUnseenSeparator && it.message.id.value == firstUnseenMessageId
                            }

                            val targetIndex = if (unseenSeparatorIndex >= 0) {
                                unseenSeparatorIndex
                            } else if (firstUnseenMessageIndex >= 0) {
                                // If no separator found, scroll to the first unseen message itself
                                firstUnseenMessageIndex
                            } else {
                                -1
                            }

                            if (targetIndex >= 0) {
                                kotlinx.coroutines.delay(100) // Allow UI to settle
                                listState.scrollToItem(targetIndex, 0)
                                wasAtBottom = false
                                hasScrolledToUnseen = true
                                println("Scrolled to unseen message at index: $targetIndex")
                            }
                        } else if ((hasNewMessages || hasNewMessage) && wasAtBottom && hasScrolledToUnseen) {
                            // Normal new message behavior - scroll to bottom if user was at bottom
                            kotlinx.coroutines.delay(10)
                            listState.scrollToItem(0, 0)
                        } else if (isInitialLoad && !shouldShowUnseenSeparator) {
                            // No unseen messages, scroll to bottom normally
                            kotlinx.coroutines.delay(10)
                            listState.scrollToItem(0, 0)
                        }

                        previousItemsSize = newSize
                        previousLastMessageId = currentLastMessageId
                    }

                    // Track if user is at bottom
                    LaunchedEffect(isAtBottom) {
                        wasAtBottom = isAtBottom
                        if (isAtBottom && shouldShowUnseenSeparator) {
                            chatViewModel.hideUnseenSeparator() // Use hideUnseenSeparator instead of readMessages
                        }
                    }

                    LaunchedEffect(messageListData.chatId) {
                        // Reset scroll state when changing chats
                        hasScrolledToUnseen = false

                        chatViewModel.onNewMessageCallback = {
                            localScope.launch {
                                // Force scroll to bottom for any new message when at bottom
                                if (wasAtBottom) {
                                    kotlinx.coroutines.delay(50)
                                    listState.animateScrollToItem(0, 0)
                                    wasAtBottom = true
                                }
                            }
                        }
                    }

                    LaunchedEffect(isAtBottom, items.size) {
                        if (isAtBottom) {
                            val latest = items.firstOrNull { !it.isSeparator }
                            bottomAnchorMessageId = latest?.message?.id?.value
                            unseenIncomingWhileScrolledUp = 0
                        }
                    }

                    LaunchedEffect(items.size, bottomAnchorMessageId) {
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
                            dashboardViewModel,
                            shouldSetCallback = false
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
                                        listState.animateScrollToItem(0, 0)
                                        wasAtBottom = true
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

private fun updateItemsEfficiently(
    items: SnapshotStateList<ChatMessage>,
    newMessages: List<ChatMessage>
) {
    if (items.isEmpty()) {
        items.addAll(newMessages)
        return
    }

    if (items.size == newMessages.size) {
        var anyChanged = false
        for (i in newMessages.indices) {
            val old = items[i]
            val neu = newMessages[i]
            val contentChanged =
                (old.isSeparator != neu.isSeparator) ||
                        (old.message != neu.message)

            if (contentChanged) {
                items[i] = neu
                anyChanged = true
            }
        }
        if (!anyChanged) return
        return
    }

    if (newMessages.size > items.size) {
        val sizeDiff = newMessages.size - items.size
        val existingMessagesMatch = items.isEmpty() ||
                items.zip(newMessages.drop(sizeDiff)).all { (existing, new) ->
                    existing.message.id == new.message.id
                }

        if (existingMessagesMatch) {
            val newMessagesToAdd = newMessages.take(sizeDiff)
            newMessagesToAdd.reversed().forEach { newMessage ->
                items.add(0, newMessage)
            }
            return
        }
    }

    items.clear()
    items.addAll(newMessages)
}

@Composable
private fun rememberIsAtBottom(listState: LazyListState): State<Boolean> {
    return remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val firstVisibleIndex = listState.firstVisibleItemIndex
            val firstVisibleOffset = listState.firstVisibleItemScrollOffset

            // Consider at bottom if:
            // 1. At absolute bottom (index 0, offset 0)
            // 2. Very close to bottom (within 50 pixels)
            // 3. Only one item visible and it's the first item
            val atAbsoluteBottom = firstVisibleIndex == 0 && firstVisibleOffset == 0
            val nearBottom = firstVisibleIndex == 0 && firstVisibleOffset <= 50
            val singleItemAtTop = layoutInfo.visibleItemsInfo.size == 1 &&
                    layoutInfo.visibleItemsInfo.firstOrNull()?.index == 0

            atAbsoluteBottom || nearBottom || singleItemAtTop
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
    dashboardViewModel: DashboardViewModel,
    shouldSetCallback: Boolean = true
) {
    val scope = rememberCoroutineScope()
    val isLoadingMore by chatViewModel.isLoadingMoreMessages.collectAsState()

    LazyColumn(
        state = listState,
        reverseLayout = true,
        contentPadding = PaddingValues(8.dp)
    ) {
        // Only set callback if needed (for backward compatibility)
        if (shouldSetCallback) {
            chatViewModel.onNewMessageCallback = {
                scope.launch {
                    if (listState.firstVisibleItemIndex <= 1 && listState.firstVisibleItemScrollOffset <= 100) {
                        listState.scrollToItem(0, 0)
                    }
                }
            }
        }

        itemsIndexed(
            items,
            key = { _, item ->
                when {
                    item.isUnseenSeparator -> "unseen-separator-${item.message.id}"
                    item.isSeparator -> "date-separator-${item.message.id}"
                    else -> "message-${item.message.id}"
                }
            }
        ) { index, item ->

            LaunchedEffect(index) {
                if (index >= items.size - 10 && !isLoadingMore) {
                    chatViewModel.loadMoreMessages()
                }
            }

            when {
                item.isUnseenSeparator -> {
                    UnseenSeparator()
                }

                item.isSeparator -> {
                    DateSeparator(item)
                }

                else -> {
                    ChatMessageUI(item, chatViewModel)
                }
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
                    contentColor = MaterialTheme.colorScheme.onBackground
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