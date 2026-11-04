package com.auth0.universalcomponents.feature.mfachallenge

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MfaActionTest {

    @Test
    fun `equality - two actions that do different things - are equal`() {
        assertThat(MfaAction { }).isEqualTo(MfaAction { error("different") })
        assertThat(MfaSelectAction { }).isEqualTo(MfaSelectAction { error("different") })
    }

    @Test
    fun `models - with different actions - are equal when their data is`() {
        val first = LoadingModel(cancel = MfaAction { })
        val second = LoadingModel(cancel = MfaAction { error("different") })

        assertThat(first).isEqualTo(second)
        assertThat(first.hashCode()).isEqualTo(second.hashCode())
    }

    @Test
    fun `models - built for a preview or a test - need only their data`() {
        val list = AuthenticatorListModel(
            header = AuthenticatorListHeader.Challenge,
            rows = listOf(AuthenticatorListModel.Row("push:0", MfaFactorId.Push, "Jane's phone")),
        )
        val push = PushChallengeModel(deviceName = "Jane's phone")

        assertThat(list.round).isEqualTo(1)
        assertThat(list.pendingRowId).isNull()
        assertThat(push.canResend).isFalse()
        assertThat(push.resendAvailableIn).isEqualTo(0)
        assertThat(ErrorModel(MfaError.Rejected()).exit).isEqualTo(MfaExit.StartOver)
        assertThat(MfaFactorStep(MfaFactorId.Push, push).round).isEqualTo(1)
        // The default actions are safe to call.
        push.resend()
        push.cancel()
        list.select("push:0")
    }

    @Test
    fun `invoke - an action - runs what it was given`() {
        var ran = 0
        var picked = ""

        MfaAction { ran++ }()
        MfaSelectAction { picked = it }("push:1")

        assertThat(ran).isEqualTo(1)
        assertThat(picked).isEqualTo("push:1")
    }

    @Test
    fun `factor id - push - is named push, and compares by its value`() {
        assertThat(MfaFactorId.Push.value).isEqualTo("push")
        assertThat(MfaFactorId("push")).isEqualTo(MfaFactorId.Push)
    }

    @Test
    fun `stream - a step published twice with equal data - is one announcement`() {
        val stream = MfaStepStream()
        val seen = mutableListOf<MfaStep?>()
        // StateFlow keeps the latest value only; equal values are the same value.
        stream.showStep(MfaStep.Loading(LoadingModel(MfaAction { })))
        seen += stream.step.value
        stream.showStep(MfaStep.Loading(LoadingModel(MfaAction { error("different") })))

        assertThat(stream.step.value).isEqualTo(seen.single())
    }

    @Test
    fun `stream - a result - is kept until the next flow clears it`() {
        val stream = MfaStepStream()

        stream.finish(MfaResult.SignedIn)
        assertThat(stream.result.value).isEqualTo(MfaResult.SignedIn)

        stream.finish(null)
        assertThat(stream.result.value).isNull()
    }
}
