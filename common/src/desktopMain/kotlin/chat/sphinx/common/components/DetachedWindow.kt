package chat.sphinx.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import chat.sphinx.common.state.MessageListData
import chat.sphinx.common.state.MessageListState
import chat.sphinx.common.viewmodel.DashboardViewModel
import chat.sphinx.common.viewmodel.ThreadsViewModel
import chat.sphinx.common.viewmodel.chat.ChatTribeViewModel
import chat.sphinx.common.viewmodel.chat.ChatViewModel
import chat.sphinx.utils.getPreferredWindowSize
import com.soywiz.korio.serialization.xml.Xml.Companion.Text

@Composable
fun DetachedWindow(
    dashboardViewModel: DashboardViewModel,
    chatViewModel: ChatViewModel?
) {
    val detachedContent by dashboardViewModel.detachedWindowStateFlow.collectAsState()

    if (detachedContent != null) {
        Window(
            onCloseRequest = { dashboardViewModel.closeDetachedWindow() },
            title = when (detachedContent) {
                is DashboardViewModel.SplitContentType.Threads -> "Threads"
                is DashboardViewModel.SplitContentType.Thread -> "Thread"
                else -> "Detached Window"
            },
            state = WindowState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = getPreferredWindowSize(480, 830)
            )
        ) {
            when (detachedContent) {
                is DashboardViewModel.SplitContentType.Threads -> {
                    val threadsDetached = detachedContent as DashboardViewModel.SplitContentType.Threads
                    val nnChatViewModel = chatViewModel ?: remember {
                        ChatTribeViewModel(threadsDetached.chatId, dashboardViewModel)
                    }
                    val threadsViewModel = remember {
                        ThreadsViewModel(threadsDetached.chatId, dashboardViewModel, nnChatViewModel)
                    }
                    ThreadsListUI(
                        threadsViewModel = threadsViewModel,
                        dashboardViewModel = dashboardViewModel,
                        chatViewModel = nnChatViewModel
                    )
                }

                is DashboardViewModel.SplitContentType.Thread -> {
                    val threadContent = detachedContent as DashboardViewModel.SplitContentType.Thread
                    val threadUUID = threadContent.threadUUID
                    val chatId = threadContent.chatId

                    val threadChatViewModel = remember {
                        ChatTribeViewModel(chatId, dashboardViewModel)
                    }

                    Scaffold(
                        topBar = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                val messageListData = MessageListState.threadScreenState()
                                if (messageListData is MessageListData.PopulatedMessageListData) {
                                    val threadHeader = messageListData.messages.lastOrNull()
                                    threadHeader?.let {
                                        ThreadHeaderUI(it, threadChatViewModel)
                                    }
                                }
                            }
                        },
                        bottomBar = {
                            SphinxChatDetailBottomAppBar(
                                dashboardChat = null,
                                chatViewModel = threadChatViewModel,
                                threadUUID = threadUUID,
                                isThreadView = false
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            MessageListUI(
                                chatViewModel = threadChatViewModel,
                                dashboardViewModel = dashboardViewModel,
                                dashboardChat = null,
                                isThreadView = true
                            )
                        }
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No content available")
                    }
                }
            }
        }
    }
}
