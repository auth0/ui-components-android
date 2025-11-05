package com.auth0.android.ui_components.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.android.result.AuthenticationMethod
import com.auth0.android.ui_components.domain.error.Auth0Error
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.model.EnrollmentInput
import com.auth0.android.ui_components.domain.model.EnrollmentResult
import com.auth0.android.ui_components.domain.model.VerificationInput
import com.auth0.android.ui_components.domain.network.onError
import com.auth0.android.ui_components.domain.network.onSuccess
import com.auth0.android.ui_components.domain.usecase.EnrollAuthenticatorUseCase
import com.auth0.android.ui_components.domain.usecase.VerifyAuthenticatorUseCase
import com.auth0.android.ui_components.presentation.ui.UiError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


/**
 * Represent different UI state while enrolling an authenticator
 */
data class EnrollmentUiState(
    val enrollingAuthenticator: Boolean = false,
    val verifyingAuthenticator: Boolean = false,
    val otpError: Boolean = false,
    val uiError: UiError? = null
)

/**
 * Represent  events while enrolling an authenticator
 */
sealed interface EnrollmentEvent {

    data class EnrollmentChallengeSuccess(
        val enrollmentResult: EnrollmentResult,
        val authenticationMethodId: String,
        val authSession: String
    ) : EnrollmentEvent

    data class VerificationSuccess(
        val authenticationMethod: AuthenticationMethod
    ) : EnrollmentEvent
}


class EnrollmentViewModel(
    private val enrollAuthenticatorUseCase: EnrollAuthenticatorUseCase,
    private val verifyAuthenticatorUseCase: VerifyAuthenticatorUseCase,
    authenticatorType: AuthenticatorType,
    startDefaultEnrollment: Boolean = true
) : ViewModel() {

    private companion object {
        private const val TAG = "EnrollmentViewModel"
    }

    private val eventChannel = Channel<EnrollmentEvent>()
    val events = eventChannel.receiveAsFlow()

    private val _uiState = MutableStateFlow(EnrollmentUiState())

    val uiState: StateFlow<EnrollmentUiState> = _uiState
        .onStart {
            when (authenticatorType) {
                AuthenticatorType.RECOVERY_CODE,
                AuthenticatorType.PUSH,
                AuthenticatorType.TOTP -> {
                    if (startDefaultEnrollment)
                        startEnrollment(authenticatorType)
                }

                else -> {
                    Log.d(TAG, "No need to fetch the data during initialization")
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = when (authenticatorType) {
                AuthenticatorType.RECOVERY_CODE,
                AuthenticatorType.PUSH,
                AuthenticatorType.TOTP -> {
                    if (startDefaultEnrollment)
                        EnrollmentUiState(enrollingAuthenticator = true)
                    else EnrollmentUiState()
                }

                else -> EnrollmentUiState()
            }
        )

    fun startEnrollment(
        authenticatorType: AuthenticatorType,
        input: EnrollmentInput = EnrollmentInput.None
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(enrollingAuthenticator = true)
            }
            enrollAuthenticatorUseCase(authenticatorType, input)
                .onSuccess { enrollmentResult ->
                    Log.d(TAG, "Enrollment initiated successfully")

                    val (authMethodId, authSession) = when (enrollmentResult) {
                        is EnrollmentResult.RecoveryCodeEnrollment ->
                            enrollmentResult.authenticationMethodId to enrollmentResult.authSession

                        is EnrollmentResult.TotpEnrollment ->
                            enrollmentResult.authenticationMethodId to enrollmentResult.authSession

                        is EnrollmentResult.DefaultEnrollment ->
                            enrollmentResult.authenticationMethodId to enrollmentResult.authSession
                    }

                    eventChannel.send(
                        EnrollmentEvent.EnrollmentChallengeSuccess(
                            enrollmentResult = enrollmentResult,
                            authenticationMethodId = authMethodId,
                            authSession = authSession
                        )
                    )

                    _uiState.update {
                        it.copy(enrollingAuthenticator = false)
                    }
                }
                .onError { error ->
                    _uiState.update {
                        EnrollmentUiState(
                            uiError = UiError(
                                error,
                                onRetry = { startEnrollment(authenticatorType, input) })
                        )
                    }
                }
        }
    }

    fun verifyWithOtp(
        authenticationMethodId: String,
        otpCode: String,
        authSession: String
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(verifyingAuthenticator = true)
            }
            Log.d(TAG, "Verifying with OTP")

            val input = VerificationInput.WithOtp(
                authenticationMethodId = authenticationMethodId,
                otpCode = otpCode,
                authSession = authSession
            )

            verifyAuthenticatorUseCase(input)
                .onSuccess { authenticationMethod ->
                    Log.d(TAG, "Verification successful")

                    _uiState.update {
                        EnrollmentUiState()
                    }
                    eventChannel.send(EnrollmentEvent.VerificationSuccess(authenticationMethod))
                }
                .onError { error ->
                    Log.e(TAG, "Verification failed", error.cause)
                    when (error) {
                        is Auth0Error.InvalidOTP -> {
                            _uiState.update {
                                it.copy(
                                    verifyingAuthenticator = false,
                                    otpError = true,
                                    uiError = null
                                )
                            }
                        }

                        else -> {
                            _uiState.update {
                                it.copy(
                                    verifyingAuthenticator = false, uiError = UiError(
                                        error = error,
                                        onRetry = {
                                            verifyWithOtp(
                                                authenticationMethodId,
                                                otpCode,
                                                authSession
                                            )
                                        }
                                    ))
                            }
                        }
                    }
                }
        }
    }


    fun verifyWithoutOtp(
        authenticationMethodId: String,
        authSession: String
    ) {
        viewModelScope.launch {
            _uiState.update {
                EnrollmentUiState(verifyingAuthenticator = true)
            }
            Log.d(TAG, "Verifying without OTP")

            val input = VerificationInput.WithoutOtp(
                authenticationMethodId = authenticationMethodId,
                authSession = authSession
            )

            verifyAuthenticatorUseCase(input)
                .onSuccess { authenticationMethod ->
                    Log.d(TAG, "Verification successful")
                    _uiState.update {
                        EnrollmentUiState()
                    }
                    eventChannel.send(EnrollmentEvent.VerificationSuccess(authenticationMethod))
                }
                .onError { error ->
                    Log.e(TAG, "Verification failed", error.cause)
                    _uiState.update {
                        EnrollmentUiState(
                            uiError = UiError(
                                error = error,
                                onRetry = { verifyWithoutOtp(authenticationMethodId, authSession) }
                            ))
                    }
                }
        }
    }
}

