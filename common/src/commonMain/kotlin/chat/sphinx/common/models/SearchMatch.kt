package chat.sphinx.common.models

import chat.sphinx.wrapper.message.Message
import chat.sphinx.wrapper.message.MessageId

data class SearchMatch(
    val messageId: MessageId,
    val message: Message,
    val matchText: String
)
