package com.auth0.android.ui_components.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.android.result.AuthenticationMethod
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.model.EnrollmentInput
import com.auth0.android.ui_components.domain.model.EnrollmentResult
import com.auth0.android.ui_components.domain.model.VerificationInput
import com.auth0.android.ui_components.domain.usecase.EnrollAuthenticatorUseCase
import com.auth0.android.ui_components.domain.usecase.VerifyAuthenticatorUseCase
import com.auth0.android.ui_components.domain.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for authenticator enrollment flow
 * Handles enrollment initiation and verification for all authenticator types
 * Designed to be reused across TOTP, Push, Email, SMS, Recovery Code screens
 */
class EnrollmentViewModel(
    private val enrollAuthenticatorUseCase: EnrollAuthenticatorUseCase,
    private val verifyAuthenticatorUseCase: VerifyAuthenticatorUseCase
) : ViewModel() {

    private companion object {
        private const val TAG = "EnrollmentViewModel"
    }

    private val _uiState = MutableStateFlow<EnrollmentUiState>(EnrollmentUiState.Idle)
    val uiState: StateFlow<EnrollmentUiState> = _uiState.asStateFlow()

    /**
     * Initiates enrollment for specified authenticator type
     * @param authenticatorType Type of authenticator to enroll
     * @param input Additional input (email/phone) if required
     */
    fun startEnrollment(
        authenticatorType: AuthenticatorType,
        input: EnrollmentInput = EnrollmentInput.None
    ) {
        viewModelScope.launch {
            _uiState.value = EnrollmentUiState.Loading
            Log.d(TAG, "Starting enrollment: $authenticatorType")

            when (val result = enrollAuthenticatorUseCase(authenticatorType, input)) {
                is Result.Success -> {
                    Log.d(TAG, "Enrollment initiated successfully")
                    _uiState.value = EnrollmentUiState.EnrollmentInitiated(result.data)
                }

                is Result.Error -> {
                    Log.e(TAG, "Enrollment failed", result.exception)
                    _uiState.value = EnrollmentUiState.Error(result.exception)
                }
            }
        }
    }

    /**
     * Verifies the enrolled authenticator with OTP
     * @param authenticationMethodId ID from enrollment challenge
     * @param otpCode OTP code entered by user
     * @param authSession Session token from enrollment
     */
    fun verifyWithOtp(
        authenticationMethodId: String,
        otpCode: String,
        authSession: String
    ) {
        viewModelScope.launch {
            _uiState.value = EnrollmentUiState.Verifying
            Log.d(TAG, "Verifying with OTP")

            val input = VerificationInput.WithOtp(
                authenticationMethodId = authenticationMethodId,
                otpCode = otpCode,
                authSession = authSession
            )

            when (val result = verifyAuthenticatorUseCase(input)) {
                is Result.Success -> {
                    Log.d(TAG, "Verification successful")
                    _uiState.value = EnrollmentUiState.Success(result.data)
                }

                is Result.Error -> {
                    Log.e(TAG, "Verification failed", result.exception)
                    _uiState.value = EnrollmentUiState.Error(result.exception)
                }
            }
        }
    }

    /**
     * Verifies the enrolled authenticator without OTP (push notification)
     * @param authenticationMethodId ID from enrollment challenge
     * @param authSession Session token from enrollment
     */
    fun verifyWithoutOtp(
        authenticationMethodId: String,
        authSession: String
    ) {
        viewModelScope.launch {
            _uiState.value = EnrollmentUiState.Verifying
            Log.d(TAG, "Verifying without OTP")

            val input = VerificationInput.WithoutOtp(
                authenticationMethodId = authenticationMethodId,
                authSession = authSession
            )

            when (val result = verifyAuthenticatorUseCase(input)) {
                is Result.Success -> {
                    Log.d(TAG, "Verification successful")
                    _uiState.value = EnrollmentUiState.Success(result.data)
                }

                is Result.Error -> {
                    Log.e(TAG, "Verification failed", result.exception)
                    _uiState.value = EnrollmentUiState.Error(result.exception)
                }
            }
        }
    }

    /**
     * Resets the state to idle (useful for retry or navigation)
     */
    fun resetState() {
        _uiState.value = EnrollmentUiState.Idle
    }
}

/**
 * UI State for enrollment flow
 */
sealed interface EnrollmentUiState {
    /** Initial state - no operation started */
    object Idle : EnrollmentUiState

    /** Loading state during enrollment API call */
    object Loading : EnrollmentUiState

    /** Enrollment initiated successfully - show challenge data to user */
    data class EnrollmentInitiated(val enrollmentResult: EnrollmentResult) : EnrollmentUiState

    /** Verifying the OTP or confirmation */
    object Verifying : EnrollmentUiState

    /** Enrollment and verification completed successfully */
    data class Success(val authenticationMethod: AuthenticationMethod) : EnrollmentUiState

    /** Error occurred during enrollment or verification */
    data class Error(val exception: Throwable) : EnrollmentUiState
}
