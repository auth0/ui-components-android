package com.auth0.android.ui_components.presentation.ui.utils

object UiStringFormat {

    /**
     * Formats the top bar title for the given authenticator type
     */
    fun formatTopBarTitleForAuthenticator(authenticatorType: String): String {
        return when (authenticatorType) {
            "totp" -> "Authenticator"
            "phone" -> "Phone for SMS OTP"
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
            "phone" -> "Saved Phones for SMS OTP"
            "email" -> "Saved Emails for OTP"
            "push-notification" -> "Saved Apps for Push"
            else -> "Generated Recovery code"
        }
    }

    /**
     * Formats the default name for the given authenticator type
     */
    fun formatDefaultNameForAuthenticatorItems(authenticatorType: String): String {
        return when (authenticatorType) {
            "totp" -> "Authenticator"
            "phone" -> "Phone"
            "email" -> "Email"
            "push-notification" -> "Push"
            else -> "Recovery code"
        }
    }


    /**
     * Formats the default name for the given authenticator type
     */
    fun formatEmptyStateMessageForAuthenticatorItems(authenticatorType: String): String {
        return when (authenticatorType) {
            "totp" -> "No Authenticator was saved."
            "phone" -> "No Phone was saved."
            "email" -> "No Email was saved."
            "push-notification" -> "No Push was saved."
            else -> "No Recovery code was saved."
        }
    }
}