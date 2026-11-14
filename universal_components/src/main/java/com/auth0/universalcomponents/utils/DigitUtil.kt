package com.auth0.universalcomponents.utils

/**
 * A digit as the server reads one in a code: 0 to 9. `Char.isDigit` also takes the digits of other scripts (Arabic-Indic
 * ٣, fullwidth ３), which the server would count as a wrong code, and as one more attempt.
 */
internal fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

/**
 * This text as the ASCII digits the server reads. The digits of other scripts are converted (a keyboard set to Arabic,
 * Persian or Hindi types them, and the user means the same number), and everything else is dropped.
 */
internal fun String.toAsciiDigits(): String = buildString {
    for (char in this@toAsciiDigits) {
        val digit = char.digitToIntOrNull() ?: continue
        append(digit.digitToChar())
    }
}
