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
        assertThat(email.isCodeExpired).isFalse()
        assertThat(email.requestError).isNull()
        assertThat(MfaFactorStep(MfaFactorId.Email, email).round).isEqualTo(1)
        email.submit("123456")
        email.resend()
        email.switchFactor()
        email.cancel()
    }

    @Test
    fun `factor id - totp - is named totp, and compares by its value`() {
        assertThat(MfaFactorId.Totp.value).isEqualTo("totp")
        assertThat(MfaFactorId("totp")).isEqualTo(MfaFactorId.Totp)
        assertThat(MfaFactorId.Totp).isNotEqualTo(MfaFactorId.Phone)
    }

    @Test
    fun `totp model - built for a preview or a test - needs no data, and its actions are safe to call`() {
        val totp = TotpChallengeModel()

        assertThat(totp.isWrongCode).isFalse()
        assertThat(totp.isVerifying).isFalse()
        assertThat(totp.canSwitch).isFalse()
        assertThat(totp.requestError).isNull()
        assertThat(MfaFactorStep(MfaFactorId.Totp, totp).round).isEqualTo(1)
        totp.submit("123456")
        totp.switchFactor()
        totp.cancel()
    }

    @Test
    fun `factor id - recovery code - is named recovery_code, and compares by its value`() {
        assertThat(MfaFactorId.RecoveryCode.value).isEqualTo("recovery_code")
        assertThat(MfaFactorId("recovery_code")).isEqualTo(MfaFactorId.RecoveryCode)
        assertThat(MfaFactorId.RecoveryCode).isNotEqualTo(MfaFactorId.Totp)
    }

    @Test
    fun `recovery code model - built for a preview or a test - needs no data, and its actions are safe to call`() {
        val recovery = RecoveryCodeChallengeModel()

        assertThat(recovery.newCode).isNull()
        assertThat(recovery.isWrongCode).isFalse()
        assertThat(recovery.isVerifying).isFalse()
        assertThat(recovery.canSwitch).isFalse()
        assertThat(recovery.requestError).isNull()
        assertThat(MfaFactorStep(MfaFactorId.RecoveryCode, recovery).round).isEqualTo(1)
        recovery.submit("ABCD-1234")
        recovery.confirm()
        recovery.switchFactor()
        recovery.cancel()
    }

    @Test
    fun `recovery code model - never writes the new code out, so logging or dumping it cannot leak it`() {
        val recovery = RecoveryCodeChallengeModel(newCode = "YHARATFUJJKZNV5N3X1L2ZEC")

        assertThat(recovery.toString()).doesNotContain("YHARATFUJJKZNV5N3X1L2ZEC")
        assertThat(recovery.toString()).contains("hasNewCode=true")
        val step = MfaFactorStep(MfaFactorId.RecoveryCode, recovery)
        assertThat(step.toString()).doesNotContain("YHARATFUJJKZNV5N3X1L2ZEC")
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
        TotpChallengeModel::class.java.getConstructor()
        RecoveryCodeChallengeModel::class.java.getConstructor()
        AuthenticatorListModel::class.java.getConstructor(AuthenticatorListHeader::class.java, List::class.java)
        ErrorModel::class.java.getConstructor(MfaError::class.java)
        LoadingModel::class.java.getConstructor()
        MfaConfiguration::class.java.getConstructor()
        MfaConfiguration::class.java.getConstructor(MfaMessages::class.java)
    }
}
