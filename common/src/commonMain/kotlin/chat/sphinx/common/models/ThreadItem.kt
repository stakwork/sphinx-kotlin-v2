package chat.sphinx.common.models

import chat.sphinx.wrapper.PhotoUrl
import chat.sphinx.wrapper.contact.ContactAlias
import chat.sphinx.wrapper.message.media.FileName
import chat.sphinx.wrapper.message.media.FileSize
import okio.Path

data class ThreadItem(
    val aliasAndColorKey: Pair<ContactAlias?, String?>,
    val photoUrl: PhotoUrl?,
    val date: String,
    val message: String,
    val usersReplies: List<ChatMessage.ReplyUserHolder>?,
    val usersCount: Int,
    val repliesAmount: String,
    val lastReplyDate: String?,
    val uuid: String,
    val imageAttachment: Pair<String, Path?>?,
    val videoAttachment: Path?,
    val fileAttachment: FileAttachment?,
    val audioAttachment: Boolean?
)

data class FileAttachment(
    val fileName: FileName?,
    val fileSize: FileSize,
    val isPdf: Boolean,
    val pageCount: Int
)