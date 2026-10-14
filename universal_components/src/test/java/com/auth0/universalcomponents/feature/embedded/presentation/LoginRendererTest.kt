package com.auth0.universalcomponents.feature.embedded.presentation

import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.domain.model.FailureMessage
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginStep
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaChallenge
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LoginRendererTest {

    @Test
    fun `plan - each step - gets its screen and data`() {
        val message = FailureMessage.Local(R.string.embedded_login_error_generic)
        val expected = mapOf(
            LoginStep.Loading to ScreenPlan.Loading,
            LoginStep.CollectOtpIdentifier(EMAIL) to ScreenPlan.Identifier(EMAIL),
            LoginStep.CollectOtpCode(EMAIL) to ScreenPlan.OtpCode(EMAIL),
            LoginStep.StepUpMfa(MfaChallenge(MfaFactor.EMAIL, EMAIL)) to ScreenPlan.MfaCode(MfaFactor.EMAIL, EMAIL),
            LoginStep.Authenticated to ScreenPlan.Authenticated,
            LoginStep.Failed(message) to ScreenPlan.Failed(message),
            LoginStep.NoMethodsAvailable to ScreenPlan.NoMethods,
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
    fun `transitionIdentity - code step to step-up - animates`() {
        assertThat(ScreenPlan.MfaCode(MfaFactor.EMAIL, EMAIL).transitionIdentity)
            .isNotEqualTo(ScreenPlan.OtpCode(EMAIL).transitionIdentity)
        assertThat(ScreenPlan.MfaCode(MfaFactor.EMAIL, null).transitionIdentity)
            .isNotEqualTo(ScreenPlan.MfaCode(MfaFactor.OTP, null).transitionIdentity)
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

    @Test
    fun `navigationDepth - code step and step-up - sit at the same depth`() {
        assertThat(ScreenPlan.MfaCode(MfaFactor.OTP, null).navigationDepth)
            .isEqualTo(ScreenPlan.OtpCode(EMAIL).navigationDepth)
    }

    private companion object {
        const val EMAIL = "jane@example.com"
    }
}
