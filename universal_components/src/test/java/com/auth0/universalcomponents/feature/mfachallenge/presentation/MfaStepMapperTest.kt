package com.auth0.universalcomponents.feature.mfachallenge.presentation

import com.auth0.android.embedded.authorize.PhoneDeliveryMethod
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.mfachallenge.AuthenticatorListHeader
import com.auth0.universalcomponents.feature.mfachallenge.AuthenticatorListModel
import com.auth0.universalcomponents.feature.mfachallenge.CodeChallengeModel
import com.auth0.universalcomponents.feature.mfachallenge.EmailChallengeModel
import com.auth0.universalcomponents.feature.mfachallenge.MfaError
import com.auth0.universalcomponents.feature.mfachallenge.MfaExit
import com.auth0.universalcomponents.feature.mfachallenge.MfaFactorId
import com.auth0.universalcomponents.feature.mfachallenge.MfaStep
import com.auth0.universalcomponents.feature.mfachallenge.PhoneChallengeModel
import com.auth0.universalcomponents.feature.mfachallenge.PhoneDelivery
import com.auth0.universalcomponents.feature.mfachallenge.PushChallengeModel
import com.auth0.universalcomponents.feature.mfachallenge.SentCodeChallengeModel
import com.auth0.universalcomponents.feature.mfachallenge.TotpChallengeModel
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.MfaFactor
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MfaStepMapperTest {

    private val intents = mutableListOf<MfaIntent>()
    private var left = 0

    private fun toStep(state: MfaState) = state.toMfaStep(onIntent = { intents += it }, onLeave = { left++ })

    // region what each state shows

    @Test
    fun `choose factor - first round - the list headed with the challenge, one row per device`() {
        val step = toStep(MfaState.ChooseFactor(TWO)) as MfaStep.AuthenticatorList

        assertThat(step.model.header).isEqualTo(AuthenticatorListHeader.Challenge)
        assertThat(step.model.round).isEqualTo(1)
        assertThat(step.model.pendingRowId).isNull()
        assertThat(step.model.rows).containsExactly(
            AuthenticatorListModel.Row("push:0", MfaFactorId.Push, "Jane's phone", null, R.drawable.ic_device),
            AuthenticatorListModel.Row("push:1", MfaFactorId.Push, "Jane's tablet", null, R.drawable.ic_device),
        ).inOrder()
    }

    @Test
    fun `choose factor - later round - headed with the additional challenge, even with one factor`() {
        val step = toStep(MfaState.ChooseFactor(MfaRound(2, listOf(TABLET)))) as MfaStep.AuthenticatorList

        assertThat(step.model.header).isEqualTo(AuthenticatorListHeader.ChallengeAdditional)
        assertThat(step.model.round).isEqualTo(2)
        assertThat(step.model.rows).hasSize(1)
    }

    @Test
    fun `sending a challenge - over the list - marks that row as pending`() {
        val step = toStep(MfaState.SendingChallenge(TWO, TABLET)) as MfaStep.AuthenticatorList

        assertThat(step.model.pendingRowId).isEqualTo("push:1")
    }

    @Test
    fun `sending a challenge - with one factor and no list - is a loading step that can be cancelled`() {
        val step = toStep(MfaState.SendingChallenge(ONE, PHONE)) as MfaStep.Loading

        assertThat(step.model.cancel).isNotNull()
    }

    @Test
    fun `push waiting - the push model carries the device, the resend state and the choice`() {
        val state = waiting(TWO, PHONE).copy(canResend = true, resendSecondsLeft = 42)

        val factor = (toStep(state) as MfaStep.Factor).step
        val model = factor.model as PushChallengeModel

        assertThat(factor.factor).isEqualTo(MfaFactorId.Push)
        assertThat(factor.round).isEqualTo(1)
        assertThat(model.deviceName).isEqualTo("Jane's phone")
        assertThat(model.canResend).isTrue()
        assertThat(model.resendAvailableIn).isEqualTo(42)
        assertThat(model.canSwitch).isTrue()
        assertThat(model.isResending).isFalse()
    }

    @Test
    fun `push waiting - one device - cannot switch`() {
        val model = ((toStep(waiting(ONE, PHONE)) as MfaStep.Factor).step.model) as PushChallengeModel

        assertThat(model.canSwitch).isFalse()
    }

    @Test
    fun `push waiting - while it is sent again - says so, and offers no resend`() {
        val state = waiting(TWO, PHONE).copy(phase = PushPhase.Resending, canResend = false)

        val model = ((toStep(state) as MfaStep.Factor).step.model) as PushChallengeModel

        assertThat(model.isResending).isTrue()
        assertThat(model.canResend).isFalse()
    }

    @Test
    fun `push waiting - in a later round - reports that round`() {
        val state = waiting(MfaRound(3, listOf(PHONE, TABLET)), PHONE)

        assertThat((toStep(state) as MfaStep.Factor).step.round).isEqualTo(3)
    }

    @Test
    fun `phone code - the model carries the number, how the code came, and the wait states`() {
        val state = codeReady(TWO_WITH_SMS).copy(resend = RESEND_WAITING, isWrongCode = true)

        val factor = (toStep(state) as MfaStep.Factor).step
        val model = factor.model as PhoneChallengeModel

        assertThat(factor.factor).isEqualTo(MfaFactorId.Phone)
        assertThat(model.phoneNumber).isEqualTo("***9239")
        assertThat(model.delivery).isEqualTo(PhoneDelivery.Text)
        assertThat(model.isWrongCode).isTrue()
        assertThat(model.canResend).isTrue()
        assertThat(model.resendAvailableIn).isEqualTo(42)
        assertThat(model.canSwitch).isTrue()
        assertThat(model.isVerifying).isFalse()
        assertThat(model.isResending).isFalse()
    }

    @Test
    fun `phone code - being checked, or sent again - says so`() {
        val verifying = codeReady(TWO_WITH_SMS).copy(phase = CodePhase.Verifying)
        val resending = codeReady(TWO_WITH_SMS).copy(phase = CodePhase.Resending, resend = RESEND_SENT)

        val checked = (toStep(verifying) as MfaStep.Factor).step.model as PhoneChallengeModel
        val sent = (toStep(resending) as MfaStep.Factor).step.model as PhoneChallengeModel

        assertThat(checked.isVerifying).isTrue()
        assertThat(sent.isResending).isTrue()
        assertThat(sent.canResend).isFalse()
    }

    @Test
    fun `phone code - a code by voice - is told apart from a text`() {
        val state = codeReady(MfaRound(1, listOf(CALL))).copy(factor = CALL)

        val model = (toStep(state) as MfaStep.Factor).step.model as PhoneChallengeModel

        assertThat(model.delivery).isEqualTo(PhoneDelivery.Voice)
    }

    @Test
    fun `list - a phone reachable two ways - is two rows, each with its number and how it is reached`() {
        val step = toStep(MfaState.ChooseFactor(MfaRound(1, listOf(SMS, CALL)))) as MfaStep.AuthenticatorList

        val (text, voice) = step.model.rows

        assertThat(step.model.rows).hasSize(2)
        assertThat(text.id).isEqualTo("phone:3:TEXT")
        assertThat(voice.id).isEqualTo("phone:3:VOICE")
        listOf(text, voice).forEach { row ->
            assertThat(row.factor).isEqualTo(MfaFactorId.Phone)
            assertThat(row.title).isEqualTo("***9239")
        }
        // A text shows the message icon, a call the handset.
        assertThat(text.icon).isEqualTo(R.drawable.ic_mfa_sms)
        assertThat(voice.icon).isEqualTo(R.drawable.ic_mfa_voice)
        assertThat(text.delivery).isEqualTo(PhoneDelivery.Text)
        assertThat(voice.delivery).isEqualTo(PhoneDelivery.Voice)
    }

    @Test
    fun `phone code - the taps - come back as intents`() {
        val model = (toStep(codeReady(TWO_WITH_SMS)) as MfaStep.Factor).step.model as PhoneChallengeModel

        model.submit("123456")
        model.resend()
        model.switchFactor()
        model.cancel()

        assertThat(intents).containsExactly(
            MfaIntent.SubmitCode("123456"),
            MfaIntent.Resend,
            MfaIntent.ChooseAnotherFactor,
            MfaIntent.Cancel,
        ).inOrder()
    }

    @Test
    fun `email code - the model carries the address, and the wait states`() {
        val state = emailReady(TWO_WITH_EMAIL).copy(resend = RESEND_WAITING, isWrongCode = true)

        val factor = (toStep(state) as MfaStep.Factor).step
        val model = factor.model as EmailChallengeModel

        assertThat(factor.factor).isEqualTo(MfaFactorId.Email)
        assertThat(factor.round).isEqualTo(1)
        assertThat(model.address).isEqualTo("j***@example.com")
        assertThat(model.isWrongCode).isTrue()
        assertThat(model.canResend).isTrue()
        assertThat(model.resendAvailableIn).isEqualTo(42)
        assertThat(model.canSwitch).isTrue()
        assertThat(model.isVerifying).isFalse()
        assertThat(model.isResending).isFalse()
    }

    @Test
    fun `email code - being checked, or sent again - says so`() {
        val verifying = emailReady(TWO_WITH_EMAIL).copy(phase = CodePhase.Verifying)
        val resending = emailReady(TWO_WITH_EMAIL).copy(phase = CodePhase.Resending, resend = RESEND_SENT)

        val checked = (toStep(verifying) as MfaStep.Factor).step.model as EmailChallengeModel
        val sent = (toStep(resending) as MfaStep.Factor).step.model as EmailChallengeModel

        assertThat(checked.isVerifying).isTrue()
        assertThat(sent.isResending).isTrue()
        assertThat(sent.canResend).isFalse()
    }

    @Test
    fun `code screens - a connection error - reaches the model of a phone, an email and an authenticator app`() {
        val offline = MfaError.Network()

        val phone = (toStep(codeReady(TWO_WITH_SMS).copy(requestError = offline)) as MfaStep.Factor).step.model
        val email = (toStep(emailReady(TWO_WITH_EMAIL).copy(requestError = offline)) as MfaStep.Factor).step.model
        val app = (toStep(appReady(TWO_WITH_APP).copy(requestError = offline)) as MfaStep.Factor).step.model

        assertThat((phone as PhoneChallengeModel).requestError).isSameInstanceAs(offline)
        assertThat((email as EmailChallengeModel).requestError).isSameInstanceAs(offline)
        assertThat((app as TotpChallengeModel).requestError).isSameInstanceAs(offline)
    }

    @Test
    fun `code screens - an expired code - reaches the phone and email models, and Resend opens at once`() {
        val expired = codeReady(TWO_WITH_SMS).copy(resend = RESEND_OPEN, isCodeExpired = true)

        val phone = (toStep(expired) as MfaStep.Factor).step.model as PhoneChallengeModel
        val email = (toStep(expired.copy(factor = EMAIL)) as MfaStep.Factor).step.model as EmailChallengeModel

        assertThat(phone.isCodeExpired).isTrue()
        assertThat(phone.canResend).isTrue()
        assertThat(phone.resendAvailableIn).isEqualTo(0)
        assertThat(email.isCodeExpired).isTrue()
    }

    @Test
    fun `code screens - a resend refused as too many - reaches the model as the reason`() {
        val refused = MfaError.TooManyAttempts()

        val phone = (toStep(codeReady(TWO_WITH_SMS).copy(requestError = refused)) as MfaStep.Factor).step.model

        assertThat((phone as SentCodeChallengeModel).requestError).isSameInstanceAs(refused)
    }

    @Test
    fun `code screens - every code factor's model - is a code challenge, and only a sent code is also resendable`() {
        val phone = (toStep(codeReady(TWO_WITH_SMS)) as MfaStep.Factor).step.model
        val email = (toStep(emailReady(TWO_WITH_EMAIL)) as MfaStep.Factor).step.model
        val app = (toStep(appReady(TWO_WITH_APP)) as MfaStep.Factor).step.model

        assertThat(phone).isInstanceOf(SentCodeChallengeModel::class.java)
        assertThat(email).isInstanceOf(SentCodeChallengeModel::class.java)
        assertThat(app).isInstanceOf(CodeChallengeModel::class.java)
        assertThat(app).isNotInstanceOf(SentCodeChallengeModel::class.java)
    }

    @Test
    fun `code screens - no connection error - leave the model's error empty`() {
        val phone = (toStep(codeReady(TWO_WITH_SMS)) as MfaStep.Factor).step.model as PhoneChallengeModel
        val app = (toStep(appReady(TWO_WITH_APP)) as MfaStep.Factor).step.model as TotpChallengeModel

        assertThat(phone.requestError).isNull()
        assertThat(app.requestError).isNull()
    }

    @Test
    fun `email code - the only factor - cannot switch`() {
        val model = (toStep(emailReady(MfaRound(1, listOf(EMAIL)))) as MfaStep.Factor).step.model as EmailChallengeModel

        assertThat(model.canSwitch).isFalse()
    }

    @Test
    fun `email code - the taps - come back as intents`() {
        val model = (toStep(emailReady(TWO_WITH_EMAIL)) as MfaStep.Factor).step.model as EmailChallengeModel

        model.submit("123456")
        model.resend()
        model.switchFactor()
        model.cancel()

        assertThat(intents).containsExactly(
            MfaIntent.SubmitCode("123456"),
            MfaIntent.Resend,
            MfaIntent.ChooseAnotherFactor,
            MfaIntent.Cancel,
        ).inOrder()
    }

    @Test
    fun `list - email addresses - are one row each, named by the address, with the mail icon`() {
        val step = toStep(MfaState.ChooseFactor(MfaRound(1, listOf(EMAIL, OTHER_EMAIL)))) as MfaStep.AuthenticatorList

        assertThat(step.model.rows).containsExactly(
            AuthenticatorListModel.Row("email:5", MfaFactorId.Email, "j***@example.com", null, R.drawable.ic_mfa_email),
            AuthenticatorListModel.Row("email:6", MfaFactorId.Email, "w***@example.com", null, R.drawable.ic_mfa_email),
        ).inOrder()
    }

    @Test
    fun `select - an email row - picks that address`() {
        val step = toStep(MfaState.ChooseFactor(MfaRound(1, listOf(EMAIL, OTHER_EMAIL)))) as MfaStep.AuthenticatorList

        step.model.select("email:6")

        assertThat(intents).containsExactly(MfaIntent.SelectFactor(OTHER_EMAIL))
    }

    @Test
    fun `app code - the model carries the wrong-code and switch states`() {
        val state = appReady(TWO_WITH_APP).copy(isWrongCode = true)

        val factor = (toStep(state) as MfaStep.Factor).step
        val model = factor.model as TotpChallengeModel

        assertThat(factor.factor).isEqualTo(MfaFactorId.Totp)
        assertThat(factor.round).isEqualTo(1)
        assertThat(model.isWrongCode).isTrue()
        assertThat(model.canSwitch).isTrue()
        assertThat(model.isVerifying).isFalse()
    }

    @Test
    fun `app code - being checked - says so`() {
        val verifying = appReady(TWO_WITH_APP).copy(phase = CodePhase.Verifying)

        val model = (toStep(verifying) as MfaStep.Factor).step.model as TotpChallengeModel

        assertThat(model.isVerifying).isTrue()
    }

    @Test
    fun `app code - the only factor - cannot switch`() {
        val model = (toStep(appReady(MfaRound(1, listOf(APP)))) as MfaStep.Factor).step.model as TotpChallengeModel

        assertThat(model.canSwitch).isFalse()
    }

    @Test
    fun `app code - the taps - come back as intents`() {
        val model = (toStep(appReady(TWO_WITH_APP)) as MfaStep.Factor).step.model as TotpChallengeModel

        model.submit("123456")
        model.switchFactor()
        model.cancel()

        assertThat(intents).containsExactly(
            MfaIntent.SubmitCode("123456"),
            MfaIntent.ChooseAnotherFactor,
            MfaIntent.Cancel,
        ).inOrder()
    }

    @Test
    fun `list - the authenticator app - is one row with no name of its own, and the app icon`() {
        val step = toStep(MfaState.ChooseFactor(TWO_WITH_APP)) as MfaStep.AuthenticatorList

        assertThat(step.model.rows).containsExactly(
            AuthenticatorListModel.Row("totp", MfaFactorId.Totp, "", null, R.drawable.ic_authenticator),
            AuthenticatorListModel.Row("push:0", MfaFactorId.Push, "Jane's phone", null, R.drawable.ic_device),
        ).inOrder()
    }

    @Test
    fun `select - the authenticator app row - picks it`() {
        val step = toStep(MfaState.ChooseFactor(TWO_WITH_APP)) as MfaStep.AuthenticatorList

        step.model.select("totp")

        assertThat(intents).containsExactly(MfaIntent.SelectFactor(APP))
    }

    @Test
    fun `failed - any error, including no usable factor - is the error step with Start over`() {
        val errors = listOf(MfaError.Rejected(), MfaError.NoEligibleFactors(), MfaError.Network())

        errors.forEach { error ->
            val step = toStep(MfaState.Failed(error)) as MfaStep.Error

            assertThat(step.model.error).isSameInstanceAs(error)
            assertThat(step.model.exit).isEqualTo(MfaExit.StartOver)
        }
    }

    // endregion

    // region actions reach the engine

    @Test
    fun `select - a row - picks that device`() {
        val step = toStep(MfaState.ChooseFactor(TWO)) as MfaStep.AuthenticatorList

        step.model.select("push:1")

        assertThat(intents).containsExactly(MfaIntent.SelectFactor(TABLET))
    }

    @Test
    fun `select - a row that is not on offer - does nothing`() {
        val step = toStep(MfaState.ChooseFactor(TWO)) as MfaStep.AuthenticatorList

        step.model.select("push:9")

        assertThat(intents).isEmpty()
    }

    @Test
    fun `cancel - on the list, the loading step and the push - leaves MFA`() {
        val list = toStep(MfaState.ChooseFactor(TWO)) as MfaStep.AuthenticatorList
        val loading = toStep(MfaState.SendingChallenge(ONE, PHONE)) as MfaStep.Loading
        val push = (toStep(waiting(TWO, PHONE)) as MfaStep.Factor).step.model as PushChallengeModel

        list.model.cancel()
        loading.model.cancel!!()
        push.cancel()

        assertThat(intents).containsExactly(MfaIntent.Cancel, MfaIntent.Cancel, MfaIntent.Cancel)
    }

    @Test
    fun `resend and switch - on the push - reach the engine`() {
        val push = (toStep(waiting(TWO, PHONE)) as MfaStep.Factor).step.model as PushChallengeModel

        push.resend()
        push.switchFactor()

        assertThat(intents).containsExactly(MfaIntent.Resend, MfaIntent.ChooseAnotherFactor).inOrder()
    }

    @Test
    fun `leave - on the error step - goes to the host, not the engine`() {
        val step = toStep(MfaState.Failed(MfaError.Rejected())) as MfaStep.Error

        step.model.leave()

        assertThat(left).isEqualTo(1)
        assertThat(intents).isEmpty()
    }

    // endregion

    private fun codeReady(round: MfaRound) = MfaState.CodeEntry(
        round = round,
        factor = SMS,
        phase = CodePhase.Ready,
        resend = RESEND_OPEN,
    )

    private fun emailReady(round: MfaRound) = codeReady(round).copy(factor = EMAIL)

    /** Nothing is sent to an authenticator app, so it never offers Resend. */
    private fun appReady(round: MfaRound) = codeReady(round).copy(factor = APP, resend = null)

    private fun waiting(round: MfaRound, factor: MfaFactor.Push) = MfaState.PushWaiting(
        round = round,
        factor = factor,
        phase = PushPhase.Waiting,
        everyMs = POLL,
        canResend = true,
        resendSecondsLeft = 0,
    )

    private companion object {
        val PHONE = MfaFactor.Push(0, "Jane's phone")
        val TABLET = MfaFactor.Push(1, "Jane's tablet")
        val ONE = MfaRound(1, listOf(PHONE))
        val TWO = MfaRound(1, listOf(PHONE, TABLET))
        val SMS = MfaFactor.Phone(3, "***9239", PhoneDeliveryMethod.TEXT)
        val CALL = SMS.copy(delivery = PhoneDeliveryMethod.VOICE)
        val TWO_WITH_SMS = MfaRound(1, listOf(SMS, PHONE))
        val EMAIL = MfaFactor.Email(5, "j***@example.com")
        val OTHER_EMAIL = MfaFactor.Email(6, "w***@example.com")
        val TWO_WITH_EMAIL = MfaRound(1, listOf(EMAIL, PHONE))
        val APP = MfaFactor.AuthenticatorApp
        val TWO_WITH_APP = MfaRound(1, listOf(APP, PHONE))

        /** A resend on offer, with 42 seconds of wait left. */
        val RESEND_WAITING = CodeResend(isOffered = true, secondsLeft = 42)

        /** A resend that is going out, or was just used: not on offer. */
        val RESEND_SENT = CodeResend(isOffered = false, secondsLeft = 0)

        /** A resend the server offers, with no wait left. */
        val RESEND_OPEN = CodeResend(isOffered = true, secondsLeft = 0)
        const val POLL = 3_000L
    }
}
