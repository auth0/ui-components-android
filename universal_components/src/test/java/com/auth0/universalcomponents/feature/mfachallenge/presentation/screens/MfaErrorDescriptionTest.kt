package com.auth0.universalcomponents.feature.mfachallenge.presentation.screens

import com.auth0.universalcomponents.feature.mfachallenge.MfaError
import com.auth0.universalcomponents.feature.mfachallenge.ResolvedMfaMessages
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MfaErrorDescriptionTest {

    private val words = ResolvedMfaMessages.Message(title = "Title", description = "Our words.")

    @Test
    fun `unexpected - the server explained - its short explanation is shown`() {
        val error = MfaError.Unexpected("Your account is blocked.")

        assertThat(error.descriptionToShow(words)).isEqualTo("Your account is blocked.")
    }

    @Test
    fun `unexpected - a very long explanation - is cut short, with an ellipsis`() {
        val error = MfaError.Unexpected("x".repeat(MAX_SERVER_EXPLANATION + 50))

        val shown = error.descriptionToShow(words)

        assertThat(shown).hasLength(MAX_SERVER_EXPLANATION + 1)
        assertThat(shown).endsWith("…")
    }

    @Test
    fun `unexpected - no explanation, or a blank one - shows our words`() {
        assertThat(MfaError.Unexpected().descriptionToShow(words)).isEqualTo("Our words.")
        assertThat(MfaError.Unexpected("   ").descriptionToShow(words)).isEqualTo("Our words.")
    }

    @Test
    fun `any other error - shows our words`() {
        assertThat(MfaError.Rejected().descriptionToShow(words)).isEqualTo("Our words.")
    }
}
