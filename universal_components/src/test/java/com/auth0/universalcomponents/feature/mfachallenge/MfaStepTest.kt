package com.auth0.universalcomponents.feature.mfachallenge

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MfaStepTest {

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
        // The default functions are safe to call.
        push.resend()
        push.cancel()
        list.select("push:0")
    }

    @Test
    fun `factor id - push - is named push, and compares by its value`() {
        assertThat(MfaFactorId.Push.value).isEqualTo("push")
        assertThat(MfaFactorId("push")).isEqualTo(MfaFactorId.Push)
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
