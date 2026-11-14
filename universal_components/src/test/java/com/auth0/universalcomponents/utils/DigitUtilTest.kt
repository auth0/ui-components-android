package com.auth0.universalcomponents.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DigitUtilTest {

    @Test
    fun `ascii digit - 0 to 9 - is a digit`() {
        assertThat(('0'..'9').all(Char::isAsciiDigit)).isTrue()
    }

    @Test
    fun `ascii digit - digits of other scripts - are not, though Char isDigit takes them`() {
        val arabicIndic = '٣'
        val fullwidth = '３'

        assertThat(arabicIndic.isDigit()).isTrue()
        assertThat(fullwidth.isDigit()).isTrue()
        assertThat(arabicIndic.isAsciiDigit()).isFalse()
        assertThat(fullwidth.isAsciiDigit()).isFalse()
    }

    @Test
    fun `ascii digit - letters, signs and spaces - are not`() {
        assertThat("a-+ .,/:".any(Char::isAsciiDigit)).isFalse()
    }

    @Test
    fun `filtering a typed or pasted code - keeps only the ascii digits`() {
        assertThat("12 3-4a5٦７".filter(Char::isAsciiDigit)).isEqualTo("12345")
    }

    @Test
    fun `to ascii digits - digits of other scripts - are converted, not dropped`() {
        assertThat("\u0663\u0664".toAsciiDigits()).isEqualTo("34") // Arabic-Indic
        assertThat("\u06F1\u06F2".toAsciiDigits()).isEqualTo("12") // Persian
        assertThat("\u0967\u0968".toAsciiDigits()).isEqualTo("12") // Devanagari
        assertThat("\uFF11\uFF12".toAsciiDigits()).isEqualTo("12") // fullwidth
    }

    @Test
    fun `to ascii digits - anything that is not a digit - is dropped`() {
        assertThat("1a 2-3\u00B2".toAsciiDigits()).isEqualTo("123")
        assertThat("".toAsciiDigits()).isEqualTo("")
    }

    @Test
    fun `to ascii digits - ascii digits - are kept as they are`() {
        assertThat("012345".toAsciiDigits()).isEqualTo("012345")
    }
}
