package com.auth0.universalcomponents.feature.embedded

import androidx.compose.runtime.Composable
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EmbeddedEntryConfigurationTest {

    private val themeLogo: @Composable () -> Unit = {}
    private val appLogo: @Composable () -> Unit = {}

    @Test
    fun `nothing set - the theme's logo, the centered layout and the way's own title`() {
        val entry = EmbeddedEntryConfiguration()

        assertThat(entry.logo).isEqualTo(EmbeddedEntryLogo.Theme)
        assertThat(entry.layout).isSameInstanceAs(EmbeddedEntryLayout.Centered)
        assertThat(entry.messages.loginTitle).isNull()
    }

    @Test
    fun `one part set - the others keep their default`() {
        val entry = EmbeddedEntryConfiguration(messages = EmbeddedEntryMessages(loginTitle = "Welcome back"))

        assertThat(entry.messages.loginTitle).isEqualTo("Welcome back")
        assertThat(entry.logo).isEqualTo(EmbeddedEntryLogo.Theme)
        assertThat(entry.layout).isSameInstanceAs(EmbeddedEntryLayout.Centered)
    }

    @Test
    fun `theme logo - the theme's is drawn`() {
        assertThat(EmbeddedEntryLogo.Theme.resolve(themeLogo)).isSameInstanceAs(themeLogo)
    }

    @Test
    fun `custom logo - the app's own is drawn, not the theme's`() {
        assertThat(EmbeddedEntryLogo.Custom(appLogo).resolve(themeLogo)).isSameInstanceAs(appLogo)
    }

    @Test
    fun `hidden logo - nothing is drawn`() {
        assertThat(EmbeddedEntryLogo.Hidden.resolve(themeLogo)).isNull()
    }

    @Test
    fun `the two ready-made layouts - are two different layouts`() {
        assertThat(EmbeddedEntryLayout.Centered).isNotSameInstanceAs(EmbeddedEntryLayout.Top)
    }
}
