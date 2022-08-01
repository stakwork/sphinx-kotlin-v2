package chat.sphinx.common.chatMesssageUI

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.sphinx.common.Res
import chat.sphinx.common.components.MessageMediaImage
import chat.sphinx.common.components.PhotoUrlImage
import chat.sphinx.common.models.ChatMessage
import chat.sphinx.common.viewmodel.chat.ChatViewModel
import chat.sphinx.platform.imageResource
import chat.sphinx.wrapper.chat.isTribe
import chat.sphinx.wrapper.util.getInitials
import utils.conditional

@Composable
fun DirectPaymentUI(
    chatMessage: ChatMessage,
    chatViewModel: ChatViewModel
) {
    Column(
        horizontalAlignment = if (chatMessage.isSent) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth(0.25f).conditional(
            chatMessage.message.messageContentDecrypted?.value?.isEmpty()
                ?.not() == true
        ) { Modifier.fillMaxWidth(0.3f) }
    ) {
        Row(
            horizontalArrangement = if (chatMessage.isSent) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
                .padding(top = 16.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (chatMessage.chat.isTribe()) {
                val recipientColor = chatMessage.colors[-chatMessage.message.id.value]

                Box(modifier = Modifier.weight(1f).padding(start = 8.dp, end = 8.dp)) {
                    PhotoUrlImage(
                        photoUrl = chatMessage.message.recipientPic,
                        modifier = Modifier
                            .size(25.dp)
                            .clip(
                                CircleShape
                            ),
                        color = if (recipientColor != null) Color(recipientColor) else null,
                        firstNameLetter = chatMessage.message.recipientAlias?.value?.getInitials(),
                        fontSize = 9
                    )
                }
            }
            if (chatMessage.isReceived && chatMessage.chat.isTribe().not()) {
                Image(
                    painter = imageResource(Res.drawable.ic_received),
                    contentDescription = "Sent Icon",
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.inverseSurface)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                chatMessage.message.amount.value.toString(),
                color = if (chatMessage.isSent) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground
            )

            if (chatMessage.isSent || chatMessage.chat.isTribe().not()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "sats",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 10.sp
                )
            }

            if (chatMessage.isSent||chatMessage.chat.isTribe())
            {
                Spacer(modifier = Modifier.width(6.dp))
                Image(
                    painter = imageResource(Res.drawable.ic_sent),
                    contentDescription = "Sent Icon",
                    modifier = Modifier.size(20.dp),
                    colorFilter = if (chatMessage.isSent) ColorFilter.tint(MaterialTheme.colorScheme.tertiary) else ColorFilter.tint(
                        MaterialTheme.colorScheme.onBackground
                    )
                )
            }


        }
        if (chatMessage.message.messageContentDecrypted?.value?.isEmpty()
                ?.not() == true
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .padding(top = 8.dp, start = 12.dp, end = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    chatMessage.message.messageContentDecrypted?.value ?: "",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        chatMessage.message.messageMedia?.let {
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(8.dp)
            ) {

                MessageMediaImage(
                    chatMessage.message,
                    chatViewModel = chatViewModel,
                    modifier = Modifier.wrapContentHeight().fillMaxWidth()
                )

            }
        }
        if (chatMessage.message.messageMedia == null)
            Spacer(modifier = Modifier.height(16.dp))
    }
}