package chat.sphinx.common.viewmodel.dashboard

import androidx.annotation.ColorInt
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import chat.sphinx.common.models.DashboardChat
import chat.sphinx.common.state.ChatDetailData
import chat.sphinx.common.state.ChatDetailState
import chat.sphinx.common.state.ChatListData
import chat.sphinx.common.state.ChatListState
import chat.sphinx.di.container.SphinxContainer
import chat.sphinx.utils.UserColorsHelper
import chat.sphinx.utils.notifications.createSphinxNotificationManager
import chat.sphinx.wrapper.chat.Chat
import chat.sphinx.wrapper.chat.ChatStatus
import chat.sphinx.wrapper.chat.getColorKey
import chat.sphinx.wrapper.chat.isConversation
import chat.sphinx.wrapper.contact.*
import chat.sphinx.wrapper.dashboard.ChatId
import chat.sphinx.wrapper.dashboard.ContactId
import chat.sphinx.wrapper.invite.Invite
import chat.sphinx.wrapper.isTrue
import chat.sphinx.wrapper.lightning.NodeBalance
import chat.sphinx.wrapper.message.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import theme.badge_red
import theme.primary_green
import utils.getRandomColorRes

@Suppress("NOTHING_TO_INLINE")
private inline fun List<DashboardChat>.filterDashboardChats(
    filter: CharSequence
): List<DashboardChat> =
    filter {
        it.chatName?.contains(filter, ignoreCase = true) == true
    }

class ChatListViewModel {
    val scope = SphinxContainer.appModule.applicationScope
    val dispatchers = SphinxContainer.appModule.dispatchers

    private var dashboardChats: ArrayList<DashboardChat> = ArrayList()
    private val sphinxNotificationManager = createSphinxNotificationManager()
    private val repositoryDashboard = SphinxContainer.repositoryModule(sphinxNotificationManager).repositoryDashboard

    private val colorsHelper = UserColorsHelper(SphinxContainer.appModule.dispatchers)

    private val _contactsStateFlow: MutableStateFlow<List<Contact>> by lazy {
        MutableStateFlow(emptyList())
    }

    private val _accountOwnerStateFlow: MutableStateFlow<Contact?> by lazy {
        MutableStateFlow(null)
    }

    private val accountOwnerStateFlow: StateFlow<Contact?>
        get() = _accountOwnerStateFlow.asStateFlow()

    private suspend fun getAccountBalance(): StateFlow<NodeBalance?> =
        repositoryDashboard.getAccountBalanceStateFlow()

    var searchText: MutableState<TextFieldValue?> = mutableStateOf(null)

    private var contactsCollectionInitialized: Boolean = false
    private var chatsCollectionInitialized: Boolean = false

    private val collectionLock = Mutex()
    private val updateDashboardChatLock = Mutex()

    suspend fun getUnseenReceivedMessages(): Flow<List<Message>?> {
        return repositoryDashboard.getUnseenReceivedMessages()
    }

    suspend fun getUnseenReceivedMentions(): Flow<List<Message>?> {
        return repositoryDashboard.getUnseenReceivedMentions()
    }

    init {
        scope.launch(dispatchers.mainImmediate) {
            repositoryDashboard.getAllNotBlockedContacts.distinctUntilChanged().collect { contacts ->
                val unseenMessages = getUnseenReceivedMessages().firstOrNull()
                val unseenMessagesByChatId: Map<ChatId, List<Message>> = unseenMessages?.groupBy { it.chatId } ?: mapOf()

                updateChatListContacts(contacts, unseenMessagesByChatId)
            }
        }

        scope.launch(dispatchers.mainImmediate) {
            delay(25L)

            repositoryDashboard.getAllChatsFlow.distinctUntilChanged().collect { chats ->
                val unseenMessages = getUnseenReceivedMessages().firstOrNull()
                val unseenMessagesByChatId: Map<ChatId, List<Message>> = unseenMessages?.groupBy { it.chatId } ?: mapOf()

                val unseenMentions = getUnseenReceivedMentions().firstOrNull()
                val unseenMentionsByChatId: Map<ChatId, List<Message>> = unseenMentions?.groupBy { it.chatId } ?: mapOf()

                collectionLock.withLock {
                    chatsCollectionInitialized = true

                    val newList = ArrayList<DashboardChat>(chats.size)
                    val contactsAdded = mutableListOf<ContactId>()

                    for (chat in chats) {
                        val message: Message? = chat.latestMessageId?.let {
                            repositoryDashboard.getMessageById(it).firstOrNull()
                        }

                        // Calculate unseen message counts
                        val rawUnseenMessages = if (!chat.seen.isTrue()) {
                            unseenMessagesByChatId[chat.id]?.size ?: 0
                        } else 0
                        val rawUnseenMentions = if (!chat.seen.isTrue()) {
                            unseenMentionsByChatId[chat.id]?.size ?: 0
                        } else 0

                        val isOpen = isChatCurrentlyOpen(chat.id)
                        val chatUnseenMessagesCount = if (isOpen) 0 else rawUnseenMessages
                        val chatUnseenMentionsCount = if (isOpen) 0 else rawUnseenMentions

                        if (chat.type.isConversation()) {
                            val contactId: ContactId = chat.contactIds.lastOrNull() ?: continue

                            val contact: Contact = repositoryDashboard.getContactById(contactId)
                                .firstOrNull() ?: continue

                            (ChatDetailState.screenState() as? ChatDetailData.SelectedChatDetailData.SelectedContactDetail)?.let {
                                if (contactId == it.contactId) {
                                    reloadChatDetailsOnFirstMessageSent(chat, contact, it. dashboardChat, chatUnseenMessagesCount)
                                }
                            }

                            if (contact.status is ContactStatus.Pending) {
                                if (contact.isInviteContact()) {
                                    var contactInvite: Invite? = null

                                    contact.inviteId?.let { inviteId ->
                                        contactInvite = withContext(dispatchers.io) {
                                            repositoryDashboard.getInviteById(inviteId).firstOrNull()
                                        }
                                    }

                                    if (contactInvite != null) {
                                        newList.add(
                                            DashboardChat.Inactive.Invite(contact, contactInvite!!, null)
                                        )
                                    }
                                } else {
                                    newList.add(
                                        DashboardChat.Inactive.Conversation(contact, null)
                                    )
                                }
                            }

                            if (!contact.isBlocked() && chat.status is ChatStatus.Approved) {
                                contactsAdded.add(contactId)

                                newList.add(
                                    DashboardChat.Active.Conversation(
                                        chat,
                                        message,
                                        contact,
                                        getColorFor(contact, chat),
                                        chatUnseenMessagesCount,
                                    )
                                )
                            }
                        } else {
                            newList.add(
                                DashboardChat.Active.GroupOrTribe(
                                    chat,
                                    message,
                                    accountOwnerStateFlow.value,
                                    getColorFor(null, chat),
                                    chatUnseenMessagesCount,
                                    chatUnseenMentionsCount
                                )
                            )
                        }
                    }

                    dashboardChats = ArrayList(newList.sortedByDescending { it.sortBy })
                    filterChats(searchText.value)
                }
            }
        }
    }

    private suspend fun reloadChatDetailsOnFirstMessageSent(
        chat: Chat,
        contact: Contact,
        dashboardChat: DashboardChat,
        chatUnseenMessagesCount: Int
    ) {
        val message: Message? = chat.latestMessageId?.let {
            repositoryDashboard.getMessageById(it).firstOrNull()
        }

        val dashboard = DashboardChat.Active.Conversation(
            chat,
            message,
            contact,
            dashboardChat.color,
            chatUnseenMessagesCount
        )

        ChatDetailState.screenState(
            ChatDetailData.SelectedChatDetailData.SelectedContactChatDetail(
                chat.id,
                contact.id,
                dashboard
            )
        )

        (ChatListState.screenState() as? ChatListData.PopulatedChatListData)?.let { currentState ->
            ChatListState.screenState(
                ChatListData.PopulatedChatListData(
                    currentState.dashboardChats,
                    dashboard.dashboardChatId
                )
            )
        }
    }

    private fun resetChatDetailOnContactDeleted(contactId: ContactId) {
        (ChatDetailState.screenState() as? ChatDetailData.SelectedChatDetailData)?.let { chatDetailState ->
            if (contactId == chatDetailState.contactId) {
                ChatDetailState.screenState(ChatDetailData.EmptyChatDetailData)
            }
        }
    }

    fun filterChats(filter: TextFieldValue?) {
        searchText.value = filter

        val currentChatListState = ChatListState.screenState()

        val filterText = filter?.text ?: ""

        if (filterText.isEmpty()) {
            ChatListState.screenState(
                ChatListData.PopulatedChatListData(
                    dashboardChats,
                    (currentChatListState as? ChatListData.PopulatedChatListData)?.selectedDashboardId
                )
            )
        } else {
            val filteredChats = dashboardChats.filterDashboardChats(
                filterText
            )

            ChatListState.screenState(
                ChatListData.PopulatedChatListData(
                    filteredChats,
                    (currentChatListState as? ChatListData.PopulatedChatListData)?.selectedDashboardId
                )
            )
        }
    }

    private fun isChatCurrentlyOpen(chatId: ChatId): Boolean {
        return when (val s = ChatDetailState.screenState()) {
            is ChatDetailData.SelectedChatDetailData.SelectedContactChatDetail -> s.chatId == chatId
            is ChatDetailData.SelectedChatDetailData.SelectedTribeChatDetail   -> s.chatId == chatId
            else -> false
        }
    }

    fun chatRowSelected(dashboardChat: DashboardChat) {
        val cleared = when (dashboardChat) {
            is DashboardChat.Active.Conversation -> DashboardChat.Active.Conversation(
                chat = dashboardChat.chat,
                message = dashboardChat.message,
                contact = dashboardChat.contact,
                color = dashboardChat.color,
                unseenMessagesCount = 0
            )
            is DashboardChat.Active.GroupOrTribe -> DashboardChat.Active.GroupOrTribe(
                chat = dashboardChat.chat,
                message = dashboardChat.message,
                owner = accountOwnerStateFlow.value,
                color = dashboardChat.color,
                unseenMessagesCount = 0,
                unseenMentionsCount = 0
            )
            else -> dashboardChat
        }

        (ChatListState.screenState() as? ChatListData.PopulatedChatListData)?.let { currentState ->
            val updated = currentState.dashboardChats.map {
                if (it.dashboardChatId == dashboardChat.dashboardChatId) cleared else it
            }
            ChatListState.screenState(
                ChatListData.PopulatedChatListData(
                    updated,
                    cleared.dashboardChatId
                )
            )
        }

        // Keep the internal cache (used by filter/search) in sync as well
        dashboardChats = ArrayList(
            dashboardChats.map {
                if (it.dashboardChatId == dashboardChat.dashboardChatId) cleared else it
            }
        )

        // Navigate using the cleared instance so detail header also shows no badge
        ChatDetailState.screenState(
            when (cleared) {
                is DashboardChat.Active.Conversation -> {
                    ChatDetailData.SelectedChatDetailData.SelectedContactChatDetail(
                        cleared.chat.id,
                        cleared.contact.id,
                        cleared
                    )
                }
                is DashboardChat.Active.GroupOrTribe -> {
                    ChatDetailData.SelectedChatDetailData.SelectedTribeChatDetail(
                        cleared.chat.id,
                        cleared
                    )
                }
                is DashboardChat.Inactive.Conversation -> {
                    ChatDetailData.SelectedChatDetailData.SelectedContactDetail(
                        cleared.contact.id,
                        cleared
                    )
                }
                else -> ChatDetailData.EmptyChatDetailData
            }
        )
    }


    private suspend fun updateChatListContacts(
        contacts: List<Contact>,
        unseenMessagesByChatId: Map<ChatId, List<Message>>
    ) {
        collectionLock.withLock {
            contactsCollectionInitialized = true
            if (contacts.isEmpty()) return@withLock

            val contactIds = ArrayList<ContactId>(contacts.size)

            withContext(dispatchers.default) {
                for (contact in contacts) {
                    if (contact.isOwner.isTrue()) {
                        _accountOwnerStateFlow.value = contact
                    } else {
                        contactIds.add(contact.id)
                    }
                }
            }

            if (!chatsCollectionInitialized) return@withLock

            withContext(dispatchers.default) {
                val currentState = ChatListState.screenState()
                val currentChats: MutableList<DashboardChat> = when (currentState) {
                    is ChatListData.PopulatedChatListData -> currentState.dashboardChats.toMutableList()
                    else -> mutableListOf()
                }

                val existingContactIdsInList = mutableListOf<ContactId>()
                var updateChatViewState = false

                for (chat in currentChats.toList()) {
                    val contact: Contact? = when (chat) {
                        is DashboardChat.Active.Conversation   -> chat.contact
                        is DashboardChat.Inactive.Conversation -> chat.contact
                        is DashboardChat.Inactive.Invite       -> chat.contact
                        else                                   -> null
                    }

                    contact?.let { c ->
                        existingContactIdsInList.add(c.id)

                        if (!contactIds.contains(c.id)) {
                            updateChatViewState = true
                            currentChats.remove(chat)
                            existingContactIdsInList.remove(c.id)
                            if (ChatDetailState.screenState() is ChatDetailData.SelectedChatDetailData
                                && (ChatDetailState.screenState() as ChatDetailData.SelectedChatDetailData).contactId == c.id
                            ) {
                                ChatDetailState.screenState(ChatDetailData.EmptyChatDetailData)
                            }
                        }

                        if (repositoryDashboard.updatedContactIds.contains(c.id)) {
                            updateChatViewState = true
                            currentChats.remove(chat)
                            existingContactIdsInList.remove(c.id)
                        }
                    }
                }

                for (contact in contacts) {
                    if (contact.isOwner.isTrue()) continue
                    if (existingContactIdsInList.contains(contact.id)) continue

                    updateChatViewState = true

                    if (contact.isInviteContact()) {
                        val invite = contact.inviteId?.let { inviteId ->
                            withContext(dispatchers.io) { repositoryDashboard.getInviteById(inviteId).firstOrNull() }
                        }

                        if (invite != null) {
                            currentChats.add(
                                DashboardChat.Inactive.Invite(
                                    contact,
                                    invite,
                                    getColorFor(contact, null)
                                )
                            )
                            continue
                        }
                    }

                    var row: DashboardChat = DashboardChat.Inactive.Conversation(
                        contact = contact,
                        color = getColorFor(contact, null)
                    )

                    repositoryDashboard.getConversationByContactIdFlow(contact.id).firstOrNull()?.let { contactChat ->
                        val lastMessage: Message? = contactChat.latestMessageId?.let {
                            repositoryDashboard.getMessageById(it).firstOrNull()
                        }

                        val baseUnseen = if (!contactChat.seen.isTrue()) {
                            unseenMessagesByChatId[contactChat.id]?.size ?: 0
                        } else 0

                        val unseenForRow = if (isChatCurrentlyOpen(contactChat.id)) 0 else baseUnseen

                        row = DashboardChat.Active.Conversation(
                            chat = contactChat,
                            message = lastMessage,
                            contact = contact,
                            color = getColorFor(contact, contactChat),
                            unseenMessagesCount = unseenForRow
                        )
                    }

                    currentChats.add(row)
                }

                if (updateChatViewState) {
                    // Persist in-memory cache (used by search/filter)
                    dashboardChats = ArrayList(currentChats.sortedByDescending { it.sortBy })

                    // Re-emit UI state, keeping current selection if any
                    val selectedId = (currentState as? ChatListData.PopulatedChatListData)?.selectedDashboardId
                    ChatListState.screenState(
                        ChatListData.PopulatedChatListData(
                            dashboardChats,
                            selectedId
                        )
                    )

                    filterChats(searchText.value)

                    repositoryDashboard.updatedContactIds = mutableListOf()
                }
            }
        }
    }

    fun payForInvite(invite: Invite) {
        scope.launch(dispatchers.mainImmediate) {
            getAccountBalance().firstOrNull()?.let { balance ->
                if (balance.balance.value < (invite.price?.value ?: 0)) {
                    toast("Can't pay invite. Balance is too low", badge_red)
                    return@launch
                }
            }

            confirm(
                "Pay Invite",
                "Are you sure you want to pay for this invite?"
            ) {
                // TODO V2 implement or remove payForInvite
            }
        }
    }

    fun deleteInvite(invite: Invite) {
        confirm(
            "Delete Invite",
            "Are you sure you want to delete this invite?"
        ) {
            scope.launch(dispatchers.mainImmediate) {
                repositoryDashboard.deleteInvite(invite)
            }
        }
    }

    private fun toast(
        message: String,
        color: Color = primary_green,
        delay: Long = 2000L
    ) {
        scope.launch(dispatchers.mainImmediate) {
            sphinxNotificationManager.toast(
                "Sphinx",
                message,
                color.value,
                delay
            )
        }
    }

    private fun confirm(
        title: String,
        message: String,
        callback: () -> Unit
    ) {
        scope.launch(dispatchers.mainImmediate) {
            sphinxNotificationManager.confirmAlert(
                "Sphinx",
                title,
                message,
                callback
            )
        }
    }

    @ColorInt
    suspend fun getColorFor(
        contact: Contact?,
        chat: Chat?
    ): Int? {
        (contact?.getColorKey() ?: chat?.getColorKey())?.let { colorKey ->
            return colorsHelper.getColorIntForKey(
                colorKey,
                Integer.toHexString(getRandomColorRes().hashCode())
            )
        }
        return null
    }
}

