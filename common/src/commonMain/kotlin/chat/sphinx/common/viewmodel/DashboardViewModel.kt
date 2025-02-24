package chat.sphinx.common.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import chat.sphinx.common.components.toast
import chat.sphinx.common.state.*
import chat.sphinx.concepts.repository.connect_manager.model.NetworkStatus
import chat.sphinx.concepts.repository.message.model.SendPaymentRequest
import chat.sphinx.database.core.SphinxDatabaseQueries
import chat.sphinx.di.container.SphinxContainer
import chat.sphinx.features.repository.util.deleteAll
import chat.sphinx.utils.notifications.createSphinxNotificationManager
import chat.sphinx.wrapper.contact.Contact
import chat.sphinx.wrapper.dashboard.ChatId
import chat.sphinx.wrapper.dashboard.RestoreProgress
import chat.sphinx.wrapper.dashboard.toContactId
import chat.sphinx.wrapper.eeemmddhmma
import chat.sphinx.wrapper.lightning.*
import chat.sphinx.wrapper.message.Message
import chat.sphinx.wrapper.message.MessageType
import chat.sphinx.wrapper.message.SenderAlias
import chat.sphinx.wrapper.mqtt.InvoiceBolt11.Companion.toInvoiceBolt11
import chat.sphinx.wrapper.toDateTime
import chat.sphinx.wrapper.tribe.TribeJoinLink
import chat.sphinx.wrapper_message.ThreadUUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import theme.badge_red
import theme.primary_red
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener

class DashboardViewModel(): WindowFocusListener {
    val scope = SphinxContainer.appModule.applicationScope
    val dispatchers = SphinxContainer.appModule.dispatchers
    private val coreDB = SphinxContainer.appModule.coreDBImpl
    private val authenticationStorage = SphinxContainer.authenticationModule.authenticationStorage
    private val viewModelScope = SphinxContainer.appModule.applicationScope
    private val sphinxNotificationManager = createSphinxNotificationManager()
    private val repositoryDashboard = SphinxContainer.repositoryModule(sphinxNotificationManager).repositoryDashboard
    private val contactRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).contactRepository
    private val lightningRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).lightningRepository
    private val messageRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).messageRepository
    private val connectManagerRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).connectManagerRepository

    enum class WebViewState {
        NonInitialized,
        Loading,
        Initialized,
        Error,
        RestartRequired
    }

    sealed class SplitContentType {
        object Default : SplitContentType()
        data class Threads(val chatId: ChatId) : SplitContentType()
        data class Thread(val chatId: ChatId, val threadUUID: ThreadUUID, val fromThreadsScreen: Boolean) : SplitContentType()
    }

    sealed class FullScreenView {
        object None : FullScreenView()
        object Profile : FullScreenView()
    }

    data class SplitScreenState(
        val isOpen: Boolean,
        val type: SplitContentType? = null
    )

    private val _splitScreenStateFlow: MutableStateFlow<SplitScreenState> by lazy {
        MutableStateFlow(SplitScreenState(isOpen = false, type = null))
    }

    val splitScreenStateFlow: StateFlow<SplitScreenState>
        get() = _splitScreenStateFlow.asStateFlow()

    fun toggleSplitScreen(isOpen: Boolean, type: SplitContentType? = null) {
        _splitScreenStateFlow.value = SplitScreenState(isOpen, type)
    }

    private val _fullScreenViewStateFlow: MutableStateFlow<FullScreenView> by lazy {
        MutableStateFlow(FullScreenView.None)
    }

    val fullScreenViewStateFlow: StateFlow<FullScreenView>
        get() = _fullScreenViewStateFlow.asStateFlow()

    fun showFullScreenView(view: FullScreenView) {
        _fullScreenViewStateFlow.value = view
    }

    fun closeFullScreenView() {
        _fullScreenViewStateFlow.value = FullScreenView.None
    }

    private val webViewState: MutableStateFlow<WebViewState> by lazy {
        MutableStateFlow(WebViewState.NonInitialized)
    }

    private val _balanceStateFlow: MutableStateFlow<NodeBalance?> by lazy {
        MutableStateFlow(null)
    }

    val balanceStateFlow: StateFlow<NodeBalance?>
        get() = _balanceStateFlow.asStateFlow()

    private val _selectedTabStateFlow: MutableStateFlow<Int> by lazy {
        MutableStateFlow(0)
    }

    val selectedTabStateFlow: StateFlow<Int>
        get() = _selectedTabStateFlow.asStateFlow()

    fun setSelectedTab(tab: Int) {
        _selectedTabStateFlow.value = tab
    }

    val accountOwnerStateFlow: StateFlow<Contact?>
        get() = contactRepository.accountOwner

    private val _payInvoiceInfoStateFlow: MutableStateFlow<PayInvoiceInfo> by lazy {
        MutableStateFlow(PayInvoiceInfo(null))
    }
    val payInvoiceInfoStateFlow: StateFlow<PayInvoiceInfo>
        get() = _payInvoiceInfoStateFlow.asStateFlow()

    fun setInvoiceString(invoice: String) {
        _payInvoiceInfoStateFlow.value = _payInvoiceInfoStateFlow.value.copy(invoiceString = invoice)
    }

    private val _packageVersionAndUpgrade: MutableStateFlow<Pair<String?, Boolean>> by lazy {
        MutableStateFlow(Pair(null, false))
    }

    val packageVersionAndUpgrade: StateFlow<Pair<String?, Boolean>>
        get() = _packageVersionAndUpgrade.asStateFlow()


    private val _contactWindowStateFlow: MutableStateFlow<Pair<Boolean, ContactScreenState?>> by lazy {
        MutableStateFlow(Pair(false, null))
    }

    val contactWindowStateFlow: StateFlow<Pair<Boolean, ContactScreenState?>>
        get() = _contactWindowStateFlow.asStateFlow()


    fun toggleContactWindow(open: Boolean, screen: ContactScreenState?) {
        _contactWindowStateFlow.value = Pair(open, screen)
    }

    private val _payInvoiceWindowStateFlow: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow(false)
    }

    private val _createInvoiceWindowStateFlow: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow(false)
    }

    val payInvoiceWindowStateFlow: StateFlow<Boolean>
        get() = _payInvoiceWindowStateFlow.asStateFlow()

    val createInvoiceWindowStateFlow: StateFlow<Boolean>
        get() = _createInvoiceWindowStateFlow.asStateFlow()

    val profileSetInfoRestoreStateFlow: StateFlow<Boolean?>
        get() = connectManagerRepository.profileSetInfoRestore.asStateFlow()

    fun togglePayInvoiceWindow(open: Boolean) {
        _payInvoiceWindowStateFlow.value = open
    }

    fun toggleCreateInvoiceWindow(open: Boolean) {
        _createInvoiceWindowStateFlow.value = open
    }

    fun setWebViewState(state: WebViewState) {
        webViewState.value = state
    }

    fun isWebViewLoading() : Boolean {
        return webViewState.value == WebViewState.Loading
    }

    fun isWebViewLoaded() : Boolean {
        return webViewState.value == WebViewState.Initialized
    }

    fun getWebViewState() : WebViewState {
        return webViewState.value
    }

    private val _aboutSphinxStateFlow: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow(false)
    }

    val aboutSphinxStateFlow: StateFlow<Boolean>
        get() = _aboutSphinxStateFlow.asStateFlow()

    fun toggleAboutSphinxWindow(open: Boolean) {
        _aboutSphinxStateFlow.value = open
    }

    private val _tribeDetailWindowStateFlow: MutableStateFlow<Pair<Boolean, ChatId?>> by lazy {
        MutableStateFlow(Pair(false, null))
    }

    val tribeDetailStateFlow: StateFlow<Pair<Boolean, ChatId?>>
        get() = _tribeDetailWindowStateFlow.asStateFlow()

    fun toggleTribeDetailWindow(open: Boolean, chatId: ChatId?) {
        _tribeDetailWindowStateFlow.value = Pair(open, chatId)
    }

    private val _tribeMembersStateFlow: MutableStateFlow<Pair<Boolean, ChatId?>> by lazy {
        MutableStateFlow(Pair(false, null))
    }

    val tribeMembersStateFlow: StateFlow<Pair<Boolean, ChatId?>>
        get() = _tribeMembersStateFlow.asStateFlow()

    fun toggleTribeMembersWindow(open: Boolean, chatId: ChatId?) {
        _tribeMembersStateFlow.value = Pair(open, chatId)
    }

    private val _qrWindowStateFlow: MutableStateFlow<Pair<Boolean, Pair<String, String>?>> by lazy {
        MutableStateFlow(Pair(false, null))
    }

    val qrWindowStateFlow: StateFlow<Pair<Boolean, Pair<String, String>?>>
        get() = _qrWindowStateFlow.asStateFlow()

    fun toggleQRWindow(
        open: Boolean,
        title: String? = null,
        value: String? = null
    ) {
        title?.let { nnTitle ->
            value?.let { nnValue ->
                _qrWindowStateFlow.value = Pair(open, Pair(nnTitle, nnValue))
                return
            }
        }
        _qrWindowStateFlow.value = Pair(open, null)
    }

    private val _transactionsStateFlow: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow(false)
    }

    val transactionsStateFlow: StateFlow<Boolean>
        get() = _transactionsStateFlow.asStateFlow()

    fun toggleTransactionsWindow(open: Boolean) {
        _transactionsStateFlow.value = open
    }

    private val _createTribeStateFlow: MutableStateFlow<Pair<Boolean, ChatId?>> by lazy {
        MutableStateFlow(Pair(false, null))
    }

    val createTribeStateFlow: StateFlow<Pair<Boolean, ChatId?>>
        get() = _createTribeStateFlow.asStateFlow()

    fun toggleCreateTribeWindow(open: Boolean, chatId: ChatId?) {
        _createTribeStateFlow.value = Pair(open, chatId)
    }

    private val _joinTribeStateFlow: MutableStateFlow<Pair<Boolean, TribeJoinLink?>> by lazy {
        MutableStateFlow(Pair(false, null))
    }

    val joinTribeStateFlow: StateFlow<Pair<Boolean, TribeJoinLink?>>
        get() = _joinTribeStateFlow.asStateFlow()

    fun toggleJoinTribeWindow(open: Boolean, tribeJoinLink: TribeJoinLink? = null) {
        _joinTribeStateFlow.value = Pair(open, tribeJoinLink)
    }

    private val _confirmationStateFlow: MutableStateFlow<Pair<Boolean, ConfirmationType?>> by lazy {
        MutableStateFlow(Pair(false, null))
    }

    val confirmationStateFlow: StateFlow<Pair<Boolean, ConfirmationType?>>
        get() = _confirmationStateFlow.asStateFlow()

    fun toggleConfirmationWindow(open: Boolean, confirmationType: ConfirmationType? = null) {
        _confirmationStateFlow.value = Pair(open, confirmationType)
    }

    private val _backUpWindowStateFlow: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow(false)
    }

    val backUpWindowStateFlow: StateFlow<Boolean>
        get() = _backUpWindowStateFlow.asStateFlow()

    fun toggleBackUpWindow(open: Boolean) {
        _backUpWindowStateFlow.value = open
    }

    private val _changePinWindowStateFlow: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow(false)
    }

    val changePinWindowStateFlow: StateFlow<Boolean>
        get() = _changePinWindowStateFlow.asStateFlow()

    fun toggleChangePinWindow(open: Boolean) {
        _changePinWindowStateFlow.value = open
    }

    val unseenTribeMessagesCount: StateFlow<Long?> = repositoryDashboard.getUnseenTribeMessagesCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0L)

    private fun getNodeDescriptor(owner: Contact): LightningNodeDescriptor? {
        owner.routeHint?.let {
            if (it.value.isNotEmpty()) {
                return VirtualLightningNodeAddress("${owner.nodePubKey?.value ?: ""}_${it.value}")
            }
        }
        return owner.nodePubKey
    }

    fun triggerOwnerQRCode() {
        val owner = accountOwnerStateFlow.value
        val nodeDescriptor = owner?.let { getNodeDescriptor(it) }
        toggleQRWindow(true, "PUBLIC KEY", nodeDescriptor?.value ?: "")
    }

    fun forceDisconnectMqtt() {
        connectManagerRepository.disconnectMqtt()
    }

    private fun getPackageVersion(){
        val currentAppVersion = "1.0.3"

        viewModelScope.launch(dispatchers.mainImmediate) {
            // TODO V2 getAppVersions
//            networkQueryVersion.getAppVersions().collect { loadResponse ->
//                when (loadResponse) {
//                    is Response.Error -> {
//                        _packageVersionAndUpgrade.value = Pair(currentAppVersion, false)
//                    }
//                    is Response.Success -> {
//                        val serverHubVersion = loadResponse.value.kmm
//
//                        currentAppVersion.replace(".", "").toIntOrNull()?.let { currentVersion ->
//                            if (serverHubVersion > currentVersion) {
//                                _packageVersionAndUpgrade.value = Pair(currentAppVersion, true)
//                            }
//                            else {
//                                _packageVersionAndUpgrade.value = Pair(currentAppVersion, false)
//                            }
//                        }
//                    }
//                    is LoadResponse.Loading -> {
//
//                    }
//                }
//            }
        }
    }


    fun kickMemberFromTribe(memberPubKey: LightningNodePubKey, alias: SenderAlias?, chatId: ChatId) {
        scope.launch(dispatchers.mainImmediate) {
            messageRepository.processMemberRequest(
                chatId,
                null,
                memberPubKey,
                MessageType.GroupAction.Kick,
                alias
            )
        }
    }

    private var screenInit: Boolean = false
    fun screenInit() {
        if (screenInit) {
            return
        } else {
            screenInit = true
        }
        connectManagerRepository.connectAndSubscribeToMqtt()
        triggerSetProfileInfoRestore()
        networkRefresh()
        getPackageVersion()
        // TODO V2 getAccountBalanceStateFlow

        viewModelScope.launch(dispatchers.mainImmediate) {
            repositoryDashboard.getAccountBalanceStateFlow().collect {
                _balanceStateFlow.value = it
            }
        }
    }

    // Payments

    private val sendPaymentRequestBuilder = SendPaymentRequest.Builder()

    var createInvoiceState: CreateInvoiceState by mutableStateOf(initialInvoiceState())

    private fun initialInvoiceState(): CreateInvoiceState = CreateInvoiceState()

    private inline fun setCreateInvoiceState(update: CreateInvoiceState.() -> CreateInvoiceState) {
        createInvoiceState = createInvoiceState.update()
    }

    fun onInvoiceAmountChange(text: String) {
        setCreateInvoiceState {
            copy(amount = text)
        }
    }

    fun onInvoiceMemoChange(text: String) {
        setCreateInvoiceState {
            copy(memo = text)
        }
    }

    fun requestPayment() {
        sendPaymentRequestBuilder.clear()
        sendPaymentRequestBuilder.setChatId(null)
        sendPaymentRequestBuilder.setContactId(null)
        sendPaymentRequestBuilder.setAmount(createInvoiceState.amount.toLongOrNull() ?: 0)
        sendPaymentRequestBuilder.setMemo(createInvoiceState.memo)

        viewModelScope.launch(dispatchers.mainImmediate) {
            val requestPayment = sendPaymentRequestBuilder.build()

            if (requestPayment != null) {
                val invoiceAndHash = connectManagerRepository.createInvoice(requestPayment.amount, requestPayment.memo ?: "")

                if (invoiceAndHash != null) {
                    toggleQRWindow(true, "Payment Request", invoiceAndHash.first)
                    createInvoiceState = initialInvoiceState()

                } else {
                    toast("Failed to request payment", badge_red)
                }
            } else {
                toast("Failed to request payment", badge_red)
            }
        }
    }

    fun verifyInvoice() {
        viewModelScope.launch(dispatchers.mainImmediate) {
            val invoice = _payInvoiceInfoStateFlow.value.invoiceString?.toLightningPaymentRequestOrNull()
            if (invoice != null) {
                val bolt11 = connectManagerRepository.getInvoiceInfo(invoice.value)?.toInvoiceBolt11()
                val amount = bolt11?.getSatsAmount()

                if (amount != null) {
                    _payInvoiceInfoStateFlow.value = _payInvoiceInfoStateFlow.value.copy(
                        amount = amount.value,
                        expirationDate = bolt11.getExpiryTime()?.toDateTime()?.eeemmddhmma(),
                        memo = bolt11.getMemo()
                    )
                }
            } else {
                toast("Invalid invoice", badge_red)
            }
        }
    }

    fun processInvoicePayment() {
        viewModelScope.launch(dispatchers.mainImmediate) {
            val invoice = _payInvoiceInfoStateFlow.value.invoiceString?.toLightningPaymentRequestOrNull()
            val bolt11 = invoice?.value?.let { connectManagerRepository.getInvoiceInfo(it)?.toInvoiceBolt11() }

            if (invoice != null && bolt11 != null) {
                lightningRepository.processLightningPaymentRequest(invoice, bolt11, callback = {
                    toast(it)
                })
                togglePayInvoiceWindow(false)
                clearInvoice()
            } else {
                toast("Unable to process payment", badge_red)
            }
        }
    }

    private var payInvoiceJob: Job? = null
    fun payContactInvoice(message: Message) {
        if (payInvoiceJob?.isActive == true) {
            return
        }
        payInvoiceJob = scope.launch(dispatchers.mainImmediate) {
            repositoryDashboard.getAccountBalanceStateFlow().firstOrNull()?.let { balance ->
                if (message.amount.value > balance.balance.value) {
                    toast("Insufficient balance", badge_red)
                } else {
                    connectManagerRepository.payContactPaymentRequest(message.paymentRequest)
                }
            }
        }
    }

    fun clearInvoice() {
        _payInvoiceInfoStateFlow.value = PayInvoiceInfo(null)
    }

    override fun windowGainedFocus(p0: WindowEvent?) {
        if (DashboardScreenState.screenState() == DashboardScreenType.Unlocked) {
            networkRefresh()
        }
    }

    override fun windowLostFocus(p0: WindowEvent?) { }

    private val _restoreStateFlow: MutableStateFlow<RestoreProgress?> by lazy {
        MutableStateFlow(null)
    }

    val networkStatusStateFlow: StateFlow<NetworkStatus>
        get() = connectManagerRepository.networkStatus.asStateFlow()

    val restoreProgressStateFlow: StateFlow<RestoreProgress?>
        get() = connectManagerRepository.restoreProgress.asStateFlow()

    var isRestoreCancelledState: Boolean by mutableStateOf(initialRestoreCancelledState())

    private fun initialRestoreCancelledState(): Boolean = false

    private var jobRestore: Job? = null

    fun networkRefresh() {
        jobRestore = viewModelScope.launch(dispatchers.mainImmediate) {
            restoreProgressStateFlow.collect { response ->
                response?.let { restoreProgress ->
                    if (restoreProgress.restoring) {
                        _restoreStateFlow.value = restoreProgress
                    }
                }
            }
//
//            repositoryDashboard.networkRefreshLatestContacts.collect { response ->
//                Exhaustive@
//                when (response) {
//                    is LoadResponse.Loading -> {
//                        _networkStateFlow.value = response
//                    }
//                    is Response.Error -> {
//                        _networkStateFlow.value = response
//                    }
//                    is Response.Success -> {
//                        val restoreProgress = response.value
//
//                        if (restoreProgress.restoring) {
//                            _restoreStateFlow.value = restoreProgress
//                        }
//                    }
//                }
//            }
//
//            if (_networkStateFlow.value is Response.Error) {
//                jobNetworkRefresh?.cancel()
//            }
//

//
//            if (_networkStateFlow.value is Response.Error) {
//                jobNetworkRefresh?.cancel()
//            }
        }
    }

    fun deleteInvite(inviteString: String) {
        viewModelScope.launch(dispatchers.mainImmediate) {
            repositoryDashboard.deleteInviteAndContact(inviteString)
        }
    }

    fun deleteSelectedContact() {
        viewModelScope.launch(dispatchers.mainImmediate) {
            val populatedData = ChatListState.screenState() as? ChatListData.PopulatedChatListData
            val contactId = populatedData?.getIdAsLong()?.toContactId()

            if (contactId != null) {
                contactRepository.deleteContactById(contactId)
            } else {
                toast("Failed to delete contact, please try later", badge_red)
            }
        }
    }

    fun triggerSetProfileInfoRestore() {
        viewModelScope.launch(dispatchers.mainImmediate) {
            profileSetInfoRestoreStateFlow.collect { setProfileRestore ->
                if (setProfileRestore == true) {
                    toast("Please enter your alias and an optional profile picture to finish setting up your Profile")
                    delay(2000L)
                    showFullScreenView(FullScreenView.Profile)
                }
            }
        }
    }

    private var jobNetworkRefresh: Job? = null

    fun triggerNetworkRefresh() {
        if (jobNetworkRefresh?.isActive == true) {
            return
        }

        jobNetworkRefresh = viewModelScope.launch(dispatchers.mainImmediate) {
            connectManagerRepository.reconnectMqtt()
        }
    }

    fun cancelRestore() {
        jobRestore?.cancel()
        _restoreStateFlow.value = null
        isRestoreCancelledState = true
        connectManagerRepository.cancelRestore()
    }

    fun clearDatabase() {
        coreDB.getSphinxDatabaseQueriesOrNull()?.let { queries: SphinxDatabaseQueries ->
            queries.transaction {
                deleteAll(queries)
            }
        }
    }

}