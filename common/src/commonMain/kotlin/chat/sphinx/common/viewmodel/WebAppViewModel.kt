package chat.sphinx.common.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import chat.sphinx.common.components.toast
import chat.sphinx.common.state.AuthorizeViewState
import chat.sphinx.concepts.network.query.contact.model.PersonDataDto
import chat.sphinx.concepts.network.query.lightning.model.lightning.*
import chat.sphinx.concepts.network.query.message.model.PutPaymentRequestDto
import chat.sphinx.concepts.network.query.webview.SphinxWebViewDto
import chat.sphinx.concepts.network.query.webview.toSphinxWebViewDtoOrNull
import chat.sphinx.concepts.repository.message.model.SendPayment
import chat.sphinx.crypto.common.annotations.RawPasswordAccess
import chat.sphinx.crypto.common.clazzes.PasswordGenerator
import chat.sphinx.di.container.SphinxContainer
import chat.sphinx.response.*
import chat.sphinx.utils.notifications.createSphinxNotificationManager
import chat.sphinx.wrapper.DateTime
import chat.sphinx.wrapper.bridge.*
import chat.sphinx.wrapper.contact.Contact
import chat.sphinx.wrapper.lightning.Bolt11
import chat.sphinx.wrapper.lightning.LightningNodePubKey
import chat.sphinx.wrapper.lightning.getLspPubKey
import chat.sphinx.wrapper.lightning.toLightningPaymentRequestOrNull
import chat.sphinx.wrapper.lsat.*
import chat.sphinx.wrapper.mqtt.InvoiceBolt11
import chat.sphinx.wrapper.mqtt.InvoiceBolt11.Companion.toInvoiceBolt11
import chat.sphinx.wrapper.toDateTime
import com.multiplatform.webview.jsbridge.IJsMessageHandler
import com.multiplatform.webview.jsbridge.JsMessage
import com.multiplatform.webview.jsbridge.WebViewJsBridge
import com.multiplatform.webview.web.WebViewNavigator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.skia.impl.Log
import uniffi.sphinxrs.makeInvite

class WebAppViewModel(private val dashboardViewModel: DashboardViewModel) {
    val scope = SphinxContainer.appModule.applicationScope
    val dispatchers = SphinxContainer.appModule.dispatchers
    val viewModelScope = SphinxContainer.appModule.applicationScope
    private val sphinxNotificationManager = createSphinxNotificationManager()
    private val contactRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).contactRepository
    private val lightningRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).lightningRepository
    private val messageRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).messageRepository
    private val chatRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).chatRepository
    private val connectManagerRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).connectManagerRepository
    private val networkQueryContact = SphinxContainer.repositoryModule(sphinxNotificationManager).networkQueryContact

    // Main StateFlow for WebView DTO (similar to Android)
    private val _sphinxWebViewDtoStateFlow: MutableStateFlow<SphinxWebViewDto?> = MutableStateFlow(null)
    val sphinxWebViewDtoStateFlow: StateFlow<SphinxWebViewDto?> = _sphinxWebViewDtoStateFlow.asStateFlow()

    private val sendPaymentBuilder = SendPayment.Builder()
    private var password = generatePassword()

    private val _budgetStateFlow: MutableStateFlow<Int> = MutableStateFlow(0)
    val budgetStateFlow: StateFlow<Int> get() = _budgetStateFlow.asStateFlow()

    var callback: ((String) -> Unit)? = null


    var budgetState: Int? by mutableStateOf(null)

    init {
        handleWebAppJson()
    }

    // Central handler for WebView DTO messages (similar to Android's handleWebAppJson)
    private fun handleWebAppJson() {
        viewModelScope.launch(dispatchers.mainImmediate) {
            sphinxWebViewDtoStateFlow.collect { dto ->
                println("Collecting DTO: $dto")
                when (dto?.type) {
                    SphinxWebViewDto.TYPE_AUTHORIZE -> {
                        dashboardViewModel.openAuthorizeView()
                    }
                    SphinxWebViewDto.TYPE_GET_LSAT -> {
                        processGetLsat()
                    }
                    SphinxWebViewDto.TYPE_SET_BUDGET -> {
                        dashboardViewModel.toggleSetBudgetView()
                    }
                    SphinxWebViewDto.TYPE_SIGN -> {
                        processSign()
                    }
                    SphinxWebViewDto.TYPE_LSAT -> {
                        processLsat()
                    }
                    SphinxWebViewDto.TYPE_KEYSEND -> {
                        sendKeysend()
                    }
                    SphinxWebViewDto.TYPE_PAYMENT -> {
                        processPayment()
                    }
                    SphinxWebViewDto.TYPE_UPDATE_LSAT -> {
                        processUpdateLsat()
                    }
                    SphinxWebViewDto.TYPE_GET_PERSON_DATA -> {
                        processGetPersonData()
                    }
                    SphinxWebViewDto.TYPE_GET_BUDGET -> {
                        processGetBudget()
                    }
                    SphinxWebViewDto.TYPE_GET_SECOND_BRAIN_LIST -> {
                        processGetSecondBrainList()
                    }
                    else -> {}
                }
            }
        }
    }

    fun onAmountTextChanged(text: String) {
        var amount: Int? = try {
            text.toInt()
        } catch (e: NumberFormatException) {
            null
        }
        setBudgetValue(amount)
    }

    private fun setBudgetValue(budget: Int?) {
        budgetState = budget
    }


    val customWebViewNavigator: WebViewNavigator
        get() = WebViewNavigator(CoroutineScope(Dispatchers.IO))

    val customJsBridge: WebViewJsBridge
        get() = WebViewJsBridge(customWebViewNavigator)

    // Main entry point for JS messages - now updates the StateFlow instead of direct processing
    fun onJsBridgeMessageReceived(message: JsMessage, callback: (String) -> Unit) {
        this.callback = callback
        println("MESSAGE RECEIVED: $message")

        viewModelScope.launch(dispatchers.mainImmediate) {
            // Parse the message and update the StateFlow
            message.params.toSphinxWebViewDtoOrNull()?.let { dto ->
                _sphinxWebViewDtoStateFlow.value = dto
            } ?: run {
                println("Failed to parse message: ${message.params}")
            }
        }
    }

    fun processAuthorize() {
        dashboardViewModel.closeAuthorizeView()
        viewModelScope.launch(dispatchers.mainImmediate) {
            delay(1000L)
            getOwner().nodePubKey?.value?.let { pubkey ->
                password = generatePassword()

                val message = BridgeMessage(
                    budget = null,
                    pubkey = pubkey,
                    type = SphinxWebViewDto.TYPE_AUTHORIZE,
                    password = password,
                    application = SphinxWebViewDto.APPLICATION_NAME,
                    signature = null
                ).toJson()

                callback?.invoke(message)
                callback = null
            }
        }
    }

    fun processSetBudget(amount: Int) {
        dashboardViewModel.closeAuthorizeView()
        viewModelScope.launch(dispatchers.mainImmediate) {
            delay(1000L)
            getOwner().nodePubKey?.value?.let { pubkey ->
                _budgetStateFlow.value = amount
                println("Budget set to $amount")

                val message = BridgeMessage(
                    pubkey = pubkey,
                    type = SphinxWebViewDto.TYPE_SET_BUDGET,
                    password = password,
                    application = SphinxWebViewDto.APPLICATION_NAME,
                    budget = amount,
                    signature = null
                ).toJson()

                callback?.invoke(message)
                callback = null
            }
        }
    }

    private suspend fun processGetLsat() {
        val webViewDto = sphinxWebViewDtoStateFlow.value
        val issuer = webViewDto?.issuer?.toLsatIssuer()

        val lastLsat = if (issuer != null) {
            chatRepository.getLastLsatByIssuer(issuer).firstOrNull()
        } else {
            chatRepository.getLastLsatActive().firstOrNull()
        }

        password = generatePassword()

        val message = if (lastLsat != null) {
            SendActiveLSatMessage(
                type = SphinxWebViewDto.TYPE_GET_LSAT,
                application = SphinxWebViewDto.APPLICATION_NAME,
                password = password,
                macaroon = lastLsat.macaroon.value,
                paymentRequest = lastLsat.paymentRequest!!.value,
                preimage = lastLsat.preimage!!.value,
                identifier = lastLsat.id.value,
                success = 1,
                status = lastLsat.status.value.toString(),
                paths = lastLsat.paths?.value ?: "",
                issuer = issuer?.value!!
            ).toJson()
        } else {
            SendActiveLSatFailedMessage(
                type = SphinxWebViewDto.TYPE_GET_LSAT,
                application = SphinxWebViewDto.APPLICATION_NAME,
                password = password,
                success = 0,
                issuer = issuer?.value ?: ""
            ).toJson()
        }

        callback?.invoke(message)
        callback = null
    }

    private fun processSign() {
        val webViewDto = sphinxWebViewDtoStateFlow.value
        val message = webViewDto?.message
        val signature = message?.let { connectManagerRepository.signChallenge(it) }

        val responseMessage = if (signature != null) {
            SendSignMessage(
                SphinxWebViewDto.TYPE_SIGN,
                SphinxWebViewDto.APPLICATION_NAME,
                password,
                signature,
                1
            ).toJson()
        } else {
            SendFailedSignMessage(
                SphinxWebViewDto.TYPE_SIGN,
                SphinxWebViewDto.APPLICATION_NAME,
                password,
                0
            ).toJson()
        }

        callback?.invoke(responseMessage)
        callback = null
    }

    private suspend fun sendKeysend() {
        val webViewDto = sphinxWebViewDtoStateFlow.value
        val dest = webViewDto?.dest
        val amt = webViewDto?.amt

        if (dest != null && amt != null && checkCanPay(amt)) {
            sendPaymentBuilder.setAmount(amt.toLong())
            sendPaymentBuilder.setDestinationKey(LightningNodePubKey(dest))

            val sendPayment = sendPaymentBuilder.build()
            val response: Response<Any, ResponseError> = messageRepository.sendPayment(sendPayment)

            val success = when (response) {
                is Response.Error -> false
                is Response.Success -> true
            }

            sendKeysendMessage(success)
        } else {
            sendKeysendMessage(false)
        }
    }

    private fun processGetBudget() {
        password = generatePassword()

        val message = SendGetBudgetMessage(
            SphinxWebViewDto.TYPE_GET_BUDGET,
            SphinxWebViewDto.APPLICATION_NAME,
            password,
            budgetStateFlow.value,
            true
        ).toJson()

        callback?.invoke(message)
        callback = null
    }

    private suspend fun processLsat() {
        val webViewDto = sphinxWebViewDtoStateFlow.value
        val macaroon = webViewDto?.macaroon
        val issuer = webViewDto?.issuer
        val paymentRequestStr = webViewDto?.paymentRequest

        val paymentRequest = paymentRequestStr?.toLightningPaymentRequestOrNull()?.let {
            Bolt11.decode(it)
        }

        val paymentAmount = paymentRequest?.getSatsAmount()
        val requestedAmount = paymentAmount?.value?.toInt()
        val budget = budgetStateFlow.value

        val isAmountValid = paymentAmount != null
        val isBudgetSufficient = budget >= (paymentAmount?.value ?: 0)
        val areRequiredFieldsPresent = !paymentRequestStr.isNullOrEmpty() &&
                !macaroon.isNullOrEmpty() &&
                !issuer.isNullOrEmpty()

        if (isAmountValid && isBudgetSufficient && areRequiredFieldsPresent) {
            val identifier = connectManagerRepository.getIdFromMacaroon(macaroon!!)?.toLsatIdentifier()

            identifier?.let { lspIdentifier ->
                val identifierDbRecord = chatRepository.getLsatByIdentifier(lspIdentifier).firstOrNull()

                if (identifierDbRecord == null) {
                    val invoice = connectManagerRepository.getInvoiceInfo(paymentRequestStr!!)?.toInvoiceBolt11()
                    val invoiceAmount = invoice?.getSatsAmount()?.value
                    val invoicePubKey = invoice?.getPubKey()
                    val paymentHash = invoice?.payment_hash

                    if (invoicePubKey != null && paymentHash != null && invoiceAmount != null && invoiceAmount <= budget) {
                        val routerUrl = connectManagerRepository.retrieveRouterUrl()
                        val routerPubKey = connectManagerRepository.retrieveRouterPubKey()

                        if (routerUrl != null) {
                            if (issuer != null) {
                                processLsatPayment(
                                    paymentRequestStr, macaroon, issuer, lspIdentifier,
                                    invoiceAmount, invoicePubKey, paymentHash, routerUrl, routerPubKey, invoice
                                )
                            }
                        } else {
                            sendLsatFailure()
                        }
                    } else {
                        sendLsatFailure()
                    }
                } else {
                    sendLsatFailure()
                }
            } ?: sendLsatFailure()
        } else {
            sendLsatFailure()
        }
    }

    private suspend fun processLsatPayment(
        paymentRequestStr: String,
        macaroon: String,
        issuer: String,
        lspIdentifier: LsatIdentifier,
        invoiceAmount: Long,
        invoicePubKey: LightningNodePubKey,
        paymentHash: String,
        routerUrl: String,
        routerPubKey: String?,
        invoice: InvoiceBolt11
    ) {
        viewModelScope.launch {
            if (invoice.retrieveLspPubKey() == contactRepository.accountOwner.value?.routeHint?.getLspPubKey()) {
                val nnPaymentRequest = paymentRequestStr.toLightningPaymentRequestOrNull() ?: return@launch

                try {
                    connectManagerRepository.payInvoice(
                        paymentRequest = nnPaymentRequest,
                        null,
                        null,
                        milliSatAmount = convertToMilliSat(invoiceAmount),
                        paymentHash = paymentHash,
                    )

                    connectManagerRepository.webViewPreImage.collect { preimage ->
                        if (preimage?.isNotEmpty() == true) {
                            val lsatToSave = Lsat(
                                paymentRequest = nnPaymentRequest,
                                macaroon = macaroon.toMacaroon()!!,
                                issuer = issuer.toLsatIssuer()!!,
                                id = lspIdentifier,
                                preimage = preimage.toLsatPreImage(),
                                status = LsatStatus.Active,
                                createdAt = DateTime.nowUTC().toDateTime(),
                                paths = null,
                                metaData = null
                            )

                            chatRepository.upsertLsat(lsatToSave)
                            connectManagerRepository.clearWebViewPreImage()
                            sendLsatSuccess(macaroon, preimage)
                            return@collect
                        }
                    }
                } catch (e: Exception) {
                    sendLsatFailure()
                }
            } else {
                try {
                    networkQueryContact.getRoutingNodes(
                        routerUrl,
                        invoicePubKey,
                        convertToMilliSat(invoiceAmount)
                    ).collect { response ->
                        when (response) {
                            is LoadResponse.Loading -> {}
                            is Response.Error -> {
                                sendLsatFailure()
                            }
                            is Response.Success -> {
                                if (response.value.isEmpty()) {
                                    sendLsatFailure()
                                    return@collect
                                }

                                try {
                                    val nnPaymentRequest = paymentRequestStr.toLightningPaymentRequestOrNull()
                                        ?: return@collect

                                    connectManagerRepository.payInvoice(
                                        paymentRequest = nnPaymentRequest,
                                        response.value,
                                        routerPubKey,
                                        milliSatAmount = convertToMilliSat(invoiceAmount),
                                        paymentHash = paymentHash,
                                    )

                                    connectManagerRepository.webViewPreImage.collect { preimage ->
                                        if (preimage?.isNotEmpty() == true) {
                                            val lsatToSave = Lsat(
                                                paymentRequest = nnPaymentRequest,
                                                macaroon = macaroon.toMacaroon()!!,
                                                issuer = issuer.toLsatIssuer()!!,
                                                id = lspIdentifier,
                                                preimage = preimage.toLsatPreImage(),
                                                status = LsatStatus.Active,
                                                createdAt = DateTime.nowUTC().toDateTime(),
                                                paths = null,
                                                metaData = null
                                            )

                                            chatRepository.upsertLsat(lsatToSave)
                                            connectManagerRepository.clearWebViewPreImage()
                                            sendLsatSuccess(macaroon, preimage)
                                            return@collect
                                        }
                                    }
                                } catch (e: Exception) {
                                    sendLsatFailure()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    sendLsatFailure()
                }
            }
        }
    }

    private suspend fun processUpdateLsat() {
        val webViewDto = sphinxWebViewDtoStateFlow.value

        if (webViewDto?.status == LsatStatus.EXPIRED_STRING) {
            val identifier = webViewDto.identifier?.toLsatIdentifier()
            val lsatOnDb = identifier?.let { chatRepository.getLsatByIdentifier(it).firstOrNull() }

            if (lsatOnDb != null) {
                chatRepository.updateLsatStatus(identifier, LsatStatus.Expired)

                val message = SendUpdateLSatMessage(
                    SphinxWebViewDto.TYPE_UPDATE_LSAT,
                    SphinxWebViewDto.APPLICATION_NAME,
                    password,
                    1,
                    retrieveLsatString(lsatOnDb.macaroon.value, lsatOnDb.preimage?.value),
                ).toJson()

                callback?.invoke(message)
                callback = null
            }
        }
    }

    private suspend fun processPayment() {
        val webViewDto = sphinxWebViewDtoStateFlow.value
        val paymentRequestStr = webViewDto?.paymentRequest
        val budget = budgetStateFlow.value

        val invoice = connectManagerRepository.getInvoiceInfo(paymentRequestStr ?: "")?.toInvoiceBolt11()
        val paymentAmount = invoice?.getSatsAmount()?.value
        val invoicePubKey = invoice?.getPubKey()

        val isAmountValid = paymentAmount != null
        val isBudgetSufficient = budget >= (paymentAmount ?: 0)
        val lightningPaymentRequest = paymentRequestStr?.toLightningPaymentRequestOrNull()
        val ownerLsp = contactRepository.accountOwner.value?.routeHint?.getLspPubKey()

        if (isAmountValid && isBudgetSufficient && lightningPaymentRequest != null) {
            if (invoice?.retrieveLspPubKey() == ownerLsp) {
                connectManagerRepository.payInvoice(
                    lightningPaymentRequest,
                    endHops = null,
                    routerPubKey = null,
                    paymentAmount ?: 0
                )
                sendPaymentMessage(true)
            } else {
                val isAvailableRoute = connectManagerRepository.isRouteAvailable(
                    invoicePubKey?.value ?: "",
                    null,
                    paymentAmount ?: 0
                )

                if (isAvailableRoute) {
                    connectManagerRepository.payInvoice(
                        lightningPaymentRequest,
                        endHops = null,
                        routerPubKey = null,
                        paymentAmount ?: 0
                    )
                    sendPaymentMessage(true)
                } else {
                    val routerUrl = connectManagerRepository.retrieveRouterUrl()
                    if (invoicePubKey != null && routerUrl != null) {
                        networkQueryContact.getRoutingNodes(
                            routerUrl,
                            invoicePubKey,
                            paymentAmount ?: 0
                        ).collect { response ->
                            when (response) {
                                is LoadResponse.Loading -> {}
                                is Response.Error -> sendPaymentMessage(false)
                                is Response.Success -> {
                                    try {
                                        val routerPubKey = connectManagerRepository.retrieveRouterPubKey()
                                        connectManagerRepository.payInvoice(
                                            paymentRequest = lightningPaymentRequest,
                                            response.value,
                                            routerPubKey,
                                            milliSatAmount = paymentAmount ?: 0,
                                        )
                                        sendPaymentMessage(true)
                                    } catch (e: Exception) {
                                        sendPaymentMessage(false)
                                    }
                                }
                            }
                        }
                    } else {
                        sendPaymentMessage(false)
                    }
                }
            }
        } else {
            sendPaymentMessage(false)
        }
    }

    private suspend fun processGetPersonData() {
        contactRepository.getPersonData().collect { loadResponse ->
            when (loadResponse) {
                is LoadResponse.Loading -> {}
                is Response.Error -> {
                    sendPersonDataMessage(null, false)
                }
                is Response.Success -> {
                    sendPersonDataMessage(loadResponse.value, true)
                }
            }
        }
    }

    private fun processGetSecondBrainList() {
//        viewModelScope.launch {
//            val webViewDto = sphinxWebViewDtoStateFlow.value
//            val secondBrainList = chatRepository.getSecondBrainTribes().firstOrNull()
//                ?.filter { it?.secondBrainUrl != null && it.secondBrainUrl?.value?.isNotEmpty() == true }
//                ?.map { it!!.secondBrainUrl!!.value }
//
//            val message = SendSecondBrainListData(
//                type = webViewDto?.type ?: "",
//                application = webViewDto?.application ?: "",
//                password = password,
//                secondBrainList = secondBrainList ?: emptyList()
//            ).toJson()
//
//            callback?.invoke(message)
//            callback = null
//        }
    }

    // Helper methods
    private fun sendKeysendMessage(success: Boolean) {
        password = generatePassword()
        val message = SendKeysendMessage(
            SphinxWebViewDto.TYPE_KEYSEND,
            SphinxWebViewDto.APPLICATION_NAME,
            password,
            success
        ).toJson()

        callback?.invoke(message)
        callback = null
    }

    private fun sendLsatSuccess(macaroon: String, preimage: String) {
        val message = SendLSatMessage(
            SphinxWebViewDto.TYPE_LSAT,
            SphinxWebViewDto.APPLICATION_NAME,
            1,
            budgetStateFlow.value,
            password,
            lsat = retrieveLsatString(macaroon, preimage)
        ).toJson()
        callback?.invoke(message)
        callback = null
    }

    private fun sendLsatFailure() {
        val message = SendLSatFailedMessage(
            SphinxWebViewDto.TYPE_LSAT,
            SphinxWebViewDto.APPLICATION_NAME,
            0,
            password
        ).toJson()
        callback?.invoke(message)
        callback = null
    }

    private fun sendPaymentMessage(success: Boolean) {
        password = generatePassword()
        val message = SendPaymentMessage(
            SphinxWebViewDto.TYPE_PAYMENT,
            SphinxWebViewDto.APPLICATION_NAME,
            password,
            sphinxWebViewDtoStateFlow.value?.paymentRequest ?: "",
            success
        ).toJson()

        callback?.invoke(message)
        callback = null
    }

    private fun sendPersonDataMessage(personData: PersonDataDto?, success: Boolean) {
        password = generatePassword()

        val message = if (personData != null && success) {
            SendPersonDataMessage(
                SphinxWebViewDto.TYPE_GET_PERSON_DATA,
                SphinxWebViewDto.APPLICATION_NAME,
                password,
                personData.publicKey,
                personData.alias,
                personData.photoUrl ?: "",
                success
            ).toJson()
        } else {
            SendPersonDataFailedMessage(
                SphinxWebViewDto.TYPE_GET_PERSON_DATA,
                SphinxWebViewDto.APPLICATION_NAME,
                password,
                success
            ).toJson()
        }

        callback?.invoke(message)
        callback = null
    }

    private fun checkCanPay(amount: Int): Boolean {
        val currentBudget = _budgetStateFlow.value
        println("Checking if can pay: budget=$currentBudget, amount=$amount")
        if (amount == -1) return false
        if (currentBudget >= amount) {
            _budgetStateFlow.value = currentBudget - amount
            println("Budget after payment: ${_budgetStateFlow.value}")
            return true
        }
        println("Cannot pay: budget too low")
        return false
    }

    private fun generatePassword(): String {
        @OptIn(RawPasswordAccess::class)
        return PasswordGenerator(passwordLength = 16).password.value.joinToString("")
    }

    private suspend fun getOwner(): Contact {
        return contactRepository.accountOwner.value.let { contact ->
            if (contact != null) {
                contact
            } else {
                var resolvedOwner: Contact? = null
                try {
                    contactRepository.accountOwner.collect { ownerContact ->
                        if (ownerContact != null) {
                            resolvedOwner = ownerContact
                            throw Exception()
                        }
                    }
                } catch (e: Exception) {
                }
                delay(25L)
                resolvedOwner!!
            }
        }
    }

    private fun convertToMilliSat(amount: Long): Long {
        return amount * 1000
    }

    private fun retrieveLsatString(macaroon: String?, preimage: String?): String {
        return "LSAT $macaroon:$preimage"
    }
}

class JsMessageHandler(
    private val webAppViewModel: WebAppViewModel
) : IJsMessageHandler {

    override fun methodName(): String {
        return "sphinx-bridge"
    }

    override fun handle(
        message: JsMessage,
        navigator: WebViewNavigator?,
        callback: (String) -> Unit
    ) {
        webAppViewModel.onJsBridgeMessageReceived(message, callback)
    }
}