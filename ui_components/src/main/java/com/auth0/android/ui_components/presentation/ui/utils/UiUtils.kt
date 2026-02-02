package com.auth0.android.ui_components.presentation.ui.utils

import com.auth0.android.ui_components.domain.model.AuthenticatorType

object UiUtils {

    /**
     * Formats the top bar title for the given authenticator type
     */
    fun formatTopBarTitleForAuthenticator(authenticatorType: String): String {
        return when (authenticatorType) {
            "totp" -> "Authenticator"
            "phone" -> "Phone for SMS OTP"
            "email" -> "Email OTP"
            "push-notification" -> "Push Notification"
            "passkey" -> "Passkey"
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
            "passkey" -> "Saved Passkeys"
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
            "passkey" -> "Passkey"
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
            "passkey" -> "No Passkey was saved."
            else -> "No Recovery code was saved."
        }
    }


    /**
     * Returns appropriate text content based on authenticator type
     */
    internal fun getOTPVerificationScreenText(
        authenticatorType: AuthenticatorType,
        phoneNumberOrEmail: String?
    ): ScreenTextContent {
        return when (authenticatorType) {
            AuthenticatorType.PHONE -> ScreenTextContent(
                topBarTitle = "Verify it's you",
                primaryText = "Enter the 6-digit code we sent to ${phoneNumberOrEmail ?: "your phone"}",
            )

            AuthenticatorType.EMAIL -> ScreenTextContent(
                topBarTitle = "Verify it's you",
                primaryText = "Enter the 6-digit code we sent to $phoneNumberOrEmail",
            )

            else -> ScreenTextContent(
                topBarTitle = "Add and Authenticator",
                primaryText = "Enter the 6-digit code",
                description = "From your Authenticator App"
            )
        }
    }

    internal data class ScreenTextContent(
        val topBarTitle: String,
        val primaryText: String,
        val description: String? = null
    )
}