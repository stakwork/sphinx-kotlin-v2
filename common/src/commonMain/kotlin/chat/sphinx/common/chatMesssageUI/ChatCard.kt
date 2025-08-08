package chat.sphinx.common.chatMesssageUI

import Roboto
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import chat.sphinx.common.Res
import chat.sphinx.common.components.*
import chat.sphinx.common.models.ChatMessage
import chat.sphinx.common.state.BubbleBackground
import chat.sphinx.common.viewmodel.chat.ChatViewModel
import chat.sphinx.platform.imageResource
import chat.sphinx.utils.linkify.SphinxLinkify
import chat.sphinx.utils.toAnnotatedString
import chat.sphinx.wrapper.message.*
import chat.sphinx.wrapper.message.MessageType
import chat.sphinx.wrapper.message.isSphinxCallLink
import chat.sphinx.wrapper.message.media.*
import chat.sphinx.wrapper.message.retrieveTextToShow
import chat.sphinx.wrapper.thumbnailUrl
import chat.sphinx.wrapper.util.getInitials
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import theme.*
import java.awt.MediaTracker
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URL
import java.net.http.HttpClient
import javax.imageio.ImageIO
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.JLabel

@Composable
fun ChatCard(
    chatMessage: ChatMessage,
    chatViewModel: ChatViewModel,
    modifier: Modifier? = null
) {
    val uriHandler = LocalUriHandler.current

    val backgroundColor = when {
        chatMessage.isSent && chatMessage.message.isPaidInvoice ->
            MaterialTheme.colorScheme.onSecondaryContainer

        chatMessage.isReceived && chatMessage.message.isPaidInvoice ->
            MaterialTheme.colorScheme.inversePrimary

        chatMessage.isSent && chatMessage.message.isExpiredInvoice() ->
            MaterialTheme.colorScheme.inversePrimary

        chatMessage.isReceived && (chatMessage.message.isPaidInvoice || chatMessage.message.isExpiredInvoice()) ->
            MaterialTheme.colorScheme.onSecondaryContainer

        chatMessage.message.type.isInvoice() ->
            MaterialTheme.colorScheme.background

        chatMessage.isReceived ->
            MaterialTheme.colorScheme.onSecondaryContainer

        else ->
            MaterialTheme.colorScheme.inversePrimary
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
                JitsiAudioVideoCall(chatMessage, chatViewModel.dashboardViewModel)
            }
            chatMessage.message.type == MessageType.DirectPayment -> {
                DirectPaymentUI(chatMessage, chatViewModel)
            }
            chatMessage.message.type == MessageType.BotRes -> {
                BotResponse(chatMessage, chatViewModel)
            }
            chatMessage.message.type == MessageType.Invoice -> {
                InvoiceMessage(chatMessage, chatViewModel, backgroundColor)
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
                        chatMessage.message.giphyData?.let { giphy ->
                        GiphyMessageBubble(
                            giphyData = giphy,
                            modifier = Modifier
                                .wrapContentHeight()
                                .fillMaxWidth()
                        )
                    } ?: chatMessage.message.messageMedia?.let { media ->
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
                        val isThread = chatMessage.threadState != null
                        MessageTextLabel(chatMessage, chatViewModel, uriHandler, isThread)
                        FailedContainer(chatMessage)

                        if (isThread) {
                            BubbleThreadLayout(
                                thread = chatMessage.threadState,
                                chatMessage = chatMessage,
                                chatViewModel = chatViewModel,
                            )
                        }

                        if (!isThread) {
                            BoostedFooter(
                                chatMessage,
                                modifier = Modifier.width(
                                    maxOf(rowWidth, 200.dp)
                                ).padding(12.dp, 0.dp, 12.dp, 12.dp)
                            )
                        }

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
fun GiphyMessageBubble(
    giphyData: GiphyData,
    modifier: Modifier = Modifier
) {
    Box(Modifier.size(200.dp)) {
        DesktopGifImage(
            url = giphyData.url,
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

@Composable
fun DesktopGifImage(
    url: String,
    modifier: Modifier = Modifier
) {
    var gifFrames by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var currentFrame by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var frameDuration by remember { mutableStateOf(100L) }

    // Load GIF frames
    LaunchedEffect(url) {
        isLoading = true
        hasError = false
        try {
            val frames = loadGifFrames(url)
            gifFrames = frames
            isLoading = false
        } catch (e: Exception) {
            println("Error loading GIF: ${e.message}")
            hasError = true
            isLoading = false
        }
    }

    // Animate frames
    LaunchedEffect(gifFrames) {
        if (gifFrames.isNotEmpty()) {
            while (true) {
                delay(frameDuration)
                currentFrame = (currentFrame + 1) % gifFrames.size
            }
        }
    }

    Box(
        modifier = modifier
            .size(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Loading GIF...",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            hasError -> {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "Error loading GIF",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Failed to load GIF",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            gifFrames.isNotEmpty() -> {
                Image(
                    bitmap = gifFrames[currentFrame],
                    contentDescription = "Animated GIF",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

private suspend fun loadGifFrames(url: String): List<ImageBitmap> = withContext(Dispatchers.IO) {
    val httpClient = HttpClient()
    try {
        val response = httpClient.get(url)
        val bytes = response.readBytes()

        val input = ByteArrayInputStream(bytes)
        val readers = ImageIO.getImageReadersByFormatName("gif")

        if (!readers.hasNext()) {
            val bitmap = org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
            return@withContext listOf(bitmap)
        }

        val reader = readers.next()
        val iis = ImageIO.createImageInputStream(input)
        reader.input = iis

        val frames = mutableListOf<ImageBitmap>()
        val numFrames = reader.getNumImages(true)

        for (i in 0 until numFrames) {
            val bufferedImage = reader.read(i)
            val bitmap = bufferedImageToImageBitmap(bufferedImage)
            frames.add(bitmap)
        }

        reader.dispose()
        iis.close()
        frames
    } finally {
        httpClient.close()
    }
}

private fun bufferedImageToImageBitmap(bufferedImage: BufferedImage): ImageBitmap {
    val baos = ByteArrayOutputStream()
    ImageIO.write(bufferedImage, "png", baos)
    val bytes = baos.toByteArray()
    return org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
}

private suspend fun loadImageFromUrl(url: String): ImageBitmap = withContext(Dispatchers.IO) {
    val response = HttpClient().use { client ->
        client.get(url)
    }
    val bytes = response.readBytes()
    loadImageBitmap(ByteArrayInputStream(bytes))
}

private fun loadImageBitmap(inputStream: InputStream): ImageBitmap {
    return org.jetbrains.skia.Image.makeFromEncoded(inputStream.readBytes()).toComposeImageBitmap()
}

@Composable
fun MessageTextLabel(
    chatMessage: ChatMessage,
    chatViewModel: ChatViewModel,
    uriHandler: UriHandler,
    isThread: Boolean
) {
    val topPadding = if (chatMessage.message.isPaidTextMessage && chatMessage.isSent) 44.dp else 12.dp
    val messageText = chatMessage.message.retrieveTextToShow()?.trim() ?: ""
    val isThreadHeader = chatMessage.isThreadHeader
    val isTribeLink = SphinxLinkify.gatherLinks(text = messageText, mask = SphinxLinkify.TRIBE_LINK).isNotEmpty()

    if (messageText.isNotEmpty()) {
        val annotatedString = messageText.toAnnotatedString()

        Row(
            modifier = Modifier
                .padding(12.dp, topPadding, 12.dp, 12.dp)
                .wrapContentWidth(if (chatMessage.isSent) Alignment.End else Alignment.Start)
                .then(if (isThreadHeader || isTribeLink) Modifier.width(316.dp) else Modifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClickableText(
                text = annotatedString,
                style = TextStyle(
                    fontWeight = FontWeight.W400,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 13.sp,
                    fontFamily = Roboto
                ),
                maxLines = if (isThread) 2 else Int.MAX_VALUE,
                onClick = { offset ->
                    annotatedString.getStringAnnotations(start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            if (
                                annotation.tag.equals("URL", ignoreCase = true) ||
                                annotation.tag.equals("WebURL", ignoreCase = true)
                            ) {
                                uriHandler.openUri(annotation.item)
                            }
                        }
                }
            )
        }
    } else if (chatMessage.message.messageDecryptionError) {
        Text(
            modifier = Modifier
                .wrapContentWidth(if (chatMessage.isSent) Alignment.End else Alignment.Start)
                .padding(12.dp),
            text = "DECRYPTION ERROR",
            fontWeight = FontWeight.W300,
            fontFamily = Roboto,
            fontSize = 13.sp,
            color = Color.Red
        )
    } else if (chatMessage.message.isPaidTextMessage) {
        Text(
            modifier = Modifier
                .wrapContentWidth(Alignment.Start)
                .padding(12.dp, topPadding, 12.dp, 12.dp),
            text = "Loading message...",
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
                    NewTribePreview(chatMessage, tribeLinkPreview, chatViewModel)
                }
            }
            is ChatMessage.LinkPreview.HttpUrlPreview -> {
//                (linkPreview.value as? ChatMessage.LinkPreview.HttpUrlPreview)?.let { webLinkPreview ->
//                    URLPreview(webLinkPreview, chatViewModel, uriHandler)
//                }
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
fun InvoiceMessage(chatMessage: ChatMessage, chatViewModel: ChatViewModel, columnBackground: Color) {
    val isInvoiceExpired = chatMessage.message.isExpiredInvoice()
    val borderColor = if (chatMessage.isSent) MaterialTheme.colorScheme.onBackground else primary_green
    val cornerRadius = 14.dp
    val dashWidth = 8.dp
    val dashGap = 4.dp
    val alphaValue = if (isInvoiceExpired && !chatMessage.message.isPaidInvoice) 0.5f else 1.0f

    Box(
        modifier = Modifier
            .padding(6.dp)
            .width(260.dp)
            .wrapContentHeight()
            .then(
                if (!isInvoiceExpired && (!chatMessage.message.isPaidInvoice)) Modifier.drawBehind {
                    val strokeWidth = 1.5.dp.toPx()
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
                .padding(6.dp)
                .background(columnBackground, shape = RoundedCornerShape(10.dp))
                .padding(
                    start = 6.dp,
                    top = 4.dp,
                    end = 4.dp,
                    bottom = 4.dp
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (chatMessage.message.isPaidInvoice) {
                    val icon = if (chatMessage.isSent) Res.drawable.ic_received else Res.drawable.ic_sent
                    val color = if (chatMessage.isSent) primary_blue else MaterialTheme.colorScheme.tertiary

                    Image(
                        painter = imageResource(icon),
                        contentDescription = "Icon",
                        modifier = Modifier.size(28.dp),
                        colorFilter = ColorFilter.tint(color.copy(alpha = alphaValue))
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "Invoice Icon",
                        tint = MaterialTheme.colorScheme.tertiary.copy(alpha = alphaValue),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = chatMessage.message.amount.value.toString() ?: "",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = alphaValue)
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "sat",
                    style = TextStyle(
                        fontWeight = FontWeight.Light,
                        fontSize = 12.sp,
                        color = if (chatMessage.message.isPaidInvoice) wash_out_send else MaterialTheme.colorScheme.tertiary.copy(alpha = alphaValue)
                    )
                )
            }

            if (chatMessage.isReceived && !chatMessage.message.isPaidInvoice) {
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val alpha = !chatMessage.message.isExpiredInvoice()
                    Button(
                        onClick = {
                            chatViewModel.payContactInvoice(chatMessage.message)
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = primary_green.copy(alpha = if (alpha) 1.0f else 0.5f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "PAY",
                                modifier = Modifier.align(Alignment.Center),
                                fontSize = 12.sp,
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
                                    .size(22.dp),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.tertiary.copy(alpha = alphaValue))
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun BubbleThreadLayout(
    thread: ChatMessage.ThreadHolder?,
    chatMessage: ChatMessage,
    chatViewModel: ChatViewModel,
    fixedWidth: Dp = 360.dp
) {
    if (thread == null) return

    val hasMoreReplies = thread.users.size > 3
    val hasAtTwoReplies = thread.users.size == 2
    val hasMoreThanTwoReplies = thread.users.size > 2

    Box(
        modifier = Modifier
            .width(fixedWidth)
            .clickable {
                chatViewModel.navigateToThreadChat(chatMessage.message.uuid?.value, false)
            }
            .background(
                color = if (chatMessage.isSent) MaterialTheme.colorScheme.inversePrimary
                else MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        // First Reply
        if (thread.users.isNotEmpty()) {
            ReplyRow(
                user = thread.users[0],
                chatMessage = chatMessage,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .zIndex(1f) // Highest layer
            )
        }

        // Second Reply (if applicable)
        if (hasMoreThanTwoReplies) {
            ReplyRow(
                user = thread.users[1],
                chatMessage = chatMessage,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(top = 20.dp) // Overlapping without offset
                    .zIndex(2f)
            )
        }

        // "More Replies" Row
        if (hasMoreReplies) {
            MoreRepliesRow(
                remainingCount = thread.replyCount - 3,
                isSentMessage = thread.isSentMessage,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(top = 40.dp) // Further overlap
                    .zIndex(3f)
            )
        }

        val lastReplyPadding = if (hasMoreReplies) 80.dp else if (hasAtTwoReplies) 20.dp else 40.dp

        LastReplyRow(
            lastReplyUser = thread.lastReplyUser,
            lastReplyMessage = thread.lastReplyMessage,
            lastReplyDate = thread.lastReplyDate,
            isSentMessage = thread.isSentMessage,
            mediaAttachment = thread.lastReplyAttachment,
            chatMessage = chatMessage,
            chatViewModel = chatViewModel,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(top = lastReplyPadding)
                .zIndex(4f)

        )
    }
}

@Composable
fun ReplyRow(
    user: ChatMessage.ReplyUserHolder,
    chatMessage: ChatMessage,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .border(
                    width = 1.dp,
                    color = light_divider,
                    shape = RoundedCornerShape(8.dp)
                )
                .background(
                    color = md_theme_dark_onSurfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                val color = chatMessage.colors[chatMessage.message.id.value]

                PhotoUrlImage(
                    photoUrl = user.photoUrl?.thumbnailUrl,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .then( Modifier.drawWithContent {
                                drawContent()
                                drawRect(
                                    color = Color.White.copy(alpha = 0.3f), // 30% white overlay
                                    size = size
                                )
                            }
                        ),
                    color = if (color != null) Color(color) else null,
                    firstNameLetter = user.alias?.value?.getInitials(),
                    fontSize = 12
                )

                // Add additional content here if needed
            }
        }
    }
}

@Composable
fun MoreRepliesRow(
    remainingCount: Int,
    isSentMessage: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .border(
                    width = 1.dp,
                    color = light_divider,
                    shape = RoundedCornerShape(8.dp)
                )
                .background(
                    color = md_theme_dark_onSurfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // White circle with number
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(26.dp)
                        .background(
                            color = MaterialTheme.colorScheme.tertiary,
                            shape = CircleShape
                        )
                ) {
                    Text(
                        text = remainingCount.toString(),
                        fontFamily = Roboto,
                        fontWeight = FontWeight.W600,
                        color = md_theme_dark_onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // "More Replies" text
                Text(
                    text = "more replies",
                    fontFamily = Roboto,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W400,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun LastReplyRow(
    lastReplyUser: ChatMessage.ReplyUserHolder,
    lastReplyMessage: String?,
    lastReplyDate: String,
    isSentMessage: Boolean,
    mediaAttachment: ChatMessage?,
    chatMessage: ChatMessage,
    chatViewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (isSentMessage)
                    md_theme_dark_onSecondaryContainer
                else
                    light_divider,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            val color = lastReplyUser.colorKey ?: chatMessage.colors[chatMessage.message.id.value]

            PhotoUrlImage(
                lastReplyUser.photoUrl?.thumbnailUrl ?: chatMessage.message.senderPic?.thumbnailUrl,
                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                    .size(30.dp)
                    .clip(CircleShape),
                color = if (color != null) Color(color) else null,
                firstNameLetter = lastReplyUser.alias?.value?.getInitials() ?: chatMessage.message.senderAlias?.value?.getInitials(),
                fontSize = 12
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // User Name
                Text(
                    text = lastReplyUser.alias?.value ?: "Unknown",
                    fontFamily = Roboto,
                    fontWeight = FontWeight.W500,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(Modifier.width(4.dp))
                // Reply Date
                Text(
                    text = lastReplyDate,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = Roboto,
                    fontSize = 11.sp
                )
            }
        }

        // Last Reply Message
        if (!lastReplyMessage.isNullOrEmpty()) {
            Text(
                text = lastReplyMessage,
                fontWeight = FontWeight.W400,
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = 13.sp,
                fontFamily = Roboto,
                modifier = Modifier.padding(6.dp)
            )
        }

        // Media Attachment
        if (mediaAttachment != null) {
            mediaAttachment.message.messageMedia?.let { media ->
                if (media.mediaType.isImage) {
                    MessageMediaImage(
                        mediaAttachment,
                        chatViewModel = chatViewModel,
                        modifier = Modifier.wrapContentHeight().fillMaxWidth()
                    )
                } else if (media.mediaType.isUnknown || media.mediaType.isPdf) {
                        MessageFile(
                        chatMessage = mediaAttachment,
                        chatViewModel = chatViewModel,
                    )
                } else if (media.mediaType.isVideo) {
                    MessageVideo(
                        chatMessage = mediaAttachment,
                        chatViewModel = chatViewModel,
                        modifier = Modifier.wrapContentHeight().fillMaxWidth()
                    )
                } else if (media.mediaType.isAudio) {
                    MessageAudio(
                        chatMessage = mediaAttachment,
                        chatViewModel = chatViewModel,
                    )
                }
            }

        }
    }
}

@Composable
fun MediaAttachment(type: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
    ) {
        Text(
            text = "Media ($type)",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
