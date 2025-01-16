package chat.sphinx.common.components.chat

import Roboto
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.sphinx.common.components.FileUI
import chat.sphinx.common.components.ImageFullScreen
import chat.sphinx.common.viewmodel.chat.ChatViewModel
import chat.sphinx.wrapper.message.media.FileName
import chat.sphinx.wrapper.message.media.MediaType
import chat.sphinx.wrapper.message.media.isImage
import chat.sphinx.wrapper.message.retrieveTextToShow
import okio.Path
import theme.primary_blue


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
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)),
                contentAlignment = Alignment.TopStart
            ) {
                IconButton(
                    onClick = { chatViewModel?.dismissPinFullContentScreen() },
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned Content",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = chatViewModel?.pinMessageState?.pinMessage?.value?.retrieveTextToShow()
                                ?: "Pinned Message Content",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
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

