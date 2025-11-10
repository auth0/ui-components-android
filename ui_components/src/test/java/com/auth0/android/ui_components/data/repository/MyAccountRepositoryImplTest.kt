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
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
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
    private lateinit var repository: MyAccountRepositoryImpl

    @Before
    fun setup() {
        myAccountProvider = mockk()
        myAccountClient = mockk()
        every { myAccountProvider.getMyAccount(any()) } returns myAccountClient
        repository = MyAccountRepositoryImpl(myAccountProvider)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `getFactors - successful response - returns list of factors`() = runTest {
        val accessToken = "valid_access_token"
        val expectedFactors = TestData.sampleFactorList

        val request = FakeRequestImpl<List<Factor>, MyAccountException>(response = expectedFactors)
        every { myAccountClient.getFactors() } returns request

        val result = repository.getFactors(accessToken)

        assertThat(result).isEqualTo(expectedFactors)
        assertThat(result).hasSize(2)
        coVerify(exactly = 1) { myAccountProvider.getMyAccount(accessToken) }
        coVerify(exactly = 1) { myAccountClient.getFactors() }
    }

    @Test
    fun `getFactors - API call fails - throws exception`() {
        val accessToken = "valid_access_token"
        val expectedException = MyAccountException("Network error")

        val request =
            FakeRequestImpl<List<Factor>, MyAccountException>(exception = expectedException)
        every { myAccountClient.getFactors() } returns request

        val exception = assertThrows(MyAccountException::class.java) {
            runTest {
                repository.getFactors(accessToken)
            }
        }
        assertThat(exception.message).isEqualTo("Network error")
    }

    @Test
    fun `getFactors - empty list returned - returns empty list successfully`() = runTest {
        val accessToken = "valid_access_token"
        val emptyFactors = emptyList<Factor>()

        val request = FakeRequestImpl<List<Factor>, MyAccountException>(response = emptyFactors)
        every { myAccountClient.getFactors() } returns request

        val result = repository.getFactors(accessToken)
        assertThat(result).isEmpty()
    }


    @Test
    fun `getAuthenticatorMethods - valid access token - returns list of authentication methods`() =
        runTest {
            val accessToken = "valid_access_token"
            val expectedMethods = listOf(
                TestData.phoneAuthMethod,
                TestData.totpAuthMethod
            )

            val request =
                FakeRequestImpl<List<AuthenticationMethod>, MyAccountException>(response = expectedMethods)
            every { myAccountClient.getAuthenticationMethods() } returns request

            val result = repository.getAuthenticatorMethods(accessToken)

            assertThat(result).isEqualTo(expectedMethods)
            assertThat(result).hasSize(2)
            coVerify(exactly = 1) { myAccountClient.getAuthenticationMethods() }
        }

    @Test
    fun `getAuthenticatorMethods - API call fails - throws exception`() {
        val accessToken = "valid_access_token"
        val expectedException = MyAccountException("API error")

        val request =
            FakeRequestImpl<List<AuthenticationMethod>, MyAccountException>(exception = expectedException)
        every { myAccountClient.getAuthenticationMethods() } returns request

        val exception = assertThrows(MyAccountException::class.java) {
            runTest {
                repository.getAuthenticatorMethods(accessToken)
            }
        }
        assertThat(exception.message).isEqualTo("API error")
    }

    @Test
    fun `getAuthenticatorMethods - empty list returned - returns empty list successfully`() =
        runTest {
            val accessToken = "valid_access_token"
            val emptyMethods = emptyList<AuthenticationMethod>()

            val request =
                FakeRequestImpl<List<AuthenticationMethod>, MyAccountException>(response = emptyMethods)
            every { myAccountClient.getAuthenticationMethods() } returns request

            val result = repository.getAuthenticatorMethods(accessToken)

            assertThat(result).isEmpty()
        }


    @Test
    fun `deleteAuthenticationMethod - valid method ID and token - returns null successfully`() =
        runTest {
            val methodId = "auth_method_123"
            val accessToken = "valid_access_token"

            val request = FakeRequestImpl<Void?, MyAccountException>(response = null)
            every { myAccountClient.deleteAuthenticationMethod(methodId) } returns request

            repository.deleteAuthenticationMethod(methodId, accessToken)
            coVerify(exactly = 1) { myAccountClient.deleteAuthenticationMethod(methodId) }
        }

    @Test
    fun `deleteAuthenticationMethod - API call fails - throws exception`()  {
        val methodId = "auth_method_123"
        val accessToken = "valid_access_token"
        val expectedException = MyAccountException("Deletion failed")

        val request = FakeRequestImpl<Void?, MyAccountException>(exception = expectedException)
        every { myAccountClient.deleteAuthenticationMethod(methodId) } returns request


        val exception = assertThrows(MyAccountException::class.java) {
            runTest {
                repository.deleteAuthenticationMethod(methodId, accessToken)
            }
        }
        assertThat(exception.message).isEqualTo("Deletion failed")
    }

    @Test
    fun `enrollTotp - valid access token - returns TotpEnrollmentChallenge`() = runTest {
        val accessToken = "valid_access_token"

        val request =
            FakeRequestImpl<TotpEnrollmentChallenge, MyAccountException>(response = TestData.totpEnrollmentChallenge)
        every { myAccountClient.enrollTotp() } returns request

        val result = repository.enrollTotp(accessToken)

        assertThat(result.id).isEqualTo("totp_id_123")
        assertThat(result.barcodeUri).isEqualTo("otpauth://totp/...")
        coVerify(exactly = 1) { myAccountClient.enrollTotp() }
    }

    @Test
    fun `enrollTotp - API call fails - throws exception`()  {
        val accessToken = "valid_access_token"
        val expectedException = MyAccountException("Enrollment failed")

        val request =
            FakeRequestImpl<TotpEnrollmentChallenge, MyAccountException>(exception = expectedException)
        every { myAccountClient.enrollTotp() } returns request

        val exception = assertThrows(MyAccountException::class.java) {
            runTest {
                repository.enrollTotp(accessToken)
            }
        }
        assertThat(exception.message).isEqualTo("Enrollment failed")
    }


    @Test
    fun `enrollPushNotification - valid access token - returns TotpEnrollmentChallenge`() =
        runTest {
            val accessToken = "valid_access_token"

            val request =
                FakeRequestImpl<TotpEnrollmentChallenge, MyAccountException>(response = TestData.pushEnrollmentChallenge)
            every { myAccountClient.enrollPushNotification() } returns request

            val result = repository.enrollPushNotification(accessToken)

            assertThat(result.id).isEqualTo("push_id_123")
            coVerify(exactly = 1) { myAccountClient.enrollPushNotification() }
        }

    @Test
    fun `enrollPushNotification - API call fails - throws exception`()  {
        val accessToken = "valid_access_token"
        val expectedException = MyAccountException("Push enrollment failed")

        val request =
            FakeRequestImpl<TotpEnrollmentChallenge, MyAccountException>(exception = expectedException)
        every { myAccountClient.enrollPushNotification() } returns request


        val exception = assertThrows(MyAccountException::class.java) {
            runTest {
                repository.enrollPushNotification(accessToken)
            }
        }
        assertThat(exception.message).isEqualTo("Push enrollment failed")

    }

    @Test
    fun `enrollRecoveryCode - valid access token - returns RecoveryCodeEnrollmentChallenge`() =
        runTest {
            val accessToken = "valid_access_token"

            val request =
                FakeRequestImpl<RecoveryCodeEnrollmentChallenge, MyAccountException>(response = TestData.recoveryCodeEnrollmentChallenge)
            every { myAccountClient.enrollRecoveryCode() } returns request

            val result = repository.enrollRecoveryCode(accessToken)

            assertThat(result.recoveryCode).isEqualTo("RECOVERY-CODE-123")
            coVerify(exactly = 1) { myAccountClient.enrollRecoveryCode() }
        }

    @Test
    fun `enrollRecoveryCode - API call fails - throws exception`()  {
        val accessToken = "valid_access_token"
        val expectedException = MyAccountException("Recovery code enrollment failed")

        val request =
            FakeRequestImpl<RecoveryCodeEnrollmentChallenge, MyAccountException>(exception = expectedException)
        every { myAccountClient.enrollRecoveryCode() } returns request


        val exception = assertThrows(MyAccountException::class.java) {
            runTest {
                repository.enrollRecoveryCode(accessToken)
            }
        }

        assertThat(exception.message).isEqualTo("Recovery code enrollment failed")
    }


    @Test
    fun `enrollEmail - valid email and token - returns MfaEnrollmentChallenge`() = runTest {
        val email = "user@example.com"
        val accessToken = "valid_access_token"

        val request =
            FakeRequestImpl<EnrollmentChallenge, MyAccountException>(response = TestData.emailEnrollmentChallenge)
        every { myAccountClient.enrollEmail(email) } returns request

        val result = repository.enrollEmail(email, accessToken)

        assertThat(result.id).isEqualTo("email_123")
        coVerify(exactly = 1) { myAccountClient.enrollEmail(email) }
    }

    @Test
    fun `enrollEmail - API call fails - throws exception`()  {
        val email = "user@example.com"
        val accessToken = "valid_access_token"
        val expectedException = MyAccountException("Email enrollment failed")

        val request =
            FakeRequestImpl<EnrollmentChallenge, MyAccountException>(exception = expectedException)
        every { myAccountClient.enrollEmail(email) } returns request

        val exception = assertThrows(MyAccountException::class.java) {
            runTest {
                repository.enrollEmail(email, accessToken)
            }
        }
        assertThat(exception.message).isEqualTo("Email enrollment failed")
    }


    @Test
    fun `enrollPhone - valid phone number with SMS method - returns MfaEnrollmentChallenge`() =
        runTest {
            val phoneNumber = "+15551234567"
            val preferredMethod = PhoneAuthenticationMethodType.SMS
            val accessToken = "valid_access_token"

            val request =
                FakeRequestImpl<EnrollmentChallenge, MyAccountException>(response = TestData.phoneEnrollmentChallenge)
            every { myAccountClient.enrollPhone(phoneNumber, preferredMethod) } returns request

            val result = repository.enrollPhone(phoneNumber, preferredMethod, accessToken)

            assertThat(result.id).isEqualTo("phone_123")
            coVerify(exactly = 1) { myAccountClient.enrollPhone(phoneNumber, preferredMethod) }
        }

    @Test
    fun `enrollPhone - valid phone number with Voice method - returns MfaEnrollmentChallenge`() =
        runTest {
            val phoneNumber = "+15551234567"
            val preferredMethod = PhoneAuthenticationMethodType.VOICE
            val accessToken = "valid_access_token"

            val request =
                FakeRequestImpl<EnrollmentChallenge, MyAccountException>(response = TestData.phoneVoiceEnrollmentChallenge)
            every { myAccountClient.enrollPhone(phoneNumber, preferredMethod) } returns request

            val result = repository.enrollPhone(phoneNumber, preferredMethod, accessToken)

            assertThat(result.id).isEqualTo("phone_voice_123")
            coVerify(exactly = 1) {
                myAccountClient.enrollPhone(
                    phoneNumber,
                    PhoneAuthenticationMethodType.VOICE
                )
            }
        }

    @Test
    fun `enrollPhone - API call fails - throws exception`()  {
        val phoneNumber = "+15551234567"
        val preferredMethod = PhoneAuthenticationMethodType.SMS
        val accessToken = "valid_access_token"
        val expectedException = MyAccountException("Phone enrollment failed")

        val request =
            FakeRequestImpl<EnrollmentChallenge, MyAccountException>(exception = expectedException)
        every { myAccountClient.enrollPhone(phoneNumber, preferredMethod) } returns request


        val exception = assertThrows(MyAccountException::class.java) {
            runTest {
                repository.enrollPhone(phoneNumber, preferredMethod, accessToken)
            }
        }

        assertThat(exception.message).isEqualTo("Phone enrollment failed")
    }


    @Test
    fun `verifyOtp - valid OTP code - returns verified AuthenticationMethod`() = runTest {
        val authMethodId = "auth_method_123"
        val otpCode = "123456"
        val authSession = "session_token"
        val accessToken = "valid_access_token"

        val request =
            FakeRequestImpl<AuthenticationMethod, MyAccountException>(response = TestData.totpAuthMethod)
        every { myAccountClient.verifyOtp(authMethodId, otpCode, authSession) } returns request

        val result = repository.verifyOtp(authMethodId, otpCode, authSession, accessToken)

        assertThat(result).isEqualTo(TestData.totpAuthMethod)
        coVerify(exactly = 1) { myAccountClient.verifyOtp(authMethodId, otpCode, authSession) }
    }

    @Test
    fun `verifyOtp - invalid OTP code - throws verification exception`()  {
        val authMethodId = "auth_method_123"
        val invalidOtp = "000000"
        val authSession = "session_token"
        val accessToken = "valid_access_token"
        val expectedException = MyAccountException("Invalid OTP code")

        val request =
            FakeRequestImpl<AuthenticationMethod, MyAccountException>(exception = expectedException)
        every {
            myAccountClient.verifyOtp(
                authMethodId,
                invalidOtp,
                authSession
            )
        } returns request

        val exception = assertThrows(MyAccountException::class.java) {
            runTest {
                repository.verifyOtp(authMethodId, invalidOtp, authSession, accessToken)
            }
        }

        assertThat(exception.message).isEqualTo("Invalid OTP code")

    }

    @Test
    fun `verifyOtp - expired session token - throws exception`()  {
        val authMethodId = "auth_method_123"
        val otpCode = "123456"
        val expiredSession = "expired_session"
        val accessToken = "valid_access_token"
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

        val exception = assertThrows(MyAccountException::class.java) {
            runTest {
                repository.verifyOtp(authMethodId, otpCode, expiredSession, accessToken)
            }
        }

        assertThat(exception.message).isEqualTo("Session expired")
    }


    @Test
    fun `verifyWithoutOtp - valid auth session - returns verified AuthenticationMethod`() =
        runTest {
            val authMethodId = "auth_method_123"
            val authSession = "valid_session"
            val accessToken = "valid_access_token"

            val request =
                FakeRequestImpl<AuthenticationMethod, MyAccountException>(response = TestData.phoneAuthMethod)
            every { myAccountClient.verify(authMethodId, authSession) } returns request

            val result = repository.verifyWithoutOtp(authMethodId, authSession, accessToken)

            assertThat(result).isEqualTo(TestData.phoneAuthMethod)
            coVerify(exactly = 1) { myAccountClient.verify(authMethodId, authSession) }
        }

    @Test
    fun `verifyWithoutOtp - invalid session token - throws exception`()  {
        val authMethodId = "auth_method_123"
        val invalidSession = "invalid_session"
        val accessToken = "valid_access_token"
        val expectedException = MyAccountException("Invalid session token")

        val request =
            FakeRequestImpl<AuthenticationMethod, MyAccountException>(exception = expectedException)
        every { myAccountClient.verify(authMethodId, invalidSession) } returns request

        val exception = assertThrows(MyAccountException::class.java) {
            runTest {
                repository.verifyWithoutOtp(authMethodId, invalidSession, accessToken)
            }
        }

        assertThat(exception.message).isEqualTo("Invalid session token")
    }

    @Test
    fun `verifyWithoutOtp - expired session - throws exception`()  {
        val authMethodId = "auth_method_123"
        val expiredSession = "expired_session"
        val accessToken = "valid_access_token"
        val expectedException = MyAccountException("Session has expired")

        val request =
            FakeRequestImpl<AuthenticationMethod, MyAccountException>(exception = expectedException)
        every { myAccountClient.verify(authMethodId, expiredSession) } returns request

        val exception = assertThrows(MyAccountException::class.java) {
            runTest {
                repository.verifyWithoutOtp(authMethodId, expiredSession, accessToken)
            }
        }
        assertThat(exception.message).isEqualTo("Session has expired")
    }
}
