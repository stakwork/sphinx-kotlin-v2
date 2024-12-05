package chat.sphinx.common.viewmodel.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import chat.sphinx.common.state.TribeMembersViewState
import chat.sphinx.di.container.SphinxContainer
import chat.sphinx.response.LoadResponse
import chat.sphinx.response.Response
import chat.sphinx.response.ResponseError
import chat.sphinx.utils.notifications.createSphinxNotificationManager
import chat.sphinx.wrapper.contact.Contact
import chat.sphinx.wrapper.dashboard.ChatId
import chat.sphinx.wrapper.lightning.LightningNodePubKey
import chat.sphinx.wrapper.message.MessageType
import chat.sphinx.wrapper.message.SenderAlias
import chat.sphinx.wrapper.mqtt.TribeMember
import chat.sphinx.wrapper.mqtt.TribeMembersResponse
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class TribeMembersViewModel(
    val chatId: ChatId
) {
    val scope = SphinxContainer.appModule.applicationScope
    val dispatchers = SphinxContainer.appModule.dispatchers
    private val sphinxNotificationManager = createSphinxNotificationManager()
    private val repositoryModule = SphinxContainer.repositoryModule(sphinxNotificationManager)
    private val connectManagerRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).connectManagerRepository
    private val messageRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).messageRepository
    private val chatRepository = repositoryModule.chatRepository

    var tribeMembersViewState: TribeMembersViewState by mutableStateOf(initialState())

    private fun initialState(): TribeMembersViewState = TribeMembersViewState()

    private inline fun setTribeMembersViewState(update: TribeMembersViewState.() -> TribeMembersViewState) {
        tribeMembersViewState = tribeMembersViewState.update()
    }

    init {
        scope.launch(dispatchers.mainImmediate) {
            loadTribeMembers()
            fetchTribeMembers()
        }
    }

    private suspend fun loadTribeMembers() {
        val chat = chatRepository.getChatById(chatId)
        val tribeServerPubKey = connectManagerRepository.getTribeServerPubKey()

        chat?.uuid?.value?.let { tribePubKey ->
            if (tribeServerPubKey != null) {
                connectManagerRepository.getTribeMembers(
                    tribeServerPubKey,
                    tribePubKey
                )
            } else { }
        }
    }

    private suspend fun fetchTribeMembers(){
        connectManagerRepository.tribeMembersState.collect { tribeMembersList ->
            if (tribeMembersList != null) {
                processMembers(tribeMembersList)
            }
        }
    }

    private fun processMembers(
        membersResponse: TribeMembersResponse,
    ) {
        setTribeMembersViewState {
            copy(
                tribeMembersList = membersResponse.confirmed ?: emptyList(),
                pendingTribeMembersList = membersResponse.pending ?: emptyList()
            )
        }
    }

    fun processMemberRequest(
        alias: SenderAlias,
        type: MessageType.GroupAction
    ): LoadResponse<Any, ResponseError> {
        var response: LoadResponse<Any, ResponseError>  = Response.Error(ResponseError(("")))

        scope.launch(dispatchers.mainImmediate) {
            val message = messageRepository.getTribeLastMemberRequestBySenderAlias(
                alias,
                chatId
            ).firstOrNull()

            message?.uuid?.let { messageUuid ->
                messageRepository.processMemberRequest(
                    message.chatId,
                    messageUuid,
                    null,
                    type,
                    alias
                )
            }
        }

        return response
    }
}