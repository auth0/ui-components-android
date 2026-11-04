package com.auth0.universalcomponents.feature.mfachallenge

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MfaScreensTest {

    @Test
    fun `nothing set - every screen is the standard one`() {
        val screens = MfaScreens()

        assertThat(screens.authenticatorList).isNull()
        assertThat(screens.error).isNull()
        assertThat(screens.factors).isEmpty()
    }

    @Test
    fun `one screen set - that one is replaced and the others stay standard`() {
        val screens = MfaScreens { pushChallenge { _, _ -> } }

        assertThat(screens.factors).containsKey(MfaFactorId.Push)
        assertThat(screens.authenticatorList).isNull()
        assertThat(screens.error).isNull()
    }

    @Test
    fun `replace - a factor by its id - is the same as its typed shortcut`() {
        val byId = MfaScreens { replace(MfaFactorId.Push) { _, _ -> } }
        val byShortcut = MfaScreens { pushChallenge { _, _ -> } }

        assertThat(byId.factors.keys).isEqualTo(byShortcut.factors.keys)
    }

    @Test
    fun `replace - several screens - are all kept`() {
        val screens = MfaScreens {
            authenticatorList { _, _ -> }
            error { _, _ -> }
            replace(MfaFactorId("other")) { _, _ -> }
        }

        assertThat(screens.authenticatorList).isNotNull()
        assertThat(screens.error).isNotNull()
        assertThat(screens.factors).containsKey(MfaFactorId("other"))
    }

    @Test
    fun `configuration - nothing set - is standard presentation with default words and screens`() {
        val configuration = MfaConfiguration()

        assertThat(configuration.presentation).isEqualTo(MfaPresentation.Standard)
        assertThat(configuration.screens.factors).isEmpty()
    }

    @Test
    fun `headless - holds the app's stream`() {
        val stream = MfaStepStream()

        val presentation = MfaPresentation.Headless(stream)

        assertThat(presentation.stream).isSameInstanceAs(stream)
    }
}
