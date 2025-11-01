package com.auth0.android.ui_components.utils

import android.util.Patterns

/**
 * Utility object for input validation
 * Provides reusable validation logic for email, phone, etc.
 */
object ValidationUtil {

    /**
     * Validates if an email address is in a valid format
     * Uses Android's built-in email pattern matcher
     *
     * @param email The email address to validate
     * @return true if email is valid, false otherwise
     */
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Validates if a phone number is in a valid format
     *
     * @param phoneNumber The phone number to validate
     * @return true if phone number is valid, false otherwise
     */
    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        val digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")
        return digitsOnly.length >= 10
    }

    /**
     * Validates if an OTP code is valid
     * Checks if the code is exactly 6 digits
     *
     * @param otp The OTP code to validate
     * @return true if OTP is valid, false otherwise
     */
    fun isValidOTP(otp: String): Boolean {
        return otp.length == 6 && otp.all { it.isDigit() }
    }

    /**
     * Gets a user-friendly error message for invalid email
     *
     * @param email The email that failed validation
     * @return Error message string
     */
    fun getEmailErrorMessage(email: String): String {
        return when {
            email.isBlank() -> "Email address is required"
            !email.contains("@") -> "Email must contain @"
            else -> "Invalid email format"
        }
    }
}
