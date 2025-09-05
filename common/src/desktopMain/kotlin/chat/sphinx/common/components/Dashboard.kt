package chat.sphinx.common.components

import CommonButton
import Roboto
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Gif
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import chat.sphinx.common.components.tribe.TribeDetailView
import chat.sphinx.common.components.tribe.TribeMembersView
import chat.sphinx.common.models.DashboardChat
import chat.sphinx.common.state.*
import chat.sphinx.common.viewmodel.*
import chat.sphinx.common.viewmodel.chat.ChatContactViewModel
import chat.sphinx.common.viewmodel.chat.ChatTribeViewModel
import chat.sphinx.common.viewmodel.chat.ChatViewModel
import chat.sphinx.common.viewmodel.chat.TribeMembersViewModel
import chat.sphinx.common.viewmodel.contact.QRCodeViewModel
import chat.sphinx.platform.imageResource
import chat.sphinx.response.LoadResponse
import chat.sphinx.response.Response
import chat.sphinx.utils.onKeyUp
import chat.sphinx.wrapper.DateTime
import chat.sphinx.wrapper.chat.*
import chat.sphinx.wrapper.dashboard.ChatId
import chat.sphinx.wrapper.dashboard.RestoreProgress
import chat.sphinx.wrapper.feed.FeedId
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

    val webAppViewModel = remember { WebAppViewModel(dashboardViewModel) }
    val feedViewModel = remember { FeedViewModel(dashboardViewModel) }

    val splitScreenState by dashboardViewModel.splitScreenStateFlow.collectAsState()
    val fullScreenViewState by dashboardViewModel.fullScreenViewStateFlow.collectAsState()
    val isSidebarHidden by dashboardViewModel.isSidebarHiddenFlow.collectAsState()
    val selectedTabIndex by dashboardViewModel.selectedTabStateFlow.collectAsState()
    val floatingPlayerState by dashboardViewModel.floatingPlayerStateFlow.collectAsState()

    LaunchedEffect(selectedTabIndex, splitScreenState) {
        val playingContent = dashboardViewModel.mediaPlayerHolder.getPlayingContent()
        if (playingContent?.third == true) { // isPlaying
            val isPodcastSplit = splitScreenState.type is DashboardViewModel.SplitContentType.Podcast
            val isFeedTabSelected = selectedTabIndex == 2

            if (isFeedTabSelected && isPodcastSplit) {
                dashboardViewModel.hideFloatingPlayer()
            } else if (playingContent.first.isNotEmpty()) {
                dashboardViewModel.showFloatingPlayer(
                    ChatId(ChatId.NULL_CHAT_ID.toLong()),
                    FeedId(playingContent.first)
                )
            }
        } else {
            dashboardViewModel.hideFloatingPlayer()
        }
    }

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
                    else -> null
                }

                first(if (isSidebarHidden) 0.dp else 300.dp) {
                    DashboardSidebarUI(dashboardViewModel, webAppViewModel, feedViewModel)
                }

                second(if (isSidebarHidden) 1000.dp else 700.dp) {
                    if (splitScreenState.isOpen) {
                        HorizontalSplitPane {

                            first(500.dp) {
                                val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))

                                val isFeedTabSelected = selectedTabIndex == 2
                                val isPodcastSplit = splitScreenState.type is DashboardViewModel.SplitContentType.Podcast

                                if (isFeedTabSelected && isPodcastSplit) {
                                    Scaffold(
                                        scaffoldState = scaffoldState,
                                    ) { paddingValues ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                                                .padding(paddingValues)
                                        ) {
                                            FeedListUI(feedViewModel)
                                        }
                                    }
                                } else {
                                    Scaffold(
                                        scaffoldState = scaffoldState,
                                        topBar = {
                                            SphinxChatDetailTopAppBar(
                                                dashboardChat,
                                                chatViewModel,
                                                dashboardViewModel,
                                                webAppViewModel
                                            )
                                        },
                                        bottomBar = {
                                            SphinxChatDetailBottomAppBar(dashboardChat, chatViewModel, isThreadView = false)
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
                                            SphinxChatDetailBottomAppBar(
                                                dashboardChat,
                                                chatViewModel,
                                                screen.threadUUID,
                                                true
                                            )
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
                                                val threadsViewModel = remember {
                                                    ThreadsViewModel(
                                                        screen.chatId,
                                                        dashboardViewModel,
                                                        chatViewModel
                                                    )
                                                }

                                                ThreadsListUI(
                                                    threadsViewModel = threadsViewModel,
                                                    dashboardViewModel = dashboardViewModel,
                                                    chatViewModel = chatViewModel
                                                )
                                            }

                                            is DashboardViewModel.SplitContentType.Thread -> {
                                                chatViewModel?.let {
                                                    MessageListUI(it, dashboardViewModel, dashboardChat, true)
                                                    AttachmentPreview(
                                                        chatViewModel,
                                                        Modifier.padding(innerPadding),
                                                        true
                                                    )

                                                }
                                            }

                                            is DashboardViewModel.SplitContentType.TribeDetail -> {
                                                TribeDetailView(dashboardViewModel, screen.chatId)
                                            }

                                            is DashboardViewModel.SplitContentType.TribeMembers -> {
                                                val tribeMemberViewModel =
                                                    remember { TribeMembersViewModel(screen.chatId) }
                                                TribeMembersView(tribeMemberViewModel, dashboardViewModel)
                                            }

                                            is DashboardViewModel.SplitContentType.ContactDetails -> {
                                                ContactForm(dashboardViewModel, screen.contactId)
                                            }

                                            is DashboardViewModel.SplitContentType.QRDetail -> {
                                                val qrCodeViewModel = QRCodeViewModel(screen.title, screen.value)
                                                QRDetailSplitScreen(dashboardViewModel, qrCodeViewModel)
                                            }

                                            is DashboardViewModel.SplitContentType.Podcast -> {
                                                val chatId = screen.chatId
                                                val feedId = screen.feedId
                                                val mediaPlayerHolder = dashboardViewModel.mediaPlayerHolder
                                                val podcastViewModel = dashboardViewModel.getPodcastViewModel(chatId, feedId)

                                                PodcastMainPlayer(
                                                    chatId = chatId ?: ChatId(ChatId.NULL_CHAT_ID.toLong()),
                                                    mediaPlayerHolder = mediaPlayerHolder,
                                                    dashboardViewModel = dashboardViewModel,
                                                    podcastViewModel = podcastViewModel
                                                )
                                            }
                                            else -> {}
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
                        val isFeedTabSelected = selectedTabIndex == 2
                        val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))

                        if (isFeedTabSelected) {
                            Scaffold(
                                scaffoldState = scaffoldState,
                            ) { paddingValues ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                                        .padding(paddingValues)
                                ) {
                                    FeedListUI(feedViewModel)

                                    if (isSidebarHidden) {
                                        IconButton(
                                            onClick = { dashboardViewModel.toggleSidebarVisibility() },
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(start = 0.dp, top = 64.dp, end = 0.dp, bottom = 0.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = "Show Sidebar",
                                                tint = Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Scaffold(
                                scaffoldState = scaffoldState,
                                topBar = {
                                    SphinxChatDetailTopAppBar(
                                        dashboardChat,
                                        chatViewModel,
                                        dashboardViewModel,
                                        webAppViewModel
                                    )
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
                                    if (isSidebarHidden) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                                                .padding(paddingValues)
                                        ) {
                                            IconButton(
                                                onClick = { dashboardViewModel.toggleSidebarVisibility() },
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(start = 0.dp, top = 16.dp, 0.dp, bottom = 16.dp)
                                            ) {
                                                androidx.compose.material.Icon(
                                                    Icons.Default.ChevronRight,
                                                    contentDescription = "Hide",
                                                    tint = Color.White.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                        }
                                    }

                                    chatViewModel?.let { chatViewModel ->
                                        MessageListUI(chatViewModel, dashboardViewModel, dashboardChat)
                                    }
                                }

                                val isGiphyPickerVisible = chatViewModel?.isGiphyPickerVisible?.collectAsState()?.value ?: false

                                if (isGiphyPickerVisible && chatViewModel != null) {
                                    GiphyPickerUI(chatViewModel!!, Modifier.padding(8.dp))
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
            }

            floatingPlayerState?.let { playerState ->
                if (playerState.isVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        val podcastViewModel = dashboardViewModel.getPodcastViewModel(
                            playerState.chatId,
                            playerState.feedId
                        )

                        FloatingPodcastPlayer(
                            chatId = playerState.chatId,
                            mediaPlayerHolder = dashboardViewModel.mediaPlayerHolder,
                            dashboardViewModel = dashboardViewModel,
                            podcastViewModel = podcastViewModel,
                            onClose = { dashboardViewModel.hideFloatingPlayer() }
                        )
                    }
                }
            }

            FullScreenOverlay(
                fullScreenView = fullScreenViewState,
                dashboardViewModel = dashboardViewModel,
                onClose = { dashboardViewModel.closeFullScreenView() }
            )

            ImageFullScreen(fullScreenImageState)
            VideoFullScreen(fullScreenVideoState)

            DetachedWindow(
                dashboardViewModel = dashboardViewModel,
                chatViewModel = chatViewModel,
            )

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
                                    chatViewModel?.chatId?.let {
                                        dashboardViewModel?.toggleTribeDetailSplitScreen(
                                            true,
                                            it
                                        )
                                    }
                                } else {
                                    dashboardViewModel?.toggleEditContactSplitScreen(true, contactId)
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
                        val chat = (dashboardChat as? DashboardChat.Active)?.chat
                        val timezone = chat?.remoteTimezoneIdentifier?.value?.let {
                            DateTime.getLocalTimeFor(it, null)
                        }

                        if (!timezone.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = timezone,
                                fontSize = 11.sp,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
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
                                    text = "Price per message: ${
                                        nnChat.pricePerMessage?.asFormattedString(
                                            ' ',
                                            false
                                        ) ?: 0
                                    } - Amount to stake: ${nnChat.escrowAmount?.asFormattedString(' ', false) ?: 0}",
                                    fontSize = 11.sp,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
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
                                dashboardViewModel?.toggleWebAppWindow(true, tribeData?.appUrl?.value)
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
    threadUUID: ThreadUUID? = null,
    isThreadView: Boolean = false
) {
    val isRecording = if (isThreadView) {
        chatViewModel?.isThreadRecording == true
    } else {
        chatViewModel?.isRecording == true && chatViewModel?.isThreadRecording != true
    }

    if (isRecording && chatViewModel != null) {
        RecordingBottomBar(chatViewModel, threadUUID, isThreadView)
        return
    }

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

                // + button
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
                        contentDescription = "Attach",
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(21.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // TextField + Giphy + Emoji
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            androidx.compose.material3.MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val textValue = if (isThreadView) {
                            chatViewModel?.threadMessageState?.messageText?.value ?: TextFieldValue("")
                        } else {
                            chatViewModel?.editMessageState?.messageText?.value ?: TextFieldValue("")
                        }

                        CustomTextField(
                            trailingIcon = null,
                            modifier = Modifier
                                .defaultMinSize(Dp.Unspecified, 32.dp)
                                .onKeyEvent(onKeyUp(Key.Enter) {
                                    if (chatViewModel?.aliasMatcherState?.isOn == true) {
                                        chatViewModel?.onAliasSelected()
                                    } else {
                                        chatViewModel?.onSendMessage(threadUUID?.value)
                                    }
                                })
                                .onKeyEvent(onKeyUp(Key.DirectionDown) {
                                    chatViewModel?.onAliasNextFocus()
                                })
                                .onKeyEvent(onKeyUp(Key.DirectionUp) {
                                    chatViewModel?.onAliasPreviousFocus()
                                })
                                .onKeyEvent(onKeyUp(Key.Tab) {
                                    chatViewModel?.onAliasSelected()
                                }),
                            color = Color.White,
                            fontSize = 16.sp,
                            placeholderText = "Message...",
                            singleLine = false,
                            maxLines = 4,
                            onValueChange = { newValue ->
                                val proposedText = newValue.text
                                val proposedTextBytes = proposedText.toByteArray().size
                                if (proposedTextBytes <= 592) {
                                    if (isThreadView) {
                                        chatViewModel?.onThreadMessageTextChanged(newValue)
                                    } else {
                                        chatViewModel?.onMessageTextChanged(newValue)
                                    }
                                }
                            },
                            value = textValue,
                            cursorBrush = primary_blue,
                            enabled = !(dashboardChat?.getChatOrNull()
                                ?.isPrivateTribe() == true && dashboardChat?.getChatOrNull()?.status?.isPending() == true)
                        )
                    }

                    // GIF icon
                    IconButton(
                        onClick = {
                            chatViewModel?.toggleGiphyPicker()
                            chatViewModel?.fetchTrendingGifs()
                        },
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Gif,
                            contentDescription = "Gif",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.background,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    // Emoji icon
                    IconButton(
                        onClick = {},
                        modifier = Modifier.size(25.dp)
                    ) {
                        Icon(
                            Icons.Outlined.EmojiEmotions,
                            contentDescription = "Emoji",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp).padding(bottom = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Show PriceChip only when text is not empty
                val messageState = if (isThreadView) chatViewModel?.threadMessageState else chatViewModel?.editMessageState
                val hasContentToSend = canSendMessage(messageState)
                val hasText = messageState?.messageText?.value?.text?.isNotBlank() ?: false

                if (hasText && !isThreadView) {
                    PriceChip(chatViewModel)
                    Spacer(modifier = Modifier.width(10.dp))
                }

                // Mic or Send icon based on text
                IconButton(
                    onClick = {
                        if (hasContentToSend) {
                            chatViewModel?.onSendMessage(threadUUID?.value)
                        } else {
                            val isRecording = chatViewModel?.isRecording ?: false
                            if (isRecording) {
                                chatViewModel?.stopRecording(threadUUID?.value)
                            } else {
                                chatViewModel?.startRecording(isThreadView)
                            }
                        }
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.secondary)
                        .size(36.dp),
                ) {
                    Icon(
                        imageVector = if (hasContentToSend) Icons.Default.Send else Icons.Default.Mic,
                        contentDescription = if (hasContentToSend) "Send Message" else "Record",
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))
            }
        }
    }
}

@Composable
private fun RecordingBottomBar(
    chatViewModel: ChatViewModel,
    threadUUID: ThreadUUID?,
    isThreadView: Boolean = false
) {
    var seconds by remember { mutableIntStateOf(0) }
    val isRecording = if (isThreadView) {
        chatViewModel.isThreadRecording
    } else {
        chatViewModel.isRecording
    }

    LaunchedEffect(isRecording) {
        seconds = 0
        if (isRecording) while (true) { kotlinx.coroutines.delay(1000); seconds += 1 }
    }
    val infinite = rememberInfiniteTransition(label = "pulse")
    val dotAlpha by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
        label = "dotAlpha"
    )
    fun fmt(t: Int) = "%02d:%02d".format(t / 60, t % 60)

    Surface(
        color = androidx.compose.material3.MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(Dp.Unspecified, 60.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(badge_red.copy(alpha = dotAlpha))
                )
                Text(
                    text = fmt(seconds),
                    fontSize = 20.sp,
                    color = Color.White
                )
            }

            Spacer(Modifier.width(16.dp))

            // cancel
            IconButton(
                onClick = { chatViewModel.cancelRecording(isThreadView) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(badge_red)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Discard recording",
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // accept
            IconButton(
                onClick = { chatViewModel.stopRecording(threadUUID?.value) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(primary_green)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = "Use recording",
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

fun canSendMessage(messageState: EditMessageState?): Boolean {
    return messageState?.let {
        it.messageText.value.text.isNotBlank() ||
                it.attachmentInfo.value != null ||
                it.giphyPreview.value != null
    } == true
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
                .padding(
                    start = if (splitType is DashboardViewModel.SplitContentType.TribeDetail ||
                        splitType is DashboardViewModel.SplitContentType.ContactDetails
                    ) 12.dp else 0.dp
                )
        ) {

            if (splitType !is DashboardViewModel.SplitContentType.TribeDetail &&
                splitType !is DashboardViewModel.SplitContentType.ContactDetails &&
                splitType !is DashboardViewModel.SplitContentType.Podcast
            ) {
                IconButton(
                    onClick = {
                        when (splitType) {
                            is DashboardViewModel.SplitContentType.QRDetail -> {
                                if (dashboardViewModel?.previousSplitType != null) {
                                    dashboardViewModel.toggleSplitScreen(true, dashboardViewModel.previousSplitType)
                                    dashboardViewModel.previousSplitType = null
                                } else {
                                    dashboardViewModel?.toggleSplitScreen(false, null)
                                }
                            }

                            is DashboardViewModel.SplitContentType.Thread -> {
                                dashboardViewModel?.toggleSplitScreen(
                                    true,
                                    chatViewModel?.chatId?.let { DashboardViewModel.SplitContentType.Threads(it) }
                                )
                            }

                            is DashboardViewModel.SplitContentType.TribeMembers -> {
                                dashboardViewModel?.toggleSplitScreen(
                                    true,
                                    DashboardViewModel.SplitContentType.TribeDetail(splitType.chatId)
                                )
                            }

                            else -> {
                                dashboardViewModel?.toggleSplitScreen(false, null)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            val titleText = when (splitType) {
                is DashboardViewModel.SplitContentType.Threads -> "Threads List"
                is DashboardViewModel.SplitContentType.Thread -> "Thread"
                is DashboardViewModel.SplitContentType.TribeDetail -> "Tribe Info"
                is DashboardViewModel.SplitContentType.TribeMembers -> "Tribe Members"
                is DashboardViewModel.SplitContentType.ContactDetails -> "Contact Details"
                is DashboardViewModel.SplitContentType.QRDetail -> splitType.title
                is DashboardViewModel.SplitContentType.Podcast -> "Podcast"
                else -> ""
            }

            Text(
                modifier = Modifier.padding(start = if (splitType is DashboardViewModel.SplitContentType.Podcast) 16.dp else 8.dp),
                text = titleText,
                fontFamily = Roboto,
                fontSize = 16.sp,
                fontWeight = FontWeight.W700,
                color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary
            )

            Spacer(modifier = Modifier.weight(1f))

            if (splitType is DashboardViewModel.SplitContentType.Threads || splitType is DashboardViewModel.SplitContentType.Thread) {
                IconButton(
                    onClick = {
                        dashboardViewModel?.detachSection(splitType)
                        dashboardViewModel?.toggleSplitScreen(false, null)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Detach",
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            IconButton(
                onClick = {
                    dashboardViewModel?.toggleSplitScreen(false, null)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (splitType is DashboardViewModel.SplitContentType.Thread) {
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
                        val backgroundColor =
                            if (index == chatViewModel.aliasMatcherState.selectedItem) androidx.compose.material3.MaterialTheme.colorScheme.background else androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer
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
                .background(
                    SolidColor(androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant),
                    RoundedCornerShape(10.dp)
                )
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
