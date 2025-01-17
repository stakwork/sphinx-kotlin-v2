package chat.sphinx.common.components.chat

import CommonButton
import Roboto
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import chat.sphinx.common.chatMesssageUI.ImageProfile
import chat.sphinx.common.chatMesssageUI.MessageTextLabel
import chat.sphinx.common.chatMesssageUI.getBubbleShape
import chat.sphinx.common.components.CommonMenuButton
import chat.sphinx.common.components.FileUI
import chat.sphinx.common.components.ImageFullScreen
import chat.sphinx.common.models.ChatMessage
import chat.sphinx.common.viewmodel.chat.ChatViewModel
import chat.sphinx.utils.toAnnotatedString
import chat.sphinx.wrapper.chat.isTribeOwnedByAccount
import chat.sphinx.wrapper.message.isPaidTextMessage
import chat.sphinx.wrapper.message.media.FileName
import chat.sphinx.wrapper.message.media.MediaType
import chat.sphinx.wrapper.message.media.isImage
import chat.sphinx.wrapper.message.retrieveTextToShow
import okio.Path
import theme.primary_blue
import theme.primary_red
import theme.wash_out_received


@Composable
fun AttachmentPreview(
    chatViewModel: ChatViewModel?,
    modifier: Modifier = Modifier
) {
    chatViewModel?.editMessageState?.attachmentInfo?.value?.let { attachmentInfo ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.85f))
        ) {
            if (attachmentInfo.mediaType.isImage) {
                ImageFullScreen(attachmentInfo.filePath) {
                    chatViewModel.resetMessageFile()
                }
            } else {
                FilePreview(
                    attachmentInfo.filePath,
                    attachmentInfo.fileName,
                    attachmentInfo.mediaType
                ) {
                    chatViewModel.resetMessageFile()
                }
            }
        }
    }
}

@Composable
fun MessagePinnedPopUp(
    chatViewModel: ChatViewModel?,
    modifier: Modifier = Modifier
) {
    chatViewModel?.pinMessageState?.let { state ->
        when {
            state.isPinning -> {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .padding(50.dp)
                            .background(
                                color = primary_blue,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Message Pinned",
                                fontWeight = FontWeight.W600,
                                fontSize = 14.sp,
                                fontFamily = Roboto,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
                chatViewModel.dismissPinMessagePopUp()
            }
            state.isUnpinning -> {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .padding(50.dp)
                            .background(
                                color = primary_blue,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Message Unpinned",
                                fontWeight = FontWeight.W600,
                                fontSize = 14.sp,
                                fontFamily = Roboto,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
                chatViewModel.dismissPinMessagePopUp()
            }
        }
    }
}

@Composable
fun MessagePinnedFullContent(
    chatViewModel: ChatViewModel?,
    modifier: Modifier = Modifier
) {
    chatViewModel?.pinMessageState?.pinFullContentScreen.let { state ->
        if (state == true) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(min = 300.dp, max = 400.dp)
                        .wrapContentHeight()
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        IconButton(
                            onClick = { chatViewModel?.dismissPinFullContentScreen() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned Content",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pinned Message",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            chatViewModel?.pinMessageState?.pinMessage?.value?.let { pinnedMessage ->
                                ImageProfile(
                                    chatMessage = pinnedMessage,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }

                            chatViewModel?.pinMessageState?.pinMessage?.value?.let { pinnedMessage ->
                                Text(
                                    text = pinnedMessage.message.senderAlias?.value ?: "",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        chatViewModel?.pinMessageState?.pinMessage?.value?.let { pinnedMessage ->
                            PinMessageCard(
                                chatMessage = pinnedMessage,
                                chatViewModel = chatViewModel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                            )
                        }

                        chatViewModel?.pinMessageState?.pinMessage?.value?.let { pinnedMessage ->
                            if (pinnedMessage.chat.isTribeOwnedByAccount(pinnedMessage.accountOwner().nodePubKey)) {
                                Spacer(modifier = Modifier.height(16.dp))
                                CommonMenuButton(
                                    text = "Unpin Message",
                                    customColor = wash_out_received,
                                    iconColor = primary_red,
                                    startIcon = Icons.Default.PushPin,
                                    centerContent = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .padding(horizontal = 32.dp),
                                    textColor = MaterialTheme.colorScheme.tertiary,
                                    callback = {
                                        chatViewModel.dismissPinFullContentScreen()
                                        chatViewModel.onUnpinnedClicked(pinnedMessage)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinMessageCard(
    chatMessage: ChatMessage,
    chatViewModel: ChatViewModel,
    modifier: Modifier? = null
) {
    val uriHandler = LocalUriHandler.current

    val backgroundColor = MaterialTheme.colorScheme.inversePrimary

    Card(
        backgroundColor = backgroundColor,
        shape = RoundedCornerShape(topEnd = 10.dp, topStart = 0.dp, bottomEnd = 10.dp, bottomStart = 10.dp),
        modifier = modifier ?: Modifier
    ) {
        val density = LocalDensity.current
        var rowWidth by remember { mutableStateOf(0.dp) }

        Column(modifier = Modifier.onSizeChanged {
            rowWidth = with(density) { it.width.toDp() }
        }) {
            Column {
                PinMessageTextLabel(chatMessage, chatViewModel, uriHandler)
            }
        }
    }
}

@Composable
fun PinMessageTextLabel(
    chatMessage: ChatMessage,
    chatViewModel: ChatViewModel,
    uriHandler: UriHandler
) {
    val topPadding = 12.dp

    val messageText = chatMessage.message.retrieveTextToShow()?.trim() ?: ""

    if (messageText.isNotEmpty()) {
        val annotatedString = messageText.toAnnotatedString()

        Row(
            modifier = Modifier
                .padding(12.dp, topPadding, 12.dp, 12.dp)
                .wrapContentWidth(Alignment.Start),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClickableText(
                text = annotatedString,
                style = TextStyle(
                    fontWeight = FontWeight.W400,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 13.sp,
                    fontFamily = Roboto,
                ),
                onClick = { offset ->
                    annotatedString.getStringAnnotations("URL", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            uriHandler.openUri(annotation.item)
                        }
                }
            )
        }
    } else if (chatMessage.message.messageDecryptionError) {
        androidx.compose.material.Text(
            modifier = Modifier
                .wrapContentWidth(Alignment.Start)
                .padding(12.dp),
            text = "DECRYPTION ERROR",
            fontWeight = FontWeight.W300,
            fontFamily = Roboto,
            fontSize = 13.sp,
            color = Color.Red
        )
    } else if (chatMessage.message.isPaidTextMessage) {
        androidx.compose.material.Text(
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
fun FilePreview(
    path: Path? = null,
    fileName: FileName? = null,
    mediaType: MediaType? = null,
    callback: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = false, onClick = {} ),
        contentAlignment = Alignment.Center,
    ) {
        FileUI(
            path,
            fileName,
            mediaType
        )
        Box(
            modifier = Modifier.padding(20.dp).align(Alignment.TopStart)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close fullscreen image view",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(30.dp).clickable(enabled = true, onClick = {
                    callback()
                })
            )
        }
    }
}

