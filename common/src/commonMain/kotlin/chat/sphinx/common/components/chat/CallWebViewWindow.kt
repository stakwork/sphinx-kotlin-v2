package chat.sphinx.common.components.chat

import Roboto
import androidx.compose.ui.platform.LocalUriHandler
import chat.sphinx.utils.getPreferredWindowSize
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import chat.sphinx.common.Res
import chat.sphinx.common.viewmodel.DashboardViewModel
import chat.sphinx.platform.imageResource
import chat.sphinx.wrapper.message.isValidJitsiCallLink
import chat.sphinx.wrapper.message.isValidLiveKitCallLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@Composable
fun CallWebViewWindow(
    url: String,
    dashboardViewModel: DashboardViewModel,
    onClose: () -> Unit
) {
    var isOpen by remember { mutableStateOf(true) }
    val sphinxIcon = imageResource(Res.drawable.sphinx_logo)
    val uriHandler = LocalUriHandler.current

    if (isOpen) {
        Window(
            onCloseRequest = {
                isOpen = false
                onClose()
            },
            title = "Sphinx Call",
            state = WindowState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = getPreferredWindowSize(1200, 800)
            ),
            icon = sphinxIcon
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(color = androidx.compose.material3.MaterialTheme.colorScheme.background)
            ) {
                // Check if WebView is available
                when (dashboardViewModel.getWebViewState()) {
                    DashboardViewModel.WebViewState.Loading -> {
                        Text(
                            text = "WebView is loading, please wait...",
                            fontSize = 14.sp,
                            fontFamily = Roboto,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    DashboardViewModel.WebViewState.NonInitialized,
                    DashboardViewModel.WebViewState.Error -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "WebView not available",
                                fontSize = 14.sp,
                                fontFamily = Roboto,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Opening in browser...",
                                fontSize = 12.sp,
                                fontFamily = Roboto,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // Fallback to browser
                        LaunchedEffect(url) {
                            uriHandler.openUri(url)

                            onClose()
                        }
                    }
                    DashboardViewModel.WebViewState.RestartRequired -> {
                        Text(
                            text = "Please run Sphinx as administrator to use calls",
                            fontSize = 14.sp,
                            fontFamily = Roboto,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        if (dashboardViewModel.isWebViewLoaded()) {
                            MaterialTheme {
                                val webViewState = rememberWebViewState(url)
                                val webViewNavigator = remember {
                                    WebViewNavigator(CoroutineScope(Dispatchers.IO))
                                }

                                // Initialize WebView settings
                                webViewState.webSettings.apply {
                                    zoomLevel = 1.0
                                    isJavaScriptEnabled = true
                                    customUserAgentString = "Sphinx-Call"
                                    // Allow microphone and camera access
                                    androidWebSettings.domStorageEnabled = true
                                }

                                Column(Modifier.fillMaxSize()) {
                                    // Top bar with call info and close button
                                    TopAppBar(
                                        title = {
                                            Text(
                                                text = when {
                                                    url.isValidJitsiCallLink -> "Jitsi Call"
                                                    url.isValidLiveKitCallLink -> "LiveKit Call"
                                                    else -> "Sphinx Call"
                                                },
                                                fontWeight = FontWeight.W600
                                            )
                                        },
                                        backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
                                        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
                                        actions = {
                                            IconButton(onClick = {
                                                isOpen = false
                                                onClose()
                                            }) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Close Call",
                                                    tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                                                )
                                            }
                                        }
                                    )

                                    WebView(
                                        state = webViewState,
                                        modifier = Modifier.fillMaxSize(),
                                        navigator = webViewNavigator
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Loading WebView...",
                                fontSize = 14.sp,
                                fontFamily = Roboto,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        }
    }
}