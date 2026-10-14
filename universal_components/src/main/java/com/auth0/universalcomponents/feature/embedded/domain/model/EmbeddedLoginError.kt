package com.auth0.universalcomponents.feature.embedded.domain.model

/** What went wrong in the login flow. It only names the problem; the presentation layer picks the words. */
internal sealed interface EmbeddedLoginError {

    data object ConnectionMissing : EmbeddedLoginError

    data object Network : EmbeddedLoginError

    data object InvalidCode : EmbeddedLoginError

    data object CodeExpired : EmbeddedLoginError

    data object TooManyWrongCodes : EmbeddedLoginError

    data object TooManyAttempts : EmbeddedLoginError

    data object TooManyLogins : EmbeddedLoginError

    /** The server asks for a further step, such as MFA, that isn't supported yet. */
    data object ExtraStepUnsupported : EmbeddedLoginError

    /** Any other error the server explained; [text] is its own words. */
    data class Server(val text: String) : EmbeddedLoginError

    /** Nothing better to say than the generic text. */
    data object Unknown : EmbeddedLoginError
}

/**
 * The one failure the data layer throws.
 * [canRetryStep] is true when the server still holds the session, so the same step can be tried again.
 * [cause] is the original failure, kept for its stack trace; it's null only for the checks we make ourselves.
 */
internal class EmbeddedLoginException(
    val error: EmbeddedLoginError,
    val canRetryStep: Boolean = false,
    cause: Throwable? = null,
) : Exception(error::class.simpleName, cause)
