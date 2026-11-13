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
    fun `factor id - email - is named email, and compares by its value`() {
        assertThat(MfaFactorId.Email.value).isEqualTo("email")
        assertThat(MfaFactorId("email")).isEqualTo(MfaFactorId.Email)
        assertThat(MfaFactorId.Email).isNotEqualTo(MfaFactorId.Phone)
    }

    @Test
    fun `email model - built for a preview or a test - needs only the address, and its actions are safe to call`() {
        val email = EmailChallengeModel(address = "j***@example.com")

        assertThat(email.isWrongCode).isFalse()
        assertThat(email.canResend).isFalse()
        assertThat(email.canSwitch).isFalse()
        assertThat(email.resendAvailableIn).isEqualTo(0)
        assertThat(MfaFactorStep(MfaFactorId.Email, email).round).isEqualTo(1)
        email.submit("123456")
        email.resend()
        email.switchFactor()
        email.cancel()
    }

    @Test
    fun `stream - a result - is kept until the next flow clears it`() {
        val stream = MfaStepStream()

        stream.finish(MfaResult.SignedIn)
        assertThat(stream.result.value).isEqualTo(MfaResult.SignedIn)

        stream.finish(null)
        assertThat(stream.result.value).isNull()
    }

    @Test
    fun `constructors - the shortest form of each model - stays, so a field added later does not break a compiled app`() {
        // Only the models without a value-class parameter: Kotlin mangles the constructors of the others.
        PushChallengeModel::class.java.getConstructor(String::class.java)
        EmailChallengeModel::class.java.getConstructor(String::class.java)
        AuthenticatorListModel::class.java.getConstructor(AuthenticatorListHeader::class.java, List::class.java)
        ErrorModel::class.java.getConstructor(MfaError::class.java)
        LoadingModel::class.java.getConstructor()
        MfaConfiguration::class.java.getConstructor()
        MfaConfiguration::class.java.getConstructor(MfaMessages::class.java)
    }
}
