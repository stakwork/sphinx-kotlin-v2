package chat.sphinx.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.toArgb
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.scene.media.Media
import javafx.scene.media.MediaException
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import okio.Path
import java.io.File
import java.awt.Color as AwtColor
import javafx.scene.paint.Color as FxColor
import androidx.compose.ui.graphics.Color // Compose Color

private fun Color.toAwt(): AwtColor =
    AwtColor(red, green, blue, alpha)

private fun Color.toFx(): FxColor =
    FxColor(red.toDouble(), green.toDouble(), blue.toDouble(), alpha.toDouble())


@Composable
fun VideoFullScreen(
    fullScreenVideoState: MutableState<Path?>
) {
    fullScreenVideoState.value?.let { videoPath ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f))
                .clickable(enabled = true, onClick = {
                    fullScreenVideoState.value = null
                })
        ) {
            VideoFullScreenContent(videoPath) {
                fullScreenVideoState.value = null
            }
        }
    }
}

@Composable
fun VideoFullScreenContent(
    path: Path,
    callback: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        VideoPlayer(
            filePath = path.toString(),
            modifier = Modifier
                .size(width = 640.dp, height = 480.dp), // Fixed 16:10 aspect ratio
            autoPlay = true,
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
            Icon(
                Icons.Default.Close,
                contentDescription = "Close fullscreen video",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(30.dp)
                    .clickable(enabled = true, onClick = {
                        callback()
                    })
            )
        }
    }
}@Composable
fun VideoPlayer(
    filePath: String,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    onError: (String) -> Unit = { println(it) }
) {
    val holder = remember(filePath) { FxPlayerHolder() }

    val themeColor = MaterialTheme.colorScheme.onSurfaceVariant
    val bgAwt = remember(themeColor) { themeColor.toAwt() }
    val bgFx  = remember(themeColor) { themeColor.toFx() }

    DisposableEffect(filePath) {
        onDispose {
            holder.player?.let { p ->
                Platform.runLater {
                    try { p.stop() } catch (_: Exception) {}
                    try { p.dispose() } catch (_: Exception) {}
                }
            }
            holder.player = null
        }
    }

    SwingPanel(
        modifier = modifier,
        factory = {
            val jfxPanel = JFXPanel().also { panel ->
                panel.background = bgAwt
                holder.jfxPanel = panel
            }

            Platform.runLater {
                try {
                    val uri = File(filePath).toURI().toString()
                    val media = Media(uri).apply {
                        errorProperty().addListener { _, _, err ->
                            onError("Media Error: ${err?.message ?: "unknown"}")
                        }
                    }

                    val player = MediaPlayer(media).also { holder.player = it }
                    player.setOnError {
                        val msg = player.error?.message ?: "MediaPlayer Error (unknown)"
                        onError(msg)
                    }

                    val root = StackPane().apply {
                        minWidth = StackPane.USE_COMPUTED_SIZE
                        minHeight = StackPane.USE_COMPUTED_SIZE
                    }

                    val scene = Scene(root).apply {
                        fill = bgFx
                    }
                    jfxPanel.scene = scene

                    val mediaView = MediaView(player).apply {
                        isPreserveRatio = true
                        isSmooth = true
                    }
                    mediaView.fitWidthProperty().bind(root.widthProperty())
                    mediaView.fitHeightProperty().bind(root.heightProperty())
                    StackPane.setAlignment(mediaView, Pos.CENTER)
                    root.children.add(mediaView)

                    player.setOnReady { if (autoPlay) player.play() }
                } catch (e: MediaException) {
                    onError("MediaException: ${e.message}")
                } catch (t: Throwable) {
                    onError("Video init failed: ${t.message}")
                }
            }

            jfxPanel
        },
        update = { jfxPanel ->
            jfxPanel.background = bgAwt
            jfxPanel.scene?.fill = bgFx
        }
    )
}


private class FxPlayerHolder(
    var jfxPanel: JFXPanel? = null,
    var player: MediaPlayer? = null
)
