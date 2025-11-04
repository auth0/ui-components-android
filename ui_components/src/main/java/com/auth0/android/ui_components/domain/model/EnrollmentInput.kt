package com.auth0.android.ui_components.domain.model

import com.auth0.android.myaccount.PhoneAuthenticationMethodType


sealed class EnrollmentInput {

    object None : EnrollmentInput()

    data class Email(val email: String) : EnrollmentInput()

    data class Phone(
        val phoneNumber: String,
        val preferredMethod: PhoneAuthenticationMethodType
    ) : EnrollmentInput()
}
