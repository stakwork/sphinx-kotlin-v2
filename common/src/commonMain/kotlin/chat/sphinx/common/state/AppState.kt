package chat.sphinx.common.state

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import chat.sphinx.common.models.ChatMessage
import chat.sphinx.common.viewmodel.chat.ChatViewModel
import okio.Path

enum class ScreenType {
    SplashScreen,
    DashboardScreen,
    LandingScreen
}

object AppState {
    private var screen: MutableState<ScreenType> = mutableStateOf(ScreenType.SplashScreen)

    fun screenState() : ScreenType {
        return screen.value
    }

    fun screenState(state: ScreenType) {
        screen.value = state
    }
}

data class FullScreenVideoData(
    val path: Path,
    val chatMessage: ChatMessage,
    val chatViewModel: ChatViewModel
)

val fullScreenImageState = mutableStateOf<Path?>(null)
val fullScreenVideoState = mutableStateOf<FullScreenVideoData?>(null)

