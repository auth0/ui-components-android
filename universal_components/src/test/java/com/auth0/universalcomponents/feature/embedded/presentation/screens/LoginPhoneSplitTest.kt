package com.auth0.universalcomponents.feature.embedded.presentation.screens

import com.auth0.universalcomponents.presentation.ui.mfa.phone.Country
import com.auth0.universalcomponents.presentation.ui.mfa.phone.forIso
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LoginPhoneSplitTest {

    private val us = checkNotNull(Country.forIso("US"))
    private val canada = checkNotNull(Country.forIso("CA"))
    private val india = checkNotNull(Country.forIso("IN"))

    @Test
    fun `a number whose code several countries share - comes back on the device's country when it is one of them`() {
        assertThat(splitPhone("+14155550123", fallback = canada)).isEqualTo(canada to "4155550123")
        assertThat(splitPhone("+14155550123", fallback = us)).isEqualTo(us to "4155550123")
    }

    @Test
    fun `a number whose code several countries share - on a device outside them, comes back as the United States`() {
        // Not the first listed (Antigua and Barbuda), as on iOS.
        assertThat(splitPhone("+14155550123", fallback = india)).isEqualTo(us to "4155550123")
    }

    @Test
    fun `a number with a code of its own - comes back on that country, whatever the device`() {
        assertThat(splitPhone("+919876543210", fallback = us)).isEqualTo(india to "9876543210")
    }

    @Test
    fun `a number with no plus - is taken as typed, in the fallback country`() {
        assertThat(splitPhone("4155550123", fallback = india)).isEqualTo(india to "4155550123")
    }
}
