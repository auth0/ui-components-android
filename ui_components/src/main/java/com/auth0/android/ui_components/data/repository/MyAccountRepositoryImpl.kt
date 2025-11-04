package com.auth0.android.ui_components.data.repository

import com.auth0.android.myaccount.PhoneAuthenticationMethodType
import com.auth0.android.result.AuthenticationMethod
import com.auth0.android.result.Factor
import com.auth0.android.ui_components.data.MyAccountProvider
import com.auth0.android.ui_components.domain.mapper.toDomainModel
import com.auth0.android.ui_components.domain.model.MfaEnrollmentChallenge
import com.auth0.android.ui_components.domain.model.RecoveryCodeEnrollmentChallenge
import com.auth0.android.ui_components.domain.model.TotpEnrollmentChallenge
import com.auth0.android.ui_components.domain.repository.MyAccountRepository
import com.auth0.android.result.MfaEnrollmentChallenge as SdkMfaEnrollmentChallenge

/**
 * Repository that handles MyAccount API calls
 */
class MyAccountRepositoryImpl(private val myAccountProvider: MyAccountProvider) :
    MyAccountRepository {

    /**
     * Fetches factors using provided access token
     * @param accessToken Pre-fetched access token with required scopes
     * @return list of [Factor]
     */
    override suspend fun getFactors(accessToken: String): List<Factor> {
        val client = myAccountProvider.getMyAccount(accessToken)
        return client.getFactors().await()
    }

    /**
     * Fetches authentication methods using provided access token
     * @param accessToken Pre-fetched access token with required scopes
     * @return list of [AuthenticationMethod]
     */
    override suspend fun getAuthenticatorMethods(accessToken: String): List<AuthenticationMethod> {
        val client = myAccountProvider.getMyAccount(accessToken)
        return client.getAuthenticationMethods().await()
    }

    /**
     * Deletes an authentication method
     */
    override suspend fun deleteAuthenticationMethod(
        authenticationMethodId: String,
        accessToken: String
    ): Void? {
        val client = myAccountProvider.getMyAccount(accessToken)
        return client.deleteAuthenticationMethod(authenticationMethodId).await()
    }

    /**
     * Enrolls TOTP authenticator
     * @param accessToken Pre-fetched access token with required scopes
     * @return [TotpEnrollmentChallenge]  containing QR code and secret
     */
    override suspend fun enrollTotp(accessToken: String): TotpEnrollmentChallenge {
        val client = myAccountProvider.getMyAccount(accessToken)
        val sdkChallenge = client.enrollTotp().await()
        return sdkChallenge.toDomainModel()
    }

    /**
     * Enrolls Push Notification authenticator
     * @param accessToken Pre-fetched access token with required scopes
     * @return [TotpEnrollmentChallenge] domain model containing enrollment data
     */
    override suspend fun enrollPushNotification(accessToken: String): TotpEnrollmentChallenge {
        val client = myAccountProvider.getMyAccount(accessToken)
        val sdkChallenge = client.enrollPushNotification().await()
        return sdkChallenge.toDomainModel()
    }

    /**
     * Enrolls Recovery Code authenticator
     * @param accessToken Pre-fetched access token with required scopes
     * @return [RecoveryCodeEnrollmentChallenge] containing recovery codes
     */
    override suspend fun enrollRecoveryCode(accessToken: String): RecoveryCodeEnrollmentChallenge {
        val client = myAccountProvider.getMyAccount(accessToken)
        val sdkChallenge = client.enrollRecoveryCode().await()
        return sdkChallenge.toDomainModel()
    }

    /**
     * Enrolls Email authenticator
     * @param email Email address for authentication
     * @param accessToken Pre-fetched access token with required scopes
     * @return [MfaEnrollmentChallenge] containing enrollment session data
     */
    override suspend fun enrollEmail(
        email: String,
        accessToken: String
    ): MfaEnrollmentChallenge {
        val client = myAccountProvider.getMyAccount(accessToken)
        val sdkChallenge = client.enrollEmail(email).await() as SdkMfaEnrollmentChallenge
        return sdkChallenge.toDomainModel()
    }

    /**
     * Enrolls Phone authenticator
     * @param phoneNumber Phone number for authentication
     * @param preferredMethod SMS or Voice call method
     * @param accessToken Pre-fetched access token with required scopes
     * @return [MfaEnrollmentChallenge] containing enrollment session data
     */
    override suspend fun enrollPhone(
        phoneNumber: String,
        preferredMethod: PhoneAuthenticationMethodType,
        accessToken: String
    ): MfaEnrollmentChallenge {
        val client = myAccountProvider.getMyAccount(accessToken)
        val sdkChallenge =
            client.enrollPhone(phoneNumber, preferredMethod).await() as SdkMfaEnrollmentChallenge
        return sdkChallenge.toDomainModel()
    }

    /**
     * Verifies enrollment with OTP code
     * @param authenticationMethodId ID of the authentication method to verify
     * @param otpCode The OTP code entered by user
     * @param authSession Session token from enrollment
     * @param accessToken Pre-fetched access token with required scopes
     * @return Verified [AuthenticationMethod]
     * @throws Exception if API call fails or OTP is invalid
     */
    override suspend fun verifyOtp(
        authenticationMethodId: String,
        otpCode: String,
        authSession: String,
        accessToken: String
    ): AuthenticationMethod {
        val client = myAccountProvider.getMyAccount(accessToken)
        return client.verifyOtp(authenticationMethodId, otpCode, authSession).await()
    }

    /**
     * Verifies enrollment without OTP (e.g., push notification confirmation)
     * @param authenticationMethodId ID of the authentication method to verify
     * @param authSession Session token from enrollment
     * @param accessToken Pre-fetched access token with required scopes
     * @return Verified [AuthenticationMethod]
     */
    override suspend fun verifyWithoutOtp(
        authenticationMethodId: String,
        authSession: String,
        accessToken: String
    ): AuthenticationMethod {
        val client = myAccountProvider.getMyAccount(accessToken)
        return client.verify(authenticationMethodId, authSession).await()
    }
}
