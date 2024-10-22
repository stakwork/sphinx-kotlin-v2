package chat.sphinx.common.viewmodel.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import chat.sphinx.common.state.BackupKeysState
import chat.sphinx.common.viewmodel.PinAuthenticationViewModel
import chat.sphinx.concepts.authentication.coordinator.AuthenticationRequest
import chat.sphinx.concepts.authentication.coordinator.AuthenticationResponse
import chat.sphinx.crypto.common.annotations.RawPasswordAccess
import chat.sphinx.crypto.common.clazzes.Password
import chat.sphinx.di.container.SphinxContainer
import chat.sphinx.features.authentication.core.AuthenticationCoreCoordinator
import io.ktor.util.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.cryptonode.jncryptor.AES256JNCryptor
import org.cryptonode.jncryptor.CryptorException
import kotlin.text.toCharArray

class PinExportKeysViewModel : PinAuthenticationViewModel() {

    var backupKeysState: BackupKeysState by mutableStateOf(initialState())

    val relayDataHandler = SphinxContainer.networkModule.relayDataHandlerImpl
    val authenticationCoordinator: AuthenticationCoreCoordinator = SphinxContainer.authenticationModule.authenticationCoreCoordinator

    private fun initialState(): BackupKeysState = BackupKeysState()

    private inline fun setBackupKeysState(update: BackupKeysState.() -> BackupKeysState) {
        backupKeysState = backupKeysState.update()
    }

    override fun onPINTextChanged(text: String) {
        super.onPINTextChanged(text)
        backupKeysState = initialState()
    }


    @OptIn(RawPasswordAccess::class, InternalAPI::class)
    override fun onAuthenticationSucceed() {
        scope.launch(dispatchers.mainImmediate) {

            authenticationCoordinator.submitAuthenticationRequest(
                AuthenticationRequest.GetEncryptionKey()
            ).firstOrNull().let { keyResponse ->
                Exhaustive@
                when (keyResponse) {
                    null,
                    is AuthenticationResponse.Failure -> {
                        setBackupKeysState {
                            copy(
                                restoreString = null,
                                error = true
                            )
                        }
                    }

                    is AuthenticationResponse.Success.Authenticated -> {}

                    is AuthenticationResponse.Success.Key -> {
                        relayDataHandler.retrieveWalletMnemonic()?.let { mnemonic ->
                            setBackupKeysState {
                                copy(
                                    restoreString = mnemonic.value,
                                    error = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
