package chat.sphinx.common.chatMesssageUI

import Roboto
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.sphinx.common.models.ChatMessage
import chat.sphinx.common.state.BubbleBackground
import chat.sphinx.wrapper.chat.isTribe
import chat.sphinx.wrapper.chatTimeFormat
import chat.sphinx.wrapper.invoiceExpirationTimeFormat
import chat.sphinx.wrapper.message.MessageType
import chat.sphinx.wrapper.message.isDeleted
import chat.sphinx.wrapper.message.isExpiredInvoice
import chat.sphinx.wrapper.message.isInvoice
import chat.sphinx.wrapper.time
import chat.sphinx.wrapper.toDateTime
import theme.place_holder_text
import theme.primary_green

@Composable
fun DisplayConditionalIcons(
    chatMessage: ChatMessage
) {
    if (
        chatMessage.background !is BubbleBackground.First
    ) {
        return
    }

    val color = chatMessage.colors[chatMessage.message.id.value]

    val isSentLikeMessage = chatMessage.isSent || (chatMessage.message.type == MessageType.Payment && chatMessage.isReceived)

    Row(
        modifier = Modifier
            .height(15.dp)
            .padding(bottom = 2.dp, end = if (isSentLikeMessage) 5.dp else 0.dp, start = if (isSentLikeMessage) 0.dp else 5.dp)
            .fillMaxWidth(),
        horizontalArrangement = if (isSentLikeMessage) Arrangement.End else Arrangement.Start
    ) {

        if (isSentLikeMessage && chatMessage.message.type.isInvoice() && !chatMessage.message.status.isDeleted()) {
            val expirationDate = chatMessage.message.expirationDate?.invoiceExpirationTimeFormat()
            val text = if (chatMessage.message.isExpiredInvoice()) "Expired Invoice" else "EXPIRES AT: $expirationDate"
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 10.sp,
                fontFamily = Roboto,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(end = 8.dp)
            )
        }


        if (chatMessage.message.type == MessageType.Payment && chatMessage.isReceived) {
            Icon(
                Icons.Default.FlashOn,
                "Confirmed",
                tint = primary_green,
                modifier = Modifier
                    .height(14.dp)
                    .width(13.dp)
                    .padding(bottom = 1.dp)
            )
        } else if (chatMessage.showBoltIcon) {
            Icon(
                Icons.Default.FlashOn,
                "Confirmed",
                tint = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .height(14.dp)
                    .width(13.dp)
                    .padding(bottom = 1.dp)
            )
        }

        if (
            chatMessage.chat.isTribe() &&
            chatMessage.isReceived &&
            (chatMessage.isDeleted.not() && chatMessage.isFlagged.not())
        ) {
            Text(
                text = chatMessage.message.senderAlias?.value ?: "",
                color = if (color != null) Color(color) else Color.Unspecified,
                fontSize = 10.sp,
                fontFamily = Roboto,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f, fill = true)
            )
            Spacer(
                modifier = Modifier.width(4.dp)
            )
        }

        if (chatMessage.showSendingIcon) {
            CircularProgressIndicator(
                modifier = Modifier
                    .height(14.dp)
                    .width(14.dp)
                    .padding(end = 4.dp, bottom = 2.dp),
                color = MaterialTheme.colorScheme.tertiary,
                strokeWidth = 2.dp
            )
        }

        if ((chatMessage.showLockIcon && isSentLikeMessage) || (chatMessage.message.type == MessageType.Payment && chatMessage.isReceived)) {
            Icon(
                Icons.Default.Lock,
                "Secure chat",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .height(14.dp)
                    .width(13.dp)
                    .padding(end = 1.dp, bottom = 2.dp)
            )
        }

        Text(
            text = chatMessage.message.date.chatTimeFormat(),
            fontWeight = FontWeight.W400,
            fontFamily = Roboto,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 10.sp,
            textAlign = if (isSentLikeMessage) TextAlign.End else TextAlign.Start,
            modifier = Modifier.weight(1f, fill = false)
        )

        if (chatMessage.showLockIcon && chatMessage.isReceived && !isSentLikeMessage) {
            Icon(
                Icons.Default.Lock,
                "Secure chat",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .height(14.dp)
                    .width(13.dp)
                    .padding(end = 1.dp, bottom = 2.dp)
            )
        }

        if (!isSentLikeMessage && chatMessage.message.type.isInvoice() && !chatMessage.message.status.isDeleted()) {
            val expirationDate = chatMessage.message.expirationDate?.invoiceExpirationTimeFormat()
            val text = if (chatMessage.message.isExpiredInvoice()) "Expired Invoice" else "EXPIRES AT: $expirationDate"
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 10.sp,
                fontFamily = Roboto,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
