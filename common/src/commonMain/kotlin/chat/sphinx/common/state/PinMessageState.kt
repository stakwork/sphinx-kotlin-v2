package chat.sphinx.common.state

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import chat.sphinx.common.models.ChatMessage

data class PinMessageState(
    val pinMessage: MutableState<ChatMessage?> = mutableStateOf(null),
    val isPinning: Boolean = false,
)
