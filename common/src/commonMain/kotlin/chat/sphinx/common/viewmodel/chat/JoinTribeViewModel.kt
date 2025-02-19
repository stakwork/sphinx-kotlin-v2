package chat.sphinx.common.viewmodel.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import chat.sphinx.common.state.JoinTribeState
import chat.sphinx.common.viewmodel.DashboardViewModel
import chat.sphinx.concepts.network.query.chat.NetworkQueryChat
import chat.sphinx.concepts.network.query.chat.model.NewTribeDto
import chat.sphinx.concepts.network.query.chat.model.TribeDto
import chat.sphinx.concepts.network.query.chat.model.escrowMillisToHours
import chat.sphinx.concepts.repository.message.model.AttachmentInfo
import chat.sphinx.di.container.SphinxContainer
import chat.sphinx.response.LoadResponse
import chat.sphinx.response.Response
import chat.sphinx.utils.ServersUrlsHelper
import chat.sphinx.utils.notifications.createSphinxNotificationManager
import chat.sphinx.wrapper.PhotoUrl
import chat.sphinx.wrapper.chat.ChatHost
import chat.sphinx.wrapper.chat.ChatUUID
import chat.sphinx.wrapper.chat.fixedAlias
import chat.sphinx.wrapper.contact.Contact
import chat.sphinx.wrapper.lightning.LightningNodePubKey
import chat.sphinx.wrapper.message.media.MediaType
import chat.sphinx.wrapper.message.media.toFileName
import chat.sphinx.wrapper.toPhotoUrl
import chat.sphinx.wrapper.tribe.TribeJoinLink
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.Path
import theme.badge_red
import theme.primary_green
import theme.primary_red

class JoinTribeViewModel(
    private val tribeJoinLink: TribeJoinLink,
    val dashboardViewModel: DashboardViewModel
) {

    val scope = SphinxContainer.appModule.applicationScope
    val dispatchers = SphinxContainer.appModule.dispatchers
    private val networkQueryChat: NetworkQueryChat = SphinxContainer.networkModule.networkQueryChat
    private val sphinxNotificationManager = createSphinxNotificationManager()
    private val contactRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).contactRepository
    private val chatRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).chatRepository
    private val connectManagerRepository = SphinxContainer.repositoryModule(sphinxNotificationManager).connectManagerRepository

    var joinTribeState: JoinTribeState by mutableStateOf(initialState())

    private fun initialState(): JoinTribeState = JoinTribeState()

    private inline fun setJoinTribeState(update: JoinTribeState.() -> JoinTribeState) {
        joinTribeState = joinTribeState.update()
    }

    private var newTribeInfo : NewTribeDto? = null
    private val isProductionEnvironment = ServersUrlsHelper().getEnvironmentType()

    init {
        loadTribeData()
    }
    private fun loadTribeData(){
        scope.launch(dispatchers.mainImmediate) {
            val owner = getOwner()

            networkQueryChat.getTribeInfo(
                ChatHost(tribeJoinLink.tribeHost),
                LightningNodePubKey(tribeJoinLink.tribePubkey),
                isProductionEnvironment
            ).collect { loadResponse ->
                when (loadResponse) {
                    is Response.Success -> {
                        loadResponse.apply {

                            newTribeInfo = value
                            newTribeInfo?.set(tribeJoinLink.tribeHost, tribeJoinLink.tribePubkey)

                            val hourToStake: Long = (value.escrow_millis / 60 / 60 / 1000)

                            setJoinTribeState {
                                copy(
                                    name = value.name,
                                    description = value.description.toString(),
                                    img = value.img?.toPhotoUrl(),
                                    price_to_join = value.getPriceToJoinInSats().toString(),
                                    price_per_message = value.getPricePerMessageInSats().toString(),
                                    escrow_amount = value.getEscrowAmountInSats().toString(),
                                    hourToStake = value.escrow_millis.escrowMillisToHours().toString(),
                                    userAlias = (owner.alias?.value ?: "").fixedAlias(),
                                    myPhotoUrl = owner.photoUrl,
                                    loadingTribe = false
                                )
                            }
                        }
                    }
                    is Response.Error -> {
                        setJoinTribeState {
                            copy(
                                loadingTribe = false
                            )
                        }
                        toast("There was an error loading the tribe. Please try again later", badge_red)
                    }

                    else -> {}
                }
            }
        }
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

    private var joinTribeJob: Job? = null
    fun joinTribe() {
        if (joinTribeJob?.isActive == true) {
            return
        }
        val tribeInfo = newTribeInfo
        val host = tribeInfo?.host

        joinTribeJob = scope.launch(dispatchers.mainImmediate) {
            setJoinTribeState {
                copy(
                    status = LoadResponse.Loading
                )
            }

            tribeInfo?.myAlias = joinTribeState.userAlias
            tribeInfo?.amount = joinTribeState.price_to_join.toLong()

            if (host != null) {
                connectManagerRepository.joinTribe(
                    host,
                    tribeInfo.pubkey,
                    tribeInfo.route_hint,
                    tribeInfo.name,
                    tribeInfo.img,
                    tribeInfo.private ?: false,
                    joinTribeState.userAlias,
                    tribeInfo.getPricePerMessageInSats(),
                    tribeInfo.getEscrowAmountInSats(),
                    tribeInfo.getPriceToJoinInSats(),
                    )
                dashboardViewModel.toggleJoinTribeWindow(false)
            } else {
                 toast("There was an error joining the tribe. Please try again later", badge_red)
            }
        }
    }

    fun onAliasTextChanged(text: String) {
        val fixedAlias = text.fixedAlias()

        if (text != fixedAlias) {
            toast("Only letters, numbers and underscore\nare allowed in tribe aliases")
        }

        setJoinTribeState {
            copy(
                userAlias = fixedAlias
            )
        }
    }

    fun onProfilePictureChanged(filepath: Path) {
        val ext = filepath.toFile().extension
        val mediaType = MediaType.Image(MediaType.IMAGE + "/$ext")

        setJoinTribeState {
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
        newTribeInfo?.setProfileImageFile(filepath.toFile())
    }

    private fun toast(
        message: String,
        color: Color = primary_green,
        delay: Long = 2000L
    ) {
        scope.launch(dispatchers.mainImmediate) {
            sphinxNotificationManager.toast(
                "Join Tribe",
                message,
                color.value,
                delay
            )
        }
    }
}