package com.auth0.universalcomponents.feature.mfachallenge.presentation.screens

import android.content.res.Resources
import com.auth0.universalcomponents.feature.mfachallenge.AuthenticatorListModel
import com.auth0.universalcomponents.feature.mfachallenge.MfaFactorId
import com.auth0.universalcomponents.feature.mfachallenge.MfaMessages
import com.auth0.universalcomponents.feature.mfachallenge.presentation.resolve
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class MfaEmailWordsTest {

    /** Stands in for the string resources: each default is its resource id and its argument. */
    private val resources = mockk<Resources> {
        every { getString(any<Int>()) } answers { "default(${firstArg<Int>()})" }
        every { getString(any<Int>(), any()) } answers {
            "default(${firstArg<Int>()},${secondArg<Array<Any>>().joinToString(",")})"
        }
    }

    private val words = MfaMessages().resolve(resources)

    private fun emailRow(address: String) =
        AuthenticatorListModel.Row(id = "email-6", factor = MfaFactorId.Email, title = address)

    @Test
    fun `email row - an address - leads with the factor's name and names the address under it`() {
        val shown = emailRow("j***@example.com").words(words)

        assertThat(shown.title).isEqualTo(words.emailChallenge.row.title)
        assertThat(shown.description).isEqualTo("j***@example.com")
    }

    @Test
    fun `email row - no address - leads with the factor's name and its own description under it`() {
        val shown = emailRow("").words(words)

        assertThat(shown.title).isEqualTo(words.emailChallenge.row.title)
        assertThat(shown.description).isEqualTo(words.emailChallenge.row.description)
        assertThat(shown.description).isNotEmpty()
    }

    @Test
    fun `email row - a blank address - is read as no address`() {
        val shown = emailRow("   ").words(words)

        assertThat(shown.description).isEqualTo(words.emailChallenge.row.description)
    }

    @Test
    fun `code screen subtitle - an address - names where the code went`() {
        val subtitle = words.emailChallenge.subtitleFor("j***@example.com")

        assertThat(subtitle).isEqualTo(words.emailChallenge.description("j***@example.com"))
        assertThat(subtitle).contains("j***@example.com")
    }

    @Test
    fun `code screen subtitle - no address, or a blank one - says it went by email without naming it`() {
        assertThat(words.emailChallenge.subtitleFor("")).isEqualTo(words.emailChallenge.descriptionUnnamed)
        assertThat(words.emailChallenge.subtitleFor("  ")).isEqualTo(words.emailChallenge.descriptionUnnamed)
    }
}
