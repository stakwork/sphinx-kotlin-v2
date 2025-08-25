package chat.sphinx.common.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.*
import javafx.geometry.Pos
import javafx.scene.layout.StackPane
import javafx.scene.media.MediaException
import okio.Path
import java.io.File
import java.awt.Color as AwtColor
import javafx.scene.paint.Color as FxColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import chat.sphinx.common.components.media_player.EnhancedFxPlayerHolder
import chat.sphinx.common.components.media_player.VideoPlayerController
import chat.sphinx.common.state.FullScreenVideoData
import chat.sphinx.utils.saveFile
import chat.sphinx.wrapper.message.media.FileName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import theme.primary_blue
import javafx.animation.PauseTransition
import javafx.embed.swing.JFXPanel
import javafx.embed.swing.SwingFXUtils
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.SnapshotParameters
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import javafx.beans.value.ChangeListener
import java.io.ByteArrayOutputStream
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import org.jetbrains.skia.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.CompletableDeferred
import javafx.application.Platform
import javafx.util.Duration
import javax.swing.SwingUtilities

private fun Color.toAwt(): AwtColor =
    AwtColor(red, green, blue, alpha)

private fun Color.toFx(): FxColor =
    FxColor(red.toDouble(), green.toDouble(), blue.toDouble(), alpha.toDouble())


@Composable
fun VideoFullScreen(
    fullScreenVideoState: MutableState<FullScreenVideoData?>
) {
    fullScreenVideoState.value?.let { videoData ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f))
                .clickable(enabled = true, onClick = {
                    fullScreenVideoState.value = null
                })
        ) {
            VideoFullScreenContent(
                videoData = videoData
            ) {
                fullScreenVideoState.value = null
            }
        }
    }
}
@Composable
fun EnhancedVideoPlayer(
    filePath: String,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    onError: (String) -> Unit = { println(it) },
    onPlayerReady: (VideoPlayerController) -> Unit = {},
    showControls: Boolean = true
) {

    val holder = remember { EnhancedFxPlayerHolder() }
    val controller = remember { VideoPlayerController(holder) }



    val themeColor = MaterialTheme.colorScheme.onSurfaceVariant
    val bgAwt = remember(themeColor) { themeColor.toAwt() }
    val bgFx = remember(themeColor) { themeColor.toFx() }

    LaunchedEffect(filePath) {
        onPlayerReady(controller)
    }

    DisposableEffect(filePath) {
        onDispose {
            holder.cleanup()
        }
    }

    Box(modifier = modifier) {
        SwingPanel(
            modifier = Modifier.fillMaxSize(),
            factory = {
                val jfxPanel = JFXPanel().also {
                    it.background = bgAwt
                    holder.jfxPanel = it
                }

                // Ensure FX runtime never auto-exits when last window/player stops.
                Platform.runLater {
                    try {
                        javafx.application.Platform.setImplicitExit(false)

                        val root = StackPane().apply {
                            minWidth = StackPane.USE_COMPUTED_SIZE
                            minHeight = StackPane.USE_COMPUTED_SIZE
                        }
                        val scene = Scene(root).apply { fill = bgFx }
                        jfxPanel.scene = scene

                        val mv = MediaView().apply {
                            isPreserveRatio = true
                            isSmooth = true
                        }
                        mv.fitWidthProperty().bind(root.widthProperty())
                        mv.fitHeightProperty().bind(root.heightProperty())
                        StackPane.setAlignment(mv, Pos.CENTER)
                        root.children.add(mv)

                        holder.mediaView = mv
                    } catch (t: Throwable) {
                        onError("FX init failed: ${t.message}")
                    }
                }

                jfxPanel
            },
            update = { jfxPanel ->
                jfxPanel.background = bgAwt
                jfxPanel.scene?.fill = bgFx

                if (holder.currentPath != filePath) {
                    holder.currentPath = filePath
                    val uri = File(filePath).toURI().toString()

                    Platform.runLater {
                        try {
                            // Dispose old player (safe if null)
                            holder.player?.let { old ->
                                try { old.stop() } catch (_: Exception) {}
                                try { old.dispose() } catch (_: Exception) {}
                            }

                            val media = Media(uri).apply {
                                errorProperty().addListener { _, _, err ->
                                    onError("Media Error: ${err?.message ?: "unknown"}")
                                }
                            }

                            val player = MediaPlayer(media)
                            holder.player = player
                            controller.initialize(player)

                            // Attach to existing MediaView (created in factory)
                            val mv = holder.mediaView
                            if (mv != null) {
                                mv.mediaPlayer = player
                            } else {
                                onError("MediaView not ready; FX scene not initialized")
                                return@runLater
                            }

                            player.setOnReady {
                                holder.isReady = true
                                if (autoPlay) player.play()
                            }
                            player.setOnError {
                                onError(player.error?.message ?: "MediaPlayer Error (unknown)")
                            }
                        } catch (t: Throwable) {
                            onError("Reload failed: ${t.message}")
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun VideoControlsOverlay(
    controller: VideoPlayerController,
    modifier: Modifier = Modifier
) {
    var showControls by remember { mutableStateOf(true) }
    var isUserSeeking by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableStateOf(0f) }
    var showVolumeSlider by remember { mutableStateOf(false) }
    var showSpeedSelector by remember { mutableStateOf(false) }

    // Auto-hide controls
    LaunchedEffect(showControls) {
        if (showControls && controller.isPlaying) {
            delay(3000)
            showControls = false
        }
    }

    // Update slider position when not seeking
    LaunchedEffect(controller.currentTime, controller.duration) {
        if (!isUserSeeking && controller.duration > 0) {
            sliderPosition = (controller.currentTime.toFloat() / controller.duration.toFloat()) * 100f
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
    ) {
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            VideoControlsBar(
                controller = controller,
                sliderPosition = sliderPosition,
                isUserSeeking = isUserSeeking,
                showVolumeSlider = showVolumeSlider,
                showSpeedSelector = showSpeedSelector,
                onSliderChange = { position ->
                    isUserSeeking = true
                    sliderPosition = position
                },
                onSliderChangeFinished = {
                    val newTimeMillis = (controller.duration * (sliderPosition / 100f)).toLong()
                    controller.seek(newTimeMillis)
                    isUserSeeking = false
                },
                onVolumeToggle = { showVolumeSlider = !showVolumeSlider },
                onSpeedToggle = { showSpeedSelector = !showSpeedSelector },
                onControlsInteraction = { showControls = true }
            )
        }
    }
}

@Composable
fun VideoControlsBar(
    controller: VideoPlayerController,
    sliderPosition: Float,
    isUserSeeking: Boolean,
    showVolumeSlider: Boolean,
    showSpeedSelector: Boolean,
    onSliderChange: (Float) -> Unit,
    onSliderChangeFinished: () -> Unit,
    onVolumeToggle: () -> Unit,
    onSpeedToggle: () -> Unit,
    onControlsInteraction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.Black.copy(alpha = 0.72f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // 1) Rewind
        IconButton(
            onClick = {
                onControlsInteraction()
                controller.rewind()
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Replay10,
                contentDescription = "Rewind 10s",
                tint = Color.White
            )
        }

        // 2) Play / Pause
        IconButton(
            onClick = {
                onControlsInteraction()
                if (controller.isPlaying) controller.pause() else controller.play()
            },
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = if (controller.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (controller.isPlaying) "Pause" else "Play",
                tint = Color.White
            )
        }

        // 3) Forward
        IconButton(
            onClick = {
                onControlsInteraction()
                controller.forward()
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Forward10,
                contentDescription = "Forward 10s",
                tint = Color.White
            )
        }

        // 4) Current time
        Text(
            text = formatMillis(controller.currentTime),
            color = Color(0xFFE0E0E0),
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 6.dp, end = 6.dp)
        )

        // 5) Seekbar (weighted to take remaining space)
        Slider(
            value = sliderPosition,
            onValueChange = {
                onControlsInteraction()
                onSliderChange(it)
            },
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = 0f..100f,
            modifier = Modifier
                .weight(1f)
                .height(24.dp),
            colors = SliderDefaults.colors(
                thumbColor = primary_blue,
                activeTrackColor = primary_blue,
                inactiveTrackColor = Color.DarkGray
            )
        )

        // 6) Total time
        Text(
            text = formatMillis(controller.duration),
            color = Color(0xFFE0E0E0),
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 6.dp, end = 6.dp)
        )

        // 7) Volume
        Box(modifier = Modifier.padding(start = 4.dp)) {
            // --- inside the volume Box in VideoControlsBar() ---
            IconButton(
                onClick = {
                    onControlsInteraction()
                    onVolumeToggle()
                },
                modifier = Modifier.size(32.dp)
            ) {
                val vol = controller.volume
                val icon = when {
                    vol == 0.0 -> Icons.Filled.VolumeOff
                    vol < 0.5 -> Icons.Filled.VolumeDown
                    else -> Icons.Filled.VolumeUp
                }
                Icon(icon, contentDescription = "Volume", tint = Color.White)
            }

            if (showVolumeSlider) {
                VolumePopup(
                    volume = controller.volume.toFloat(),
                    onVolumeChange = { controller.setVolume(it.toDouble()) },
                    onDismiss = onVolumeToggle
                )
            }
        }

        // 8) Playback speed (popup above)
        Box(
            modifier = Modifier.padding(start = 2.dp)
        ) {
            TextButton(
                onClick = {
                    onControlsInteraction()
                    onSpeedToggle()
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = "${controller.playbackSpeed}x",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (showSpeedSelector) {
                PlaybackSpeedPopup(
                    currentSpeed = controller.playbackSpeed.toFloat(),
                    onSpeedChange = { controller.setPlaybackSpeed(it.toDouble()) },
                    onDismiss = onSpeedToggle
                )
            }
        }
    }
}

@Composable
private fun VolumePopup(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val positionProvider = rememberAboveAnchorPositionProvider(gap = 10.dp)

    Popup(
        popupPositionProvider = positionProvider,
        properties = PopupProperties(focusable = true),
        onDismissRequest = onDismiss
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2E3440).copy(alpha = 0.95f)
            ),
            modifier = Modifier.wrapContentSize().padding(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(180.dp)
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f,
                    modifier = Modifier
                        .requiredWidth(156.dp) // becomes vertical travel after rotation
                        .rotate(-90f),
                    colors = SliderDefaults.colors(
                        thumbColor = primary_blue,
                        activeTrackColor = primary_blue,
                        inactiveTrackColor = Color.DarkGray
                    )
                )
            }
        }
    }
}

/** Centers the popup horizontally over its anchor and places it above with a gap. */
@Composable
private fun rememberAboveAnchorPositionProvider(gap: Dp): PopupPositionProvider {
    val gapPx = with(LocalDensity.current) { gap.roundToPx() }
    return remember(gapPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val centeredX = anchorBounds.left + anchorBounds.width / 2 - popupContentSize.width / 2
                val aboveY = anchorBounds.top - gapPx - popupContentSize.height
                // Keep inside window bounds
                val x = centeredX.coerceIn(0, windowSize.width - popupContentSize.width)
                val y = aboveY.coerceAtLeast(0)
                return IntOffset(x, y)
            }
        }
    }
}

@Composable
private fun PlaybackSpeedPopup(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val positionProvider = rememberAboveAnchorPositionProvider(gap = 10.dp)
    val speeds = listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f)

    Popup(
        popupPositionProvider = positionProvider,
        properties = PopupProperties(focusable = true),
        onDismissRequest = onDismiss
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2E3440).copy(alpha = 0.95f)
            ),
            modifier = Modifier.wrapContentSize().padding(6.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .width(96.dp)
                    .padding(vertical = 6.dp)
            ) {
                items(speeds) { speed ->
                    TextButton(
                        onClick = {
                            onSpeedChange(speed)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${speed}x",
                            color = if (speed == currentSpeed) primary_blue else Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (speed == currentSpeed) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoFullScreenContent(
    videoData: FullScreenVideoData,
    callback: () -> Unit
) {
    var controller by remember { mutableStateOf<VideoPlayerController?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        EnhancedVideoPlayer(
            filePath = videoData.path.toString(),
            modifier = Modifier
                .size(width = 640.dp, height = 480.dp),
            autoPlay = true,
            showControls = false,
            onPlayerReady = { controller = it },
            onError = { error ->
                println("Video playback error: $error")
            }
        )

        Box(
            modifier = Modifier
                .padding(20.dp)
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close fullscreen video",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .size(30.dp)
                            .clickable(enabled = true) { callback() }
                    )

                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Save video",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .size(30.dp)
                            .clickable(enabled = true) {
                                scope.launch {
                                    val fileName = videoData.chatMessage.message.messageMedia?.fileName
                                        ?: FileName("video.mp4")
                                    saveFile(fileName, videoData.path)
                                }
                            }
                    )
                }

                controller?.let {
                    VideoControlsOverlay(
                        controller = it,
                        modifier = Modifier.wrapContentSize()
                    )
                }
            }
        }
    }
}

