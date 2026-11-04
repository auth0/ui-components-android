package com.auth0.universalcomponents.feature.mfachallenge

import android.content.res.Resources
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class MfaStringsTest {

    // Strict: a call that isn't stubbed fails the test, so reading a word the wrong way is caught.
    private val resources = mockk<Resources>()

    @Test
    fun `no argument - the word is read as it is, so a percent sign in it is safe`() {
        every { resources.getString(WORD) } returns "100% sure"

        assertThat(resources.asMfaStrings().get(WORD)).isEqualTo("100% sure")
        // getString(id, *args) formats even with no args, and "100% sure" is not a valid format.
        verify(exactly = 0) { resources.getString(WORD, *anyVararg()) }
    }

    @Test
    fun `one argument - the word is formatted with it`() {
        every { resources.getString(WORD, "Jane's phone") } returns "Sent to Jane's phone."

        assertThat(resources.asMfaStrings().get(WORD, "Jane's phone")).isEqualTo("Sent to Jane's phone.")
    }

    private companion object {
        const val WORD = 7
    }
}
