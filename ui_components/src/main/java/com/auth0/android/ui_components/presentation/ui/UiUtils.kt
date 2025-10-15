package com.auth0.android.ui_components.presentation.ui

object UiUtils {

    /**
     * Formats the top bar title for the given authenticator type
     */
    fun formatTopBarTitleForAuthenticator(authenticatorType: String): String {
        return when (authenticatorType) {
            "totp" -> "Authenticator"
            "sms" -> "SMS OTP"
            "email" -> "Email OTP"
            "push-notification" -> "Push Notification"
            else -> "Recovery Code"
        }
    }

    /**
     * Formats the description for the given authenticator type
     */
    fun formatDescriptionForAuthenticator(authenticatorType: String): String {
        return when (authenticatorType) {
            "totp" -> "Saved Authenticators"
            "sms" -> "Saved Phones for SMS OTP"
            "email" -> "Saved Emails for OTP"
            "push-notification" -> "Saved Apps for Push"
            else -> "Generated Recovery Code"
        }
    }

    /**
     * Formats the default name for the given authenticator type
     */
    fun formatDefaultNameForAuthenticatorItems(authenticatorType: String): String {
        return when (authenticatorType) {
            "totp" -> "Authenticator"
            "sms" -> "Phone"
            "email" -> "Email"
            "push-notification" -> "Push"
            else -> "Recovery code generated"
        }
    }
}