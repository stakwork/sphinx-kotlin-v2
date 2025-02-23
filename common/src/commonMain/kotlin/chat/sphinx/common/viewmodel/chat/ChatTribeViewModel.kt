package chat.sphinx.common.viewmodel.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import chat.sphinx.common.state.*
import chat.sphinx.common.viewmodel.DashboardViewModel
import chat.sphinx.response.LoadResponse
import chat.sphinx.response.ResponseError
import chat.sphinx.utils.ServersUrlsHelper
import chat.sphinx.wrapper.DateTime.Companion.getThreeMonthsAgo
import chat.sphinx.wrapper.PhotoUrl
import chat.sphinx.wrapper.before
import chat.sphinx.wrapper.chat.*
import chat.sphinx.wrapper.contact.Contact
import chat.sphinx.wrapper.dashboard.ChatId
import chat.sphinx.wrapper.feed.FeedType
import chat.sphinx.wrapper.feed.toFeedType
import chat.sphinx.wrapper.feed.toFeedUrl
import chat.sphinx.wrapper.message.*
import chat.sphinx.wrapper.toPhotoUrl
import chat.sphinx.wrapper.toSecondBrainUrl
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatTribeViewModel(
    chatId: ChatId,
    dashboardViewModel: DashboardViewModel
): ChatViewModel(
    chatId,
    dashboardViewModel
) {

    private val _tribeDataStateFlow: MutableStateFlow<TribeData?> by lazy {
        MutableStateFlow(null)
    }

    override val tribeDataStateFlow: StateFlow<TribeData?>
        get() = _tribeDataStateFlow.asStateFlow()

    override val chatSharedFlow: SharedFlow<Chat?> = flow {
        chatId?.let { emitAll(chatRepository.getChatByIdFlow(it)) }
    }.distinctUntilChanged().shareIn(
        scope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    override var pinMessageState: PinMessageState by mutableStateOf(initialPinMessageState())

    // TODO V2 fetch pin message
    override fun initialPinMessageState(): PinMessageState = PinMessageState(
        pinMessage = mutableStateOf(null),
        isPinning = false,
    )

    private val isProductionEnvironment = ServersUrlsHelper().getEnvironmentType()

    init {
        scope.launch(dispatchers.mainImmediate) {
            chatRepository.getChatById(chatId)?.let { chat ->

                chatRepository.updateTribeInfo(chat, isProductionEnvironment)?.let { tribeData ->

                    _tribeDataStateFlow.value = TribeData(
                        chat.host ?: return@launch,
                        chat.uuid,
                        tribeData.app_url?.toAppUrl(),
                        tribeData.feed_url?.toFeedUrl(),
                        tribeData.feed_type?.toFeedType() ?: FeedType.Podcast,
                        tribeData.second_brain_url?.toSecondBrainUrl(),
                    )

                    updatePinnedMessageState(chat, tribeData.pin?.toMessageUUID())
                }
            }
        }
    }

    private suspend fun updatePinnedMessageState(
        chat: Chat,
        messageUUID: MessageUUID?
    ) {
//        if (isThreadChat()) {
//            return
//        }

        messageUUID?.let { uuid ->
            if (uuid.value.isNotEmpty()) {
                messageRepository.getMessageByUUID(uuid).firstOrNull()?.let { message ->
                   val chatMessage = processSingleMessage(chat, message)

                    setPinMessageState {
                        copy(
                            pinMessage = mutableStateOf(chatMessage),
                        )
                    }
                }
            }
        }
    }

    override suspend fun getChatInfo(): Triple<ChatName?, PhotoUrl?, String>? = null

    override suspend fun getContact(): Contact? {
        return null
    }

    override val checkChatStatus: Flow<LoadResponse<Boolean, ResponseError>> = flow {
        // TODO V2 checkRoute

//        networkQueryLightning.checkRoute(chatId).collect { response ->
//            when (response) {
//                is LoadResponse.Loading -> {
//                    emit(response)
//                }
//                is Response.Error -> {
//                    emit(response)
//                }
//                is Response.Success -> {
//                    emit(Response.Success(response.value.isRouteAvailable))
//                }
//            }
//        }
    }

    override suspend fun processMemberRequest(
        chatId: ChatId,
        messageUuid: MessageUUID,
        type: MessageType.GroupAction,
        senderAlias: SenderAlias?,
    ) {
        val errorMessage = if (type.isMemberApprove()) {
            "Failed to approve member"
        } else {
            "Failed to reject member"
        }

        if (type.isMemberApprove() || type.isMemberReject()) {
            messageRepository.processMemberRequest(chatId, messageUuid, null, type, senderAlias)
        }
    }

    override fun deleteTribe() {
        scope.launch(dispatchers.mainImmediate){
            val chat = getChat()
            if (chat != null) {
                chatRepository.exitAndDeleteTribe(chat)
            }

            ChatDetailState.screenState(ChatDetailData.EmptyChatDetailData)
        }
    }

    override fun pinMessage(message: Message) {
        scope.launch(dispatchers.mainImmediate) {
            if (chatId != null) {
                chatRepository.togglePinMessage(
                    chatId,
                    message,
                    false,
                    "Failed to pin message",
                    isProductionEnvironment
                )
            }
        }
    }

    override fun unPinMessage(message: Message?) {
        scope.launch(dispatchers.mainImmediate) {
            if (chatId != null && message != null) {
                chatRepository.togglePinMessage(
                    chatId,
                    message,
                    true,
                    "Failed to unpin message",
                    isProductionEnvironment
                )
            }
        }
    }

    override var editMessageState: EditMessageState by mutableStateOf(initialState())

    override var threadMessageState: EditMessageState by mutableStateOf(threadInitialState())

    override fun initialState(): EditMessageState = EditMessageState(
        chatId = chatId
    )

    override fun threadInitialState(): EditMessageState = EditMessageState(
        chatId = chatId,
    )

    override fun aliasMatcher(text: String) {
        if (!text.contains("@") || text.isEmpty()) {
            resetAliasMatcher()
            return
        }

        val cursorPosition = editMessageState.messageText.value.selection.start
        val textUntilCursor = text.substring(0, cursorPosition)

        if (textUntilCursor.lastOrNull()?.toString() == " ") {
            resetAliasMatcher()
            return
        }

        textUntilCursor.split(" ").lastOrNull()?.let { lastWord ->
            if (!lastWord.contains("@") || lastWord == "@") {
                resetAliasMatcher()
                return
            }

            setAliasMatcherState {
                copy(
                    isOn = true
                )
            }

            val atPosition = text.lastIndexOf(lastWord)

            text.substring(atPosition + 1).substringBefore(" ").let {
                setAliasMatcherState {
                    copy(
                        inputText = it
                    )
                }
                generateSuggestedAliasList()
            }
        }
    }

    private fun resetAliasMatcher() {
        aliasMatcherState = AliasMatcherState(
            false,
            "",
            listOf(Triple("",null, null)),
            0
        )
    }

    private fun generateSuggestedAliasList() {
        val messageListData = MessageListState.screenState()
        if (messageListData is MessageListData.PopulatedMessageListData) {
            val inputText = aliasMatcherState.inputText.trim()
            if (inputText.isEmpty()) return

            val threeMonthsAgo = getThreeMonthsAgo()

            val seenPictures = mutableSetOf<String?>()
            val seenAliases = mutableSetOf<String>()
            val aliasAndPictures = mutableListOf<Triple<String, PhotoUrl?, Int?>>()

            messageListData.messages
                .asReversed()
                .forEach { chatMessage ->
                    val alias = chatMessage.message.senderAlias?.value ?: return@forEach
                    val picture = chatMessage.message.senderPic?.value ?: ""

                    if (chatMessage.message.date.before(threeMonthsAgo)) return@forEach

                    if (alias.startsWith(inputText, ignoreCase = true)) {
                        if (!seenPictures.contains(picture) && !seenAliases.contains(alias)) {
                            seenPictures.add(picture)
                            seenAliases.add(alias)
                            val color = chatMessage.colors[chatMessage.message.id.value]

                            aliasAndPictures.add(
                                Triple(
                                    alias,
                                    picture.toPhotoUrl(),
                                    color
                                )
                            )
                        }
                    }
                }

            val suggestedList = aliasAndPictures.reversed()

            setAliasMatcherState {
                copy(suggestedAliasAndPicList = suggestedList)
            }
        }
    }

    override fun onAliasNextFocus() {
        if (!aliasMatcherState.isOn) {
            return
        }

        var selectedItem = aliasMatcherState.selectedItem

        if (aliasMatcherState.selectedItem < aliasMatcherState.suggestedAliasAndPicList.lastIndex) {
            selectedItem++
        } else {
            selectedItem = 0
        }

        setAliasMatcherState {
            copy(
                selectedItem = selectedItem
            )
        }
    }

    override fun onAliasPreviousFocus() {
        if (!aliasMatcherState.isOn) {
            return
        }

        var selectedItem = aliasMatcherState.selectedItem

        if (aliasMatcherState.selectedItem > 0) {
            selectedItem--
        } else {
            selectedItem = aliasMatcherState.suggestedAliasAndPicList.lastIndex
        }

        setAliasMatcherState {
            copy(
                selectedItem = selectedItem
            )
        }
    }

    override fun onAliasSelected() {
        if (!aliasMatcherState.isOn) {
            return
        }
        val oldString = "@" + aliasMatcherState.inputText
        val newString = "@" + aliasMatcherState.suggestedAliasAndPicList[aliasMatcherState.selectedItem].first + " "
        val replacedString = editMessageState.messageText.value.text.replace(oldString, newString)
        val cursorPosition = replacedString.lastIndexOf(newString) + newString.length
        editMessageState.messageText.value = TextFieldValue(replacedString, TextRange(cursorPosition))
        resetAliasMatcher()
    }

    override fun getUniqueKey(): String {
        return "TRIBE-$chatId"
    }

}