package chat.sphinx.common.viewmodel

import androidx.annotation.ColorInt
import androidx.compose.material.MaterialTheme.colors
import chat.sphinx.common.models.ChatMessage
import chat.sphinx.common.models.FileAttachment
import chat.sphinx.common.models.ThreadItem
import chat.sphinx.common.viewmodel.chat.ChatViewModel
import chat.sphinx.di.container.SphinxContainer
import chat.sphinx.utils.UserColorsHelper
import chat.sphinx.utils.notifications.createSphinxNotificationManager
import chat.sphinx.wrapper.chat.Chat
import chat.sphinx.wrapper.chat.getColorKey
import chat.sphinx.wrapper.chatTimeFormat
import chat.sphinx.wrapper.contact.Contact
import chat.sphinx.wrapper.contact.getColorKey
import chat.sphinx.wrapper.contact.toContactAlias
import chat.sphinx.wrapper.dashboard.ChatId
import chat.sphinx.wrapper.message.*
import chat.sphinx.wrapper.message.media.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.Path
import utils.getRandomColorRes

class ThreadsViewModel(
    val chatId: ChatId,
    val dashboardViewModel: DashboardViewModel
) {
    val scope = SphinxContainer.appModule.applicationScope
    val dispatchers = SphinxContainer.appModule.dispatchers
    private val sphinxNotificationManager = createSphinxNotificationManager()
    private val contactRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).contactRepository
    private val chatRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).chatRepository
    private val messageRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).messageRepository
    private val colorsHelper = UserColorsHelper(SphinxContainer.appModule.dispatchers)

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
                val threadItems = generateThreadItemsList(messages)
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

        val senderInfo = if (isSenderOwner) {
            Pair(owner?.alias, owner?.getColorKey())
        } else {
            Pair(
                originalMessage?.senderAlias?.value?.toContactAlias(),
                originalMessage?.getColorKey()
            )
        }

        val senderPhotoUrl = if (isSenderOwner) owner?.photoUrl else originalMessage?.senderPic

        val repliesList = messagesForThread?.drop(1)?.distinctBy { it.senderAlias }

        val imageAttachment = originalMessage?.retrieveImageUrlAndMessageMedia()?.let { mediaData ->
            Pair(mediaData.first, mediaData.second?.localFile)
        }
        val videoAttachment: Path? = originalMessage?.messageMedia?.let { nnMessageMedia ->
            if (nnMessageMedia.mediaType.isVideo) { nnMessageMedia.localFile } else null
        }
        val fileAttachment: FileAttachment? = originalMessage?.messageMedia?.let { nnMessageMedia ->
            if (nnMessageMedia.mediaType.isImage || nnMessageMedia.mediaType.isAudio) {
                null
            } else {
                nnMessageMedia.localFile?.let { nnFile ->
//                    val pageCount = if (nnMessageMedia.mediaType.isPdf) {
//                        val fileDescriptor =
//                            ParcelFileDescriptor.open(nnFile, ParcelFileDescriptor.MODE_READ_ONLY)
//                        val renderer = PdfRenderer(fileDescriptor)
//                        renderer.pageCount
//                    } else {
//                        0
//                    }

                    FileAttachment(
                        nnMessageMedia.fileName,
                        FileSize(nnFile.toFile().length()),
                        nnMessageMedia.mediaType.isPdf,
                        0
                    )
                }
            }
        }

        val audioAttachment: Boolean? = originalMessage?.messageMedia?.let { nnMessageMedia ->
            if (nnMessageMedia.mediaType.isAudio) {
                true
            } else {
                null
            }
        }

        val threadMessage = originalMessage?.messageContentDecrypted?.value ?: ""

        return ThreadItem(
            aliasAndColorKey = senderInfo,
            photoUrl = senderPhotoUrl,
            date = originalMessage?.date?.chatTimeFormat() ?: "",
            message = threadMessage,
            usersReplies = createReplyUserHolders(repliesList, chat, owner),
            usersCount = repliesList?.size ?: 0,
            repliesAmount = messagesForThread?.drop(1)?.size?.toString() ?: "0",
            lastReplyDate = messagesForThread?.first()?.date?.chatTimeFormat(),
            uuid = uuid ?: "",
            imageAttachment = imageAttachment,
            videoAttachment = videoAttachment,
            fileAttachment = fileAttachment,
            audioAttachment = audioAttachment
        )
    }

    private suspend fun createReplyUserHolders(
        repliesList: List<Message>?,
        chat: Chat?,
        owner: Contact?
    ): List<ChatMessage.ReplyUserHolder>? {
        return repliesList?.take(6)?.map {
            val isSenderOwner: Boolean = it.sender == chat?.contactIds?.firstOrNull()

            val contactColorKey = owner?.getColorKey()?.let {
                colorsHelper.getColorIntForKey(it, Integer.toHexString(getRandomColorRes().hashCode()))
            }

            ChatMessage.ReplyUserHolder(
                photoUrl = if (isSenderOwner) owner?.photoUrl else it.senderPic,
                alias = if (isSenderOwner) owner?.alias else it.senderAlias?.value?.toContactAlias(),
                colorKey = contactColorKey
            )
        }
    }

}