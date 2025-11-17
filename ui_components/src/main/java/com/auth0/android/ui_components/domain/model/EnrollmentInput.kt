package com.auth0.android.ui_components.domain.model


sealed class EnrollmentInput {

    object None : EnrollmentInput()

    data class Email(val email: String) : EnrollmentInput()

    data class Phone(
        val phoneNumber: String,
    ) : EnrollmentInput()
}
