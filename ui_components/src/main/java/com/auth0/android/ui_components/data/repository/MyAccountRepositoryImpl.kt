package com.auth0.android.ui_components.data.repository

import android.util.Log
import com.auth0.android.myaccount.PhoneAuthenticationMethodType
import com.auth0.android.result.AuthenticationMethod
import com.auth0.android.result.Factor
import com.auth0.android.result.MfaEnrollmentChallenge
import com.auth0.android.result.RecoveryCodeEnrollmentChallenge
import com.auth0.android.result.TotpEnrollmentChallenge
import com.auth0.android.ui_components.data.MyAccountProvider
import com.auth0.android.ui_components.domain.repository.MyAccountRepository

/**
 * Repository that handles MyAccount API calls
 * Receives access token from caller - does NOT fetch tokens
 */
class MyAccountRepositoryImpl(private val myAccountProvider: MyAccountProvider) :
    MyAccountRepository {

    companion object {
        private const val TAG = "MyAccountRepository"
    }

    /**
     * Fetches factors using provided access token
     * @param accessToken Pre-fetched access token with required scopes
     * @throws Exception if API call fails
     */
    override suspend fun getFactors(accessToken: String): List<Factor> {
        Log.d(TAG, "Fetching factors")
        val client = myAccountProvider.getMyAccount(accessToken)
        return client.getFactors().await()
    }

    /**
     * Fetches authentication methods using provided access token
     * @param accessToken Pre-fetched access token with required scopes
     * @throws Exception if API call fails
     */
    override suspend fun getAuthenticatorMethods(accessToken: String): List<AuthenticationMethod> {
        Log.d(TAG, "Fetching authentication methods")
        val client = myAccountProvider.getMyAccount(accessToken)
        return client.getAuthenticationMethods().await()
    }

    /**
     * Deletes an authentication method
     *
     */
    override suspend fun deleteAuthenticationMethod(
        authenticationMethodId: String,
        accessToken: String
    ): Void? {
        Log.d(TAG, "Deleting authentication method $authenticationMethodId")
        val client = myAccountProvider.getMyAccount(accessToken)
        return client.deleteAuthenticationMethod(authenticationMethodId).await()
    }

    /**
     * Enrolls TOTP authenticator
     * @param accessToken Pre-fetched access token with required scopes
     * @return TotpEnrollmentChallenge containing QR code and secret
     * @throws Exception if API call fails
     */
    override suspend fun enrollTotp(accessToken: String): TotpEnrollmentChallenge {
        Log.d(TAG, "Enrolling TOTP authenticator")
        val client = myAccountProvider.getMyAccount(accessToken)
        return client.enrollTotp().await()
    }

    /**
     * Enrolls Push Notification authenticator
     * @param accessToken Pre-fetched access token with required scopes
     * @return TotpEnrollmentChallenge containing enrollment data
     * @throws Exception if API call fails
     */
    override suspend fun enrollPushNotification(accessToken: String): TotpEnrollmentChallenge {
        Log.d(TAG, "Enrolling Push Notification authenticator")
        val client = myAccountProvider.getMyAccount(accessToken)
        return client.enrollPushNotification().await()
    }

    /**
     * Enrolls Recovery Code authenticator
     * @param accessToken Pre-fetched access token with required scopes
     * @return RecoveryCodeEnrollmentChallenge containing recovery codes
     * @throws Exception if API call fails
     */
    override suspend fun enrollRecoveryCode(accessToken: String): RecoveryCodeEnrollmentChallenge {
        Log.d(TAG, "Enrolling Recovery Code authenticator")
        val client = myAccountProvider.getMyAccount(accessToken)
        return client.enrollRecoveryCode().await()
    }

    /**
     * Enrolls Email authenticator
     * @param email Email address for authentication
     * @param accessToken Pre-fetched access token with required scopes
     * @return EnrollmentChallenge containing enrollment session data
     * @throws Exception if API call fails
     */
    override suspend fun enrollEmail(
        email: String,
        accessToken: String
    ): MfaEnrollmentChallenge {
        Log.d(TAG, "Enrolling Email authenticator: $email")
        val client = myAccountProvider.getMyAccount(accessToken)
        return client.enrollEmail(email).await() as MfaEnrollmentChallenge
    }

    /**
     * Enrolls Phone authenticator
     * @param phoneNumber Phone number for authentication
     * @param preferredMethod SMS or Voice call method
     * @param accessToken Pre-fetched access token with required scopes
     * @return EnrollmentChallenge containing enrollment session data
     * @throws Exception if API call fails
     */
    override suspend fun enrollPhone(
        phoneNumber: String,
        preferredMethod: PhoneAuthenticationMethodType,
        accessToken: String
    ): MfaEnrollmentChallenge {
        Log.d(TAG, "Enrolling Phone authenticator: $phoneNumber with method: $preferredMethod")
        val client = myAccountProvider.getMyAccount(accessToken)
        return client.enrollPhone(phoneNumber, preferredMethod).await() as MfaEnrollmentChallenge
    }

    /**
     * Verifies enrollment with OTP code
     * @param authenticationMethodId ID of the authentication method to verify
     * @param otpCode The OTP code entered by user
     * @param authSession Session token from enrollment
     * @param accessToken Pre-fetched access token with required scopes
     * @return Verified AuthenticationMethod
     * @throws Exception if API call fails or OTP is invalid
     */
    override suspend fun verifyOtp(
        authenticationMethodId: String,
        otpCode: String,
        authSession: String,
        accessToken: String
    ): AuthenticationMethod {
        Log.d(TAG, "Verifying OTP for authentication method: $authenticationMethodId")
        val client = myAccountProvider.getMyAccount(accessToken)
        return client.verifyOtp(authenticationMethodId, otpCode, authSession).await()
    }

    /**
     * Verifies enrollment without OTP (e.g., push notification confirmation)
     * @param authenticationMethodId ID of the authentication method to verify
     * @param authSession Session token from enrollment
     * @param accessToken Pre-fetched access token with required scopes
     * @return Verified AuthenticationMethod
     * @throws Exception if API call fails
     */
    override suspend fun verifyWithoutOtp(
        authenticationMethodId: String,
        authSession: String,
        accessToken: String
    ): AuthenticationMethod {
        Log.d(TAG, "Verifying authentication method: $authenticationMethodId")
        val client = myAccountProvider.getMyAccount(accessToken)
        return client.verify(authenticationMethodId, authSession).await()
    }
}
