package chat.sphinx.common.components

import Roboto
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.sphinx.common.Res
import chat.sphinx.common.models.ChatMessage
import chat.sphinx.common.state.FullScreenVideoData
import chat.sphinx.common.state.fullScreenVideoState
import chat.sphinx.common.viewmodel.chat.ChatViewModel
import chat.sphinx.di.container.SphinxContainer
import chat.sphinx.platform.imageResource
import chat.sphinx.utils.notifications.createSphinxNotificationManager
import chat.sphinx.wrapper.message.isPaidPendingMessage
import kotlinx.coroutines.*
import theme.primary_green
import java.io.File
import java.util.concurrent.ConcurrentHashMap

// Global thumbnail cache
object VideoThumbnailCache {
    private val cache = ConcurrentHashMap<String, CachedThumbnail>()
    private val maxCacheSize = 50

    data class CachedThumbnail(
        val thumbnail: ImageBitmap?,
        val timestamp: Long = System.currentTimeMillis(),
        val hasError: Boolean = false
    )

    fun get(videoPath: String): CachedThumbnail? = cache[videoPath]

    fun put(videoPath: String, thumbnail: ImageBitmap?, hasError: Boolean = false) {
        if (cache.size >= maxCacheSize) {
            cleanOldEntries()
        }
        cache[videoPath] = CachedThumbnail(thumbnail, hasError = hasError)
    }

    private fun cleanOldEntries() {
        val sortedEntries = cache.entries.sortedBy { it.value.timestamp }
        val toRemove = sortedEntries.take(maxCacheSize / 2)
        toRemove.forEach { cache.remove(it.key) }
    }

    fun clear() = cache.clear()
}

@Composable
fun MessageVideo(
    chatMessage: ChatMessage,
    chatViewModel: ChatViewModel,
    modifier: Modifier = Modifier,
) {
    val videoLoadError = rememberSaveable { mutableStateOf(false) }
    val message = chatMessage.message
    val messageMedia = message.messageMedia
    val localFilepath = messageMedia?.localFile
    val url = messageMedia?.url?.value ?: ""

    if (message.isPaidPendingMessage && chatMessage.isReceived) {
        PaidVideoOverlay(modifier)
    } else {
        LaunchedEffect(url) {
            if (localFilepath == null) {
                chatViewModel.downloadFileMedia(message, chatMessage.isSent)
            }
        }

        if (localFilepath != null) {
            VideoThumbnailPreview(
                videoPath = localFilepath.toString(),
                messageId = message.id.value,
                modifier = modifier,
                onClick = {
                    fullScreenVideoState.value = FullScreenVideoData(
                        path = localFilepath,
                        chatMessage = chatMessage,
                        chatViewModel = chatViewModel
                    )
                }
            )
        } else if (videoLoadError.value) {
            Image(
                painter = imageResource(Res.drawable.ic_received_image_not_available),
                contentDescription = "",
                modifier = Modifier.aspectRatio(1f)
            )
        } else {
            VideoLoadingView(modifier)
        }
    }
}

@Composable
fun VideoLoadingView(modifier: Modifier) {
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
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
                "Loading/Decrypting...",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
fun PaidVideoOverlay(modifier: Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = imageResource(Res.drawable.paid_image_blurred_placeholder),
            contentDescription = "",
            modifier = modifier.fillMaxWidth().aspectRatio(1f)
        )
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = "Lock",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(29.dp).padding(2.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Pay to unlock this video",
                fontFamily = Roboto,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
fun VideoThumbnailPreview(
    videoPath: String,
    messageId: Long,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cacheKey = "${messageId}_${File(videoPath).lastModified()}"
    var thumbnail by remember(cacheKey) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(cacheKey) { mutableStateOf(true) }
    var hasError by remember(cacheKey) { mutableStateOf(false) }

    LaunchedEffect(cacheKey) {
        val cached = VideoThumbnailCache.get(cacheKey)
        if (cached != null) {
            thumbnail = cached.thumbnail
            hasError = cached.hasError
            isLoading = false
        } else {
            isLoading = true
            hasError = false

            try {
                val extractedThumbnail = extractVideoThumbnailOptimized(videoPath)
                thumbnail = extractedThumbnail
                hasError = extractedThumbnail == null
                VideoThumbnailCache.put(cacheKey, extractedThumbnail, hasError)
            } catch (e: Exception) {
                println("Error loading video thumbnail: ${e.message}")
                hasError = true
                VideoThumbnailCache.put(cacheKey, null, true)
            } finally {
                isLoading = false
            }
        }
    }

    Box(
        modifier = modifier
            .height(250.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> LoadingThumbnailView()
            hasError || thumbnail == null -> ErrorThumbnailView()
            else -> {
                Image(
                    bitmap = thumbnail!!,
                    contentDescription = "Video thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Surface(
            color = Color.Black.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxSize()
        ) {}

        Icon(
            Icons.Default.PlayCircleOutline,
            contentDescription = "Play Button",
            tint = Color.White,
            modifier = Modifier.size(80.dp)
        )
    }
}

@Composable
private fun LoadingThumbnailView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Loading thumbnail...",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun ErrorThumbnailView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.7f),
                        Color.Black.copy(alpha = 0.3f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.VideoLibrary,
                contentDescription = "Video",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Video Message",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private suspend fun extractVideoThumbnailOptimized(videoPath: String): ImageBitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(
                "ffmpeg",
                "-i", videoPath,
                "-ss", "00:00:01.000",
                "-vframes", "1",
                "-f", "image2pipe",
                "-vcodec", "png",
                "-"
            ).start()

            val imageBytes = process.inputStream.readBytes()
            process.waitFor()

            if (imageBytes.isNotEmpty()) {
                org.jetbrains.skia.Image.makeFromEncoded(imageBytes).toComposeImageBitmap()
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

fun toast(
    message: String,
    color: Color = primary_green,
    delay: Long = 3000L
) {
    val scope = SphinxContainer.appModule.applicationScope
    val dispatchers = SphinxContainer.appModule.dispatchers
    val sphinxNotificationManager = createSphinxNotificationManager()

    scope.launch(dispatchers.mainImmediate) {
        sphinxNotificationManager.toast(
            "Sphinx",
            message,
            color.value,
            delay
        )
    }
}
