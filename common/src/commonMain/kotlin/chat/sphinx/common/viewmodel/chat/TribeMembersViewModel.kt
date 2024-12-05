package chat.sphinx.common.viewmodel.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import chat.sphinx.common.state.TribeMembersViewState
import chat.sphinx.di.container.SphinxContainer
import chat.sphinx.utils.notifications.createSphinxNotificationManager
import chat.sphinx.wrapper.dashboard.ChatId
import chat.sphinx.wrapper.mqtt.TribeMember

class TribeMembersViewModel(
    val chatId: ChatId
) {
    private val sphinxNotificationManager = createSphinxNotificationManager()
    private val connectManagerRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).connectManagerRepository

    var tribeMembersViewState: TribeMembersViewState by mutableStateOf(initialState())

//    private fun initialState(): TribeMembersViewState = TribeMembersViewState()

    private inline fun setTribeMembersViewState(update: TribeMembersViewState.() -> TribeMembersViewState) {
        tribeMembersViewState = tribeMembersViewState.update()
    }

    private fun initialState(): TribeMembersViewState {
        val dummyMembers = List(20) { index ->
            TribeMember(
                alias = "Member $index",
                photo_url = null
            )
        }
        return TribeMembersViewState(tribeMembersList = dummyMembers, loadingTribeMembers = false, loadingMore = true)
    }


    private suspend fun fetchTribeMembers(){
        connectManagerRepository.tribeMembersState.collect { tribeMembersList ->
            if (tribeMembersList != null) {
            }
        }
    }
}