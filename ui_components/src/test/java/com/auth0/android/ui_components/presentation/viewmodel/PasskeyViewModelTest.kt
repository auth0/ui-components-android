package com.auth0.android.ui_components.presentation.viewmodel

import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialInterruptedException
import androidx.credentials.exceptions.CreateCredentialProviderConfigurationException
import com.auth0.android.ui_components.PasskeyConfiguration
import com.auth0.android.ui_components.TestData
import com.auth0.android.ui_components.domain.error.Auth0Error
import com.auth0.android.ui_components.domain.repository.MyAccountRepository
import com.auth0.android.ui_components.presentation.ui.passkeys.PasskeyEvent
import com.auth0.android.ui_components.presentation.ui.passkeys.PasskeyUiState
import com.auth0.android.ui_components.presentation.ui.passkeys.PasskeyViewModel
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PasskeyViewModelTest {

    private lateinit var myAccountRepository: MyAccountRepository
    private lateinit var viewModel: PasskeyViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val testPasskeyConfiguration = PasskeyConfiguration(
        credentialManager = null,
        connection = "test-connection",
        userIdentity = "user_identity_123"
    )

    private val fakeCredentialCreator: suspend (String) -> String = { _ ->
        Json.encodeToString(TestData.domainPublicKeyCredentials)
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        myAccountRepository = mockk()
        viewModel = PasskeyViewModel(
            myAccountRepository = myAccountRepository,
            passkeyConfiguration = testPasskeyConfiguration
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `initialization - uiState starts with Idle state`() = runTest {
        assertThat(viewModel.uiState.value).isEqualTo(PasskeyUiState.Idle)
    }

    @Test
    fun `enrollPasskey - success - final state is Idle`() = runTest {
        coEvery { myAccountRepository.enrollPasskey(any(), any(), any()) } returns TestData.domainPasskeyEnrollmentChallenge
        coEvery { myAccountRepository.verifyPasskey(any(), any(), any()) } returns mockk()

        val job = launch { viewModel.uiState.collect { } }

        viewModel.enrollPasskey(fakeCredentialCreator)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(PasskeyUiState.Idle)

        job.cancel()
    }

    @Test
    fun `enrollPasskey - success - emits EnrollmentSuccess event`() = runTest {
        coEvery { myAccountRepository.enrollPasskey(any(), any(), any()) } returns TestData.domainPasskeyEnrollmentChallenge
        coEvery { myAccountRepository.verifyPasskey(any(), any(), any()) } returns mockk()

        val events = mutableListOf<PasskeyEvent>()
        val eventJob = launch { viewModel.events.collect { events.add(it) } }

        viewModel.enrollPasskey(fakeCredentialCreator)
        advanceUntilIdle()

        assertThat(events).containsExactly(PasskeyEvent.EnrollmentSuccess)

        eventJob.cancel()
    }


    @Test
    fun `enrollPasskey - calls enrollPasskey with correct scope and configuration`() = runTest {
        coEvery { myAccountRepository.enrollPasskey(any(), any(), any()) } returns TestData.domainPasskeyEnrollmentChallenge
        coEvery { myAccountRepository.verifyPasskey(any(), any(), any()) } returns mockk()

        viewModel.enrollPasskey(fakeCredentialCreator)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            myAccountRepository.enrollPasskey(
                scope = "create:me:authentication_methods",
                userIdentity = "user_identity_123",
                connection = "test-connection"
            )
        }
    }

    @Test
    fun `enrollPasskey - calls verifyPasskey with correct scope, challenge, and decoded credentials`() = runTest {
        coEvery { myAccountRepository.enrollPasskey(any(), any(), any()) } returns TestData.domainPasskeyEnrollmentChallenge
        coEvery { myAccountRepository.verifyPasskey(any(), any(), any()) } returns mockk()

        viewModel.enrollPasskey(fakeCredentialCreator)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            myAccountRepository.verifyPasskey(
                publicKeyCredentials = TestData.domainPublicKeyCredentials,
                challenge = TestData.domainPasskeyEnrollmentChallenge,
                scope = "create:me:authentication_methods"
            )
        }
    }


    @Test
    fun `enrollPasskey - null userIdentity and connection - passes nulls to repository`() = runTest {
        val nullConfig = PasskeyConfiguration(
            credentialManager = null,
            connection = null,
            userIdentity = null
        )
        val vmWithNullConfig = PasskeyViewModel(
            myAccountRepository = myAccountRepository,
            passkeyConfiguration = nullConfig
        )

        coEvery { myAccountRepository.enrollPasskey(any(), any(), any()) } returns TestData.domainPasskeyEnrollmentChallenge
        coEvery { myAccountRepository.verifyPasskey(any(), any(), any()) } returns mockk()

        vmWithNullConfig.enrollPasskey(fakeCredentialCreator)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            myAccountRepository.enrollPasskey(
                scope = "create:me:authentication_methods",
                userIdentity = null,
                connection = null
            )
        }
    }

    @Test
    fun `enrollPasskey - Auth0Error - emits Error state with retry callback`() = runTest {
        coEvery { myAccountRepository.enrollPasskey(any(), any(), any()) } throws Auth0Error.NetworkError("Network failed", Exception())

        viewModel.enrollPasskey(fakeCredentialCreator)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(PasskeyUiState.Error::class.java)
        assertThat((state as PasskeyUiState.Error).error.onRetry).isNotNull()
    }

    @Test
    fun `enrollPasskey - CreateCredentialCancellationException - emits UserCancelled state`() = runTest {
        coEvery { myAccountRepository.enrollPasskey(any(), any(), any()) } returns TestData.domainPasskeyEnrollmentChallenge

        val cancellingCredentialCreator: suspend (String) -> String = { _ ->
            throw CreateCredentialCancellationException()
        }

        viewModel.enrollPasskey(cancellingCredentialCreator)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(PasskeyUiState.UserCancelled)
    }

    @Test
    fun `enrollPasskey - CreateCredentialInterruptedException - emits Error with shouldRetry true`() = runTest {
        coEvery { myAccountRepository.enrollPasskey(any(), any(), any()) } returns TestData.domainPasskeyEnrollmentChallenge

        val interruptedCredentialCreator: suspend (String) -> String = { _ ->
            throw CreateCredentialInterruptedException()
        }

        viewModel.enrollPasskey(interruptedCredentialCreator)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(PasskeyUiState.Error::class.java)
        assertThat((state as PasskeyUiState.Error).shouldRetry).isTrue()
    }

    @Test
    fun `enrollPasskey - CreateCredentialProviderConfigurationException - emits Error with shouldRetry false`() = runTest {
        coEvery { myAccountRepository.enrollPasskey(any(), any(), any()) } returns TestData.domainPasskeyEnrollmentChallenge

        val configErrorCredentialCreator: suspend (String) -> String = { _ ->
            throw CreateCredentialProviderConfigurationException()
        }

        viewModel.enrollPasskey(configErrorCredentialCreator)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(PasskeyUiState.Error::class.java)
        assertThat((state as PasskeyUiState.Error).shouldRetry).isFalse()
    }


    @Test
    fun `enrollPasskey - retry callback - calls repository again`() = runTest {
        var callCount = 0
        coEvery { myAccountRepository.enrollPasskey(any(), any(), any()) } answers {
            callCount++
            throw Auth0Error.NetworkError("Failed", Exception())
        }

        viewModel.enrollPasskey(fakeCredentialCreator)
        advanceUntilIdle()

        val errorState = viewModel.uiState.value as PasskeyUiState.Error
        errorState.error.onRetry()
        advanceUntilIdle()

        assertThat(callCount).isEqualTo(2)
    }


    @Test
    fun `enrollPasskey - verifyPasskey throws error - emits Error state`() = runTest {
        coEvery { myAccountRepository.enrollPasskey(any(), any(), any()) } returns TestData.domainPasskeyEnrollmentChallenge
        coEvery { myAccountRepository.verifyPasskey(any(), any(), any()) } throws Auth0Error.ServerError("Server error", 500, Exception())

        viewModel.enrollPasskey(fakeCredentialCreator)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(PasskeyUiState.Error::class.java)
    }
}
