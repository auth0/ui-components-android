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

class MfaTotpWordsTest {

    /** Stands in for the string resources: each default is its resource id. */
    private val resources = mockk<Resources> {
        every { getString(any<Int>()) } answers { "default(${firstArg<Int>()})" }
    }

    private val words = MfaMessages().resolve(resources)

    private fun totpRow(title: String) =
        AuthenticatorListModel.Row(id = "totp", factor = MfaFactorId.Totp, title = title)

    @Test
    fun `authenticator app row - has no name of its own - is the factor's name with its description under it`() {
        val shown = totpRow("").words(words)

        assertThat(shown.title).isEqualTo(words.totpChallenge.row.title)
        assertThat(shown.description).isEqualTo(words.totpChallenge.row.description)
        assertThat(shown.description).isNotEmpty()
    }

    @Test
    fun `authenticator app row - the app's own words - replace both lines`() {
        val custom = MfaMessages {
            totpChallenge {
                row {
                    title = "Authenticator"
                    description = "Open it"
                }
            }
        }.resolve(resources)

        val shown = totpRow("").words(custom)

        assertThat(shown.title).isEqualTo("Authenticator")
        assertThat(shown.description).isEqualTo("Open it")
    }
}
