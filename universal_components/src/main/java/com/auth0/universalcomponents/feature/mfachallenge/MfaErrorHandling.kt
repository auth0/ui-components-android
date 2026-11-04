package com.auth0.universalcomponents.feature.mfachallenge

/**
 * What an app answers when it is told MFA ended with an error. It matters for one error only,
 * [MfaError.NoEligibleFactors]; for any other, the error screen already shows and the answer changes nothing.
 */
enum class MfaErrorHandling {

    /** The app takes over, for example by opening Universal Login. The user sees a spinner, not an error. */
    Handled,

    /** MFA shows its own "verification method not available" screen, with its way out. */
    NotHandled,
}
