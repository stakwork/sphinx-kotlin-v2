package chat.sphinx.common.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import chat.sphinx.common.components.toast
import chat.sphinx.common.state.AuthorizeViewState
import chat.sphinx.concepts.network.query.contact.model.PersonDataDto
import chat.sphinx.concepts.network.query.lightning.model.lightning.*
import chat.sphinx.concepts.network.query.message.model.PutPaymentRequestDto
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
import uniffi.sphinxrs.makeInvite

class WebAppViewModel {
    val scope = SphinxContainer.appModule.applicationScope
    val dispatchers = SphinxContainer.appModule.dispatchers
    private val viewModelScope = SphinxContainer.appModule.applicationScope
    private val sphinxNotificationManager = createSphinxNotificationManager()
    private val contactRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).contactRepository
    private val lightningRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).lightningRepository
    private val messageRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).messageRepository
    private val chatRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).chatRepository
    private val connectManagerRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).connectManagerRepository
    private val networkQueryContact = SphinxContainer.repositoryModule(sphinxNotificationManager).networkQueryContact

    companion object {
        const val APPLICATION_NAME = "Sphinx"

        const val TYPE_AUTHORIZE = "AUTHORIZE"
        const val TYPE_SETBUDGET = "SETBUDGET"
        const val TYPE_GETBUDGET = "GETBUDGET"
        const val TYPE_LSAT = "LSAT"
        const val TYPE_GETLSAT = "GETLSAT"
        const val TYPE_SIGN = "SIGN"
        const val TYPE_KEYSEND = "KEYSEND"
        const val TYPE_UPDATELSAT = "UPDATELSAT"
        const val TYPE_PAYMENT = "PAYMENT"
        const val TYPE_UPDATED = "UPDATED"
        const val TYPE_GETPERSONDATA = "GETPERSONDATA"
        const val TYPE_GET_SECOND_BRAIN_LIST = "GETSECONDBRAINLIST"
    }

    private val sendPaymentBuilder = SendPayment.Builder()

    private var password = generatePassword()
    private var budget: Int? = null

    var callback: ((String) -> Unit)? = null

    private val _webAppWindowStateFlow: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow(false)
    }

    private val _webViewStateFlow: MutableStateFlow<String?> by lazy {
        MutableStateFlow(null)
    }

    private val _authorizeViewStateFlow: MutableStateFlow<AuthorizeViewState> by lazy {
        MutableStateFlow(AuthorizeViewState.Closed())
    }

    val webAppWindowStateFlow: StateFlow<Boolean>
        get() = _webAppWindowStateFlow.asStateFlow()

    val webViewStateFlow: StateFlow<String?>
        get() = _webViewStateFlow.asStateFlow()

    fun toggleWebAppWindow(
        open: Boolean,
        url: String?
    ) {
        if (_webAppWindowStateFlow.value != open) {
            _webAppWindowStateFlow.value = open
        }

        if (!open) {
            closeAuthorizeView()
            return
        }

        viewModelScope.launch(dispatchers.io) {
            delay(1000L)

            toggleWebViewWindow(url)
        }
        toast("WebView is not available at the moment")
    }

    val authorizeViewStateFlow: StateFlow<AuthorizeViewState>
        get() = _authorizeViewStateFlow.asStateFlow()

    var budgetState: Int? by mutableStateOf(null)

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

    private fun openAuthorizeView() {
        _webViewStateFlow?.value?.let { url ->
            val formattedUrl = url.replace("http://", "").replace("https://", "")
            _authorizeViewStateFlow.value = AuthorizeViewState.Opened(formattedUrl, false)
        }
    }

    private fun toggleSetBudgetView() {
        _webViewStateFlow?.value?.let { url ->
            val formattedUrl = url.replace("http://", "").replace("https://", "")
            _authorizeViewStateFlow.value = AuthorizeViewState.Opened(formattedUrl, true)
        }
    }

    fun closeAuthorizeView() {
        _authorizeViewStateFlow.value = AuthorizeViewState.Closed()
    }

    val customWebViewNavigator : WebViewNavigator
        get() {
            return WebViewNavigator(CoroutineScope(Dispatchers.IO))
        }

    val customJsBridge : WebViewJsBridge
        get() {
            return WebViewJsBridge(customWebViewNavigator)
        }

    fun onJsBridgeMessageReceived(
        message: JsMessage,
        callback: (String) -> Unit
    ) {
        this.callback = callback

        println("MESSAGE RECEIVED: $message")

        viewModelScope.launch(dispatchers.mainImmediate) {

            message.params.toBridgeAuthorizeMessageOrNull()?.let {
                if (it.type == TYPE_AUTHORIZE) {
                    openAuthorizeView()
                }
            }

            message.params.toBridgeSetBudgetMessageOrNull()?.let {
                if (it.type == TYPE_SETBUDGET) {
                    toggleSetBudgetView()
                }
            }

            message.params.toBridgeGetLSATMessageOrNull()?.let {
                if (it.type == TYPE_GETLSAT) {
                    processGetLsat(it)
                }
            }

            message.params.toBridgeSignMessageOrNull()?.let {
                if (it.type == TYPE_SIGN) {
                    processSign(it)
                }
            }

            message.params.toBridgeKeysendMessageOrNull()?.let {
                if (it.type == TYPE_KEYSEND) {
                    sendKeysend(it)
                }
            }

            message.params.toBridgeGetBudgetMessageOrNull()?.let {
                if (it.type == TYPE_GETBUDGET) {
                    processGetBudget()
                }
            }

            message.params.toBridgeLSatMessageOrNull()?.let {
                if (it.type == TYPE_LSAT) {
                    processLsat(it)
                }
            }

            message.params.toBridgeUpdateLSatMessageOrNull()?.let {
                if (it.type == TYPE_UPDATELSAT) {
                    processUpdateLsat(it)
                }
            }

            message.params.toBridgePaymentMessageOrNull()?.let {
                if (it.type == TYPE_PAYMENT) {
                    sendPayment(it)
                }
            }

            message.params.toBridgeUpdatedMessageOrNull()?.let {
                if (it.type == TYPE_UPDATED) {
                    sendUpdatedMessage()
                }
            }

            message.params.toBridgeGetPersonDataMessageOrNull()?.let {
                if (it.type == TYPE_GETPERSONDATA) {
                    getPersonData()
                }
            }
            message.params.toSendSecondBrainListDataOrNull()?.let {
                if (it.type == TYPE_GET_SECOND_BRAIN_LIST) {
                    processGetSecondBrainList(it)
                }
            }
        }
    }

    private fun toggleWebViewWindow(url: String?) {
        url?.let { nnUrl ->
            _webViewStateFlow.value = nnUrl
        }
    }

    fun processAuthorize() {
        closeAuthorizeView()

        viewModelScope.launch(dispatchers.mainImmediate) {
            delay(1000L)

            _webViewStateFlow?.value?.let { url ->

                getOwner().nodePubKey?.value?.let { pubkey ->
                    password = generatePassword()

                    val message = BridgeMessage(
                        budget = null,
                        pubkey = pubkey,
                        type = TYPE_AUTHORIZE,
                        password = password,
                        application = APPLICATION_NAME,
                        signature = null
                    ).toJson()

                    callback?.let {
                        it(message)
                    }

                    callback = null
                }
            }
        }
    }

    fun processSetBudget() {
        closeAuthorizeView()

        viewModelScope.launch(dispatchers.mainImmediate) {
            delay(1000L)

            _webViewStateFlow?.value?.let { url ->

                getOwner().nodePubKey?.value?.let { pubkey ->
                    budget = (budget ?: 0) + (budgetState ?: 0)

                    val message = BridgeMessage(
                        pubkey = pubkey,
                        type = TYPE_SETBUDGET,
                        password = password,
                        application = APPLICATION_NAME,
                        budget = (budgetState ?: 0),
                        signature = null
                    ).toJson()

                    callback?.let {
                        it(message)
                    }

                    callback = null
                }
            }
        }
    }

    private suspend fun processGetLsat(getLSATMessage: BridgeGetLSATMessage) {
        val issuer = getLSATMessage.issuer?.toLsatIssuer()

        val lastLsat = if (issuer != null) {
            chatRepository.getLastLsatByIssuer(issuer).firstOrNull()
        } else {
            chatRepository.getLastLsatActive().firstOrNull()
        }

        this.password = generatePassword()

        val message = if (lastLsat != null) {
            SendActiveLSatMessage(
                type = TYPE_GETLSAT,
                application = APPLICATION_NAME,
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
                type = TYPE_GETLSAT,
                application = APPLICATION_NAME,
                password = password,
                success = 0,
                issuer = issuer?.value!!
            ).toJson()
        }

        callback?.let {
            it(message)
        }
        callback = null
    }

    private fun processSign(bridgeSignMessage: BridgeSignMessage) {
        val signature = connectManagerRepository.signChallenge(bridgeSignMessage.message)

        if (signature != null) {
            val message = SendSignMessage(
                TYPE_SIGN,
                APPLICATION_NAME,
                password,
                signature,
                1
            ).toJson()

            callback?.let {
                it(message)
            }
            callback = null
        } else {
            val message = SendFailedSignMessage(
                TYPE_SIGN,
                APPLICATION_NAME,
                password,
                0
            ).toJson()

            callback?.let {
                it(message)
            }

            callback = null
        }
    }


//    private fun sendActiveLSAT(
//        activeLSatDto: ActiveLsatDto?,
//        success: Boolean
//    ) {
//        activeLSatDto?.let {
//            this.password = generatePassword()
//
//            val message = SendActiveLSatMessage(
//                TYPE_GETLSAT,
//                APPLICATION_NAME,
//                password,
//                it.macaroon,
//                it.paymentRequest,
//                it.preimage,
//                it.identifier,
//                success,
//                it.status,
//                it.paths ?: ""
//            ).toJson()
//
//            callback?.let {
//                it(message)
//            }
//
//            callback = null
//        } ?: run {
//            this.password = generatePassword()
//
//            val message = SendActiveLSatFailedMessage(
//                TYPE_GETLSAT,
//                APPLICATION_NAME,
//                password,
//                success
//            ).toJson()
//
//            callback?.let {
//                it(message)
//            }
//
//            callback = null
//        }
//    }

    private suspend fun signChallenge(
        bridgeSignMessage: BridgeSignMessage
    ) {
        lightningRepository.signChallenge(bridgeSignMessage.message).collect { loadResponse: LoadResponse<SignChallengeDto, ResponseError> ->
            Exhaustive@
            when (loadResponse) {
                is LoadResponse.Loading -> {}
                is Response.Error -> {
//                    sendActiveLSAT(null, false)
                }
                is Response.Success -> {
                    (loadResponse.value as? SignChallengeDto)?.let {
//                        sendSignMessage(it, true)
                    } ?: run {
//                        sendSignMessage(null, true)
                    }
                }
            }
        }
    }

    private suspend fun sendKeysend(
        keysendMessage: BridgeKeysendMessage
    ) {
        if (checkCanPay(keysendMessage.amt)) {
            sendPaymentBuilder.setAmount(keysendMessage.amt.toLong())
            sendPaymentBuilder.setDestinationKey(LightningNodePubKey(keysendMessage.dest))

            val sendPayment = sendPaymentBuilder.build()
            val response : Response<Any, ResponseError> = messageRepository.sendPayment(sendPayment)

            when (response) {
                is Response.Error -> {
                    sendKeysendMessage(keysendMessage, false)
                }
                is Response.Success -> {
                    sendKeysendMessage(keysendMessage, true)
                }
            }
        } else {
            sendKeysendMessage(keysendMessage, false)
        }
    }

    private fun processGetBudget() {
        this.password = generatePassword()

        val message = SendGetBudgetMessage(
            TYPE_GETBUDGET,
            APPLICATION_NAME,
            password,
            budget ?: 0,
            true
        ).toJson()

        callback?.let {
            it(message)
        }

        callback = null
    }

    private fun sendKeysendMessage(
        keysendMessage: BridgeKeysendMessage,
        success: Boolean
    ) {
        keysendMessage?.let {
            this.password = generatePassword()

            val message = SendKeysendMessage(
                TYPE_KEYSEND,
                APPLICATION_NAME,
                password,
                success
            ).toJson()

            callback?.let {
                it(message)
            }

            callback = null
        }
    }

    private fun checkCanPay(
        amount: Int
    ) : Boolean {
        if (amount == -1) {
            return false
        }
        if (budget != null && budget!! >= amount) {
            budget = budget!! - amount
            return true
        }
        return false
    }

//    private fun sendSignMessage(
//        signChallengeDto: SignChallengeDto?,
//        success: Boolean
//    ) {
//        signChallengeDto?.let {
//            this.password = generatePassword()
//
//            val message = SendSignMessage(
//                TYPE_SIGN,
//                APPLICATION_NAME,
//                password,
//                it.sig,
//                success
//            ).toJson()
//
//            callback?.let {
//                it(message)
//            }
//
//            callback = null
//        } ?: run {
//            this.password = generatePassword()
//
//            val message = SendFailedSignMessage(
//                TYPE_SIGN,
//                APPLICATION_NAME,
//                password,
//                false
//            ).toJson()
//
//            callback?.let {
//                it(message)
//            }
//
//            callback = null
//        }
//    }

    private suspend fun payLSat(lSatMessage: BridgeLSatMessage) {
        lSatMessage.paymentRequest.toLightningPaymentRequestOrNull()?.let {
            val decodedPaymentRequest = Bolt11.decode(it)

            decodedPaymentRequest.getSatsAmount()?.value?.toInt()?.let {amount ->
                if (checkCanPay(amount)) {
                    val payLsatSto = PayLsatDto(
                        lSatMessage.macaroon,
                        lSatMessage.paymentRequest,
                        lSatMessage.issuer
                    )

                    lightningRepository.payLSat(payLsatSto).collect { loadResponse: LoadResponse<PayLsatResponseDto, ResponseError> ->
                        Exhaustive@
                        when (loadResponse) {
                            is LoadResponse.Loading -> {}
                            is Response.Error -> {
                                sendLSat(lSatMessage, null, false)
                            }
                            is Response.Success -> {
                                sendLSat(lSatMessage, loadResponse.value.lsat, true)
                            }
                        }
                    }
                } else {
                    sendLSat(lSatMessage,null, false)
                }
            }
        }
    }

    private suspend fun processLsat(lSatMessage: BridgeLSatMessage) {
        val macaroon = lSatMessage.macaroon
        val issuer = lSatMessage.issuer

        val paymentRequest = lSatMessage.paymentRequest.toLightningPaymentRequestOrNull()?.let {
            Bolt11.decode(it)
        }
        val isBudgetSufficient = paymentRequest?.getSatsAmount()?.value?.toInt()?.let { checkCanPay(it) } ?: false

        if (isBudgetSufficient) {
            val identifier = connectManagerRepository.getIdFromMacaroon(macaroon)?.toLsatIdentifier()

            identifier?.let { lspIdentifier ->
                val identifierDbRecord = chatRepository.getLsatByIdentifier(lspIdentifier).firstOrNull()

                if (identifierDbRecord == null) {
                    val invoice = connectManagerRepository.getInvoiceInfo(lSatMessage.paymentRequest)?.toInvoiceBolt11()
                    val invoiceAmount = invoice?.getSatsAmount()?.value
                    val invoicePubKey = invoice?.getPubKey()
                    val paymentHash = invoice?.payment_hash

                    if (invoicePubKey != null && paymentHash != null && invoiceAmount != null) {
                        val routerUrl = connectManagerRepository.retrieveRouterUrl()

                        if (routerUrl != null) {
                            viewModelScope.launch {
                                if (invoice.retrieveLspPubKey() == contactRepository.accountOwner.value?.routeHint?.getLspPubKey()) {
                                    val nnPaymentRequest =
                                        lSatMessage.paymentRequest.toLightningPaymentRequestOrNull() ?: return@launch

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
                                                issuer = issuer.toLsatIssuer(),
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
                                        }
                                    }
                                } else {
                                    networkQueryContact.getRoutingNodes(
                                        routerUrl,
                                        invoicePubKey,
                                        convertToMilliSat(invoiceAmount)
                                    ).collect { response ->
                                        when (response) {
                                            is LoadResponse.Loading -> {}
                                            is Response.Error -> {}
                                            is Response.Success -> {
                                                try {
                                                    val routerPubKey = connectManagerRepository.retrieveRouterPubKey()

                                                    val nnPaymentRequest =
                                                        lSatMessage.paymentRequest.toLightningPaymentRequestOrNull()
                                                            ?: return@collect

                                                    connectManagerRepository.payInvoice(
                                                        paymentRequest = nnPaymentRequest,
                                                        response.value,
                                                        routerPubKey,
                                                        milliSatAmount = convertToMilliSat(
                                                            invoiceAmount
                                                        ),
                                                        paymentHash = paymentHash,
                                                    )
                                                    connectManagerRepository.webViewPreImage.collect { preimage ->
                                                        if (preimage?.isNotEmpty() == true) {

                                                            val lsatToSave = Lsat(
                                                                paymentRequest = lSatMessage.paymentRequest.toLightningPaymentRequestOrNull(),
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
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    // Handle exception
                                                }
                                            }
                                        }
                                    }
                                }
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

    private fun sendLsatSuccess(macaroon: String, preimage: String) {
        val message = SendLSatMessage(
            TYPE_LSAT,
            APPLICATION_NAME,
            1,
            budget,
            password,
            lsat = retrieveLsatString(macaroon, preimage)
        ).toJson()
        callback?.invoke(message)
        callback = null
    }

    private fun sendLsatFailure() {
        val message = SendLSatFailedMessage(
            TYPE_LSAT,
            APPLICATION_NAME,
            0,
            password
        ).toJson()
        callback?.invoke(message)
        callback = null
    }

    private suspend fun processUpdateLsat(updateLSatMessage: BridgeUpdateLSatMessage) {
        if (updateLSatMessage.status == LsatStatus.EXPIRED_STRING) {
            val identifier = updateLSatMessage.identifier.toLsatIdentifier()
            val lsatOnDb = identifier?.let { chatRepository.getLsatByIdentifier(it).firstOrNull() }

            if (lsatOnDb != null) {
                chatRepository.updateLsatStatus(identifier, LsatStatus.Expired)

                val message = SendUpdateLSatMessage(
                    TYPE_UPDATELSAT,
                    APPLICATION_NAME,
                    password,
                    1,
                    retrieveLsatString(lsatOnDb.macaroon.value, lsatOnDb.preimage?.value),
                ).toJson()

                callback?.let {
                    it(message)
                }
                callback = null
            }
        }
    }


    private fun sendLSat(
        lSatMessage: BridgeLSatMessage,
        lsat: String?,
        success: Boolean
    ) {
//        if (lsat != null && success) {
//            this.password = generatePassword()
//
//            val message = SendLSatMessage(
//                TYPE_LSAT,
//                APPLICATION_NAME,
//                password,
//                lSatMessage.paymentRequest,
//                lSatMessage.macaroon,
//                lSatMessage.issuer,
//                lsat,
//                budget,
//                true
//            ).toJson()
//
//            callback?.let {
//                it(message)
//            }
//
//            callback = null
//        } else {
//            this.password = generatePassword()
//
//            val message = SendLSatFailedMessage(
//                TYPE_LSAT,
//                APPLICATION_NAME,
//                password,
//                lSatMessage.paymentRequest,
//                lSatMessage.macaroon,
//                lSatMessage.issuer,
//                false
//            ).toJson()
//
//            callback?.let {
//                it(message)
//            }
//
//            callback = null
//        }
    }

    private suspend fun updateLSat(updateLSatMessage: BridgeUpdateLSatMessage) {
        val updateLsatSto = UpdateLsatDto(
            updateLSatMessage.status
        )

        lightningRepository.updateLSat(
            updateLSatMessage.identifier,
            updateLsatSto
        ).collect { loadResponse: LoadResponse<String, ResponseError> ->
            Exhaustive@
            when (loadResponse) {
                is LoadResponse.Loading -> {}
                is Response.Error -> {
                    sendUpdateLSat(updateLSatMessage, null, false)
                }
                is Response.Success -> {
                    loadResponse.value.toPayLsatResponseDtoOrNull()?.let {
                        sendUpdateLSat(updateLSatMessage, it.lsat, true)
                    } ?: run {
                        sendUpdateLSat(updateLSatMessage, null, true)
                    }
                }
            }
        }
    }

    private fun sendUpdateLSat(
        updateLSatMessage: BridgeUpdateLSatMessage,
        lsat: String?,
        success: Boolean
    ) {
//        if (lsat != null && success) {
//            this.password = generatePassword()
//
//            val message = SendUpdateLSatMessage(
//                TYPE_UPDATELSAT,
//                APPLICATION_NAME,
//                password,
//                updateLSatMessage.identifier,
//                updateLSatMessage.status,
//                lsat,
//                true
//            ).toJson()
//
//            callback?.let {
//                it(message)
//            }
//
//            callback = null
//        } else {
//            this.password = generatePassword()
//
//            val message = SendUpdateLSatFailedMessage(
//                TYPE_UPDATELSAT,
//                APPLICATION_NAME,
//                password,
//                updateLSatMessage.identifier,
//                updateLSatMessage.status,
//                success
//            ).toJson()
//
//            callback?.let {
//                it(message)
//            }
//
//            callback = null
//        }
    }

    private suspend fun sendPayment(bridgePaymentMessage: BridgePaymentMessage) {
        bridgePaymentMessage.paymentRequest.toLightningPaymentRequestOrNull()?.let {
            val decodedPaymentRequest = Bolt11.decode(it)

            decodedPaymentRequest.getSatsAmount()?.value?.toInt()?.let {amount ->
                if (checkCanPay(amount)) {
                    val putPaymentRequestDto = PutPaymentRequestDto(
                        bridgePaymentMessage.paymentRequest
                    )

                    messageRepository.payPaymentRequest(putPaymentRequestDto).collect { loadResponse: LoadResponse<Any, ResponseError> ->
                        Exhaustive@
                        when (loadResponse) {
                            is LoadResponse.Loading -> {}
                            is Response.Error -> {
                                sendPaymentMessage(bridgePaymentMessage,false)
                            }
                            is Response.Success -> {
                                sendPaymentMessage(bridgePaymentMessage,true)
                            }
                        }
                    }
                } else {
                    sendPaymentMessage(bridgePaymentMessage,false)
                }
            }
        }
    }

    private fun sendPaymentMessage(
        bridgePaymentMessage: BridgePaymentMessage,
        success: Boolean
    ) {
        this.password = generatePassword()

        val message = SendPaymentMessage(
            TYPE_PAYMENT,
            APPLICATION_NAME,
            password,
            bridgePaymentMessage.paymentRequest,
            success
        ).toJson()

        callback?.let {
            it(message)
        }

        callback = null
    }

    private fun sendUpdatedMessage() {
        this.password = generatePassword()

        val message = SendUpdatedMessage(
            TYPE_UPDATED,
            APPLICATION_NAME,
            password
        ).toJson()

        callback?.let {
            it(message)
        }

        callback = null
    }

    private suspend fun getPersonData() {
        contactRepository.getPersonData().collect { loadResponse: LoadResponse<PersonDataDto, ResponseError> ->
            Exhaustive@
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

    private fun processGetSecondBrainList(sendSecondBrainListData: SendSecondBrainListData) {
        viewModelScope.launch {
            contactRepository.accountOwner.value?.nodePubKey?.let { pubKey ->

                val type = sendSecondBrainListData.type
                val application = sendSecondBrainListData.application
                password = generatePassword()

                val message = SendAuthMessage(
                    type = type,
                    application = application,
                    password = password,
                    pubkey = pubKey.value
                ).toJson()

                callback?.let {
                    it(message)
                }

                callback = null
            }
        }
    }


    private fun sendPersonDataMessage(personData: PersonDataDto?, success: Boolean) {
        this.password = generatePassword()

        if (personData != null && success) {
            val message = SendPersonDataMessage(
                TYPE_GETPERSONDATA,
                APPLICATION_NAME,
                password,
                personData!!.publicKey,
                personData!!.alias,
                personData!!.photoUrl ?: "",
                success
            ).toJson()

            callback?.let {
                it(message)
            }

            callback = null
        } else {
            val message = SendPersonDataFailedMessage(
                TYPE_GETPERSONDATA,
                APPLICATION_NAME,
                password,
                success
            ).toJson()

            callback?.let {
                it(message)
            }

            callback = null
        }
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