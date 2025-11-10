package com.auth0.android.ui_components.data.repository

import com.auth0.android.myaccount.MyAccountAPIClient
import com.auth0.android.myaccount.MyAccountException
import com.auth0.android.myaccount.PhoneAuthenticationMethodType
import com.auth0.android.result.AuthenticationMethod
import com.auth0.android.result.EnrollmentChallenge
import com.auth0.android.result.Factor
import com.auth0.android.result.RecoveryCodeEnrollmentChallenge
import com.auth0.android.result.TotpEnrollmentChallenge
import com.auth0.android.ui_components.TestData
import com.auth0.android.ui_components.data.FakeRequestImpl
import com.auth0.android.ui_components.data.MyAccountProvider
import com.auth0.android.ui_components.data.TokenManager
import com.auth0.android.ui_components.domain.error.Auth0Error
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class MyAccountRepositoryImplTest {

    private lateinit var myAccountProvider: MyAccountProvider
    private lateinit var myAccountClient: MyAccountAPIClient
    private lateinit var tokenManager: TokenManager
    private lateinit var repository: MyAccountRepositoryImpl

    @Before
    fun setup() {
        myAccountProvider = mockk()
        myAccountClient = mockk()
        tokenManager = mockk()
        every { myAccountProvider.getMyAccount(any()) } returns myAccountClient
        every { tokenManager.getMyAccountAudience() } returns "http://myaccount.auth0.com/me"
        coEvery { tokenManager.fetchToken(any(), any()) } returns "valid_access_token"
        repository = MyAccountRepositoryImpl(myAccountProvider, tokenManager)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `getFactors - successful response - returns list of factors`() = runTest {
        val scope = "scope:factors"
        val expectedFactors = TestData.sampleFactorList

        val request = FakeRequestImpl<List<Factor>, MyAccountException>(response = expectedFactors)
        every { myAccountClient.getFactors() } returns request

        val result = repository.getFactors(scope)

        assertThat(result).isEqualTo(expectedFactors)
        assertThat(result).hasSize(2)
        coVerify(exactly = 1) { tokenManager.fetchToken(any(), eq(scope)) }
        coVerify(exactly = 1) { myAccountClient.getFactors() }
    }

    @Test
    fun `getFactors - API call fails - throws Auth0Error`() {
        val scope = "scope:factors"
        val expectedException = MyAccountException("Unknown error")

        val request =
            FakeRequestImpl<List<Factor>, MyAccountException>(exception = expectedException)
        every { myAccountClient.getFactors() } returns request

        val exception = assertThrows(Auth0Error::class.java) {
            runTest {
                repository.getFactors(scope)
            }
        }
        assertThat(exception.message).isEqualTo("Received error with code a0.sdk.internal_error.unknown")
    }

    @Test
    fun `getFactors - empty list returned - returns empty list successfully`() = runTest {
        val scope = "scope:factors"
        val emptyFactors = emptyList<Factor>()

        val request = FakeRequestImpl<List<Factor>, MyAccountException>(response = emptyFactors)
        every { myAccountClient.getFactors() } returns request

        val result = repository.getFactors(scope)
        assertThat(result).isEmpty()
    }


    @Test
    fun `getAuthenticatorMethods - valid access token - returns list of authentication methods`() =
        runTest {
            val scope = "scope:authentication_methods"
            val expectedMethods = listOf(
                TestData.phoneAuthMethod,
                TestData.totpAuthMethod
            )

            val request =
                FakeRequestImpl<List<AuthenticationMethod>, MyAccountException>(response = expectedMethods)
            every { myAccountClient.getAuthenticationMethods() } returns request

            val result = repository.getAuthenticatorMethods(scope)

            assertThat(result).isEqualTo(expectedMethods)
            assertThat(result).hasSize(2)
            coVerify(exactly = 1) { myAccountClient.getAuthenticationMethods() }
        }

    @Test
    fun `getAuthenticatorMethods - API call fails - throws Auth0Error`() {
        val scope = "scope:authentication_methods"
        val expectedException = MyAccountException("API error")

        val request =
            FakeRequestImpl<List<AuthenticationMethod>, MyAccountException>(exception = expectedException)
        every { myAccountClient.getAuthenticationMethods() } returns request

        val exception = assertThrows(Auth0Error::class.java) {
            runTest {
                repository.getAuthenticatorMethods(scope)
            }
        }
        assertThat(exception.message).isEqualTo("Received error with code a0.sdk.internal_error.unknown")
    }

    @Test
    fun `getAuthenticatorMethods - empty list returned - returns empty list successfully`() =
        runTest {
            val scope = "scope:authentication_methods"
            val emptyMethods = emptyList<AuthenticationMethod>()

            val request =
                FakeRequestImpl<List<AuthenticationMethod>, MyAccountException>(response = emptyMethods)
            every { myAccountClient.getAuthenticationMethods() } returns request

            val result = repository.getAuthenticatorMethods(scope)

            assertThat(result).isEmpty()
        }


    @Test
    fun `deleteAuthenticationMethod - valid method ID and token - returns null successfully`() =
        runTest {
            val methodId = "auth_method_123"
            val scope = "scope:authentication_methods"

            val request = FakeRequestImpl<Void?, MyAccountException>(response = null)
            every { myAccountClient.deleteAuthenticationMethod(methodId) } returns request

            repository.deleteAuthenticationMethod(methodId, scope)
            coVerify(exactly = 1) { myAccountClient.deleteAuthenticationMethod(methodId) }
        }

    @Test
    fun `deleteAuthenticationMethod - API call fails - throws Auth0Error`() {
        val methodId = "auth_method_123"
        val scope = "scope:authentication_methods"
        val expectedException = MyAccountException("Deletion failed")

        val request = FakeRequestImpl<Void?, MyAccountException>(exception = expectedException)
        every { myAccountClient.deleteAuthenticationMethod(methodId) } returns request


        val exception = assertThrows(Auth0Error::class.java) {
            runTest {
                repository.deleteAuthenticationMethod(methodId, scope)
            }
        }
        assertThat(exception.message).isEqualTo("Received error with code a0.sdk.internal_error.unknown")
    }

    @Test
    fun `enrollTotp - valid access token - returns TotpEnrollmentChallenge`() = runTest {
        val scope = "scope:authentication_methods"

        val request =
            FakeRequestImpl<TotpEnrollmentChallenge, MyAccountException>(response = TestData.totpEnrollmentChallenge)
        every { myAccountClient.enrollTotp() } returns request

        val result = repository.enrollTotp(scope)

        assertThat(result.id).isEqualTo("totp_id_123")
        assertThat(result.barcodeUri).isEqualTo("otpauth://totp/...")
        coVerify(exactly = 1) { myAccountClient.enrollTotp() }
    }

    @Test
    fun `enrollTotp - API call fails - throws Auth0Error`() {
        val scope = "scope:authentication_methods"
        val expectedException = MyAccountException("Unknown error")

        val request =
            FakeRequestImpl<TotpEnrollmentChallenge, MyAccountException>(exception = expectedException)
        every { myAccountClient.enrollTotp() } returns request

        val exception = assertThrows(Auth0Error::class.java) {
            runTest {
                repository.enrollTotp(scope)
            }
        }
        assertThat(exception.message).isEqualTo("Received error with code a0.sdk.internal_error.unknown")
    }


    @Test
    fun `enrollPushNotification - valid access token - returns TotpEnrollmentChallenge`() =
        runTest {
            val scope = "scope:authentication_methods"

            val request =
                FakeRequestImpl<TotpEnrollmentChallenge, MyAccountException>(response = TestData.pushEnrollmentChallenge)
            every { myAccountClient.enrollPushNotification() } returns request

            val result = repository.enrollPushNotification(scope)

            assertThat(result.id).isEqualTo("push_id_123")
            coVerify(exactly = 1) { myAccountClient.enrollPushNotification() }
        }

    @Test
    fun `enrollPushNotification - API call fails - throws Auth0Error`() {
        val scope = "scope:authentication_methods"
        val expectedException = MyAccountException("Push enrollment failed")

        val request =
            FakeRequestImpl<TotpEnrollmentChallenge, MyAccountException>(exception = expectedException)
        every { myAccountClient.enrollPushNotification() } returns request


        val exception = assertThrows(Auth0Error::class.java) {
            runTest {
                repository.enrollPushNotification(scope)
            }
        }
        assertThat(exception.message).isEqualTo("Received error with code a0.sdk.internal_error.unknown")

    }

    @Test
    fun `enrollRecoveryCode - valid access token - returns RecoveryCodeEnrollmentChallenge`() =
        runTest {
            val scope = "scope:authentication_methods"

            val request =
                FakeRequestImpl<RecoveryCodeEnrollmentChallenge, MyAccountException>(response = TestData.recoveryCodeEnrollmentChallenge)
            every { myAccountClient.enrollRecoveryCode() } returns request

            val result = repository.enrollRecoveryCode(scope)

            assertThat(result.recoveryCode).isEqualTo("RECOVERY-CODE-123")
            coVerify(exactly = 1) { myAccountClient.enrollRecoveryCode() }
        }

    @Test
    fun `enrollRecoveryCode - API call fails - throws Auth0Error`() {
        val scope = "scope:authentication_methods"
        val expectedException = MyAccountException("Recovery code enrollment failed")

        val request =
            FakeRequestImpl<RecoveryCodeEnrollmentChallenge, MyAccountException>(exception = expectedException)
        every { myAccountClient.enrollRecoveryCode() } returns request


        val exception = assertThrows(Auth0Error::class.java) {
            runTest {
                repository.enrollRecoveryCode(scope)
            }
        }

        assertThat(exception.message).isEqualTo("Received error with code a0.sdk.internal_error.unknown")
    }


    @Test
    fun `enrollEmail - valid email and token - returns MfaEnrollmentChallenge`() = runTest {
        val email = "user@example.com"
        val scope = "scope:authentication_methods"

        val request =
            FakeRequestImpl<EnrollmentChallenge, MyAccountException>(response = TestData.emailEnrollmentChallenge)
        every { myAccountClient.enrollEmail(email) } returns request

        val result = repository.enrollEmail(email, scope)

        assertThat(result.id).isEqualTo("email_123")
        coVerify(exactly = 1) { myAccountClient.enrollEmail(email) }
    }

    @Test
    fun `enrollEmail - API call fails - throws Auth0Error`() {
        val email = "user@example.com"
        val scope = "scope:authentication_methods"
        val expectedException = MyAccountException("Email enrollment failed")

        val request =
            FakeRequestImpl<EnrollmentChallenge, MyAccountException>(exception = expectedException)
        every { myAccountClient.enrollEmail(email) } returns request

        val exception = assertThrows(Auth0Error::class.java) {
            runTest {
                repository.enrollEmail(email, scope)
            }
        }
        assertThat(exception.message).isEqualTo("Received error with code a0.sdk.internal_error.unknown")
    }


    @Test
    fun `enrollPhone - valid phone number with SMS method - returns MfaEnrollmentChallenge`() =
        runTest {
            val phoneNumber = "+15551234567"
            val scope = "scope:authentication_methods"

            val request =
                FakeRequestImpl<EnrollmentChallenge, MyAccountException>(response = TestData.phoneEnrollmentChallenge)
            every {
                myAccountClient.enrollPhone(
                    phoneNumber,
                    PhoneAuthenticationMethodType.SMS
                )
            } returns request

            val result = repository.enrollPhone(phoneNumber, scope)

            assertThat(result.id).isEqualTo("phone_123")
            coVerify(exactly = 1) {
                myAccountClient.enrollPhone(
                    phoneNumber,
                    PhoneAuthenticationMethodType.SMS
                )
            }
        }

    @Test
    fun `enrollPhone - API call fails - throws Auth0Error`() {
        val phoneNumber = "+15551234567"
        val scope = "scope:authentication_methods"
        val expectedException = MyAccountException("Phone enrollment failed")

        val request =
            FakeRequestImpl<EnrollmentChallenge, MyAccountException>(exception = expectedException)
        every {
            myAccountClient.enrollPhone(
                phoneNumber,
                PhoneAuthenticationMethodType.SMS
            )
        } returns request


        val exception = assertThrows(Auth0Error::class.java) {
            runTest {
                repository.enrollPhone(phoneNumber, scope)
            }
        }

        assertThat(exception.message).isEqualTo("Received error with code a0.sdk.internal_error.unknown")
    }


    @Test
    fun `verifyOtp - valid OTP code - returns verified AuthenticationMethod`() = runTest {
        val authMethodId = "auth_method_123"
        val otpCode = "123456"
        val authSession = "session_token"
        val scope = "scope:authentication_methods"

        val request =
            FakeRequestImpl<AuthenticationMethod, MyAccountException>(response = TestData.totpAuthMethod)
        every { myAccountClient.verifyOtp(authMethodId, otpCode, authSession) } returns request

        val result = repository.verifyOtp(authMethodId, otpCode, authSession, scope)

        assertThat(result).isEqualTo(TestData.totpAuthMethod)
        coVerify(exactly = 1) { myAccountClient.verifyOtp(authMethodId, otpCode, authSession) }
    }

    @Test
    fun `verifyOtp - invalid OTP code - throws invalid code Auth0Error`() {
        val authMethodId = "auth_method_123"
        val invalidOtp = "000000"
        val authSession = "session_token"
        val scope = "scope:authentication_methods"
        val errorValues = mapOf(
            "type" to "forbidden",
            "title" to "Forbidden",
            "detail" to "invalid code"
        )
        val expectedException = MyAccountException(errorValues,500)

        val request =
            FakeRequestImpl<AuthenticationMethod, MyAccountException>(exception = expectedException)
        every {
            myAccountClient.verifyOtp(
                authMethodId,
                invalidOtp,
                authSession
            )
        } returns request

        val exception = assertThrows(Auth0Error::class.java) {
            runTest {
                repository.verifyOtp(authMethodId, invalidOtp, authSession, scope)
            }
        }

        assertThat(exception.message).isEqualTo("Forbidden")

    }

    @Test
    fun `verifyOtp - expired session token - throws Auth0Error`() {
        val authMethodId = "auth_method_123"
        val otpCode = "123456"
        val expiredSession = "expired_session"
        val scope = "scope:authentication_methods"
        val expectedException = MyAccountException("Session expired")

        val request =
            FakeRequestImpl<AuthenticationMethod, MyAccountException>(exception = expectedException)
        every {
            myAccountClient.verifyOtp(
                authMethodId,
                otpCode,
                expiredSession
            )
        } returns request

        val exception = assertThrows(Auth0Error::class.java) {
            runTest {
                repository.verifyOtp(authMethodId, otpCode, expiredSession, scope)
            }
        }

        assertThat(exception.message).isEqualTo("Received error with code a0.sdk.internal_error.unknown")
    }


    @Test
    fun `verifyWithoutOtp - valid auth session - returns verified AuthenticationMethod`() =
        runTest {
            val authMethodId = "auth_method_123"
            val authSession = "valid_session"
            val scope = "scope:authentication_methods"

            val request =
                FakeRequestImpl<AuthenticationMethod, MyAccountException>(response = TestData.phoneAuthMethod)
            every { myAccountClient.verify(authMethodId, authSession) } returns request

            val result = repository.verifyWithoutOtp(authMethodId, authSession, scope)

            assertThat(result).isEqualTo(TestData.phoneAuthMethod)
            coVerify(exactly = 1) { myAccountClient.verify(authMethodId, authSession) }
        }

    @Test
    fun `verifyWithoutOtp - invalid session token - throws Auth0Error`() {
        val authMethodId = "auth_method_123"
        val invalidSession = "invalid_session"
        val scope = "scope:authentication_methods"
        val expectedException = MyAccountException("Invalid session token")

        val request =
            FakeRequestImpl<AuthenticationMethod, MyAccountException>(exception = expectedException)
        every { myAccountClient.verify(authMethodId, invalidSession) } returns request

        val exception = assertThrows(Auth0Error::class.java) {
            runTest {
                repository.verifyWithoutOtp(authMethodId, invalidSession, scope)
            }
        }

        assertThat(exception.message).isEqualTo("Received error with code a0.sdk.internal_error.unknown")
    }

}
