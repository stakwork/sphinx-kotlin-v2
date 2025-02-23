package chat.sphinx.common.models

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import chat.sphinx.wrapper.chat.Chat
import chat.sphinx.wrapper.chat.ChatType
import chat.sphinx.wrapper.contact.Contact
import chat.sphinx.wrapper.invoiceExpirationTimeFormat
import chat.sphinx.wrapper.message.*
import chat.sphinx.common.state.BubbleBackground
import chat.sphinx.concepts.link_preview.model.*
import chat.sphinx.utils.linkify.LinkSpec
import chat.sphinx.wrapper.PhotoUrl
import chat.sphinx.wrapper.chat.isConversation
import chat.sphinx.wrapper.chatTimeFormat
import chat.sphinx.wrapper.contact.ContactAlias
import chat.sphinx.wrapper.contact.toContactAlias
import chat.sphinx.wrapper.lightning.LightningNodeDescriptor
import chat.sphinx.wrapper.lightning.Sat
import chat.sphinx.wrapper.tribe.TribeJoinLink
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ChatMessage(
    val chat: Chat,
    val contact: Contact?,
    val message: Message,
    val colors: Map<Long, Int>,
    val accountOwner: () -> Contact,
    val boostMessage: () -> Unit,
    val flagMessage: () -> Unit,
    val deleteMessage: () -> Unit,
    val isSeparator: Boolean = false,
    val background: BubbleBackground,
    private val previewProvider: suspend (link: LinkSpec) -> LinkPreview?,
) {

    val replyToMessageSenderAliasPreview: String by lazy {
        val senderAlias = when {
            message.sender == chat.contactIds.firstOrNull() -> {
                accountOwner().alias?.value ?: ""
            }
            contact != null -> {
                contact.alias?.value ?: ""
            }
            else -> {
                message.senderAlias?.value ?: ""
            }
        }

        senderAlias
    }

    val isThreadHeader = message.thread?.isNotEmpty() ?: false

    val isAdmin: Boolean by lazy {
        chat.ownerPubKey == accountOwner().nodePubKey
    }

    val replyToMessageTextPreview: String by lazy {
        val messageMediaText = if (message.messageMedia != null) "attachment" else ""
        message.retrieveTextToShow() ?: messageMediaText
    }

    val replyToMessageColor: Int? by lazy {
        colors[message.id.value]
    }

    val isUnsupportedType: Boolean by lazy {
        message.type.isInvoice() || message.type.isInvoicePayment()
    }

    val unsupportedTypeLabel: String by lazy {
        if (message.type.isInvoice()) {
            "Invoice"
        } else if (message.type.isInvoicePayment()) {
            "Invoice Payment"
        } else {
            ""
        }
    }

    val botResponse: String? by lazy {
        if (message.type.isBotRes()) {
            message.retrieveTextToShow()
        }
        else {
            null
        }
    }

    val boostsLayoutState: BoostLayoutState? by lazy {
        if (message == null) {
            null
        } else {
            message.reactions?.let { nnReactions ->
                if (nnReactions.isEmpty()) {
                    null
                } else {
                    val set: MutableSet<BoostSenderHolder> = LinkedHashSet(0)
                    var total: Long = 0
                    var boostedByOwner = false
                    val owner = accountOwner()

                    for (reaction in nnReactions) {
                        if (reaction.sender == owner.id) {
                            boostedByOwner = true

                            set.add(
                                BoostSenderHolder(
                                    chat.myPhotoUrl ?: owner.photoUrl,
                                    chat.myAlias?.value?.toContactAlias() ?: owner.alias,
                                    colors[reaction.id.value]
                                )
                            )
                        } else {
                            if (chat.type.isConversation()) {
                                set.add(
                                    BoostSenderHolder(
                                        contact?.photoUrl,
                                        contact?.alias,
                                        colors[reaction.id.value]
                                    )
                                )
                            } else {
                                set.add(
                                    BoostSenderHolder(
                                        reaction.senderPic,
                                        reaction.senderAlias?.value?.toContactAlias(),
                                        colors[reaction.id.value]
                                    )
                                )
                            }
                        }
                        total += reaction.amount.value
                    }

                    BoostLayoutState(
                        showSent = this.isSent,
                        boostedByOwner = boostedByOwner,
                        senders = set,
                        totalAmount = Sat(total),
                    )
                }
            }
        }
    }

    val threadState: ThreadHolder? by lazy {
        message.thread?.let { nnThread ->
            val replies = nnThread.reversed()
            if (replies.isEmpty() || chat.isConversation()){
                null
            } else {
                val users: MutableList<ReplyUserHolder> = mutableListOf()

                val owner = accountOwner()
                val ownerUserHolder = ReplyUserHolder(
                    owner.photoUrl,
                    owner.alias,
                    colors[owner.id.value]
                )

                replies.forEach { replyMessage ->
                    users.add(
                        if (replyMessage.sender == owner.id) {
                            ownerUserHolder
                        } else {
                            ReplyUserHolder(
                                replyMessage.senderPic,
                                replyMessage.senderAlias?.value?.toContactAlias(),
                                colors[replyMessage.id.value]
                            )
                        }
                    )
                }

                val lastReplyUser = replies.first().let {
                    if (it.sender == owner.id) {
                        ownerUserHolder
                    } else {
                        ReplyUserHolder(
                            it.senderPic,
                            it.senderAlias?.value?.toContactAlias(),
                            colors[it.id.value]
                        )
                    }
                }

                val sent = message.sender == chat.contactIds.firstOrNull()

                val lastReplyAttachment: Boolean = replies.first().messageMedia != null

                val mediaChatMessage = ChatMessage(
                    chat,
                    contact,
                    replies.first(),
                    colors,
                    accountOwner,
                    boostMessage,
                    flagMessage,
                    deleteMessage,
                    isSeparator,
                    background,
                    previewProvider
                )

                ThreadHolder(
                    replyCount = replies.size,
                    users = users.reversed(),
                    lastReplyMessage = replies.first().retrieveTextToShow(),
                    lastReplyDate = replies.first().date.chatTimeFormat(),
                    lastReplyUser = lastReplyUser,
                    isSentMessage = sent,
                    lastReplyAttachment = if (lastReplyAttachment) mediaChatMessage else null
                )
            }
        }
    }

    var audioState: MutableState<AudioState?> = mutableStateOf(null)

    val isSent: Boolean by lazy {
        message.sender == chat.contactIds.firstOrNull()
    }

    val isReceived: Boolean by lazy {
        !isSent
    }

    val isDeleted: Boolean by lazy {
        message.status.isDeleted()
    }

    val isFlagged: Boolean by lazy {
        message.isFlagged && !isDeleted
    }
    val showSendingIcon: Boolean by lazy {
        isSent && message.id.isProvisionalMessage && message.status.isPending()
    }

    val showLockIcon: Boolean by lazy {
        message.messageContentDecrypted != null || message.messageMedia?.mediaKeyDecrypted != null
    }

    val showBoltIcon: Boolean by lazy {
        isSent && (message.status.isReceived() || message.status.isConfirmed())
    }

    val showFailedContainer: Boolean by lazy {
        isSent && message.status.isFailed()
    }

    private var linkPreviewLayoutState: LinkPreview? = null
    private val previewLock = Mutex()
    suspend fun retrieveLinkPreview(link: LinkSpec): LinkPreview? {
        return linkPreviewLayoutState ?: previewLock.withLock {
            linkPreviewLayoutState ?: previewProvider?.invoke(link)
                ?.also { linkPreviewLayoutState = it }
        }
    }

    val invoiceExpirationHeader: String? by lazy {
        if (message.type.isInvoice() && !message.status.isDeleted()) {
            if (message.isExpiredInvoice()) {
                "REQUEST EXPIRED"
            } else {
                message.expirationDate?.invoiceExpirationTimeFormat()?.let {
                    "EXPIRES AT: $it"
                }
            }
        } else {
            null
        }
    }

    val groupActionSubjectName: String = message.senderAlias?.value ?: ""
    val groupActionLabelText: String? = when (message.type) {
        MessageType.GroupAction.Join -> {
            if (chat.type == ChatType.Tribe) {
                "$groupActionSubjectName has joined the tribe"
            } else {
                "$groupActionSubjectName has joined the group"
            }
        }
        MessageType.GroupAction.Leave -> {
            if (chat.type == ChatType.Tribe) {
                "$groupActionSubjectName has left the tribe"
            } else {
                "$groupActionSubjectName has left the group"
            }
        }
        MessageType.GroupAction.MemberApprove -> {
            if (chat.type == ChatType.Tribe) {
                "$groupActionSubjectName has joined the tribe"
            } else {
                null
            }
        }
        else -> {
            null
        }
    }

    data class ThreadHolder(
        val replyCount: Int,
        val users: List<ReplyUserHolder>,
        val lastReplyMessage: String?,
        val lastReplyDate: String,
        val lastReplyUser: ReplyUserHolder,
        val isSentMessage: Boolean,
        val lastReplyAttachment: ChatMessage?
    )

    data class ReplyUserHolder(
        val photoUrl: PhotoUrl?,
        val alias: ContactAlias?,
        val colorKey: Int?
    )

    data class AudioState(
        var length: Int,
        var currentTime: Int,
        var progress: Double,
        var isPlaying: Boolean,
    )

    data class BoostLayoutState(
        val showSent: Boolean,
        val boostedByOwner: Boolean,
        val senders: Set<BoostSenderHolder>,
        val totalAmount: Sat,
    )

    data class BoostSenderHolder(
        val photoUrl: PhotoUrl?,
        val alias: ContactAlias?,
        val color: Int?,
    )

    sealed class LinkPreview private constructor() {
        data class ContactPreview(
            val alias: ContactAlias?,
            val photoUrl: PhotoUrl?,
            val showBanner: Boolean,

            // Used only to anchor data for click listeners
            val lightningNodeDescriptor: LightningNodeDescriptor
        ): LinkPreview()

        data class HttpUrlPreview(
            val title: HtmlPreviewTitle?,
            val domainHost: HtmlPreviewDomainHost,
            val description: PreviewDescription?,
            val imageUrl: PreviewImageUrl?,
            val favIconUrl: HtmlPreviewFavIconUrl?,

            // Used only to anchor data for click listeners
            val url: String,
        ): LinkPreview()

        data class TribeLinkPreview(
            val name: TribePreviewName,
            val description: PreviewDescription?,
            val imageUrl: PreviewImageUrl?,
            val showBanner: Boolean,

            // Used only to anchor data for click listeners
            val joinLink: TribeJoinLink,
        ) : LinkPreview()
    }

}