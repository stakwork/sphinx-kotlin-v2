package chat.sphinx.common.state

import chat.sphinx.wrapper.dashboard.ChatId
import chat.sphinx.wrapper.lightning.LightningNodePubKey
import chat.sphinx.wrapper.message.Message
import chat.sphinx.wrapper.message.SenderAlias

sealed class ConfirmationType {
    data class PayInvoice(val message: Message?) : ConfirmationType()

    data class TribeDeleteMember(
        val alias: SenderAlias?,
        val memberPubKey: LightningNodePubKey,
        val chatId: ChatId
    ) : ConfirmationType()
}