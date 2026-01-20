package com.auth0.android.ui_components.presentation.ui.passkeys

import android.util.Log
import androidx.credentials.CreateCredentialRequest
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.android.result.AuthenticationMethod
import com.auth0.android.ui_components.domain.error.Auth0Error
import com.auth0.android.ui_components.domain.model.PasskeyEnrollmentChallenge
import com.auth0.android.ui_components.domain.model.PublicKeyCredentials
import com.auth0.android.ui_components.domain.repository.MyAccountRepository
import com.auth0.android.ui_components.presentation.ui.UiError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Represents the different UI states for passkey enrollment
 */

sealed interface PasskeyUiState {
    object Idle : PasskeyUiState
    object UserCancelled : PasskeyUiState
    object RequestingChallenge : PasskeyUiState
    object CreatingPasskey : PasskeyUiState
    object EnrollingPasskey : PasskeyUiState
    data class Error(val error: UiError) : PasskeyUiState
}

/**
 * Represents events emitted during passkey enrollment flow
 */
sealed interface PasskeyEvent {
    /**
     * Emitted when passkey enrollment challenge is successfully retrieved
     * @param challenge The enrollment challenge containing public key parameters
     */
    data class EnrollmentChallengeReady(
        val challenge: PasskeyEnrollmentChallenge
    ) : PasskeyEvent

    /**
     * Emitted when passkey enrollment is successfully completed
     * @param authenticationMethod The enrolled passkey authentication method
     */
    data class EnrollmentSuccess(
        val authenticationMethod: AuthenticationMethod
    ) : PasskeyEvent

    /**
     * Emitted when an error occurs during enrollment
     * @param error The Auth0 error that occurred
     */
    data class EnrollmentError(
        val error: Auth0Error
    ) : PasskeyEvent
}

/**
 * ViewModel for managing passkey enrollment flow
 *
 * This ViewModel handles:
 * - Initiating passkey enrollment to get the challenge
 * - Verifying the passkey credentials after user creates a passkey via Credential Manager
 */
class PasskeyViewModel(
    private val myAccountRepository: MyAccountRepository
) : ViewModel() {

    private companion object {
        private const val TAG = "PasskeyViewModel"
        private const val SCOPE = "create:me:authentication_methods"
    }

    private val eventChannel = Channel<PasskeyEvent>()
    val events = eventChannel.receiveAsFlow()

    private val _uiState: MutableStateFlow<PasskeyUiState> = MutableStateFlow(PasskeyUiState.Idle)
    val uiState: StateFlow<PasskeyUiState> = _uiState.asStateFlow()

    /**
     * Initiates passkey enrollment by requesting a challenge from the server
     */
    fun enrollPasskey(
        createCredential: suspend (CreateCredentialRequest) -> CreateCredentialResponse,
    ) {
        viewModelScope.launch {

            runCatching {
                val challenge =
                    myAccountRepository.enrollPasskey(SCOPE)

                val request =
                    CreatePublicKeyCredentialRequest(Json.encodeToString(challenge.authParamsPublicKey))

                val credentialResponse = createCredential(request)

                val publicKeyCredentials =
                    Json.decodeFromString<PublicKeyCredentials>((credentialResponse as CreatePublicKeyCredentialResponse).registrationResponseJson)
                val result = myAccountRepository.verifyPasskey(
                    publicKeyCredentials,
                    challenge,
                    SCOPE
                )
            }.onFailure {
                Log.e("TAG", "enrollPasskey:  ${it.stackTraceToString()}")
            }
        }
    }
}