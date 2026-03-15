package com.auth0.universalcomponents.domain.model


sealed class EnrollmentInput {

    object None : EnrollmentInput()

    data class Email(val email: String) : EnrollmentInput()

    data class Phone(
        val phoneNumber: String,
    ) : EnrollmentInput()
}
