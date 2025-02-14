package chat.sphinx.common.models

import chat.sphinx.wrapper.PhotoUrl
import chat.sphinx.wrapper.contact.ContactAlias
import chat.sphinx.wrapper.message.media.FileName
import chat.sphinx.wrapper.message.media.FileSize

data class ThreadItem(
    val aliasAndColorKey: Pair<ContactAlias?, Int?>,
    val photoUrl: PhotoUrl?,
    val date: String,
    val message: String,
    val usersReplies: List<ChatMessage.ReplyUserHolder>?,
    val usersCount: Int,
    val repliesAmount: String,
    val lastReplyDate: String?,
    val uuid: String,
    val messageMedia: ChatMessage?
)

data class FileAttachment(
    val fileName: FileName?,
    val fileSize: FileSize,
    val isPdf: Boolean,
    val pageCount: Int
)