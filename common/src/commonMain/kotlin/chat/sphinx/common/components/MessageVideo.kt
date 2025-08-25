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
import androidx.compose.ui.unit.dp
import chat.sphinx.common.models.ChatMessage
import chat.sphinx.common.viewmodel.chat.ChatViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import chat.sphinx.common.Res
import chat.sphinx.common.state.FullScreenVideoData
import chat.sphinx.common.state.fullScreenVideoState
import chat.sphinx.di.container.SphinxContainer
import chat.sphinx.platform.imageResource
import chat.sphinx.utils.notifications.createSphinxNotificationManager
import chat.sphinx.wrapper.message.isPaidPendingMessage
import javafx.animation.PauseTransition
import javafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.embed.swing.JFXPanel
import javafx.embed.swing.SwingFXUtils
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.SnapshotParameters
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import javafx.util.Duration
import kotlinx.coroutines.*
import theme.primary_green
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

// Global thumbnail cache
object VideoThumbnailCache {
    private val cache = ConcurrentHashMap<String, CachedThumbnail>()
    private val maxCacheSize = 50 // Adjust based on memory constraints

    data class CachedThumbnail(
        val thumbnail: ImageBitmap?,
        val timestamp: Long = System.currentTimeMillis(),
        val hasError: Boolean = false
    )

    fun get(videoPath: String): CachedThumbnail? = cache[videoPath]

    fun put(videoPath: String, thumbnail: ImageBitmap?, hasError: Boolean = false) {
        // Clean cache if it's getting too large
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
){
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
                messageId = message.id.value, // Use message ID for unique caching
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
fun VideoLoadingView(
    modifier: Modifier,
) {
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ){
        Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier=Modifier.height(8.dp))
            Text("Loading/Decrypting...", fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
fun PaidVideoOverlay(
    modifier: Modifier,
) {
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

@Composable
fun VideoThumbnailPreview(
    videoPath: String,
    messageId: Long,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Use messageId as cache key for better uniqueness
    val cacheKey = "${messageId}_${File(videoPath).lastModified()}"

    var thumbnail by remember(cacheKey) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(cacheKey) { mutableStateOf(true) }
    var hasError by remember(cacheKey) { mutableStateOf(false) }

    // Check cache first
    LaunchedEffect(cacheKey) {
        val cached = VideoThumbnailCache.get(cacheKey)
        if (cached != null) {
            thumbnail = cached.thumbnail
            hasError = cached.hasError
            isLoading = false
        } else {
            // Initialize JavaFX if not already initialized
            withContext(Dispatchers.IO) {
                try {
                    JFXPanel()
                } catch (e: Exception) {
                    // JavaFX already initialized or initialization failed
                }
            }

            // Extract thumbnail and cache it
            isLoading = true
            hasError = false

            try {
                val extractedThumbnail = extractVideoThumbnail(videoPath)
                thumbnail = extractedThumbnail
                hasError = extractedThumbnail == null

                // Cache the result
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
            isLoading -> {
                LoadingThumbnailView()
            }
            hasError || thumbnail == null -> {
                ErrorThumbnailView()
            }
            else -> {
                Image(
                    bitmap = thumbnail!!,
                    contentDescription = "Video thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Dark overlay and play button
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

// Utility functions remain the same
private fun bufferedImageToImageBitmap(bufferedImage: BufferedImage): ImageBitmap {
    val baos = ByteArrayOutputStream()
    ImageIO.write(bufferedImage, "png", baos)
    val bytes = baos.toByteArray()
    return org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
}

private suspend fun extractVideoThumbnail(videoPath: String): ImageBitmap? {
    return withTimeoutOrNull(12_000L) {
        val result = CompletableDeferred<ImageBitmap?>()

        Platform.runLater {
            try {
                val uri = File(videoPath).toURI().toString()
                val media = Media(uri)
                val player = MediaPlayer(media).apply {
                    isAutoPlay = false
                    volume = 0.0
                    setOnError {
                        dispose()
                        result.complete(null)
                    }
                }

                player.setOnReady {
                    try {
                        val totalMs = player.totalDuration.toMillis().coerceAtLeast(1.0)
                        val seekMs = minOf(1_000.0, totalMs * 0.1).coerceAtLeast(1.0)
                        val target = Duration.millis(seekMs)

                        val view = MediaView(player).apply {
                            fitWidth = 640.0
                            fitHeight = 360.0
                            isPreserveRatio = true
                        }
                        val scene = Scene(Group(view))

                        player.play()
                        player.seek(target)

                        var timeListener: ChangeListener<Duration>? = null
                        timeListener = ChangeListener<Duration> { _, _, now ->
                            if (now.toMillis() >= target.toMillis() - 5.0) {
                                player.currentTimeProperty().removeListener(timeListener)
                                PauseTransition(Duration.millis(60.0)).apply {
                                    setOnFinished {
                                        try {
                                            player.pause()
                                            val fxImg = view.snapshot(SnapshotParameters(), null)
                                            val buffered = SwingFXUtils.fromFXImage(fxImg, null)
                                            val imgBitmap = bufferedImageToImageBitmap(buffered)
                                            player.dispose()
                                            result.complete(imgBitmap)
                                        } catch (e: Exception) {
                                            player.dispose()
                                            result.complete(null)
                                        }
                                    }
                                }.play()
                            }
                        }
                        player.currentTimeProperty().addListener(timeListener)
                    } catch (e: Exception) {
                        player.dispose()
                        result.complete(null)
                    }
                }
            } catch (e: Exception) {
                result.complete(null)
            }
        }

        result.await()
    }
}