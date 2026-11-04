package com.auth0.universalcomponents.feature.mfachallenge.presentation

import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.mfachallenge.MfaError
import com.auth0.universalcomponents.feature.mfachallenge.MfaStep
import com.auth0.universalcomponents.feature.mfachallenge.MfaStepStream
import com.auth0.universalcomponents.feature.mfachallenge.PushChallengeModel
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.PushStatus
import com.auth0.universalcomponents.feature.mfachallenge.domain.repository.MfaRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/** A headless app draws from the stream, so a run of the real engine is read there as steps. */
@OptIn(ExperimentalCoroutinesApi::class)
class MfaStepStreamMirrorTest {

    private lateinit var repository: MfaRepository
    private val stream = MfaStepStream()
    private val effects = mutableListOf<MfaEffect>()
    private var left = 0

    @Before
    fun setup() {
        repository = mockk()
        coEvery { repository.sendPush(any()) } returns Result.Success(PushStatus.Pending(POLL, canResend = true))
        coEvery { repository.checkPushStatus() } returns Result.Success(PushStatus.Pending(POLL, canResend = true))
    }

    @Test
    fun `no MFA yet - nothing to draw and no result`() {
        assertThat(stream.step.value).isNull()
        assertThat(stream.result.value).isNull()
    }

    @Test
    fun `list - the stream shows it, and a pick from it sends the push and moves on to the push step`() = runTest {
        val engine = started(PHONE, TABLET)
        assertThat(stream.step.value).isInstanceOf(MfaStep.AuthenticatorList::class.java)

        val list = stream.step.value as MfaStep.AuthenticatorList
        list.model.select("push:1")
        runCurrent()

        val model = (stream.step.value as MfaStep.Factor).step.model as PushChallengeModel
        assertThat(model.deviceName).isEqualTo("Jane's tablet")
        assertThat(model.resendAvailableIn).isEqualTo(RESEND_SECONDS)
        assertThat(engine.isRunning).isTrue()
    }

    @Test
    fun `push step - the countdown on its model moves every second`() = runTest {
        started(PHONE)
        val before = (stream.step.value as MfaStep.Factor).step.model as PushChallengeModel

        advanceTimeBy(3_000)
        runCurrent()

        val after = (stream.step.value as MfaStep.Factor).step.model as PushChallengeModel
        assertThat(after.resendAvailableIn).isEqualTo(before.resendAvailableIn - 3)
    }

    @Test
    fun `push step - Switch from the app's own button - returns to the list`() = runTest {
        started(PHONE, TABLET, select = PHONE)
        val model = (stream.step.value as MfaStep.Factor).step.model as PushChallengeModel

        model.switchFactor()
        runCurrent()

        val list = stream.step.value as MfaStep.AuthenticatorList
        assertThat(list.model.pendingRowId).isNull()
    }

    @Test
    fun `cancel - from the app's own button - ends MFA with a restart`() = runTest {
        val engine = started(PHONE)
        val model = (stream.step.value as MfaStep.Factor).step.model as PushChallengeModel

        model.cancel()
        runCurrent()

        assertThat(effects).containsExactly(MfaEffect.Restart)
        assertThat(engine.isRunning).isFalse()
    }

    @Test
    fun `failure - the stream shows the error step, and its leave goes to the host`() = runTest {
        coEvery { repository.checkPushStatus() } returns Result.Error(MfaError.Rejected())
        started(PHONE)
        advanceTimeBy(POLL)
        runCurrent()

        val step = stream.step.value as MfaStep.Error
        step.model.leave()

        assertThat(step.model.error).isInstanceOf(MfaError.Rejected::class.java)
        assertThat(left).isEqualTo(1)
    }

    @Test
    fun `no usable factor - there is no step to draw, so the login can show its own screen`() = runTest {
        val engine = started()

        assertThat(stream.step.value).isNull()
        assertThat(engine.state.value.isNoEligibleFactors).isTrue()
        assertThat(effects).hasSize(1)
    }

    @Test
    fun `approved - the engine ends with the credentials`() = runTest {
        val credentials = mockk<Credentials>()
        coEvery { repository.checkPushStatus() } returns Result.Success(PushStatus.Approved(credentials))
        started(PHONE)

        advanceTimeBy(POLL)
        runCurrent()

        assertThat(effects).containsExactly(MfaEffect.Authenticated(credentials))
    }

    private fun TestScope.started(vararg factors: MfaFactor.Push, select: MfaFactor.Push? = null): MfaEngine {
        val engine = MfaEngine(
            reducer = MfaReducer(factors.toList()),
            repository = repository,
            hostScope = backgroundScope,
            onEffect = { effects += it },
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            stream.mirror(engine, onLeave = { left++ })
        }
        engine.start()
        if (select != null) engine.onIntent(MfaIntent.SelectFactor(select))
        runCurrent()
        return engine
    }

    private companion object {
        val PHONE = MfaFactor.Push(0, "Jane's phone")
        val TABLET = MfaFactor.Push(1, "Jane's tablet")
        const val POLL = 3_000L
        const val RESEND_SECONDS = 60
    }
}
