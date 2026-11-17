package com.auth0.universalcomponents.feature.embedded.presentation

import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption
import com.auth0.universalcomponents.feature.mfachallenge.presentation.MfaEngine
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.junit.Test

class LoginRendererTest {

    @Test
    fun `plan - each step - gets its screen and data`() {
        val error = EmbeddedLoginError.Server("Your account is blocked.")
        val expected = mapOf(
            LoginState.Loading to ScreenPlan.Loading,
            LoginState.CollectOtpIdentifier(value = EMAIL) to ScreenPlan.Identifier(EMAIL),
            LoginState.CollectOtpCode(EMAIL) to ScreenPlan.OtpCode(EMAIL),
            LoginState.Authenticated to ScreenPlan.Authenticated,
            LoginState.Failed(error) to ScreenPlan.Failed(error),
            LoginState.NoMethodsAvailable to ScreenPlan.NoMethods,
        )
        expected.forEach { (step, plan) -> assertThat(plan(step)).isEqualTo(plan) }
    }

    @Test
    fun `plan - input steps - carry their error, busy state and countdown to the screen`() {
        val expected = mapOf(
            LoginState.CollectOtpIdentifier(value = EMAIL, isSubmitting = true) to
                ScreenPlan.Identifier(EMAIL, isSubmitting = true),
            LoginState.CollectOtpCode(
                EMAIL,
                error = EmbeddedLoginError.InvalidCode,
                isSubmitting = true,
                isResending = true,
                resendSecondsLeft = 42,
            ) to ScreenPlan.OtpCode(
                EMAIL,
                error = EmbeddedLoginError.InvalidCode,
                isSubmitting = true,
                isResending = true,
                resendSecondsLeft = 42,
            ),
        )
        expected.forEach { (step, plan) -> assertThat(plan(step)).isEqualTo(plan) }
    }

    @Test
    fun `plan - second factor - hands MFA its engine to draw from`() {
        val engine = mockk<MfaEngine>()

        assertThat(plan(LoginState.Mfa(engine, EMAIL, LoginOption.EmailOtp))).isEqualTo(ScreenPlan.Mfa(engine))
    }

    @Test
    fun `plan - waiting for the app after MFA ended - is MFA's own plan, told to wait`() {
        val engine = mockk<MfaEngine>()

        assertThat(plan(LoginState.AwaitingApp(engine, EMAIL, LoginOption.EmailOtp)))
            .isEqualTo(ScreenPlan.Mfa(engine, isAwaitingApp = true))
    }

    @Test
    fun `transitionIdentity - waiting for the app - is still the MFA screen, so the wait never slides MFA out`() {
        val engine = mockk<MfaEngine>()

        assertThat(ScreenPlan.Mfa(engine, isAwaitingApp = true).transitionIdentity)
            .isEqualTo(ScreenPlan.Mfa(engine).transitionIdentity)
    }

    @Test
    fun `transitionIdentity - same screen with new data - doesn't animate`() {
        assertThat(ScreenPlan.Identifier("").transitionIdentity)
            .isEqualTo(ScreenPlan.Identifier(EMAIL).transitionIdentity)
        assertThat(ScreenPlan.OtpCode("a@example.com").transitionIdentity)
            .isEqualTo(ScreenPlan.OtpCode(EMAIL).transitionIdentity)
    }

    @Test
    fun `transitionIdentity - MFA changing its own screens - is one screen to login, so only MFA animates`() {
        val first = ScreenPlan.Mfa(mockk())
        val second = ScreenPlan.Mfa(mockk())

        assertThat(first.transitionIdentity).isEqualTo(second.transitionIdentity)
        assertThat(first.transitionIdentity).isNotEqualTo(ScreenPlan.OtpCode(EMAIL).transitionIdentity)
    }

    @Test
    fun `transitionIdentity - email and phone entry - are different screens, so switching slides`() {
        assertThat(ScreenPlan.Identifier(EMAIL).transitionIdentity)
            .isNotEqualTo(ScreenPlan.Identifier(PHONE, LoginOption.PhoneOtp).transitionIdentity)
    }

    @Test
    fun `plan - phone steps - carry the method and the ways to switch`() {
        val expected = mapOf(
            LoginState.CollectOtpIdentifier(
                LoginOption.EmailOtp,
                EMAIL,
                otherMethods = listOf(LoginOption.PhoneOtp, LoginOption.Username),
            ) to ScreenPlan.Identifier(
                EMAIL,
                LoginOption.EmailOtp,
                otherMethods = listOf(LoginOption.PhoneOtp, LoginOption.Username),
            ),
            LoginState.CollectOtpIdentifier(LoginOption.PhoneOtp, PHONE, isFirst = false) to
                ScreenPlan.Identifier(PHONE, LoginOption.PhoneOtp, isFirst = false),
            LoginState.CollectOtpCode(PHONE, LoginOption.PhoneOtp) to ScreenPlan.OtpCode(PHONE, LoginOption.PhoneOtp),
            LoginState.CollectOtpCode(PHONE, LoginOption.PhoneOtp, sentTo = "XXXXXXXXX0128") to
                ScreenPlan.OtpCode(PHONE, LoginOption.PhoneOtp, sentTo = "XXXXXXXXX0128"),
        )
        expected.forEach { (step, plan) -> assertThat(plan(step)).isEqualTo(plan) }
    }

    @Test
    fun `navigationDepth - moving forward through the flow - pushes`() {
        val depths = listOf(
            ScreenPlan.Loading,
            ScreenPlan.Identifier(EMAIL),
            ScreenPlan.Identifier(PHONE, LoginOption.PhoneOtp, isFirst = false),
            ScreenPlan.OtpCode(EMAIL),
            ScreenPlan.Mfa(mockk()),
            ScreenPlan.Authenticated,
        ).map { it.navigationDepth }

        assertThat(depths).isInStrictOrder()
    }

    @Test
    fun `plan - password and username steps - carry who, how to finish and what is on its way`() {
        val error = EmbeddedLoginError.InvalidPassword(LoginOption.EmailOtp)
        val expected = mapOf(
            LoginState.CollectPassword(EMAIL, LoginOption.EmailOtp) to ScreenPlan.Password(EMAIL),
            LoginState.CollectPassword(
                EMAIL,
                LoginOption.EmailOtp,
                canSendCode = true,
                error = error,
                isSubmitting = true,
                isSendingCode = true,
                resendSecondsLeft = 42,
            ) to ScreenPlan.Password(
                EMAIL,
                LoginOption.EmailOtp,
                canSendCode = true,
                error = error,
                isSubmitting = true,
                isSendingCode = true,
                resendSecondsLeft = 42,
            ),
            LoginState.CollectOtpIdentifier(LoginOption.Username, USERNAME, error = error) to
                ScreenPlan.Identifier(USERNAME, LoginOption.Username, error = error),
            LoginState.CollectOtpCode(EMAIL, canUsePassword = true, isOpeningPassword = true) to
                ScreenPlan.OtpCode(EMAIL, canUsePassword = true, isOpeningPassword = true),
        )
        expected.forEach { (step, plan) -> assertThat(plan(step)).isEqualTo(plan) }
    }

    @Test
    fun `transitionIdentity - password step - is its own screen, apart from the code`() {
        assertThat(ScreenPlan.Password(EMAIL).transitionIdentity)
            .isNotEqualTo(ScreenPlan.OtpCode(EMAIL).transitionIdentity)
        assertThat(ScreenPlan.Password("a@example.com").transitionIdentity)
            .isEqualTo(ScreenPlan.Password(EMAIL).transitionIdentity)
    }

    @Test
    fun `transitionIdentity - username entry - is its own screen`() {
        assertThat(ScreenPlan.Identifier(USERNAME, LoginOption.Username).transitionIdentity)
            .isNotEqualTo(ScreenPlan.Identifier(EMAIL).transitionIdentity)
    }

    @Test
    fun `isSiblingOf - the code and the password - are two ways of finishing one step, and nothing else is`() {
        assertThat(ScreenPlan.OtpCode(EMAIL).isSiblingOf(ScreenPlan.Password(EMAIL))).isTrue()
        assertThat(ScreenPlan.Password(EMAIL).isSiblingOf(ScreenPlan.OtpCode(EMAIL))).isTrue()
        assertThat(ScreenPlan.Identifier(EMAIL).isSiblingOf(ScreenPlan.Password(EMAIL))).isFalse()
        assertThat(ScreenPlan.OtpCode(EMAIL).isSiblingOf(ScreenPlan.OtpCode(EMAIL))).isFalse()
    }

    @Test
    fun `navigationDepth - a method's own screen - is one deeper than the first, whichever method it is`() {
        val first = ScreenPlan.Identifier(PHONE, LoginOption.PhoneOtp).navigationDepth
        val own = ScreenPlan.Identifier(EMAIL, LoginOption.EmailOtp, isFirst = false).navigationDepth

        assertThat(own).isEqualTo(first + 1)
        assertThat(ScreenPlan.Password(EMAIL).navigationDepth).isEqualTo(ScreenPlan.OtpCode(EMAIL).navigationDepth)
        assertThat(ScreenPlan.Password(EMAIL).navigationDepth).isGreaterThan(own)
    }

    @Test
    fun `needsNewCode - only a code that can't be checked any more`() {
        assertThat(EmbeddedLoginError.CodeExpired.needsNewCode).isTrue()
        assertThat(EmbeddedLoginError.CodeNoLongerValid.needsNewCode).isTrue()
        assertThat(EmbeddedLoginError.InvalidCode.needsNewCode).isFalse()
        assertThat(EmbeddedLoginError.TooManyWrongCodes.needsNewCode).isFalse()
    }

    private companion object {
        const val EMAIL = "jane@example.com"
        const val USERNAME = "jane.doe"
        const val PHONE = "+14233530900"
    }
}
