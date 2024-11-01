package chat.sphinx.common.viewmodel.contact

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import chat.sphinx.common.state.ContactState
import androidx.compose.runtime.setValue
import chat.sphinx.common.components.toast
import chat.sphinx.di.container.SphinxContainer
import chat.sphinx.response.LoadResponse
import chat.sphinx.response.Response
import chat.sphinx.response.ResponseError
import chat.sphinx.utils.notifications.createSphinxNotificationManager
import chat.sphinx.wrapper.contact.ContactAlias
import chat.sphinx.wrapper.contact.NewContact
import chat.sphinx.wrapper.contact.toContactAlias
import chat.sphinx.wrapper.dashboard.ContactId
import chat.sphinx.wrapper.lightning.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull


class AddContactViewModel() : ContactViewModel() {

    private val sphinxNotificationManager = createSphinxNotificationManager()
    private val contactRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).contactRepository
    private val connectManagerRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).connectManagerRepository

    override var contactState: ContactState by mutableStateOf(initialState())

    private fun initialState(): ContactState = ContactState()

    private inline fun setContactState(update: ContactState.() -> ContactState) {
        contactState = contactState.update()
    }

    private var dataAutoFilled = false
    fun fillPubKey(pubKey: String?) {
        pubKey?.let { nnPubKey ->
            if (!dataAutoFilled) {
                dataAutoFilled = true
                onAddressTextChanged(nnPubKey)
            }
        }
    }

    private fun checkValidInput() {
        setStatus(null)

        val validNickname = (contactState.contactAlias.toContactAlias() != null)
        val validAddress = (contactState.lightningNodePubKey.toLightningNodePubKey() != null)
        val validRouteHint = (contactState.lightningRouteHint.isNullOrEmpty() ||
                contactState.lightningRouteHint?.toLightningRouteHint() != null)

        if (validNickname && validAddress) {
            setSaveButtonEnabled(true)
        }
        if (!validNickname || !validAddress || !validRouteHint) {
            setSaveButtonEnabled(false)
        }
    }

    override fun onNicknameTextChanged(text: String) {
        setContactState {
            copy(
                contactAlias = text
            )
        }
        checkValidInput()
    }

    override fun onAddressTextChanged(text: String) {
        val contact = text.split("_")
        if (contact.size == 3) {
            val pubKey = contact.getOrNull(0)?.toLightningNodePubKey()?.value ?: ""
            val routeHint = "${contact.getOrNull(1)}_${contact.getOrNull(2)}"
            setContactState {
                copy(
                    lightningNodePubKey = pubKey,
                    lightningRouteHint = routeHint
                )
            }
        } else {
            setContactState {
                copy(
                    lightningNodePubKey = text
                )
            }
        }
        checkValidInput()
    }

    override fun onRouteHintTextChanged(text: String) {
        text.toVirtualLightningNodeAddress()?.let { nnVirtualAddress ->
            setContactState {
                copy(
                    lightningNodePubKey = nnVirtualAddress.getPubKey()?.value ?: "",
                    lightningRouteHint = nnVirtualAddress.getRouteHint()?.value ?: ""
                )
            }
        } ?: run {
            setContactState {
                copy(
                    lightningRouteHint = text
                )
            }
        }
        checkValidInput()
    }

    private fun setStatus(status: LoadResponse<Any, ResponseError>?) {
        setContactState {
            copy(
                status = status
            )
        }
    }

    private fun setSaveButtonEnabled(buttonEnabled: Boolean) {
        setContactState {
            copy(
                saveButtonEnabled = buttonEnabled
            )
        }
    }

    override fun saveContact() {
        if (saveContactJob?.isActive == true) {
            return
        }
        saveContactJob = scope.launch(dispatchers.mainImmediate) {

            val pubkey = contactState.lightningNodePubKey.toLightningNodePubKey()
            val exitingContact = pubkey?.let { contactRepository.getContactByPubKey(it).firstOrNull() }
            val routeHint = contactState.lightningRouteHint?.toLightningRouteHint()

            if (routeHint != null && exitingContact == null) {

                val newContact = NewContact(
                    contactState.contactAlias.toContactAlias(),
                    LightningNodePubKey(contactState.lightningNodePubKey),
                    routeHint,
                    null,
                    false,
                    null,
                    null,
                    null,
                    null,
                    null
                )
                connectManagerRepository.createContact(newContact)
                println("CREATE_CONTACT: connectManagerRepository.createContact(newContact)")
                setStatus(Response.Success(true))
            } else {
                setStatus(Response.Error(ResponseError("Invalid Contact")))
            }
        }
    }

}