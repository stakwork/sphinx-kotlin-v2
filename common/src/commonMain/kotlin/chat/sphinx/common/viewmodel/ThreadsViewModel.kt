package chat.sphinx.common.viewmodel

import chat.sphinx.common.models.ChatMessage
import chat.sphinx.common.models.ThreadItem
import chat.sphinx.common.viewmodel.chat.ChatViewModel
import chat.sphinx.di.container.SphinxContainer
import chat.sphinx.utils.UserColorsHelper
import chat.sphinx.utils.notifications.createSphinxNotificationManager
import chat.sphinx.wrapper.chat.Chat
import chat.sphinx.wrapper.chatTimeFormat
import chat.sphinx.wrapper.contact.Contact
import chat.sphinx.wrapper.contact.toContactAlias
import chat.sphinx.wrapper.dashboard.ChatId
import chat.sphinx.wrapper.message.*
import chat.sphinx.wrapper.timeAgo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import utils.getRandomColorRes

class ThreadsViewModel(
    val chatId: ChatId,
    val dashboardViewModel: DashboardViewModel,
    val chatViewModel: ChatViewModel?
) {
    val scope = SphinxContainer.appModule.applicationScope
    val dispatchers = SphinxContainer.appModule.dispatchers
    private val sphinxNotificationManager = createSphinxNotificationManager()
    private val contactRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).contactRepository
    private val chatRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).chatRepository
    private val messageRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).messageRepository
    private val colorsHelper = UserColorsHelper(SphinxContainer.appModule.dispatchers)

    private val _threadItems = MutableStateFlow<List<ThreadItem>>(emptyList())
    val threadItems: StateFlow<List<ThreadItem>> get() = _threadItems

    init {
        updateThreads()
    }

    private suspend fun getOwner(): Contact {
        return contactRepository.accountOwner.value.let { contact ->
            if (contact != null) {
                contact
            } else {
                var resolvedOwner: Contact? = null
                try {
                    contactRepository.accountOwner.collect { ownerContact ->
                        if (ownerContact != null) {
                            resolvedOwner = ownerContact
                            throw Exception()
                        }
                    }
                } catch (e: Exception) {
                }
                delay(25L)

                resolvedOwner!!
            }
        }
    }

    suspend fun getChat(): Chat? {
        return chatId.let { chatRepository.getChatById(it) }
    }

    private fun updateThreads() {
        scope.launch(dispatchers.mainImmediate) {
            messageRepository.getThreadUUIDMessagesByChatId(chatId).collect { messages ->
                val items = generateThreadItemsList(messages)
                _threadItems.value = items
            }
        }
    }


    private suspend fun generateThreadItemsList(messages: List<Message>): List<ThreadItem> {
        // Group messages by their ThreadUUID
        val groupedMessagesByThread = messages.groupBy { it.threadUUID }.filter {
            it.value.size > 1
        }

        // Fetch the header messages based on the message UUIDs
        val headerMessages = messageRepository.getAllMessagesByUUID(groupedMessagesByThread.keys.mapNotNull { it?.value?.toMessageUUID() })
        val headerMessagesMappedByUUID = headerMessages.associateBy { it.uuid?.value }

        // Generate a map of complete threads, where each thread includes its header message and its other messages
        val completeThreads = groupedMessagesByThread.mapValues { entry ->
            val threadUUID = entry.key
            val threadMessages = entry.value

            val threadHeaderMessage = headerMessagesMappedByUUID[threadUUID?.value]

            if (threadHeaderMessage != null) {
                listOf(threadHeaderMessage) + threadMessages
            } else {
                threadMessages
            }
        }

        // Prepare thread items from the complete threads
        return completeThreads.keys.map { uuid ->

            val owner = getOwner()
            val messagesForThread = completeThreads[uuid]

            val originalMessage = messagesForThread?.get(0)
            val chat = getChat()
            val isSenderOwner: Boolean = originalMessage?.sender == chat?.contactIds?.firstOrNull()

            createThreadItem(uuid?.value, owner, messagesForThread, originalMessage, chat, isSenderOwner)
        }
    }

    private suspend fun createThreadItem(
        uuid: String?,
        owner: Contact?,
        messagesForThread: List<Message>?,
        originalMessage: Message?,
        chat: Chat?,
        isSenderOwner: Boolean
    ): ThreadItem {

        val tribeAdmin = if (owner?.nodePubKey == chat?.ownerPubKey) owner else null
        val color = originalMessage?.let { getColorsMapFor(it, null, tribeAdmin) }?.get(originalMessage.id.value)

        val senderInfo = if (isSenderOwner) {
            Pair(owner?.alias, color)
        } else {
            Pair(
                originalMessage?.senderAlias?.value?.toContactAlias(),
                color
            )
        }

        val senderPhotoUrl = if (isSenderOwner) owner?.photoUrl else originalMessage?.senderPic
        val repliesList = messagesForThread?.drop(1)?.distinctBy { it.senderAlias }

        val messageMedia = if (originalMessage?.messageMedia != null && chat != null) {
            chatViewModel?.processSingleMessage(chat, originalMessage)
        } else null

        val threadMessage = originalMessage?.messageContentDecrypted?.value ?: ""

        return ThreadItem(
            aliasAndColorKey = senderInfo,
            photoUrl = senderPhotoUrl,
            date = originalMessage?.date?.chatTimeFormat() ?: "",
            message = threadMessage,
            usersReplies = createReplyUserHolders(repliesList, chat, owner)?.reversed(),
            usersCount = repliesList?.size ?: 0,
            repliesAmount = messagesForThread?.drop(1)?.size?.toString() ?: "0",
            lastReplyDate = messagesForThread?.first()?.date?.timeAgo(),
            uuid = uuid ?: "",
            messageMedia = messageMedia
        )
    }

    private suspend fun createReplyUserHolders(
        repliesList: List<Message>?,
        chat: Chat?,
        owner: Contact?
    ): List<ChatMessage.ReplyUserHolder>? {
        return repliesList?.take(6)?.map {
            val isSenderOwner: Boolean = it.sender == chat?.contactIds?.firstOrNull()
            val tribeAdmin = if (owner?.nodePubKey == chat?.ownerPubKey) owner else null

            val color = getColorsMapFor(it, null, tribeAdmin)[it.id.value]

            ChatMessage.ReplyUserHolder(
                photoUrl = if (isSenderOwner) owner?.photoUrl else it.senderPic,
                alias = if (isSenderOwner) owner?.alias else it.senderAlias?.value?.toContactAlias(),
                colorKey = color
            )
        }
    }

    suspend fun getColorsMapFor(
        message: Message,
        contactColor: Int?,
        tribeAdmin: Contact?
    ): Map<Long, Int> {
        var colors: MutableMap<Long, Int> = mutableMapOf()

        contactColor?.let {
            colors[message.id.value] = contactColor
        } ?: run {
            val colorKey = message.getColorKey()
            val colorInt = colorsHelper.getColorIntForKey(
                colorKey,
                Integer.toHexString(getRandomColorRes().hashCode())
            )

            colors[message.id.value] = colorInt

            if (message.type.isDirectPayment() && tribeAdmin != null) {
                val recipientColorKey = message.getRecipientColorKey(tribeAdmin.id)
                val recipientColorInt = colorsHelper.getColorIntForKey(
                    recipientColorKey,
                    Integer.toHexString(getRandomColorRes().hashCode())
                )

                colors[-message.id.value] = recipientColorInt
            }
        }

        for (m in message.reactions ?: listOf()) {
            contactColor?.let {
                colors[m.id.value] = contactColor
            } ?: run {
                val colorKey = m.getColorKey()
                val colorInt = colorsHelper.getColorIntForKey(
                    colorKey,
                    Integer.toHexString(getRandomColorRes().hashCode())
                )

                colors[m.id.value] = colorInt
            }
        }

        message.replyMessage?.let { replyMessage ->
            contactColor?.let {
                colors[replyMessage.id.value] = contactColor
            } ?: run {
                val colorKey = replyMessage.getColorKey()
                val colorInt = colorsHelper.getColorIntForKey(
                    colorKey,
                    Integer.toHexString(getRandomColorRes().hashCode())
                )

                colors[replyMessage.id.value] = colorInt
            }
        }

        return colors
    }



}