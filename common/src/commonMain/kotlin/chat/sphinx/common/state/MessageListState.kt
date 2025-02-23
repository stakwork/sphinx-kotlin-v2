package chat.sphinx.common.state

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import chat.sphinx.common.models.ChatMessage
import chat.sphinx.common.models.DashboardChat
import chat.sphinx.wrapper.dashboard.ChatId

abstract class MessageListData {
    object EmptyMessageListData: MessageListData()

    class PopulatedMessageListData(
        val chatId: ChatId,
        val messages: List<ChatMessage>
    ): MessageListData()
}

object MessageListState {
    private val screen: MutableState<MessageListData> = mutableStateOf(MessageListData.EmptyMessageListData)
    private val threadScreen: MutableState<MessageListData> = mutableStateOf(MessageListData.EmptyMessageListData)

    fun screenState() : MessageListData {
        return screen.value
    }

    fun screenState(state: MessageListData) {
        screen.value = state
    }

    fun threadScreenState() : MessageListData  {
        return threadScreen.value
    }

    fun threadScreenState(state: MessageListData) {
        threadScreen.value = state
    }
}