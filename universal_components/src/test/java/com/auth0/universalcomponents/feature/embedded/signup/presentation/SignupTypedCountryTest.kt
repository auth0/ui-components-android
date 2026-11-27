package com.auth0.universalcomponents.feature.embedded.signup.presentation

import com.auth0.universalcomponents.presentation.ui.mfa.phone.Country
import com.auth0.universalcomponents.presentation.ui.mfa.phone.forIso
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SignupTypedCountryTest {

    private val canada = checkNotNull(Country.forIso("CA"))

    @Test
    fun `a number typed with a country - keeps it while the field holds that number`() {
        assertThat(canada.keptFor("+14155550123")).isEqualTo(canada)
    }

    @Test
    fun `no country typed with yet - leaves the choice to the phone screen`() {
        assertThat(null.keptFor("+14155550123")).isNull()
    }

    @Test
    fun `an empty field - starts afresh, on the screen's own country`() {
        assertThat(canada.keptFor("")).isNull()
    }

    @Test
    fun `a number of another country - does not keep the old one`() {
        assertThat(canada.keptFor("+919876543210")).isNull()
    }
}
