package com.auth0.android.ui_components.presentation.ui.passkeys

import android.util.Log
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialInterruptedException
import androidx.credentials.exceptions.CreateCredentialProviderConfigurationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.android.ui_components.PasskeyConfiguration
import com.auth0.android.ui_components.domain.error.Auth0Error
import com.auth0.android.ui_components.domain.model.PublicKeyCredentials
import com.auth0.android.ui_components.domain.repository.MyAccountRepository
import com.auth0.android.ui_components.presentation.ui.UiError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Represents the different UI states for passkey enrollment
 */
sealed interface PasskeyUiState {
    data object Idle : PasskeyUiState
    data object UserCancelled : PasskeyUiState
    data object RequestingChallenge : PasskeyUiState
    data object CreatingPasskey : PasskeyUiState
    data object EnrollingPasskey : PasskeyUiState
    data class Error(val error: UiError, val shouldRetry: Boolean = true) : PasskeyUiState
}

/**
 * Represents events emitted during passkey enrollment flow
 */
sealed interface PasskeyEvent {

    /**
     * Emitted when passkey enrollment is successfully completed
     */
    data object EnrollmentSuccess : PasskeyEvent
}

/**
 * ViewModel for managing passkey enrollment flow
 *
 * This ViewModel handles:
 * - Initiating passkey enrollment to get the challenge
 * - Verifying the passkey credentials after user creates a passkey via Credential Manager
 */
class PasskeyViewModel(
    private val myAccountRepository: MyAccountRepository,
    private val passkeyConfiguration: PasskeyConfiguration
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
        createCredential: suspend (String) -> String,
    ) {
        viewModelScope.launch {
            try {
                _uiState.update {
                    PasskeyUiState.RequestingChallenge
                }
                val challenge =
                    myAccountRepository.enrollPasskey(
                        SCOPE,
                        userIdentity = passkeyConfiguration.userIdentity,
                        connection = passkeyConfiguration.connection
                    )

                _uiState.update {
                    PasskeyUiState.CreatingPasskey
                }
                val authParamsJson = Json.encodeToString(challenge.authParamsPublicKey)

                val registrationResponseJson = createCredential(authParamsJson)

                val publicKeyCredentials =
                    Json.decodeFromString<PublicKeyCredentials>(registrationResponseJson)
                _uiState.update {
                    PasskeyUiState.EnrollingPasskey
                }
                val result = myAccountRepository.verifyPasskey(
                    publicKeyCredentials,
                    challenge,
                    SCOPE
                )
                _uiState.update {
                    PasskeyUiState.Idle
                }
                eventChannel.send(PasskeyEvent.EnrollmentSuccess)
            } catch (exception: Auth0Error) {
                _uiState.update {
                    PasskeyUiState.Error(UiError(Auth0Error.Unknown(cause = exception), {
                        enrollPasskey(createCredential)
                    }))
                }
            } catch (exception: CreateCredentialException) {
                when (exception) {
                    is CreateCredentialCancellationException -> _uiState.update {
                        PasskeyUiState.UserCancelled
                    }

                    else -> {
                        val err = handleCreationFailure(exception)
                        _uiState.update {
                            PasskeyUiState.Error(UiError(err, {
                                enrollPasskey(createCredential)
                            }), err.shouldRetry)
                        }
                    }
                }
            }
        }
    }


    private fun handleCreationFailure(exception: CreateCredentialException): Auth0Error.PasskeyError {
        return when (exception) {

            is CreateCredentialInterruptedException -> {
                Auth0Error.PasskeyError(
                    "Passkey authentication was interrupted. Please retry again.",
                    exception,
                    shouldRetry = true
                )
            }

            is CreateCredentialProviderConfigurationException -> {
                Auth0Error.PasskeyError(
                    "Provider configuration dependency is missing. Ensure credentials-play-services-auth dependency is added.",
                    exception
                )
            }

            else -> {
                Log.w(TAG, "Unexpected exception type ${exception::class.java.name}")
                Auth0Error.PasskeyError(
                    "An error occurred when creating a passkey: ${exception.message ?: "Unknown error"}",
                    exception,
                )
            }
        }
    }
}