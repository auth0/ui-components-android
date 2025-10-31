package com.auth0.android.ui_components.presentation.ui.mfa.phone

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auth0.android.myaccount.PhoneAuthenticationMethodType
import com.auth0.android.ui_components.R
import com.auth0.android.ui_components.di.MyAccountModule
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.domain.model.EnrollmentInput
import com.auth0.android.ui_components.domain.model.EnrollmentResult
import com.auth0.android.ui_components.presentation.ui.components.CircularLoader
import com.auth0.android.ui_components.presentation.ui.components.ErrorHandler
import com.auth0.android.ui_components.presentation.ui.components.GradientButton
import com.auth0.android.ui_components.presentation.ui.components.TopBar
import com.auth0.android.ui_components.presentation.viewmodel.EnrollmentUiState
import com.auth0.android.ui_components.presentation.viewmodel.EnrollmentViewModel
import com.auth0.android.ui_components.theme.SectionSubtitle
import com.auth0.android.ui_components.utils.ValidationUtil

/**
 * Phone Enrollment Screen
 *
 * Allows users to enter their phone number for SMS OTP MFA enrollment.
 * Validates phone format and initiates enrollment via EnrollmentViewModel.
 * On success, navigates to OTP verification screen.
 *
 * @param authenticatorType Type of authenticator (should be SMS)
 * @param viewModel EnrollmentViewModel instance for handling enrollment
 * @param onBackClick Callback for back navigation
 * @param onContinueToOTP Callback when enrollment succeeds, passes authenticationId, authSession, and phoneNumber
 */
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
    var phoneNumber by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(Country.countries[0]) }
    var validationError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showCountrySelector by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.add_phone_sms_otp),
                onBackClick = onBackClick,
                showSeparator = false
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            when (val state = uiState) {
                is EnrollmentUiState.EnrollmentInitiated -> {
                    when (val result = state.enrollmentResult) {
                        is EnrollmentResult.DefaultEnrollment -> {
                            onContinueToOTP(
                                result.authenticationMethodId,
                                result.authSession,
                                selectedCountry.phoneCode + phoneNumber
                            )
                            viewModel.resetState()
                        }

                        else -> {
                            validationError = true
                            errorMessage = stringResource(R.string.unexpected_enrollment_result)
                        }
                    }
                }

                is EnrollmentUiState.Error -> {
                    validationError = true
                    ErrorHandler(state.uiError)
                }

                is EnrollmentUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularLoader()
                    }
                }

                else -> {
                    // Other states handled in UI
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PhoneEnrollmentHeader()

                PhoneFormField(
                    phoneNumber = phoneNumber,
                    selectedCountry = selectedCountry,
                    onPhoneNumberChange = { newPhone ->
                        phoneNumber = newPhone
                        if (validationError) {
                            validationError = false
                            errorMessage = ""
                        }
                    },
                    onCountryCodeClick = { showCountrySelector = true },
                    isValidationError = validationError,
                    errorMessage = errorMessage
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            ContinueButton(
                onClick = {
                    val fullPhoneNumber = selectedCountry.phoneCode + phoneNumber
                    if (!ValidationUtil.isValidPhoneNumber(phoneNumber)) {
                        validationError = true
                        errorMessage = "Invalid phone number."
                    } else {
                        validationError = false
                        errorMessage = ""
                        viewModel.startEnrollment(
                            authenticatorType = AuthenticatorType.PHONE,
                            input = EnrollmentInput.Phone(
                                phoneNumber = fullPhoneNumber,
                                preferredMethod = PhoneAuthenticationMethodType.SMS
                            )
                        )
                    }
                }
            )
        }
    }

    // Country Selector Bottom Sheet
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

/**
 * Phone Enrollment Header Component
 */
@Composable
private fun PhoneEnrollmentHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.enter_phone_number),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            color = Color.Black,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = stringResource(R.string.verification_code_text),
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = SectionSubtitle,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Phone Form Field Component
 */
@Composable
private fun PhoneFormField(
    phoneNumber: kotlin.String,
    selectedCountry: Country,
    onPhoneNumberChange: (kotlin.String) -> Unit,
    onCountryCodeClick: () -> Unit,
    isValidationError: Boolean,
    errorMessage: kotlin.String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.phone_number_label),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1F1F1F),
            lineHeight = 14.sp
        )

        PhoneTextField(
            phoneNumber = phoneNumber,
            selectedCountry = selectedCountry,
            onPhoneNumberChange = onPhoneNumberChange,
            onCountryCodeClick = onCountryCodeClick,
            isError = isValidationError,
            errorMessage = errorMessage
        )
    }
}

/**
 * Phone Text Field Component - Similar to EmailTextField
 */
@Composable
private fun PhoneTextField(
    phoneNumber: kotlin.String,
    selectedCountry: Country,
    onPhoneNumberChange: (kotlin.String) -> Unit,
    onCountryCodeClick: () -> Unit,
    isError: Boolean,
    errorMessage: String
) {
    val backgroundColor = if (isError) {
        Color(0xFFB82819).copy(alpha = 0.05f)
    } else {
        Color.White
    }

    val borderColor = if (isError) {
        Color(0xFFB82819).copy(alpha = 0.25f)
    } else {
        Color(0xFFE0E0E0)
    }

    val textColor = if (isError) {
        Color(0xFFCA3B2B)
    } else {
        Color(0xFF1F1F1F)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val shape = RoundedCornerShape(14.dp)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(61.dp)
                .background(
                    color = backgroundColor,
                    shape = shape
                )
                .border(
                    width = if (isError) 1.dp else 1.dp,
                    color = borderColor,
                    shape = shape
                ),
            shadowElevation = 8.dp,
            shape = shape,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Country Code Selector
                Row(
                    modifier = Modifier.clickable(onClick = onCountryCodeClick),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = selectedCountry.flagEmoji,
                        fontSize = 20.sp,
                        modifier = Modifier.size(24.dp)
                    )

                    Text(
                        text = selectedCountry.phoneCode,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 20.sp,
                        color = textColor
                    )

                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_down),
                        contentDescription = stringResource(R.string.select_country),
                        modifier = Modifier.size(16.dp),
                        tint = textColor
                    )
                }

                // Phone Number Input
                BasicTextField(
                    value = phoneNumber,
                    onValueChange = onPhoneNumberChange,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 20.sp,
                        color = textColor
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (phoneNumber.isEmpty()) {
                            Text(
                                text = stringResource(R.string.phone_number_placeholder),
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = 20.sp,
                                    color = Color(0xFF1F1F1F).copy(alpha = 0.54f)
                                )
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        if (isError && errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Color(0xFFCA3B2B),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

/**
 * Country Selector Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountrySelectorSheet(
    countries: List<Country>,
    onCountrySelected: (Country) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredCountries = remember(searchQuery) {
        if (searchQuery.isEmpty()) {
            countries
        } else {
            countries.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.phoneCode.contains(searchQuery)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.select_country_code),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Search Field
            val searchShape = RoundedCornerShape(14.dp)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                trailingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = stringResource(R.string.search),
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF000000).copy(alpha = 0.5f)
                    )
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_country),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1F1F1F).copy(alpha = 0.54f)
                    )
                },
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1F1F1F)
                ),
                singleLine = true,
                shape = searchShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE0E0E0),
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    cursorColor = Color.Gray,
                )
            )

            // Country List
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

/**
 * Country Item in the list
 */
@Composable
private fun CountryItem(
    country: Country,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp)
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
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 28.sp,
                color = Color(0xFF333C4D)
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 20.dp),
            color = Color(0xFFD9D9D9),
            thickness = 1.dp
        )
    }
}

/**
 * Continue Button Component
 */
@Composable
private fun ContinueButton(
    onClick: () -> Unit
) {
    GradientButton(
        content = { Text(stringResource(R.string.continue_button)) },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        gradient = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.15f),
                Color.Transparent
            )
        ),
    ) {
        onClick()
    }
}