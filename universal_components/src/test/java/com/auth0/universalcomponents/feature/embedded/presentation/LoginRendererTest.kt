package com.auth0.universalcomponents.feature.embedded.presentation

import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.google.common.truth.Truth.assertThat
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
    fun `transitionIdentity - same screen with new data - doesn't animate`() {
        assertThat(ScreenPlan.Identifier("").transitionIdentity)
            .isEqualTo(ScreenPlan.Identifier(EMAIL).transitionIdentity)
        assertThat(ScreenPlan.OtpCode("a@example.com").transitionIdentity)
            .isEqualTo(ScreenPlan.OtpCode(EMAIL).transitionIdentity)
    }

    @Test
    fun `navigationDepth - moving forward through the flow - pushes`() {
        val depths = listOf(
            ScreenPlan.Loading,
            ScreenPlan.Identifier(EMAIL),
            ScreenPlan.OtpCode(EMAIL),
            ScreenPlan.Authenticated,
        ).map { it.navigationDepth }

        assertThat(depths).isInStrictOrder()
    }

    private companion object {
        const val EMAIL = "jane@example.com"
    }
}
