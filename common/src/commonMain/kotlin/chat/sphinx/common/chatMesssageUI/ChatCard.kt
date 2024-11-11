package chat.sphinx.common.chatMesssageUI

import Roboto
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.sphinx.common.Res
import chat.sphinx.common.components.*
import chat.sphinx.common.models.ChatMessage
import chat.sphinx.common.state.BubbleBackground
import chat.sphinx.common.viewmodel.chat.ChatViewModel
import chat.sphinx.platform.imageResource
import chat.sphinx.utils.linkify.LinkTag
import chat.sphinx.utils.linkify.SphinxLinkify
import chat.sphinx.utils.toAnnotatedString
import chat.sphinx.wrapper.lightning.toLightningNodePubKey
import chat.sphinx.wrapper.lightning.toVirtualLightningNodeAddress
import chat.sphinx.wrapper.message.*
import theme.badge_red
import theme.light_divider
import chat.sphinx.wrapper.message.MessageType
import chat.sphinx.wrapper.message.isSphinxCallLink
import chat.sphinx.wrapper.message.media.*
import chat.sphinx.wrapper.message.retrieveTextToShow
import chat.sphinx.wrapper.tribe.toTribeJoinLink
import com.multiplatform.webview.web.WebView
import theme.primary_green
import theme.sphinx_orange

@Composable
fun ChatCard(
    chatMessage: ChatMessage,
    chatViewModel: ChatViewModel,
    modifier: Modifier? = null
) {
    val uriHandler = LocalUriHandler.current

    val backgroundColor = when {
        chatMessage.message.type.isInvoice() -> MaterialTheme.colorScheme.background
        chatMessage.isReceived -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.inversePrimary
    }

    Card(
        backgroundColor = backgroundColor,
        shape = getBubbleShape(chatMessage),
        modifier = modifier ?: Modifier
    ) {
        val density = LocalDensity.current
        var rowWidth by remember { mutableStateOf(0.dp) }

        when {
            chatMessage.message.isSphinxCallLink -> {
                JitsiAudioVideoCall(chatMessage)
            }
            chatMessage.message.type == MessageType.DirectPayment -> {
                DirectPaymentUI(chatMessage, chatViewModel)
            }
            chatMessage.message.type == MessageType.BotRes -> {
                BotResponse(chatMessage, chatViewModel)
            }
            chatMessage.message.type == MessageType.Invoice -> {
                InvoiceUI(chatMessage, chatViewModel)
            }
            else -> {
                Column(modifier = Modifier.onSizeChanged {
                    rowWidth = with(density) { it.width.toDp() }
                }) {
                    chatMessage.message.replyMessage?.let { _ ->
                        ReplyingToMessageUI(
                            chatMessage,
                            chatViewModel
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        CustomDivider(color = light_divider, modifier = Modifier.width(rowWidth))
                    }
                    chatMessage.message.feedBoost?.let { feedBoost ->
                        PodcastBoost(feedBoost)
                    }
                    chatMessage.message.messageMedia?.let { media ->
                        if (media.mediaType.isImage) {
                            MessageMediaImage(
                                chatMessage,
                                chatViewModel = chatViewModel,
                                modifier = Modifier.wrapContentHeight().fillMaxWidth()
                            )
                        } else if (media.mediaType.isUnknown || media.mediaType.isPdf) {
                            MessageFile(
                                chatMessage = chatMessage,
                                chatViewModel = chatViewModel,
                            )
                        } else if (media.mediaType.isVideo) {
                            MessageVideo(
                                chatMessage = chatMessage,
                                chatViewModel = chatViewModel,
                                modifier = Modifier.wrapContentHeight().fillMaxWidth()
                            )
                        } else if (media.mediaType.isAudio) {
                            MessageAudio(
                                chatMessage = chatMessage,
                                chatViewModel = chatViewModel,
                            )
                        }
                    }
                    Column {
                        MessageTextLabel(chatMessage, chatViewModel, uriHandler)
                        FailedContainer(chatMessage)

                        BoostedFooter(
                            chatMessage,
                            modifier = Modifier.width(
                                maxOf(rowWidth, 200.dp)
                            ).padding(12.dp, 0.dp, 12.dp, 12.dp)
                        )

                        LinkPreviews(chatMessage, chatViewModel, uriHandler)

                        ReceivedPaidMessageButton(
                            chatMessage,
                            chatViewModel,
                            modifier = Modifier.width(
                                maxOf(rowWidth, 250.dp)
                            )
                                .height(45.dp)
                        )
                    }
                }
            }
        }
        SentPaidMessage(
            chatMessage,
            modifier = Modifier.width(
                maxOf(rowWidth, 250.dp)
            )
        )
    }
}

@Composable
fun MessageTextLabel(
    chatMessage: ChatMessage,
    chatViewModel: ChatViewModel,
    uriHandler: UriHandler
) {
    val topPadding = if (chatMessage.message.isPaidTextMessage && chatMessage.isSent) 44.dp else 12.dp

    if (chatMessage.message.retrieveTextToShow() != null) {

        val messageText = chatMessage.message.retrieveTextToShow()?.trim() ?: ""

        Row(
            modifier = Modifier
                .padding(12.dp, topPadding, 12.dp, 12.dp)
                .wrapContentWidth(if (chatMessage.isSent) Alignment.End else Alignment.Start),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val annotatedString = messageText.toAnnotatedString()
            ClickableText(
                annotatedString,
                style = TextStyle(
                    fontWeight = FontWeight.W400,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 13.sp,
                    fontFamily = Roboto,
                ),
                onClick = { offset ->
                    annotatedString.getStringAnnotations(
                        start = offset,
                        end = offset
                    ).firstOrNull()?.let { annotation ->
                        when (annotation.tag) {
                            LinkTag.WebURL.name -> {
                                uriHandler.openUri(annotation.item)
                            }
                            LinkTag.BitcoinAddress.name -> {
                                val bitcoinUriScheme = if (annotation.item.startsWith("bitcoin:")) "bitcoin:" else ""
                                val bitcoinURI = "$bitcoinUriScheme${annotation.item}"

                                uriHandler.openUri(bitcoinURI)
                            }
                            LinkTag.LightningNodePublicKey.name, LinkTag.VirtualNodePublicKey.name -> {
                                chatViewModel.contactLinkClicked(
                                    annotation.item.toLightningNodePubKey()
                                        ?: annotation.item.toVirtualLightningNodeAddress()
                                )
                            }
                            LinkTag.JoinTribeLink.name -> {
                                chatViewModel.tribeLinkClicked(
                                    annotation.item.toTribeJoinLink()
                                )
                            }
                        }
                    }
                }
            )

            // TODO: Make clickable text compatible with selectable text...
            //                                SelectionContainer {
            //
            //                                }
        }
    } else if (chatMessage.isUnsupportedType) {
        Text(
            modifier = Modifier
                .wrapContentWidth(if (chatMessage.isSent) Alignment.End else Alignment.Start)
                .padding(12.dp),
            text = "Unsupported Message Type: ${chatMessage.unsupportedTypeLabel}",
            fontWeight = FontWeight.W300,
            fontFamily = Roboto,
            fontStyle = FontStyle.Italic,
            fontSize = 13.sp,
            color = sphinx_orange
        )
    } else if (chatMessage.message.messageDecryptionError) {
        Text(
            modifier = Modifier
                .wrapContentWidth(if (chatMessage.isSent) Alignment.End else Alignment.Start)
                .padding(12.dp),
            text = "DECRYPTION ERROR",
            fontWeight = FontWeight.W300,
            fontFamily = Roboto,
            fontSize = 13.sp,
            color = badge_red
        )
    } else if (chatMessage.message.isPaidTextMessage) {

        if (!chatMessage.message.isPaidPendingMessage || chatMessage.isSent) {
            val message = chatMessage.message
            val messageMedia = message.messageMedia

            LaunchedEffect(messageMedia?.url?.value ?: "") {
                chatViewModel.downloadFileMedia(message, chatMessage.isSent)
            }
        }

        val text = if (chatMessage.isReceived && chatMessage.message.isPaidPendingMessage) {
            "PAY LO UNLOCK MESSAGE"
        } else if (chatMessage.isReceived && !chatMessage.message.isPurchaseSucceeded) {
            "ERROR LOADING MESSAGE"
        } else {
            "Loading message..."
        }

        Text(
            modifier = Modifier
                .wrapContentWidth(Alignment.Start)
                .padding(12.dp, topPadding, 12.dp, 12.dp),
            text = text,
            fontWeight = FontWeight.W300,
            fontFamily = Roboto,
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
fun LinkPreviews(
    chatMessage: ChatMessage,
    chatViewModel: ChatViewModel,
    uriHandler: UriHandler
) {
    val links = SphinxLinkify.gatherLinks(
        text = chatMessage.message.retrieveTextToShow() ?: "",
        mask = SphinxLinkify.ALL
    )

    links.firstOrNull()?.let { link ->
        var linkPreview: MutableState<ChatMessage.LinkPreview?> = rememberSaveable { mutableStateOf(null) }

        LaunchedEffect(link.url) {
            if (linkPreview.value == null) {
                linkPreview.value = chatMessage.retrieveLinkPreview(link)
            }
        }

        when (linkPreview.value) {
            is ChatMessage.LinkPreview.ContactPreview -> {
                (linkPreview.value as? ChatMessage.LinkPreview.ContactPreview)?.let { contactLinkPreview ->
                    if (contactLinkPreview.showBanner) {
                        NewContactPreview(chatMessage, contactLinkPreview, chatViewModel)
                    } else {
                        ExistingContactPreview(contactLinkPreview, chatViewModel)
                    }
                }
            }
            is ChatMessage.LinkPreview.TribeLinkPreview -> {
                (linkPreview.value as? ChatMessage.LinkPreview.TribeLinkPreview)?.let { tribeLinkPreview ->
                    if (tribeLinkPreview.showBanner) {
                        NewTribePreview(chatMessage, tribeLinkPreview, chatViewModel)
                    } else {
                        ExistingTribePreview(tribeLinkPreview, chatViewModel)
                    }
                }
            }
            is ChatMessage.LinkPreview.HttpUrlPreview -> {
                (linkPreview.value as? ChatMessage.LinkPreview.HttpUrlPreview)?.let { webLinkPreview ->
                    URLPreview(webLinkPreview, chatViewModel, uriHandler)
                }
            }
            else -> {}
        }
    }
}

@Composable
fun FailedContainer(
    chatMessage: ChatMessage
) {
    if (chatMessage.showFailedContainer) {
        Row(
            modifier = Modifier.fillMaxWidth(0.3f).padding(16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = "Go back",
                tint = Color.Red,
                modifier = Modifier.size(22.dp).padding(4.dp)
            )
            Text(
                text = "Failed message",
                color = Color.Red,
                textAlign = TextAlign.Start
            )
        }
    }
}

fun getBubbleShape(chatMessage: ChatMessage): RoundedCornerShape {
    if (chatMessage.isReceived) {
        return when (chatMessage.background) {
            is BubbleBackground.First.Isolated -> {
                RoundedCornerShape(topEnd = 10.dp, topStart = 0.dp, bottomEnd = 10.dp, bottomStart = 10.dp)
            }
            is BubbleBackground.First.Grouped -> {
                RoundedCornerShape(topEnd = 10.dp, topStart = 0.dp, bottomEnd = 10.dp, bottomStart = 0.dp)
            }
            is BubbleBackground.Middle -> {
                RoundedCornerShape(topEnd = 10.dp, topStart = 0.dp, bottomEnd = 10.dp, bottomStart = 0.dp)
            }
            is BubbleBackground.Last -> {
                RoundedCornerShape(topEnd = 10.dp, topStart = 0.dp, bottomEnd = 10.dp, bottomStart = 10.dp)
            }
            else -> {
                RoundedCornerShape(topEnd = 10.dp, topStart = 0.dp, bottomEnd = 10.dp, bottomStart = 10.dp)
            }
        }
    } else {
        return when (chatMessage.background) {
            is BubbleBackground.First.Isolated -> {
                RoundedCornerShape(topEnd = 0.dp, topStart = 10.dp, bottomEnd = 10.dp, bottomStart = 10.dp)
            }
            is BubbleBackground.First.Grouped -> {
                RoundedCornerShape(topEnd = 0.dp, topStart = 10.dp, bottomEnd = 0.dp, bottomStart = 10.dp)
            }
            is BubbleBackground.Middle -> {
                RoundedCornerShape(topEnd = 0.dp, topStart = 10.dp, bottomEnd = 0.dp, bottomStart = 10.dp)
            }
            is BubbleBackground.Last -> {
                RoundedCornerShape(topEnd = 0.dp, topStart = 10.dp, bottomEnd = 10.dp, bottomStart = 10.dp)
            }
            else -> {
                RoundedCornerShape(topEnd = 0.dp, topStart = 10.dp, bottomEnd = 10.dp, bottomStart = 10.dp)
            }
        }
    }
}

@Composable
fun InvoiceUI(chatMessage: ChatMessage, chatViewModel: ChatViewModel) {
    val isInvoiceExpired = chatMessage.message.isExpiredInvoice()
    val borderColor = if (chatMessage.isSent) MaterialTheme.colorScheme.onBackground else primary_green
    val cornerRadius = 16.dp
    val dashWidth = 10.dp
    val dashGap = 6.dp
    val alphaValue = if (isInvoiceExpired) 0.5f else 1.0f

    Box(
        modifier = Modifier
            .padding(8.dp)
            .width(300.dp)
            .wrapContentHeight()
            .then(
                if (!isInvoiceExpired) Modifier.drawBehind {
                    val strokeWidth = 2.dp.toPx()
                    val cornerRadiusPx = cornerRadius.toPx()
                    val dashWidthPx = dashWidth.toPx()
                    val dashGapPx = dashGap.toPx()

                    drawRoundRect(
                        color = borderColor,
                        topLeft = Offset(x = strokeWidth / 2, y = strokeWidth / 2),
                        size = Size(width = size.width - strokeWidth, height = size.height - strokeWidth),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        style = Stroke(
                            width = strokeWidth,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashWidthPx, dashGapPx))
                        )
                    )
                } else Modifier
            )
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(12.dp))
                .padding(
                    start = 8.dp,
                    top = 6.dp,
                    end = 6.dp,
                    bottom = 6.dp
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = "Invoice Icon",
                    tint = MaterialTheme.colorScheme.tertiary.copy(alpha = alphaValue),
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = chatMessage.message.amount.value.toString() ?: "",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = alphaValue)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "sat",
                    style = TextStyle(
                        fontWeight = FontWeight.Light,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = alphaValue)
                    )
                )
            }

            if (chatMessage.isReceived) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = { /* Handle payment click here */ },
                        enabled = isInvoiceExpired || !isInvoiceExpired,
                        colors = ButtonDefaults.buttonColors(backgroundColor = primary_green),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "PAY",
                                modifier = Modifier.align(Alignment.Center),
                                fontSize = 13.sp,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary.copy(alpha = alphaValue),
                                fontWeight = FontWeight.W600,
                                fontFamily = Roboto,
                                textAlign = TextAlign.Center
                            )

                            Image(
                                painter = imageResource(Res.drawable.ic_sent),
                                contentDescription = "Sent Icon",
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(26.dp),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.tertiary.copy(alpha = alphaValue))
                            )
                        }
                    }
                }
            }
        }
    }
}
