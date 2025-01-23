package chat.sphinx.common.viewmodel.chat


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import chat.sphinx.common.state.EditMessageState
import chat.sphinx.common.state.PinMessageState
import chat.sphinx.common.viewmodel.DashboardViewModel
import chat.sphinx.concepts.network.query.lightning.model.route.RouteSuccessProbabilityDto
import chat.sphinx.concepts.network.query.lightning.model.route.isRouteAvailable
import chat.sphinx.response.LoadResponse
import chat.sphinx.response.Response
import chat.sphinx.response.ResponseError
import chat.sphinx.wrapper.PhotoUrl
import chat.sphinx.wrapper.chat.Chat
import chat.sphinx.wrapper.chat.ChatName
import chat.sphinx.wrapper.chat.TribeData
import chat.sphinx.wrapper.chat.isApproved
import chat.sphinx.wrapper.contact.Contact
import chat.sphinx.wrapper.contact.ContactAlias
import chat.sphinx.wrapper.contact.getColorKey
import chat.sphinx.wrapper.dashboard.ChatId
import chat.sphinx.wrapper.dashboard.ContactId
import chat.sphinx.wrapper_message.ThreadUUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

class ChatContactViewModel(
    chatId: ChatId?,
    val contactId: ContactId,
    dashboardViewModel: DashboardViewModel
): ChatViewModel(
    chatId,
    dashboardViewModel
) {
    override val chatSharedFlow: SharedFlow<Chat?> = flow {
        chatId?.let { chatId ->
            emitAll(chatRepository.getChatByIdFlow(chatId))
        } ?: repositoryDashboard.getConversationByContactIdFlow(contactId).collect { chat ->
            emit(chat)
        }
    }.distinctUntilChanged().shareIn(
        scope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1
    )

    private val contactSharedFlow: SharedFlow<Contact?> = flow {
        emitAll(contactRepository.getContactById(contactId))
    }.distinctUntilChanged().shareIn(
        scope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    private val _tribeDataStateFlow: MutableStateFlow<TribeData?> by lazy {
        MutableStateFlow(null)
    }

    override val tribeDataStateFlow: StateFlow<TribeData?>
        get() = _tribeDataStateFlow.asStateFlow()

    override suspend fun getChatInfo(): Triple<ChatName?, PhotoUrl?, String>? {
        contactSharedFlow.replayCache.firstOrNull()?.let { contact ->
            return Triple(
                contact.alias?.value?.let { ChatName(it) },
                contact.photoUrl?.value?.let { PhotoUrl(it) },
                contact.getColorKey()
            )
        } ?: contactSharedFlow.firstOrNull()?.let { contact ->
            return Triple(
                contact.alias?.value?.let { ChatName(it) },
                contact.photoUrl?.value?.let { PhotoUrl(it) },
                contact.getColorKey()
            )
        } ?: let {
            var alias: ContactAlias? = null
            var photoUrl: PhotoUrl? = null
            var colorKey: String = getRandomHexCode()

            try {
                contactSharedFlow.collect { contact ->
                    if (contact != null) {
                        alias = contact.alias
                        photoUrl = contact.photoUrl
                        colorKey = contact.getColorKey()
                        throw Exception()
                    }
                }
            } catch (e: Exception) {}
            delay(25L)

            return Triple(
                alias?.value?.let { ChatName(it) },
                photoUrl?.value?.let { PhotoUrl(it) },
                colorKey
            )
        }
    }

    override suspend fun getContact(): Contact? {
        return contactId?.let { contactRepository.getContactById(it).firstOrNull() }
    }

    override val checkChatStatus: Flow<LoadResponse<Boolean, ResponseError>> = flow {
        emit(LoadResponse.Loading)

        try {
            val chat = getChat()
            if (chat != null) {
                val route = chat.status.isApproved()
                emit(Response.Success(route))
            } else {
                emit(Response.Error(ResponseError("Chat not found")))
            }
        } catch (e: Exception) {
            emit(Response.Error(ResponseError("Failed to get route", e)))
        }

    }

    override var editMessageState: EditMessageState by mutableStateOf(initialState())

    override var pinMessageState: PinMessageState by mutableStateOf(initialPinMessageState())

    override fun initialPinMessageState(): PinMessageState = PinMessageState(
        pinMessage = mutableStateOf(null),
        isPinning = false
    )

    override fun initialState(): EditMessageState = EditMessageState(
        chatId = chatId,
        contactId = contactId,
    )

    override fun getUniqueKey(): String {
        return "CONTACT-$contactId"
    }

    override fun getThreadUUID(): ThreadUUID? {
        return null
    }

    override fun isThreadChat(): Boolean {
        return false
    }

}