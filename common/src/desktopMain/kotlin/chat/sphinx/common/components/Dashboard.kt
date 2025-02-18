package chat.sphinx.common.components

import CommonButton
import Roboto
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.sphinx.common.Res
import chat.sphinx.common.components.chat.AttachmentPreview
import chat.sphinx.common.components.chat.MessagePinnedFullContent
import chat.sphinx.common.components.chat.MessagePinnedPopUp
import chat.sphinx.common.components.menu.ChatAction
import chat.sphinx.common.components.pin.PINScreen
import chat.sphinx.common.components.tribe.NotificationLevel
import chat.sphinx.common.models.DashboardChat
import chat.sphinx.common.state.*
import chat.sphinx.common.viewmodel.DashboardViewModel
import chat.sphinx.common.viewmodel.LockedDashboardViewModel
import chat.sphinx.common.viewmodel.ThreadsViewModel
import chat.sphinx.common.viewmodel.WebAppViewModel
import chat.sphinx.common.viewmodel.chat.ChatContactViewModel
import chat.sphinx.common.viewmodel.chat.ChatTribeViewModel
import chat.sphinx.common.viewmodel.chat.ChatViewModel
import chat.sphinx.platform.imageResource
import chat.sphinx.response.LoadResponse
import chat.sphinx.response.Response
import chat.sphinx.utils.onKeyUp
import chat.sphinx.wrapper.chat.*
import chat.sphinx.wrapper.dashboard.RestoreProgress
import chat.sphinx.wrapper.lightning.asFormattedString
import chat.sphinx.wrapper.message.media.isImage
import chat.sphinx.wrapper.message.retrieveTextToShow
import chat.sphinx.wrapper.thumbnailUrl
import chat.sphinx.wrapper.util.getInitials
import chat.sphinx.wrapper_message.ThreadUUID
import kotlinx.coroutines.launch
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import theme.*
import utils.AnimatedContainer
import java.awt.Cursor

@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.cursorForHorizontalResize(): Modifier =
    pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))

@OptIn(ExperimentalSplitPaneApi::class)
@Composable
actual fun Dashboard(
    dashboardViewModel: DashboardViewModel
) {
    val splitterState = rememberSplitPaneState()
    var chatViewModel: ChatViewModel? = null

    val webAppViewModel = remember { WebAppViewModel() }
    val splitScreenState by dashboardViewModel.splitScreenStateFlow.collectAsState()

    when (DashboardScreenState.screenState()) {
        DashboardScreenType.Unlocked -> {

            dashboardViewModel.screenInit()

            HorizontalSplitPane(
                splitPaneState = splitterState
            ) {
                val chatDetailState = ChatDetailState.screenState()
                val dashboardChat = (chatDetailState as? ChatDetailData.SelectedChatDetailData)?.dashboardChat

                chatViewModel?.readMessages()
                chatViewModel?.cancelMessagesJob()

                chatViewModel = when (chatDetailState) {
                    is ChatDetailData.SelectedChatDetailData.SelectedContactDetail -> {
                        ChatContactViewModel(null, chatDetailState.contactId!!, dashboardViewModel)
                    }
                    is ChatDetailData.SelectedChatDetailData.SelectedContactChatDetail -> {
                        ChatContactViewModel(chatDetailState.chatId!!, chatDetailState.contactId!!, dashboardViewModel)
                    }
                    is ChatDetailData.SelectedChatDetailData.SelectedTribeChatDetail -> {
                        ChatTribeViewModel(chatDetailState.chatId!!, dashboardViewModel)
                    }
                    else -> {
                        null
                    }
                }

                first(300.dp) {
                    DashboardSidebarUI(dashboardViewModel, webAppViewModel)
                }

                second(700.dp) {
                    if (splitScreenState.isOpen) {
                        HorizontalSplitPane {

                            first(500.dp) {
                                val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))

                                Scaffold(
                                    scaffoldState = scaffoldState,
                                    topBar = {
                                        SphinxChatDetailTopAppBar(dashboardChat, chatViewModel, dashboardViewModel, webAppViewModel)
                                    },
                                    bottomBar = {
                                        SphinxChatDetailBottomAppBar(dashboardChat, chatViewModel)
                                    }
                                ) { paddingValues ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                                            .padding(paddingValues),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        chatViewModel?.let { vm ->
                                            MessageListUI(vm, dashboardViewModel, dashboardChat)
                                        }
                                    }

                                    AttachmentPreview(chatViewModel, Modifier.padding(paddingValues))
                                    MessagePinnedPopUp(chatViewModel, Modifier.padding(paddingValues))
                                    MessagePinnedFullContent(chatViewModel, Modifier.padding(paddingValues))
                                    ChatAction(chatViewModel, Modifier.padding(paddingValues))
                                    NotificationLevel(chatViewModel, Modifier.padding(paddingValues))
                                }
                            }

                            second(200.dp) {
                                val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))

                                Scaffold(
                                    scaffoldState = scaffoldState,
                                    topBar = {
                                        SplitTopBar(
                                            chatViewModel,
                                            dashboardViewModel,
                                            splitScreenState.type
                                        )
                                    },
                                    bottomBar = {
                                        val screen = splitScreenState.type
                                        if (screen is DashboardViewModel.SplitContentType.Thread) {
                                            SphinxChatDetailBottomAppBar(dashboardChat, chatViewModel, screen.threadUUID)
                                        }
                                    }
                                ) { innerPadding ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                                            .padding(innerPadding)
                                    ) {
                                        when (val screen = splitScreenState.type) {
                                            is DashboardViewModel.SplitContentType.Threads -> {
                                                val threadsViewModel = remember { ThreadsViewModel(screen.chatId, dashboardViewModel, chatViewModel) }

                                                ThreadsListUI(
                                                    threadsViewModel = threadsViewModel,
                                                    dashboardViewModel = dashboardViewModel,
                                                    chatViewModel = chatViewModel
                                                )
                                            }
                                            is DashboardViewModel.SplitContentType.Thread -> {
                                                chatViewModel?.let {
                                                    MessageListUI(it,dashboardViewModel, dashboardChat, true)
                                                    AttachmentPreview(chatViewModel, Modifier.padding(innerPadding), true)

                                                }
                                            }
                                            else -> {
                                                Text("No content type selected or not handled.")
                                            }
                                        }
                                    }
                                }
                            }

                            splitter {
                                visiblePart {
                                    Box(
                                        Modifier.width(1.dp)
                                            .fillMaxHeight()
                                            .background(MaterialTheme.colors.background)
                                    )
                                }
                                handle {
                                    Box(
                                        Modifier.markAsHandle()
                                            .cursorForHorizontalResize()
                                            .background(SolidColor(Color.Gray), alpha = 0.50f)
                                            .width(9.dp)
                                            .fillMaxHeight()
                                    )
                                }
                            }
                        }
                    } else {
                        val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))

                        Scaffold(
                            scaffoldState = scaffoldState,
                            topBar = {
                                SphinxChatDetailTopAppBar(dashboardChat, chatViewModel, dashboardViewModel, webAppViewModel)
                            },
                            bottomBar = {
                                SphinxChatDetailBottomAppBar(dashboardChat, chatViewModel)
                            }
                        ) { paddingValues ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                                    .padding(paddingValues),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                chatViewModel?.let { chatViewModel ->
                                    MessageListUI(chatViewModel, dashboardViewModel, dashboardChat)
                                }
                            }
                            AttachmentPreview(
                                chatViewModel,
                                Modifier.padding(paddingValues)
                            )
                            MessagePinnedPopUp(
                                chatViewModel,
                                Modifier.padding(paddingValues)
                            )
                            MessagePinnedFullContent(
                                chatViewModel,
                                Modifier.padding(paddingValues)
                            )
                            ChatAction(
                                chatViewModel,
                                Modifier.padding(paddingValues)
                            )
                            NotificationLevel(
                                chatViewModel,
                                Modifier.padding(paddingValues)
                            )
                        }
                    }
                }
                splitter {
                    visiblePart {
                        Box(
                            Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colors.background)
                        )
                    }
                    handle {
                        Box(
                            Modifier.markAsHandle().cursorForHorizontalResize()
                                .background(SolidColor(Color.Gray), alpha = 0.50f).width(9.dp).fillMaxHeight()
                        )
                    }
                }
            }

            ImageFullScreen(fullScreenImageState)

            val restoreState by dashboardViewModel.restoreProgressStateFlow.collectAsState()
            restoreState?.let { restoreState ->
                if (restoreState.restoring && !dashboardViewModel.isRestoreCancelledState) {
                    RestoreProgressUI(
                        dashboardViewModel,
                        restoreState
                    )
                }
            }
        }
        DashboardScreenType.Locked -> {
            val lockedDashboardViewModel = remember { LockedDashboardViewModel() }
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1f).fillMaxHeight().background(SolidColor(Color.Black), alpha = 0.50f)
                ) {
                    Column(
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        PINScreen(lockedDashboardViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun SphinxChatDetailTopAppBar(
    dashboardChat: DashboardChat?,
    chatViewModel: ChatViewModel?,
    dashboardViewModel: DashboardViewModel?,
    webAppViewModel: WebAppViewModel
) {
    val uriHandler = LocalUriHandler.current

    if (dashboardChat == null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(60.dp)
                .fillMaxWidth()
                .background(color = androidx.compose.material3.MaterialTheme.colorScheme.background)
        ) {
            Text(
                modifier = Modifier.padding(16.dp, 0.dp),
                text = "Open a conversation to start using Sphinx",
                fontFamily = Roboto,
                fontSize = 16.sp,
                fontWeight = FontWeight.W700,
                color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary
            )
        }
        return
    }

    val chatName = dashboardChat.chatName ?: "Unknown Chat"
    val contactId = chatViewModel?.editMessageState?.contactId

    Column {
        TopAppBar(
            modifier = Modifier.height(60.dp),
            title = {
                Column {
                    Row {
                        Text(
                            text = chatName, fontSize = 16.sp, fontWeight = FontWeight.W700,
                            modifier = Modifier.clickable {
                                if (dashboardChat.isTribe()) {
                                    chatViewModel?.chatId?.let { dashboardViewModel?.toggleTribeDetailWindow(true, it) }
                                } else {
                                    dashboardViewModel?.toggleContactWindow(true, ContactScreenState.EditContact(contactId))
                                }
                            }
                        )

                        Icon(
                            if (dashboardChat?.isEncrypted() == true) Icons.Default.Lock else Icons.Default.LockOpen,
                            "Lock",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(23.dp).padding(4.dp, 0.dp, 4.dp, 2.dp)
                        )

                        chatViewModel?.let {
                            val checkChatStatus by chatViewModel.checkChatStatus.collectAsState(
                                LoadResponse.Loading
                            )
                            val color = when (checkChatStatus) {
                                is LoadResponse.Loading -> {
                                    androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                                }
                                is Response.Error -> {
                                    sphinx_orange
                                }
                                is Response.Success -> {
                                    primary_green
                                }
                            }

                            Icon(
                                Icons.Default.FlashOn,
                                "Route",
                                tint = color,
                                modifier = Modifier.width(15.dp).height(23.dp).padding(0.dp, 0.dp, 0.dp, 2.dp)
                            )
                        }
                    }

                    chatViewModel?.let {
                        val chat by chatViewModel.chatSharedFlow.collectAsState(
                            (dashboardChat as? DashboardChat.Active)?.chat
                        )

                        chat?.let { nnChat ->
                            if (nnChat.isTribe()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Price per message: ${nnChat.pricePerMessage?.asFormattedString(' ', false) ?: 0} - Amount to stake: ${nnChat.escrowAmount?.asFormattedString(' ', false) ?: 0}",
                                    fontSize = 11.sp,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
                // TODO: Lighting Indicator...
            },
            backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
            elevation = 8.dp,
            navigationIcon = {
                Spacer(modifier = Modifier.width(14.dp))
                PhotoUrlImage(
                    dashboardChat.photoUrl,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape),
                    firstNameLetter = (dashboardChat.chatName ?: "Unknown Chat").getInitials(),
                    color = if (dashboardChat.color != null) Color(dashboardChat.color!!) else null,
                    fontSize = 16
                )
            },
            actions = {
                chatViewModel?.let {
                    val tribeData by chatViewModel.tribeDataStateFlow.collectAsState(null)

                    tribeData?.let {
                        if (it.appUrl != null) {
                            IconButton(onClick = {
                                webAppViewModel.toggleWebAppWindow(true, tribeData?.appUrl?.value)
                            }) {
                                Icon(
                                    Icons.Default.Apps,
                                    contentDescription = "WebApp",
                                    tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                        IconButton(onClick = {
                            dashboardViewModel?.toggleSplitScreen(true,
                                chatViewModel.chatId?.let { it1 -> DashboardViewModel.SplitContentType.Threads(it1) })
                        }) {
                            chatViewModel.let {
//                                val chat by chatViewModel.chatSharedFlow.collectAsState(
//                                    (dashboardChat as? DashboardChat.Active)?.chat
//                                )
                                Icon(
                                    Icons.Default.Forum,
                                    contentDescription = "Thread",
                                    tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
                IconButton(onClick = {
                    chatViewModel?.toggleChatMuted()
                }) {
                    chatViewModel?.let {
                        val chat by chatViewModel.chatSharedFlow.collectAsState(
                            (dashboardChat as? DashboardChat.Active)?.chat
                        )
                        Icon(
                            if (chat?.isMuted() == true) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                            contentDescription = "Mute/Unmute",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                IconButton(onClick = {
                    chatViewModel?.sendCallInvite(false) { link ->
                        uriHandler.openUri(link)
                    }
                }) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "Call",
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        )
        if (chatViewModel?.pinMessageState?.pinMessage?.value != null) {
            Box(
                modifier = Modifier
                    .height(46.dp)
                    .fillMaxWidth()
                    .background(color = androidx.compose.material3.MaterialTheme.colorScheme.background)
                    .clickable {
                        chatViewModel.pinFullContentScreen()
                    }
            ) {
                Divider(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pin",
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Text(
                        text = chatViewModel.pinMessageState.pinMessage.value?.message?.retrieveTextToShow() ?: "",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
                        fontSize = 12.sp,
                        fontFamily = Roboto,
                        fontWeight = FontWeight.W500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SphinxChatDetailBottomAppBar(
    dashboardChat: DashboardChat?,
    chatViewModel: ChatViewModel?,
    threadUUID: ThreadUUID? = null
) {
    val scope = rememberCoroutineScope()

    var textFieldValueState by remember {
        mutableStateOf(
            TextFieldValue(
                text = ""
            )
        )
    }

    Surface(
        color = androidx.compose.material3.MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        elevation = 8.dp,
    ) {
        Column {
            MessageReplyingBar(chatViewModel)

            Row(
                modifier = Modifier.fillMaxWidth().defaultMinSize(Dp.Unspecified, 60.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(
                    onClick = {
                        if (chatViewModel is ChatTribeViewModel) {
                            scope.launch {
                                ContentState.sendFilePickerDialog.awaitResult()?.let { path ->
                                    chatViewModel.hideChatActionsPopup()
                                    chatViewModel.onMessageFileChanged(path, threadUUID)
                                }
                            }
                        } else {
                            chatViewModel?.toggleChatActionsPopup(ChatViewModel.ChatActionsMode.MENU)
                        }
                    },
                    modifier = Modifier.clip(CircleShape)
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.secondary)
                        .size(30.dp),
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "content description",
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(onClick = {}, modifier = Modifier.height(25.dp).width(18.dp)) {
                    Image(
                        painter = imageResource(Res.drawable.ic_giphy),
                        contentDescription = "giphy",
                        contentScale = ContentScale.FillBounds
                    )
                }
                IconButton(
                    onClick = {},
                    modifier = Modifier.clip(CircleShape)
                        .background(color = androidx.compose.material3.MaterialTheme.colorScheme.background)
                        .wrapContentSize(),
                ) {
                    Icon(
                        Icons.Outlined.EmojiEmotions,
                        contentDescription = "Emoji",
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val textValue = if (threadUUID != null) {
                            chatViewModel?.threadMessageState?.messageText?.value ?: TextFieldValue("")
                        } else {
                            chatViewModel?.editMessageState?.messageText?.value ?: TextFieldValue("")
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        CustomTextField(
                            trailingIcon = null,
                            modifier = Modifier
                                .background(
                                    androidx.compose.material3.MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .defaultMinSize(Dp.Unspecified, 32.dp)
                                .onKeyEvent(
                                    onKeyUp(
                                        Key.Enter
                                    ) {
                                        if (chatViewModel?.aliasMatcherState?.isOn == true) {
                                            chatViewModel?.onAliasSelected()
                                        } else {
                                            chatViewModel?.onSendMessage(threadUUID?.value)
                                        }
                                    }
                                )
                                .onKeyEvent(
                                    onKeyUp(
                                        Key.DirectionDown
                                    ) {
                                        chatViewModel?.onAliasNextFocus()
                                    }
                                )
                                .onKeyEvent(
                                    onKeyUp(
                                        Key.DirectionUp
                                    ) {
                                        chatViewModel?.onAliasPreviousFocus()
                                    }
                                )
                                .onKeyEvent(
                                    onKeyUp(
                                        Key.Tab
                                    ) {
                                        chatViewModel?.onAliasSelected()
                                    }
                                ),
                            color = Color.White,
                            fontSize = 16.sp,
                            placeholderText = "Message...",
                            singleLine = false,
                            maxLines = 4,
                            onValueChange = { newValue ->
                                val proposedText = newValue.text
                                val proposedTextBytes = proposedText.toByteArray().size
                                if (proposedTextBytes <= 592) {
                                    if (threadUUID != null) {
                                        chatViewModel?.onThreadMessageTextChanged(newValue)
                                    } else {
                                        chatViewModel?.onMessageTextChanged(newValue)
                                    }
                                } else {}
                            },
                            value = textValue ,
                            cursorBrush = primary_blue,
                            enabled = !(dashboardChat?.getChatOrNull()?.isPrivateTribe() == true && dashboardChat?.getChatOrNull()?.status?.isPending() == true)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(modifier = Modifier.width(10.dp))

                        if (threadUUID == null) {
                            PriceChip(chatViewModel)
                        }

                        Spacer(modifier = Modifier.width(10.dp))
                        IconButton(
                            onClick = {
                                if (chatViewModel != null) run {
                                    chatViewModel.onSendMessage(threadUUID?.value)
                                }
                            },
                            modifier = Modifier.clip(CircleShape)
                                .background(androidx.compose.material3.MaterialTheme.colorScheme.secondary)
                                .size(30.dp),
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Send Message",
                                tint = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        // TODO: Record Action
                        IconButton(
                            onClick = {},
                            modifier = Modifier.clip(CircleShape)
                                .background(color = androidx.compose.material3.MaterialTheme.colorScheme.background)
                                .wrapContentSize(),
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Microphone",
                                tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(27.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplitTopBar(
    chatViewModel: ChatViewModel?,
    dashboardViewModel: DashboardViewModel?,
    splitType: DashboardViewModel.SplitContentType?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(60.dp)
                .fillMaxWidth()
                .background(color = androidx.compose.material3.MaterialTheme.colorScheme.background)
        ) {
            val fromThreadsView = (splitType as? DashboardViewModel.SplitContentType.Thread)?.fromThreadsScreen ?: false
            // Left Arrow Icon
            IconButton(
                onClick = {
                    if (!fromThreadsView) {
                        dashboardViewModel?.toggleSplitScreen(
                            false,
                            null
                        )
                    } else {
                        dashboardViewModel?.toggleSplitScreen(
                            true,
                            chatViewModel?.chatId?.let { DashboardViewModel.SplitContentType.Threads(it)}
                        )
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.tertiary
                )
            }

            val threadText = when (splitType) {
                is DashboardViewModel.SplitContentType.Threads -> "Threads List"
                is DashboardViewModel.SplitContentType.Thread -> "Thread"
                else -> ""
            }

            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = threadText,
                fontFamily = Roboto,
                fontSize = 16.sp,
                fontWeight = FontWeight.W700,
                color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary
            )

            Spacer(modifier = Modifier.weight(1f))

            // Close Icon
            IconButton(
                onClick = {
                    dashboardViewModel?.toggleSplitScreen(false,
                        chatViewModel?.chatId?.let { DashboardViewModel.SplitContentType.Threads(it) })
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.tertiary
                )
            }
        }
        if (splitType is DashboardViewModel.SplitContentType.Thread) {
            // Retrieve the thread header from the thread screen state.
            when (val messageListData = MessageListState.threadScreenState()) {
                is MessageListData.PopulatedMessageListData -> {
                    val chatMessages = messageListData.messages
                    val threadHeader = chatMessages.lastOrNull()
                    if (threadHeader != null) {
                        ThreadHeaderUI(threadHeader, chatViewModel)
                    }
                }
                else -> {}
            }
        }
    }
}

@Suppress("SuspiciousIndentation")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SuggestedAliasListBar(
    chatViewModel: ChatViewModel
) {

    if (chatViewModel.aliasMatcherState.isOn) {
        AnimatedContainer(
            fromTopToBottom = 20,
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .background(color = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer)
        ) {

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column() {
                    chatViewModel.aliasMatcherState.suggestedAliasAndPicList.forEachIndexed() { index, suggestedList ->
                        val backgroundColor = if (index == chatViewModel.aliasMatcherState.selectedItem) androidx.compose.material3.MaterialTheme.colorScheme.background else androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer
                            Row(
                                modifier = Modifier
                                    .height(40.dp)
                                    .fillMaxWidth()
                                    .background(backgroundColor)
                                    .padding(start = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val color = suggestedList.third

                                PhotoUrlImage(
                                    photoUrl = suggestedList.second?.thumbnailUrl,
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape),
                                    color = if (color != null) Color(color) else null,
                                    firstNameLetter = suggestedList.first.getInitials(),
                                    fontSize = 12
                                )

                                Spacer(Modifier.width(8.dp))

                                Text(
                                    suggestedList.first,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                                    fontSize = 12.sp,
                                    fontFamily = Roboto,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Divider(color = light_divider)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageReplyingBar(
    chatViewModel: ChatViewModel?
) {
    chatViewModel?.editMessageState?.replyToMessage?.value?.let { replyToMessage ->
        AnimatedContainer(
            fromTopToBottom = 20,
            modifier = Modifier
                .height(44.dp)
                .fillMaxWidth()
                .background(color = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer)
        ) {
            Box {
                Row(
                    modifier = Modifier
                        .height(44.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(
                                if (replyToMessage.replyToMessageColor != null) {
                                    Color(replyToMessage.replyToMessageColor!!)
                                } else {
                                    Color.Gray
                                }
                            )
                    )
                    replyToMessage.message.messageMedia?.let { media ->
                        if (media.mediaType.isImage) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = "Image",
                                tint = Color.Gray,
                                modifier = Modifier.height(88.dp).padding(start = 10.dp)
                            )
                        } else {
                            // show
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = "Attachment",
                                tint = Color.Gray,
                                modifier = Modifier.height(88.dp).padding(start = 10.dp)
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(start = 10.dp, end = 40.dp)
                    ) {
                        Text(
                            replyToMessage.replyToMessageSenderAliasPreview,
                            overflow = TextOverflow.Ellipsis,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
                            fontFamily = Roboto,
                            fontWeight = FontWeight.W600,
                            fontSize = 13.sp,
                            maxLines = 1,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            replyToMessage.replyToMessageTextPreview,
                            overflow = TextOverflow.Ellipsis,
                            color = place_holder_text,
                            fontWeight = FontWeight.W400,
                            fontFamily = Roboto,
                            fontSize = 11.sp,
                            maxLines = 1,
                        )
                    }
                    Box(
                        contentAlignment = Alignment.CenterEnd,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            Icons.Default.Close,
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
                            contentDescription = "Close reply to message",
                            modifier = Modifier.size(20.dp)
                                .align(Alignment.CenterEnd)
                                .clickable(
                                    onClick = {
                                        chatViewModel?.editMessageState?.replyToMessage?.value = null
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RestoreProgressUI(
    dashboardViewModel: DashboardViewModel,
    restoreState: RestoreProgress
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(SolidColor(androidx.compose.material3.MaterialTheme.colorScheme.background), alpha = 0.5f)
            .fillMaxSize()
    ) {
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(SolidColor(androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant), RoundedCornerShape(10.dp))
                .width(300.dp),
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Restoring: ${restoreState.progress}%",
                fontFamily = Roboto,
                fontSize = 15.sp,
                fontWeight = FontWeight.W500,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(32.dp))
            LinearProgressIndicator(
                progress = restoreState.progress.toFloat() / 100,
                modifier = Modifier.fillMaxWidth(0.8f),
                color = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth(0.8f)) {
                CommonButton(text = "Continue Later") {
                    dashboardViewModel.cancelRestore()
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
