package chat.sphinx.common.state

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import chat.sphinx.common.models.ChatMessage
import chat.sphinx.wrapper.message.Message

data class PinMessageState(
    val pinMessage: MutableState<Message?> = mutableStateOf(null),
    val isPinning: Boolean = false,
    val isUnpinning: Boolean = false
)
