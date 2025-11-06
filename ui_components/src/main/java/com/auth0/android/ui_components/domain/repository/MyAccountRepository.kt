package com.auth0.android.ui_components.domain.repository

import com.auth0.android.myaccount.PhoneAuthenticationMethodType
import com.auth0.android.result.AuthenticationMethod
import com.auth0.android.result.Factor
import com.auth0.android.ui_components.domain.model.MfaEnrollmentChallenge
import com.auth0.android.ui_components.domain.model.RecoveryCodeEnrollmentChallenge
import com.auth0.android.ui_components.domain.model.TotpEnrollmentChallenge

/**
 * Repository interface for MyAccount operations
 */
interface MyAccountRepository {
    suspend fun getFactors(accessToken: String): List<Factor>
    suspend fun getAuthenticatorMethods(accessToken: String): List<AuthenticationMethod>
    suspend fun deleteAuthenticationMethod(
        authenticationMethodId: String, accessToken: String
    ): Void?

    suspend fun enrollTotp(accessToken: String): TotpEnrollmentChallenge

    suspend fun enrollPushNotification(accessToken: String): TotpEnrollmentChallenge

    suspend fun enrollRecoveryCode(accessToken: String): RecoveryCodeEnrollmentChallenge

    suspend fun enrollEmail(email: String, accessToken: String): MfaEnrollmentChallenge

    suspend fun enrollPhone(
        phoneNumber: String,
        preferredMethod: PhoneAuthenticationMethodType,
        accessToken: String
    ): MfaEnrollmentChallenge

    suspend fun verifyOtp(
        authenticationMethodId: String,
        otpCode: String,
        authSession: String,
        accessToken: String
    ): AuthenticationMethod

    suspend fun verifyWithoutOtp(
        authenticationMethodId: String,
        authSession: String,
        accessToken: String
    ): AuthenticationMethod
}
