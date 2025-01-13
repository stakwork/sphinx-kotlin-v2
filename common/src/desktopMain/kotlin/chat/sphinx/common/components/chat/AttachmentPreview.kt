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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.sphinx.common.components.FileUI
import chat.sphinx.common.components.ImageFullScreen
import chat.sphinx.common.viewmodel.chat.ChatViewModel
import chat.sphinx.wrapper.message.media.FileName
import chat.sphinx.wrapper.message.media.MediaType
import chat.sphinx.wrapper.message.media.isImage
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
fun MessagePinnedOverlay(
    chatViewModel: ChatViewModel?,
    modifier: Modifier = Modifier
) {
    chatViewModel?.pinMessageState?.isPinning?.let { isPinning ->
        if (isPinning) {
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
