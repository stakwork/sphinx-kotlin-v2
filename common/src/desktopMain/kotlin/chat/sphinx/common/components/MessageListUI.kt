package chat.sphinx.common.components

import Roboto
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.ui.text.TextStyle
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
import kotlinx.coroutines.flow.distinctUntilChanged
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

    suspend fun LazyListState.scrollItemToTop(index: Int) {
        scrollToItem(index, 0)
        kotlinx.coroutines.yield()

        val li = layoutInfo
        val item = li.visibleItemsInfo.firstOrNull { it.index == index } ?: return

        val desiredTopFromStart = li.viewportEndOffset - li.afterContentPadding - item.size
        val delta = (item.offset - desiredTopFromStart).toFloat()
        if (delta != 0f) {
            scrollBy(delta)
        }
    }

    Box {
        if (isThreadView) {
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

                    LaunchedEffect(chatMessages, shouldShowUnseenSeparator, firstUnseenMessageId) {
                        val newSize = chatMessages.size
                        val currentLastMessageId = chatMessages.firstOrNull()?.message?.id?.value

                        val hasNewMessages = newSize > previousItemsSize && previousItemsSize > 0
                        val hasNewMessage = currentLastMessageId != previousLastMessageId && previousLastMessageId != null

                        updateItemsEfficiently(items, chatMessages)

                        if (shouldShowUnseenSeparator && firstUnseenMessageId != null && previousItemsSize == 0) {
                            val targetIndex = items.indexOfFirst {
                                it.isUnseenSeparator || (it.message.id.value == firstUnseenMessageId)
                            }
                            if (targetIndex >= 0) {
                                kotlinx.coroutines.delay(50)
                                listState.scrollItemToTop(targetIndex) // ⬅️ place at TOP
                                wasAtBottom = false
                            }
                        } else if ((hasNewMessages || hasNewMessage) && wasAtBottom) {
                            kotlinx.coroutines.delay(10)
                            listState.scrollToItem(0, 0)
                        }

                        previousItemsSize = newSize
                        previousLastMessageId = currentLastMessageId
                    }

                    LaunchedEffect(isAtBottom) {
                        wasAtBottom = isAtBottom
                        if (isAtBottom && shouldShowUnseenSeparator) {
                            chatViewModel.readMessages()
                        }
                    }

                    LaunchedEffect(messageListData.chatId) {
                        chatViewModel.onNewMessageCallback = {
                            localScope.launch {
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
                                modifier = Modifier.padding(end = 16.dp, bottom = 16.dp)
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
                    val items = remember(messageListData.chatId) { mutableStateListOf<ChatMessage>() }

                    var previousItemsSize by remember(messageListData.chatId) { mutableStateOf(0) }
                    var previousLastMessageId by remember(messageListData.chatId) { mutableStateOf<Long?>(null) }
                    var wasAtBottom by remember(messageListData.chatId) { mutableStateOf(true) }
                    var hasScrolledToUnseen by remember(messageListData.chatId) { mutableStateOf(false) }

                    val isAtBottom by rememberIsAtBottom(listState)
                    var bottomAnchorMessageId by remember(messageListData.chatId) { mutableStateOf<Long?>(null) }
                    var unseenIncomingWhileScrolledUp by remember(messageListData.chatId) { mutableStateOf(0) }

                    LaunchedEffect(chatMessages, shouldShowUnseenSeparator, firstUnseenMessageId) {
                        val newSize = chatMessages.size
                        val currentLastMessageId = chatMessages.firstOrNull()?.message?.id?.value

                        val hasNewMessages = newSize > previousItemsSize && previousItemsSize > 0
                        val hasNewMessage = currentLastMessageId != previousLastMessageId && previousLastMessageId != null
                        val isInitialLoad = previousItemsSize == 0

                        updateItemsEfficiently(items, chatMessages)

                        if (shouldShowUnseenSeparator && firstUnseenMessageId != null && isInitialLoad && !hasScrolledToUnseen) {
                            val unseenSeparatorIndex = items.indexOfFirst { it.isUnseenSeparator }
                            val firstUnseenMessageIndex = items.indexOfFirst {
                                !it.isSeparator && !it.isUnseenSeparator && it.message.id.value == firstUnseenMessageId
                            }
                            val targetIndex = if (unseenSeparatorIndex >= 0) unseenSeparatorIndex else firstUnseenMessageIndex

                            if (targetIndex >= 0) {
                                kotlinx.coroutines.delay(100)
                                listState.scrollItemToTop(targetIndex) // ⬅️ place at TOP
                                wasAtBottom = false
                                hasScrolledToUnseen = true

                                // Initialize badge count using items below current scroll
                                unseenIncomingWhileScrolledUp = items
                                    .take(targetIndex)
                                    .count { it.isIncomingMessage() && !it.isSeparator }
                            }
                        } else if ((hasNewMessages || hasNewMessage) && wasAtBottom) {
                            kotlinx.coroutines.delay(10)
                            listState.scrollToItem(0, 0)
                        } else if (isInitialLoad && !shouldShowUnseenSeparator) {
                            kotlinx.coroutines.delay(10)
                            listState.scrollToItem(0, 0)
                        }

                        previousItemsSize = newSize
                        previousLastMessageId = currentLastMessageId
                    }

                    LaunchedEffect(isAtBottom) {
                        wasAtBottom = isAtBottom
                        if (isAtBottom) {
                            unseenIncomingWhileScrolledUp = 0
                            if (shouldShowUnseenSeparator) {
                                chatViewModel.hideUnseenSeparator()
                            }
                        }
                    }

                    LaunchedEffect(messageListData.chatId) {
                        hasScrolledToUnseen = false
                        chatViewModel.onNewMessageCallback = {
                            localScope.launch {
                                val currentIsAtBottom = listState.firstVisibleItemIndex <= 1 &&
                                        listState.firstVisibleItemScrollOffset <= 100
                                if (currentIsAtBottom || wasAtBottom) {
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
                                        unseenIncomingWhileScrolledUp = 0
                                        chatViewModel.hideUnseenSeparator()
                                    }
                                },
                                modifier = Modifier.padding(end = 16.dp, bottom = 16.dp)
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
    var debounceJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var debouncedIsAtBottom by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    val immediateIsAtBottom = remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val firstVisibleIndex = listState.firstVisibleItemIndex
            val firstVisibleOffset = listState.firstVisibleItemScrollOffset

            val atAbsoluteBottom = firstVisibleIndex == 0 && firstVisibleOffset == 0
            val nearBottom = firstVisibleIndex == 0 && firstVisibleOffset <= 50
            val singleItemAtTop = layoutInfo.visibleItemsInfo.size == 1 &&
                    layoutInfo.visibleItemsInfo.firstOrNull()?.index == 0

            atAbsoluteBottom || nearBottom || singleItemAtTop
        }
    }

    LaunchedEffect(immediateIsAtBottom.value) {
        debounceJob?.cancel()

        if (immediateIsAtBottom.value) {
            // If at bottom, update immediately
            debouncedIsAtBottom = true
        } else {
            // If not at bottom, wait a bit before updating
            debounceJob = scope.launch {
                kotlinx.coroutines.delay(150) // Debounce delay
                debouncedIsAtBottom = immediateIsAtBottom.value
            }
        }
    }

    return remember { derivedStateOf { debouncedIsAtBottom } }
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
    val isLoadingMore by chatViewModel.isLoadingMore.collectAsState()

    LaunchedEffect(items.size) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItemsCount = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            Triple(lastVisibleItemIndex, totalItemsCount, isLoadingMore)
        }
            .distinctUntilChanged()
            .collect { (lastVisibleItemIndex, totalItemsCount, currentlyLoading) ->
                println("layoutInfo: ${listState.layoutInfo} totalItemsCount $totalItemsCount, lastVisibleItemIndex: $lastVisibleItemIndex, isLoadingMore: $currentlyLoading")

                if (lastVisibleItemIndex >= totalItemsCount - 10 &&
                    totalItemsCount > 0 &&
                    !currentlyLoading) {
                    chatViewModel.loadMoreMessages()
                    println("Triggered loadMoreMessages from snapshotFlow, items size ${items.size}")
                }
            }
    }

    LazyColumn(
        state = listState,
        reverseLayout = true,
        contentPadding = PaddingValues(8.dp)
    ) {
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
            key = { index, item ->
                when {
                    item.isUnseenSeparator -> "unseen-separator-$index"
                    item.isSeparator -> "date-separator-$index"
                    else -> "message-${item.message.id}-$index"
                }
            }
        ) { index, item ->
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

        // Show loading indicator at the END of the list (which appears at the TOP in reverse layout)
        if (isLoadingMore) {
            item(key = "loading-more-indicator") {
                LoadingMoreIndicator()
            }
        }
    }
}

@Composable
fun LoadingMoreIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Loading more messages...",
                style = TextStyle(
                    fontSize = 13.sp,
                    fontFamily = Roboto,
                    fontWeight = FontWeight.W400,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
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
