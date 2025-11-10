package com.auth0.android.ui_components.data.repository

import com.auth0.android.myaccount.PhoneAuthenticationMethodType
import com.auth0.android.result.AuthenticationMethod
import com.auth0.android.result.Factor
import com.auth0.android.ui_components.data.MyAccountProvider
import com.auth0.android.ui_components.data.TokenManager
import com.auth0.android.ui_components.data.mapper.toDomainModel
import com.auth0.android.ui_components.data.network.withErrorMapping
import com.auth0.android.ui_components.domain.model.MfaEnrollmentChallenge
import com.auth0.android.ui_components.domain.model.RecoveryCodeEnrollmentChallenge
import com.auth0.android.ui_components.domain.model.TotpEnrollmentChallenge
import com.auth0.android.ui_components.domain.repository.MyAccountRepository
import com.auth0.android.result.MfaEnrollmentChallenge as SdkMfaEnrollmentChallenge

/**
 * Repository that handles MyAccount API calls
 */
class MyAccountRepositoryImpl(
    private val myAccountProvider: MyAccountProvider,
    val tokenManager: TokenManager
) : MyAccountRepository {

    /**
     * Fetches factors using provided access token
     * @param scope Scopes required for the factors api
     * @return list of [Factor]
     */
    override suspend fun getFactors(scope: String): List<Factor> {
        return withErrorMapping(scope) {
            val audience = tokenManager.getMyAccountAudience()
            val accessToken = tokenManager.fetchToken(
                audience = audience,
                scope = scope
            )
            val client = myAccountProvider.getMyAccount(accessToken)
            client.getFactors().await()
        }
    }

    /**
     * Fetches authentication methods using provided access token
     * @param scope Required scopes for the authentication methods api
     * @return list of [AuthenticationMethod]
     */
    override suspend fun getAuthenticatorMethods(
        scope: String
    ): List<AuthenticationMethod> {
        return withErrorMapping(scope) {
            val audience = tokenManager.getMyAccountAudience()
            val accessToken = tokenManager.fetchToken(
                audience = audience,
                scope = scope
            )
            val client = myAccountProvider.getMyAccount(accessToken)
            client.getAuthenticationMethods().await()
        }
    }

    /**
     * Deletes an authentication method
     * @param scope Required scope for the delete api
     */
    override suspend fun deleteAuthenticationMethod(
        authenticationMethodId: String,
        scope: String
    ): Void? {
        return withErrorMapping(scope) {
            val audience = tokenManager.getMyAccountAudience()
            val accessToken = tokenManager.fetchToken(
                audience = audience,
                scope = scope
            )
            val client = myAccountProvider.getMyAccount(accessToken)
            client.deleteAuthenticationMethod(authenticationMethodId).await()
        }

    }

    /**
     * Enrolls TOTP authenticator
     * @param scope Required scope for the enroll api
     * @return [TotpEnrollmentChallenge]  containing QR code and secret
     */
    override suspend fun enrollTotp(scope: String): TotpEnrollmentChallenge {
        return withErrorMapping(scope) {
            val audience = tokenManager.getMyAccountAudience()
            val accessToken = tokenManager.fetchToken(
                audience = audience,
                scope = scope
            )
            val client = myAccountProvider.getMyAccount(accessToken)
            val sdkChallenge = client.enrollTotp().await()
            sdkChallenge.toDomainModel()
        }
    }

    /**
     * Enrolls Push Notification authenticator
     * @param scope Required scope for the enroll api
     * @return [TotpEnrollmentChallenge] domain model containing enrollment data
     */
    override suspend fun enrollPushNotification(
        scope: String
    ): TotpEnrollmentChallenge {
        return withErrorMapping(scope) {
            val audience = tokenManager.getMyAccountAudience()
            val accessToken = tokenManager.fetchToken(
                audience = audience,
                scope = scope
            )
            val client = myAccountProvider.getMyAccount(accessToken)
            val sdkChallenge = client.enrollPushNotification().await()
            sdkChallenge.toDomainModel()
        }
    }

    /**
     * Enrolls Recovery Code authenticator
     * @param scope Required scope for the enroll api
     * @return [RecoveryCodeEnrollmentChallenge] containing recovery codes
     */
    override suspend fun enrollRecoveryCode(
        scope: String
    ): RecoveryCodeEnrollmentChallenge {
        return withErrorMapping(scope) {
            val audience = tokenManager.getMyAccountAudience()
            val accessToken = tokenManager.fetchToken(
                audience = audience,
                scope = scope
            )
            val client = myAccountProvider.getMyAccount(accessToken)
            val sdkChallenge = client.enrollRecoveryCode().await()
            sdkChallenge.toDomainModel()
        }
    }

    /**
     * Enrolls Email authenticator
     * @param email Email address for authentication
     * @param scope Required scope for the enroll api
     * @return [MfaEnrollmentChallenge] containing enrollment session data
     */
    override suspend fun enrollEmail(
        email: String,
        scope: String
    ): MfaEnrollmentChallenge {
        return withErrorMapping(scope) {
            val audience = tokenManager.getMyAccountAudience()
            val accessToken = tokenManager.fetchToken(
                audience = audience,
                scope = scope
            )
            val client = myAccountProvider.getMyAccount(accessToken)
            val sdkChallenge = client.enrollEmail(email).await() as SdkMfaEnrollmentChallenge
            sdkChallenge.toDomainModel()
        }
    }

    /**
     * Enrolls Phone authenticator
     * @param phoneNumber Phone number for authentication
     * @param scope Required scope for the enroll api
     * @return [MfaEnrollmentChallenge] containing enrollment session data
     */
    override suspend fun enrollPhone(
        phoneNumber: String,
        scope: String
    ): MfaEnrollmentChallenge {
        return withErrorMapping(scope) {
            val audience = tokenManager.getMyAccountAudience()
            val accessToken = tokenManager.fetchToken(
                audience = audience,
                scope = scope
            )
            val client = myAccountProvider.getMyAccount(accessToken)
            val sdkChallenge =
                client.enrollPhone(phoneNumber, PhoneAuthenticationMethodType.SMS)
                    .await() as SdkMfaEnrollmentChallenge
            sdkChallenge.toDomainModel()
        }
    }

    /**
     * Verifies enrollment with OTP code
     * @param authenticationMethodId ID of the authentication method to verify
     * @param otpCode The OTP code entered by user
     * @param authSession Session token from enrollment
     * @param scope Required scope for the verify api
     * @return Verified [AuthenticationMethod]
     */
    override suspend fun verifyOtp(
        authenticationMethodId: String,
        otpCode: String,
        authSession: String,
        scope: String
    ): AuthenticationMethod {
        return withErrorMapping(scope) {
            val audience = tokenManager.getMyAccountAudience()
            val accessToken = tokenManager.fetchToken(
                audience = audience,
                scope = scope
            )
            val client = myAccountProvider.getMyAccount(accessToken)
            client.verifyOtp(authenticationMethodId, otpCode, authSession).await()
        }
    }

    /**
     * Verifies enrollment without OTP (e.g., push notification confirmation)
     * @param authenticationMethodId ID of the authentication method to verify
     * @param authSession Session token from enrollment
     * @param scope Required scope for the verify api
     * @return Verified [AuthenticationMethod]
     */
    override suspend fun verifyWithoutOtp(
        authenticationMethodId: String,
        authSession: String,
        scope: String
    ): AuthenticationMethod {
        return withErrorMapping(scope) {
            val audience = tokenManager.getMyAccountAudience()
            val accessToken = tokenManager.fetchToken(
                audience = audience,
                scope = scope
            )
            val client = myAccountProvider.getMyAccount(accessToken)
            client.verify(authenticationMethodId, authSession).await()
        }
    }
}
