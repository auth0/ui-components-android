package com.auth0.android.ui_components.domain.repository

import com.auth0.android.result.AuthenticationMethod
import com.auth0.android.result.Factor
import com.auth0.android.ui_components.domain.model.MfaEnrollmentChallenge
import com.auth0.android.ui_components.domain.model.PasskeyEnrollmentChallenge
import com.auth0.android.ui_components.domain.model.PublicKeyCredentials
import com.auth0.android.ui_components.domain.model.RecoveryCodeEnrollmentChallenge
import com.auth0.android.ui_components.domain.model.TotpEnrollmentChallenge

/**
 * Repository interface for MyAccount operations
 */
interface MyAccountRepository {
    suspend fun getFactors(scope: String): List<Factor>
    suspend fun getAuthenticatorMethods(
        scope: String
    ): List<AuthenticationMethod>

    suspend fun deleteAuthenticationMethod(
        authenticationMethodId: String,
        scope: String
    ): Void?

    suspend fun enrollTotp(scope: String): TotpEnrollmentChallenge

    suspend fun enrollPushNotification(scope: String): TotpEnrollmentChallenge

    suspend fun enrollRecoveryCode(
        scope: String
    ): RecoveryCodeEnrollmentChallenge

    suspend fun enrollEmail(
        email: String,
        scope: String
    ): MfaEnrollmentChallenge

    suspend fun enrollPhone(
        phoneNumber: String,
        scope: String
    ): MfaEnrollmentChallenge

    suspend fun enrollPasskey(
        scope: String,
        userIdentity: String? = null,
        connection: String? = null
    ): PasskeyEnrollmentChallenge

    suspend fun verifyOtp(
        authenticationMethodId: String,
        otpCode: String,
        authSession: String,
        scope: String
    ): AuthenticationMethod

    suspend fun verifyWithoutOtp(
        authenticationMethodId: String,
        authSession: String,
        scope: String
    ): AuthenticationMethod

    suspend fun verifyPasskey(
        publicKeyCredentials: PublicKeyCredentials,
        challenge: PasskeyEnrollmentChallenge,
        scope: String
    ): AuthenticationMethod
}
