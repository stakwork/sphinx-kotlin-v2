package chat.sphinx.common.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import chat.sphinx.common.state.ProfileState
import chat.sphinx.concepts.repository.message.model.AttachmentInfo
import chat.sphinx.di.container.SphinxContainer
import chat.sphinx.response.LoadResponse
import chat.sphinx.response.ResponseError
import chat.sphinx.utils.ServersUrlsHelper
import chat.sphinx.utils.notifications.createSphinxNotificationManager
import chat.sphinx.wrapper.contact.Contact
import chat.sphinx.wrapper.contact.toNotNullPrivatePhoto
import chat.sphinx.wrapper.lightning.LightningNodeDescriptor
import chat.sphinx.wrapper.lightning.Sat
import chat.sphinx.wrapper.lightning.VirtualLightningNodeAddress
import chat.sphinx.wrapper.lightning.toSat
import chat.sphinx.wrapper.message.media.MediaType
import chat.sphinx.wrapper.message.media.toFileName
import chat.sphinx.wrapper.relay.isOnionAddress
import chat.sphinx.wrapper.relay.toRelayUrl
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okio.Path
import theme.badge_red

class ProfileViewModel {

    private val sphinxNotificationManager = createSphinxNotificationManager()
    private val networkClient = SphinxContainer.networkModule.networkClient
    private val repositoryModule = SphinxContainer.repositoryModule(sphinxNotificationManager)
    private val contactRepository = repositoryModule.contactRepository
    private val lightningRepository = repositoryModule.lightningRepository
    private val relayDataHandler = SphinxContainer.networkModule.relayDataHandlerImpl
    private val serversUrls = ServersUrlsHelper()


    val scope = SphinxContainer.appModule.applicationScope
    val dispatchers = SphinxContainer.appModule.dispatchers

    private val accountOwnerStateFlow: StateFlow<Contact?>
        get() = contactRepository.accountOwner

    var profileState: ProfileState by mutableStateOf(initialState())

    private fun initialState(): ProfileState = ProfileState()

    private inline fun setProfileState(update: ProfileState.() -> ProfileState) {
        profileState = profileState.update()
    }

    private fun getNodeDescriptor(owner: Contact): LightningNodeDescriptor? {
        owner.routeHint?.let {
            if (it.value.isNotEmpty()) {
                return VirtualLightningNodeAddress("${owner.nodePubKey?.value ?: ""}_${it.value}")
            }
        }
        return owner.nodePubKey
    }

    private fun loadProfile() {
        scope.launch(dispatchers.mainImmediate) {
            accountOwnerStateFlow.collect { contactOwner ->
                contactOwner?.let { owner ->

                   val nodeDescriptor = getNodeDescriptor(owner)

                    setProfileState {
                        copy(
                            alias = owner.alias?.value ?: "",
                            nodePubKey = owner.nodePubKey?.value ?: "",
                            routeHint = owner.routeHint?.value ?: "",
                            nodeDescription = nodeDescriptor?.value ?: "",
                            photoUrl = owner.photoUrl,
                            serverUrl = "",
                            defaultTipAmount = owner.tipAmount?.value?.toString() ?: "",
                            privatePhoto = toPrivatePhotoBoolean(owner.privatePhoto.value)
                        )
                    }
                }
            }
        }
    }

    init {
        loadProfile()
        loadServerUrls()
    }

    fun onPrivatePhotoSwitchChange(checked: Boolean){
        setProfileState {
            copy(
                privatePhoto = checked,
            )
        }
        toggleSaveButton()
    }
    fun onDefaultCallServerChange(text: String){
        setProfileState {
            copy(
                meetingServerUrl = text,
            )
        }
        toggleSaveButton()
    }

    fun onDefaultTipAmountChange(text: String){
        setProfileState {
            copy(
                defaultTipAmount = text,
            )
        }
        toggleSaveButton()
    }

    fun onAliasTextChanged(text: String) {
        setProfileState {
            copy(
                alias = text,
            )
        }
        toggleSaveButton()
    }

    fun onServerUrlChanged(text: String) {
        setProfileState {
            copy(
                serverUrl = text
            )
        }
        toggleSaveButton()
    }

    private fun toggleSaveButton() {
        scope.launch(dispatchers.mainImmediate) {
            accountOwnerStateFlow.collect { contactOwner ->
                contactOwner?.let { owner ->

                    val aliasDidChange = owner.alias?.value ?: "" != profileState.alias
                    val tipAmount = (owner.tipAmount?.value?.toString() ?: "") != profileState.defaultTipAmount
                    val serverUrlDidChange = false
                    val privatePhotoDidChange = toPrivatePhotoBoolean(owner.privatePhoto.value) != profileState.privatePhoto
                    val meetingServerDidChange = serversUrls.getMeetingServer() != profileState.meetingServerUrl

                    setProfileState {
                        copy(
                            saveButtonEnabled = (aliasDidChange || serverUrlDidChange || privatePhotoDidChange || meetingServerDidChange || tipAmount)
                        )
                    }
                }
            }
        }
    }

    private fun setStatus(status: LoadResponse<Any, ResponseError>?) {
        setProfileState {
            copy(
                status = status
            )
        }
    }

    fun updateOwnerDetails() {
        scope.launch(dispatchers.mainImmediate) {
            contactRepository.updateOwner(
                alias = profileState.alias,
                privatePhoto = profileState.privatePhoto.toNotNullPrivatePhoto(),
                tipAmount = profileState.defaultTipAmount.toLongOrNull()?.toSat() ?: Sat(0)
            ).let { loadResponse ->
                setStatus(loadResponse)
            }

            serversUrls.setMeetingServer(profileState.meetingServerUrl)
        }

    }

    private fun loadServerUrls(){
        val meetingServer = serversUrls.getMeetingServer()

        setProfileState {
            copy(
                meetingServerUrl = meetingServer
            )
        }
    }

    private fun toPrivatePhotoBoolean(privatePhoto: Int?) : Boolean = privatePhoto == 1

    fun onProfilePictureChanged(filepath: Path) {
        val ext = filepath.toFile().extension
        val mediaType = MediaType.Image(MediaType.IMAGE + "/$ext")

        setProfileState {
            copy(
                profilePictureResponse = LoadResponse.Loading
            )
        }

        profileState.profilePicture.value = AttachmentInfo(
            filePath = filepath,
            mediaType = mediaType,
            fileName = filepath.name.toFileName(),
            isLocalFile = true
        )
        updateProfilePic()
    }

    private fun updateProfilePic() {
        scope.launch(dispatchers.mainImmediate){
            profileState.profilePicture.value?.apply {
                contactRepository.updateProfilePic(
                    path = filePath,
                    mediaType = mediaType,
                    fileName = fileName?.value ?: "unknown",
                    contentLength = null
                ).let { response ->
                    setProfileState {
                        copy(
                            profilePictureResponse = response
                        )
                    }
                }
            }
        }
    }

    fun toast(
        message: String,
        color: Color = badge_red,
        delay: Long = 2000L
    ) {
        scope.launch(dispatchers.mainImmediate) {
            sphinxNotificationManager.toast(
                "Profile",
                message,
                color.value,
                delay
            )
        }
    }

    private fun confirm(
        title: String,
        message: String,
        confirmButton: String? = null,
        cancelButton: String? = null,
        callback: () -> Unit,
        cancelCallback: () -> Unit,
    ) {
        scope.launch(dispatchers.mainImmediate) {
            sphinxNotificationManager.confirmAlert(
                "Profile",
                title,
                message,
                callback,
                cancelCallback,
                confirmButton,
                cancelButton
            )
        }
    }
}