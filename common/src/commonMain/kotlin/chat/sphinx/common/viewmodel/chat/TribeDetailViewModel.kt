package chat.sphinx.common.viewmodel.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import chat.sphinx.common.state.ChatDetailData
import chat.sphinx.common.state.ChatDetailState
import chat.sphinx.common.state.TribeDetailState
import chat.sphinx.common.viewmodel.DashboardViewModel
import chat.sphinx.concepts.network.query.chat.model.ChatDto
import chat.sphinx.concepts.repository.message.model.AttachmentInfo
import chat.sphinx.di.container.SphinxContainer
import chat.sphinx.response.LoadResponse
import chat.sphinx.response.Response
import chat.sphinx.response.ResponseError
import chat.sphinx.utils.ServersUrlsHelper
import chat.sphinx.utils.notifications.createSphinxNotificationManager
import chat.sphinx.wrapper.DateTime
import chat.sphinx.wrapper.chat.*
import chat.sphinx.wrapper.contact.Contact
import chat.sphinx.wrapper.dashboard.ChatId
import chat.sphinx.wrapper.localDateTimeString
import chat.sphinx.wrapper.meme_server.PublicAttachmentInfo
import chat.sphinx.wrapper.message.media.MediaType
import chat.sphinx.wrapper.message.media.toFileName
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okio.Path
import theme.primary_green

class TribeDetailViewModel(
    private val dashboardViewModel: DashboardViewModel,
    private val detailChatId: ChatId
) {

    val scope = SphinxContainer.appModule.applicationScope
    val dispatchers = SphinxContainer.appModule.dispatchers
    private val sphinxNotificationManager = createSphinxNotificationManager()
    private val chatRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).chatRepository
    private val contactRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).contactRepository
    private val tribeDefaultServerUrl = ServersUrlsHelper().getTribeServerIp()

    private var currentChat: Chat? = null

    var tribeDetailState: TribeDetailState by mutableStateOf(initialTribeDetailState())

    private fun initialTribeDetailState(): TribeDetailState = TribeDetailState()

    private inline fun setTribeDetailState(update: TribeDetailState.() -> TribeDetailState) {
        tribeDetailState = tribeDetailState.update()
    }

    private val accountOwnerStateFlow: StateFlow<Contact?>
        get() = contactRepository.accountOwner

    init {
        loadTribeDetail()
    }

    private fun loadTribeDetail() {
        scope.launch(dispatchers.mainImmediate) {
            accountOwnerStateFlow.collect { contactOwner ->
                contactOwner?.let { owner ->
                    val chat = chatRepository.getChatById(detailChatId)
                    chat?.let {
                        currentChat = chat
                        val tribeOwner = chat.isTribeOwnedByAccount(owner.nodePubKey)
                        val showTribeQrCode = chat.isTribeOwnedByAccount(owner.nodePubKey) || !chat.privateTribe.isTrue()
                        val shareTribeUrl = "sphinx.chat://?action=tribeV2&pubkey=${chat.uuid.value}&host=${tribeDefaultServerUrl}"
                        val createdAtDate = chat.createdAt.localDateTimeString(DateTime.getFormateeemmddhmma())

                        setTribeDetailState {
                            copy(
                                tribeName = chat.name?.value ?: "",
                                tribePhotoUrl = chat.photoUrl,
                                createDate = "Created on $createdAtDate",
                                tribeConfigurations = "Price per message: ${chat.pricePerMessage?.value ?: 0L} sat" + " - Amount to stake: ${chat.escrowAmount?.value ?: 0L} sat ",
                                userAlias = chat.myAlias?.value ?: owner.alias?.value ?: "",
                                userPicture = null,
                                myPhotoUrl = chat.myPhotoUrl ?: owner.photoUrl,
                                tribeOwner = tribeOwner,
                                showQrCode = showTribeQrCode,
                                shareTribeUrl = shareTribeUrl,
                                saveButtonEnable = false,
                                updateResponse = null
                            )
                        }
                    }
                }
            }
        }
    }


    fun onAliasTextChanged(text: String){
        val fixedAlias = text.fixedAlias()

        if (text != fixedAlias) {
            toast("Only letters, numbers and underscore\nare allowed in tribe aliases")
        }

        setTribeDetailState {
            copy(
                userAlias = fixedAlias,
                saveButtonEnable = true
            )
        }
    }

    fun onProfilePictureChanged(filepath: Path) {
        val ext = filepath.toFile().extension
        val mediaType = MediaType.Image(MediaType.IMAGE + "/$ext")

        setTribeDetailState {
            copy(
                userPicture = AttachmentInfo(
                    filePath = filepath,
                    mediaType = mediaType,
                    fileName = filepath.name.toFileName(),
                    isLocalFile = true
                ),
                myPhotoUrl = null
            )
        }
    }

    private var updateJob: Job? = null
    fun updateUserInfo(){
        if (updateJob?.isActive == true) {
            return
        }

        updateJob = scope.launch(dispatchers.mainImmediate) {
            var response: Response<ChatDto, ResponseError>?

            setTribeDetailState {
                copy(
                    updateResponse = LoadResponse.Loading
                )
            }

            chatRepository.updateChatProfileInfo(
                detailChatId,
                ChatAlias(tribeDetailState.userAlias)
            ).let { r ->
                response = r
            }

            tribeDetailState.userPicture?.let {
                chatRepository.updateChatProfileInfo(
                    detailChatId,
                    profilePic = PublicAttachmentInfo(
                        tribeDetailState.userPicture!!.filePath,
                        tribeDetailState.userPicture!!.mediaType,
                        tribeDetailState.userPicture!!.fileName?.value ?: "",
                        null
                    )
                ).let { r ->
                    response = r
                }
            }

            response?.let {
                updateFinished(it)
            }
        }
    }

    private fun updateFinished(response: Response<ChatDto, ResponseError>) {
        if (response is Response.Success) {
            dashboardViewModel.toggleTribeDetailWindow(false, null)
        } else if (response is Response.Error) {
            loadTribeDetail()
        }
    }

    fun exitAndDeleteTribe(){
        currentChat?.let{ chat ->
            scope.launch(dispatchers.mainImmediate){
                setTribeDetailState {
                    copy(
                        updateResponse = LoadResponse.Loading
                    )
                }

                chatRepository.exitAndDeleteTribe(chat)
                dashboardViewModel.toggleTribeDetailWindow(false, null)

                ChatDetailState.screenState(ChatDetailData.EmptyChatDetailData)
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
                "Tribe Detail",
                message,
                color.value,
                delay
            )
        }
    }
}