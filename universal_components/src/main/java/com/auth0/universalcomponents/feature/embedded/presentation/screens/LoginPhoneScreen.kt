package com.auth0.universalcomponents.feature.embedded.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginFieldLabel
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginInlineError
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginInputField
import com.auth0.universalcomponents.presentation.ui.flow.FlowPrimaryButton
import com.auth0.universalcomponents.presentation.ui.flow.FlowScreenHeader
import com.auth0.universalcomponents.presentation.ui.flow.FlowStepLayout
import com.auth0.universalcomponents.presentation.ui.mfa.phone.Country
import com.auth0.universalcomponents.presentation.ui.mfa.phone.CountrySelectorSheet
import com.auth0.universalcomponents.presentation.ui.mfa.phone.forIso
import com.auth0.universalcomponents.theme.Auth0Theme
import com.auth0.universalcomponents.utils.ValidationUtil

/** E.164 allows 15 digits in all, the country code included. */
private const val MAX_PHONE_DIGITS = 15

/** How many digits the number may have after [Country]'s dial code (which starts with a "+"). */
private fun Country.maxNumberDigits(): Int = MAX_PHONE_DIGITS - (phoneCode.length - 1)

/** The country the field starts on, as in the design. */
private const val DEFAULT_COUNTRY_ISO = "US"

/**
 * Collects the phone number the code is sent to. [onContinue] receives it in E.164 form (`+14233530900`) once it looks
 * valid; [phone] pre-fills the field in that same form, so Edit and Try again keep what was typed. The country starts
 * as the United States; a pre-filled number picks its own.
 */
@Composable
internal fun LoginPhoneScreen(
    onContinue: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.embedded_login_phone_title),
    phone: String = "",
    isLoading: Boolean = false,
) {
    val defaultCountry = remember { Country.forIso(DEFAULT_COUNTRY_ISO) ?: Country.countries.first() }
    // The countries are a fixed list, so a position survives a rotation where a Country could not.
    val (initialCountry, initialNumber) = remember { splitPhone(phone, defaultCountry) }
    var countryIndex by rememberSaveable { mutableIntStateOf(Country.countries.indexOf(initialCountry)) }
    var number by rememberSaveable { mutableStateOf(initialNumber) }
    var isInvalidPhone by rememberSaveable { mutableStateOf(false) }
    var isPickingCountry by rememberSaveable { mutableStateOf(false) }
    val country = Country.countries[countryIndex]

    val submit = {
        if (ValidationUtil.isValidPhoneNumber(number)) onContinue(country.phoneCode + number) else isInvalidPhone = true
    }

    FlowStepLayout(
        modifier = modifier,
        pageDescription = { FlowScreenHeader(title = title) },
        content = {
            LoginFieldLabel(stringResource(R.string.phone_number_label))
            LoginInputField(
                value = number,
                onValueChange = {
                    number = it.filter(Char::isDigit).take(country.maxNumberDigits())
                    isInvalidPhone = false
                },
                isInvalid = isInvalidPhone,
                enabled = !isLoading,
                keyboardType = KeyboardType.Phone,
                placeholder = stringResource(R.string.phone_number_placeholder),
                leading = { CountryChip(country, enabled = !isLoading, onClick = { isPickingCountry = true }) },
                onDone = submit,
            )
            if (isInvalidPhone) LoginInlineError(stringResource(R.string.embedded_login_error_invalid_phone))
        },
        primaryAction = {
            FlowPrimaryButton(
                title = stringResource(R.string.embedded_login_continue),
                onClick = submit,
                isLoading = isLoading,
                enabled = number.isNotBlank() && !isLoading,
            )
        },
    )

    if (isPickingCountry) {
        CountrySelectorSheet(
            countries = Country.countries,
            onCountrySelected = {
                countryIndex = Country.countries.indexOf(it)
                number = number.take(it.maxNumberDigits())
                isPickingCountry = false
            },
            onDismiss = { isPickingCountry = false },
        )
    }
}

/** The flag and dial code of the chosen country, with a chevron: tapping it opens the list. */
@Composable
private fun CountryChip(country: Country, enabled: Boolean, onClick: () -> Unit) {
    val dimensions = Auth0Theme.dimensions
    val color = Auth0Theme.colors.textBold
    Row(
        modifier = Modifier
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(end = dimensions.spacingMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingXs),
    ) {
        Text(text = country.flagEmoji, fontSize = 20.sp)
        Text(text = country.phoneCode, style = Auth0Theme.typography.body, color = color)
        Icon(
            painter = painterResource(R.drawable.ic_chevron_down),
            contentDescription = stringResource(R.string.select_country),
            modifier = Modifier.size(dimensions.spacingMd),
            tint = color,
        )
    }
}

/**
 * Splits an E.164 [phone] into its country and the number after the dial code: the longest dial code that fits, and
 * [fallback] when several share it (+1 is shared by many). A [phone] with no `+` is taken as typed, in [fallback].
 */
private fun splitPhone(phone: String, fallback: Country): Pair<Country, String> {
    val matches = Country.countries.filter { phone.startsWith(it.phoneCode) }
    val longest = matches.maxOfOrNull { it.phoneCode.length } ?: return fallback to phone.filter(Char::isDigit)
    val sharing = matches.filter { it.phoneCode.length == longest }
    val country = if (fallback in sharing) fallback else sharing.first()
    return country to phone.removePrefix(country.phoneCode)
}
