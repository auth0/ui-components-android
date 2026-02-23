package com.auth0.android.ui_components.presentation.ui.mfa.phone

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auth0.android.ui_components.R
import com.auth0.android.ui_components.di.MyAccountModule
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.model.EnrollmentInput
import com.auth0.android.ui_components.presentation.ui.components.CircularLoader
import com.auth0.android.ui_components.presentation.ui.components.ErrorHandler
import com.auth0.android.ui_components.presentation.ui.components.GradientButton
import com.auth0.android.ui_components.presentation.ui.components.TopBar
import com.auth0.android.ui_components.presentation.ui.utils.ObserveAsEvents
import com.auth0.android.ui_components.presentation.viewmodel.EnrollmentEvent
import com.auth0.android.ui_components.presentation.viewmodel.EnrollmentUiState
import com.auth0.android.ui_components.presentation.viewmodel.EnrollmentViewModel
import com.auth0.android.ui_components.theme.Auth0TokenDefaults
import com.auth0.android.ui_components.theme.interFamily
import com.auth0.android.ui_components.utils.ValidationUtil


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneEnrollmentScreen(
    authenticatorType: AuthenticatorType,
    viewModel: EnrollmentViewModel = viewModel(
        factory = MyAccountModule.provideEnrollmentViewModelFactory(authenticatorType)
    ),
    onContinueToOTP: (authenticationId: kotlin.String, authSession: kotlin.String, phoneNumber: kotlin.String) -> Unit,
    onBackClick: () -> Unit
) {
    val colors = Auth0TokenDefaults.color()

    var phoneNumber by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(Country.countries[0]) }
    var validationError by remember { mutableStateOf(false) }
    var showCountrySelector by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is EnrollmentEvent.EnrollmentChallengeSuccess -> {
                onContinueToOTP(
                    event.authenticationMethodId,
                    event.authSession,
                    selectedCountry.phoneCode + phoneNumber
                )
            }

            is EnrollmentEvent.VerificationSuccess -> {
                Log.d("PhoneEnrollmentScreen", "$event not handled ")
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.add_phone_sms_otp),
                onBackClick = onBackClick,
                showSeparator = false
            )
        },
        containerColor = colors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 32.dp)
        ) {

            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                PhoneEnrollmentHeader()

                Spacer(modifier = Modifier.height(24.dp))

                PhoneFormField(
                    phoneNumber = phoneNumber,
                    selectedCountry = selectedCountry,
                    onPhoneNumberChange = { newPhone ->
                        phoneNumber = newPhone
                        if (validationError) {
                            validationError = false
                        }
                    },
                    onCountryCodeClick = { showCountrySelector = true },
                    isValidationError = validationError,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            ContinueButton(
                onClick = {
                    val fullPhoneNumber = selectedCountry.phoneCode + phoneNumber
                    if (!ValidationUtil.isValidPhoneNumber(phoneNumber)) {
                        validationError = true
                    } else {
                        validationError = false
                        viewModel.startEnrollment(
                            authenticatorType = AuthenticatorType.PHONE,
                            input = EnrollmentInput.Phone(
                                phoneNumber = fullPhoneNumber
                            )
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        LoadingScreen(uiState)
        ErrorScreen(uiState)

    }

    if (showCountrySelector) {
        CountrySelectorSheet(
            countries = Country.countries,
            onCountrySelected = { country ->
                selectedCountry = country
                showCountrySelector = false
            },
            onDismiss = { showCountrySelector = false }
        )
    }
}


@Composable
private fun PhoneEnrollmentHeader() {
    val colors = Auth0TokenDefaults.color()
    val typography = Auth0TokenDefaults.typography()
    val dimensions = Auth0TokenDefaults.dimensions()

    Text(
        text = stringResource(R.string.enter_phone_number),
        style = typography.titleLarge,
        textAlign = TextAlign.Start,
        color = colors.textPrimary,
    )

    Spacer(Modifier.height(dimensions.spacingXs))

    Text(
        text = stringResource(R.string.verification_code_text),
        style = typography.body,
        color = colors.textSecondary
    )
}

@Composable
private fun LoadingScreen(
    state: EnrollmentUiState
) {
    if (state.enrollingAuthenticator)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            CircularLoader()
        }
}

@Composable
private fun ErrorScreen(state: EnrollmentUiState) {
    state.uiError?.let {
        ErrorHandler(it)
    }
}


@Composable
private fun PhoneFormField(
    phoneNumber: String,
    selectedCountry: Country,
    onPhoneNumberChange: (String) -> Unit,
    onCountryCodeClick: () -> Unit,
    isValidationError: Boolean,
) {
    val colors = Auth0TokenDefaults.color()
    val typography = Auth0TokenDefaults.typography()
    val dimensions = Auth0TokenDefaults.dimensions()

    Text(
        text = stringResource(R.string.phone_number_label),
        style = typography.label,
        color = colors.textPrimary,
    )
    Spacer(modifier = Modifier.height(dimensions.spacingXs))

    PhoneTextField(
        phoneNumber = phoneNumber,
        selectedCountry = selectedCountry,
        onPhoneNumberChange = onPhoneNumberChange,
        onCountryCodeClick = onCountryCodeClick,
        isError = isValidationError,
    )
}


@Composable
private fun PhoneTextField(
    phoneNumber: String,
    selectedCountry: Country,
    onPhoneNumberChange: (String) -> Unit,
    onCountryCodeClick: () -> Unit,
    isError: Boolean,
) {
    val colors = Auth0TokenDefaults.color()
    val typography = Auth0TokenDefaults.typography()
    val shapes = Auth0TokenDefaults.shapes()
    val dimensions = Auth0TokenDefaults.dimensions()

    val backgroundColor = if (isError) {
        colors.error.copy(alpha = 0.05f)
    } else {
        colors.surface
    }

    val borderColor = if (isError) {
        colors.error.copy(alpha = 0.5f)
    } else {
        colors.border
    }

    val textColor = if (isError) {
        colors.onError
    } else {
        colors.textPrimary
    }

    val shape = shapes.medium
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(
                color = backgroundColor,
                shape = shape
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape
            ),
        shadowElevation = 10.dp,
        shape = shape,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(horizontal = dimensions.spacingSm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensions.spacingMd)
        ) {
            Row(
                modifier = Modifier.clickable(onClick = onCountryCodeClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensions.spacingXxs)
            ) {
                Text(
                    text = selectedCountry.flagEmoji,
                    fontSize = 20.sp,
                    modifier = Modifier.size(24.dp)
                )

                Text(
                    modifier = Modifier.padding(horizontal = dimensions.spacingXxs),
                    text = selectedCountry.phoneCode,
                    style = typography.title,
                    color = textColor
                )

                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = stringResource(R.string.select_country),
                    modifier = Modifier.size(dimensions.spacingMd),
                    tint = textColor
                )
            }

            BasicTextField(
                value = phoneNumber,
                onValueChange = onPhoneNumberChange,
                textStyle = typography.title.copy(color = textColor),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                ),
                singleLine = true,
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (phoneNumber.isEmpty()) {
                        Text(
                            text = stringResource(R.string.phone_number_placeholder),
                            style = typography.title.copy(color = colors.textPrimary.copy(alpha = 0.54f))
                        )
                    }
                    innerTextField()
                }
            )
        }
    }

    if (isError) {
        Text(
            text = stringResource(R.string.invalid_phone_number),
            color = colors.onError,
            style = typography.body,
            modifier = Modifier.padding(start = dimensions.spacingXxs, top = dimensions.spacingXs)
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountrySelectorSheet(
    countries: List<Country>,
    onCountrySelected: (Country) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = Auth0TokenDefaults.color()
    val typography = Auth0TokenDefaults.typography()
    val shapes = Auth0TokenDefaults.shapes()
    val dimensions = Auth0TokenDefaults.dimensions()

    val sheetState = rememberModalBottomSheetState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredCountries = remember(searchQuery) {
        if (searchQuery.isEmpty()) {
            countries
        } else {
            countries.filter {
                it.name.startsWith(searchQuery, ignoreCase = true) ||
                        it.phoneCode.startsWith(searchQuery)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensions.spacingMd)
        ) {
            Text(
                text = stringResource(R.string.select_country_code),
                style = typography.title,
                modifier = Modifier.padding(bottom = dimensions.spacingMd)
            )

            val searchShape = shapes.medium
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dimensions.spacingMd, horizontal = dimensions.spacingSm),
                trailingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = stringResource(R.string.search),
                        modifier = Modifier.size(dimensions.spacingLg),
                        tint = colors.textPrimary.copy(alpha = 0.5f)
                    )
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_country),
                        style = typography.label,
                        color = colors.textPrimary.copy(alpha = 0.54f)
                    )
                },
                textStyle = typography.body.copy(
                    color = colors.textPrimary
                ),
                singleLine = true,
                shape = searchShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE0E0E0),
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    cursorColor = Color.Gray,
                )
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filteredCountries) { country ->
                    CountryItem(
                        country = country,
                        onClick = { onCountrySelected(country) }
                    )
                }
            }
        }
    }
}


@Composable
private fun CountryItem(
    country: Country,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp, horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = country.flagEmoji,
                fontSize = 20.sp,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = "${country.name} (${country.phoneCode})",
                style = Auth0TokenDefaults.typography().body,
                color = Auth0TokenDefaults.color().textPrimary
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 20.dp),
            color = Color(0xFFD9D9D9),
            thickness = 1.dp
        )
    }
}


@Composable
private fun ContinueButton(
    onClick: () -> Unit
) {
    GradientButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        onClick = onClick
    ) {
        Text(stringResource(R.string.continue_button))
    }
}