package chat.sphinx.common.chatMesssageUI

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import chat.sphinx.common.components.PhotoUrlImage
import chat.sphinx.common.models.ChatMessage
import chat.sphinx.wrapper.thumbnailUrl
import chat.sphinx.wrapper.util.getInitials

@Composable
fun ImageProfile(
    chatMessage: ChatMessage,
    modifier: Modifier = Modifier,
    applyWhiteAlpha: Boolean = false
) {
    val color = chatMessage.colors[chatMessage.message.id.value]

    PhotoUrlImage(
        photoUrl = chatMessage.contact?.photoUrl?.thumbnailUrl ?: chatMessage.message.senderPic?.thumbnailUrl,
        modifier = modifier
            .size(30.dp)
            .clip(CircleShape)
            .then(
                if (applyWhiteAlpha) Modifier.drawWithContent {
                    drawContent()
                    drawRect(
                        color = Color.White.copy(alpha = 0.3f), // 30% white overlay
                        size = size
                    )
                } else Modifier
            ),
        color = if (color != null) Color(color) else null,
        firstNameLetter = (chatMessage.contact?.alias?.value ?: chatMessage.message.senderAlias?.value)?.getInitials(),
        fontSize = 12
    )
}
