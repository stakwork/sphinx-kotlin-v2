package chat.sphinx.common.viewmodel

import chat.sphinx.authentication.model.OnBoardStep
import chat.sphinx.authentication.model.OnBoardStepHandler
import chat.sphinx.common.state.*
import chat.sphinx.di.container.SphinxContainer
import chat.sphinx.features.repository.util.upsertContact
import chat.sphinx.wrapper.dashboard.ContactId
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SphinxStore {
    private val scope = SphinxContainer.appModule.applicationScope
    private val authenticationManager = SphinxContainer.authenticationModule.authenticationCoreManager
    private val authenticationStorage = SphinxContainer.authenticationModule.authenticationStorage
    private val encryptionKeyHandler = SphinxContainer.authenticationModule.encryptionKeyHandler
    private val onBoardStepHandler = OnBoardStepHandler()

    fun removeAccount() {
        scope.launch(SphinxContainer.appModule.dispatchers.main) {
            withContext(SphinxContainer.appModule.dispatchers.io) {

                try {
                    authenticationManager.logOut()
                    encryptionKeyHandler.clearKeysToRestore()
                    authenticationStorage.clearAuthenticationStorage()

                    delay(100L)

                    val hasCredentials = authenticationStorage.hasCredential()
                    if (hasCredentials) {
                        authenticationStorage.clearAuthenticationStorage()
                        delay(100L)
                    }
                } catch (e: Exception) {
                    println("Error during account removal: ${e.message}")
                }
            }
        }
    }

    suspend fun restoreSignupStep() {
        onBoardStepHandler.retrieveOnBoardStep()?.let { onBoardStep ->
            when (onBoardStep) {
                is OnBoardStep.Step1_WelcomeMessage -> {
                    LandingScreenState.screenState(LandingScreenType.OnBoardMessage)
                }
                is OnBoardStep.Step2_Name,
                is OnBoardStep.Step3_Picture,
                is OnBoardStep.Step4_Ready-> {
                    LandingScreenState.screenState(LandingScreenType.SignupLocked)
                }
            }
        }
    }
}