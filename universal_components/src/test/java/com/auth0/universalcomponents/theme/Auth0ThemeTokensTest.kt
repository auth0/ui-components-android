package com.auth0.universalcomponents.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.auth0.universalcomponents.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class Auth0ThemeTokensTest {

    private val light = Auth0Color.light()
    private val dark = Auth0Color.dark()

    @Test
    fun `light colors - the link and success colors of iOS`() {
        assertThat(light.foregroundLinkPrimary).isEqualTo(Color(0xFF9921FE))
        assertThat(light.foregroundSuccess).isEqualTo(Color(0xFF218C4D))
    }

    @Test
    fun `dark colors - lighter ones, as on iOS`() {
        assertThat(dark.foregroundLinkPrimary).isEqualTo(Color(0xFFB570FF))
        assertThat(dark.foregroundSuccess).isEqualTo(Color(0xFF34D399))
    }

    @Test
    fun `colors written before the foreground ones - get those of the mode they are used in`() {
        val before = dark.copy(foregroundLinkPrimary = Color.Unspecified, foregroundSuccess = Color.Unspecified)

        val inDark = before.withDefaultsFrom(dark)
        val inLight = before.withDefaultsFrom(light)

        assertThat(inDark.foregroundLinkPrimary).isEqualTo(dark.foregroundLinkPrimary)
        assertThat(inDark.foregroundSuccess).isEqualTo(dark.foregroundSuccess)
        assertThat(inLight.foregroundLinkPrimary).isEqualTo(light.foregroundLinkPrimary)
        assertThat(inLight.foregroundSuccess).isEqualTo(light.foregroundSuccess)
    }

    @Test
    fun `a foreground color the app set - is kept, and the one it left out is filled in`() {
        val brand = Color(0xFF123456)
        val custom = light.copy(foregroundLinkPrimary = brand, foregroundSuccess = Color.Unspecified)

        val resolved = custom.withDefaultsFrom(dark)

        assertThat(resolved.foregroundLinkPrimary).isEqualTo(brand)
        assertThat(resolved.foregroundSuccess).isEqualTo(dark.foregroundSuccess)
    }

    @Test
    fun `built-in colors to fill in - follow the surfaces of the set, not the phone's mode`() {
        assertThat(light.builtInForSurfaces()).isEqualTo(light)
        assertThat(dark.builtInForSurfaces()).isEqualTo(dark)
    }

    @Test
    fun `a fixed light set with no foreground colors - gets the light ones, whatever the phone's mode`() {
        val before = light.copy(foregroundLinkPrimary = Color.Unspecified, foregroundSuccess = Color.Unspecified)

        val resolved = before.withDefaultsFrom(before.builtInForSurfaces())

        assertThat(resolved.foregroundLinkPrimary).isEqualTo(light.foregroundLinkPrimary)
        assertThat(resolved.foregroundSuccess).isEqualTo(light.foregroundSuccess)
    }

    @Test
    fun `a fixed dark set with no foreground colors - gets the dark ones`() {
        val before = dark.copy(foregroundLinkPrimary = Color.Unspecified, foregroundSuccess = Color.Unspecified)

        val resolved = before.withDefaultsFrom(before.builtInForSurfaces())

        assertThat(resolved.foregroundLinkPrimary).isEqualTo(dark.foregroundLinkPrimary)
        assertThat(resolved.foregroundSuccess).isEqualTo(dark.foregroundSuccess)
    }

    @Test
    fun `filling in the foreground colors - changes no other color`() {
        val custom = light.copy(textBold = Color.Red, foregroundSuccess = Color.Unspecified)

        val resolved = custom.withDefaultsFrom(dark)

        assertThat(resolved).isEqualTo(custom.copy(foregroundSuccess = dark.foregroundSuccess))
    }

    @Test
    fun `default sizes - a 40 logo and a 44 status icon, as on iOS`() {
        val sizes = Auth0Sizes.default()

        assertThat(sizes.logoHeight).isEqualTo(40.dp)
        assertThat(sizes.statusIcon).isEqualTo(44.dp)
    }

    @Test
    fun `sizes written before the logo and status icon - still compile, and get them`() {
        val before = Auth0Sizes(
            buttonHeight = 56.dp,
            inputHeight = 68.dp,
            otpFieldWidth = 52.dp,
            otpFieldHeight = 60.dp,
            codeContainerHeight = 56.dp,
            iconSmall = 16.dp,
            iconMedium = 24.dp,
            iconLarge = 32.dp,
            padding = 16.dp,
            paddingLarge = 24.dp,
        )

        assertThat(before.logoHeight).isEqualTo(40.dp)
        assertThat(before.statusIcon).isEqualTo(44.dp)
    }

    @Test
    fun `no brand set - Auth0's logo, described as Auth0, left to the library to tint`() {
        val brand = Auth0Brand()

        assertThat(brand.logo).isEqualTo(R.drawable.uc_auth0_logo)
        assertThat(brand.logoDescription).isNull()
        assertThat(brand.descriptionOrDefault()).isEqualTo("Auth0")
        assertThat(brand.logoTint).isNull()
    }

    @Test
    fun `the app's own logo with no description - is not announced as Auth0`() {
        val brand = Auth0Brand(logo = R.drawable.ic_email)

        assertThat(brand.descriptionOrDefault()).isNull()
    }

    @Test
    fun `a description the app set - is what a screen reader says, for any logo`() {
        assertThat(Auth0Brand(logoDescription = "Acme").descriptionOrDefault()).isEqualTo("Acme")
        assertThat(Auth0Brand(logo = R.drawable.uc_auth0_logo, logoDescription = "Acme").descriptionOrDefault())
            .isEqualTo("Acme")
    }

    @Test
    fun `Auth0's own logo, left to the library - is tinted in the text color`() {
        assertThat(Auth0Brand().tintOr(Color.Black)).isEqualTo(Color.Black)
    }

    @Test
    fun `the app's own logo, left to the library - is drawn as it is`() {
        val brand = Auth0Brand(logo = R.drawable.ic_email)

        assertThat(brand.tintOr(Color.Black)).isEqualTo(Color.Unspecified)
    }

    @Test
    fun `a tint the app set - wins, for its logo and for Auth0's`() {
        val tint = Color(0xFF123456)

        assertThat(Auth0Brand(logoTint = tint).tintOr(Color.Black)).isEqualTo(tint)

        val own = Auth0Brand(logo = R.drawable.ic_email, logoTint = tint)
        assertThat(own.tintOr(Color.Black)).isEqualTo(tint)
    }

    @Test
    fun `no tint asked for - Auth0's logo is drawn as it is too`() {
        assertThat(Auth0Brand(logoTint = Color.Unspecified).tintOr(Color.Black)).isEqualTo(Color.Unspecified)
    }

    @Test
    fun `a theme configuration written before the brand - has none, so Auth0's applies`() {
        assertThat(Auth0ThemeConfiguration(color = light).brand).isNull()
        assertThat(Auth0ThemeConfiguration.Default.brand).isNull()
    }
}
