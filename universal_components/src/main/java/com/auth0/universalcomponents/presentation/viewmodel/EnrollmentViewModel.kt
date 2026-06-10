package com.auth0.universalcomponents.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.android.result.AuthenticationMethod
import com.auth0.universalcomponents.domain.error.Auth0Error
import com.auth0.universalcomponents.domain.model.AuthenticatorType
import com.auth0.universalcomponents.domain.model.EnrollmentInput
import com.auth0.universalcomponents.domain.model.EnrollmentResult
import com.auth0.universalcomponents.domain.model.VerificationInput
import com.auth0.universalcomponents.domain.network.onError
import com.auth0.universalcomponents.domain.network.onSuccess
import com.auth0.universalcomponents.domain.usecase.EnrollAuthenticatorUseCase
import com.auth0.universalcomponents.domain.usecase.GetUserInfoUseCase
import com.auth0.universalcomponents.domain.usecase.VerifyAuthenticatorUseCase
import com.auth0.universalcomponents.presentation.ui.UiError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Represent different UI state while enrolling an authenticator
 */
data class EnrollmentUiState(
    val enrollingAuthenticator: Boolean = false,
    val verifyingAuthenticator: Boolean = false,
    val otpError: Boolean = false,
    val uiError: UiError? = null,
    val prefillEmail: String = ""
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
    private val getUserInfoUseCase: GetUserInfoUseCase,
    authenticatorType: AuthenticatorType,
    startDefaultEnrollment: Boolean = true
) : ViewModel() {

    private companion object {
        private const val TAG = "EnrollmentViewModel"
    }

    private val eventChannel = Channel<EnrollmentEvent>()
    val events = eventChannel.receiveAsFlow()

    private val _uiState = MutableStateFlow(EnrollmentUiState())

    val uiState: StateFlow<EnrollmentUiState> = _uiState.asStateFlow()

    init {
        when (authenticatorType) {
            AuthenticatorType.RECOVERY_CODE,
            AuthenticatorType.PUSH,
            AuthenticatorType.TOTP -> {
                if (startDefaultEnrollment) {
                    startEnrollment(authenticatorType)
                }
            }

            AuthenticatorType.EMAIL -> {
                prefillEmail()
            }

            else -> {
                Log.d(TAG, "No need to fetch the data during initialization")
            }
        }
    }

    fun startEnrollment(
        authenticatorType: AuthenticatorType,
        input: EnrollmentInput = EnrollmentInput.None
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(enrollingAuthenticator = true, uiError = null)
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
                                onRetry = { startEnrollment(authenticatorType, input) }
                            )
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
                                    verifyingAuthenticator = false,
                                    uiError = UiError(
                                        error = error,
                                        onRetry = {
                                            verifyWithOtp(
                                                authenticationMethodId,
                                                otpCode,
                                                authSession
                                            )
                                        }
                                    )
                                )
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
                            )
                        )
                    }
                }
        }
    }

    private fun prefillEmail() {
        viewModelScope.launch {
            getUserInfoUseCase()
                .onSuccess { userInfo ->
                    Log.d(TAG, "Email prefill succeeded")
                    _uiState.update { it.copy(prefillEmail = userInfo.email.orEmpty()) }
                }
                .onError { error ->
                    Log.w(TAG, "Could not prefill email: ${error.message}")
                }
        }
    }
}
