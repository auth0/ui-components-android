package com.auth0.universalcomponents.feature.embedded.presentation

import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.mfachallenge.presentation.MfaEngine
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.junit.Test

class LoginRendererTest {

    @Test
    fun `plan - each step - gets its screen and data`() {
        val error = EmbeddedLoginError.Server("Your account is blocked.")
        val expected = mapOf(
            LoginStep.Loading to ScreenPlan.Loading,
            LoginStep.CollectOtpIdentifier(EMAIL) to ScreenPlan.Identifier(EMAIL),
            LoginStep.CollectOtpCode(EMAIL) to ScreenPlan.OtpCode(EMAIL),
            LoginStep.Authenticated to ScreenPlan.Authenticated,
            LoginStep.Failed(error) to ScreenPlan.Failed(error),
            LoginStep.NoMethodsAvailable to ScreenPlan.NoMethods,
        )
        expected.forEach { (step, plan) -> assertThat(plan(step)).isEqualTo(plan) }
    }

    @Test
    fun `plan - input steps - carry their error, busy state and countdown to the screen`() {
        val expected = mapOf(
            LoginStep.CollectOtpIdentifier(EMAIL, isSubmitting = true) to
                ScreenPlan.Identifier(EMAIL, isSubmitting = true),
            LoginStep.CollectOtpCode(
                EMAIL,
                EmbeddedLoginError.InvalidCode,
                isSubmitting = true,
                isResending = true,
                resendSecondsLeft = 42,
            ) to ScreenPlan.OtpCode(
                EMAIL,
                EmbeddedLoginError.InvalidCode,
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

        assertThat(plan(LoginStep.Mfa(engine, EMAIL))).isEqualTo(ScreenPlan.Mfa(engine))
    }

    @Test
    fun `plan - waiting for the app after MFA ended - is MFA's own plan, told to wait`() {
        val engine = mockk<MfaEngine>()

        assertThat(plan(LoginStep.AwaitingApp(engine, EMAIL))).isEqualTo(ScreenPlan.Mfa(engine, isAwaitingApp = true))
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
    fun `navigationDepth - moving forward through the flow - pushes`() {
        val depths = listOf(
            ScreenPlan.Loading,
            ScreenPlan.Identifier(EMAIL),
            ScreenPlan.OtpCode(EMAIL),
            ScreenPlan.Mfa(mockk()),
            ScreenPlan.Authenticated,
        ).map { it.navigationDepth }

        assertThat(depths).isInStrictOrder()
    }

    private companion object {
        const val EMAIL = "jane@example.com"
    }
}
