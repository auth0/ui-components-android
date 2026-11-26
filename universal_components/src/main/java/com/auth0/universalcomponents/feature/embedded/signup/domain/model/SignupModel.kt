package com.auth0.universalcomponents.feature.embedded.signup.domain.model

import com.auth0.android.result.Credentials

/**
 * What a new account can be identified by. A kind the server offers that isn't here is left out, so the flow never
 * shows a screen it can't finish. Adding one is a new case: the compiler then lists each place that needs it.
 */
internal enum class SignupKind { Email }

/** What the user typed for one [kind]. An email is personal data, so [toString] leaves [value] out, as it does a code. */
internal data class SignupIdentifier(val kind: SignupKind, val value: String) {
    override fun toString(): String = "SignupIdentifier(kind=$kind)"
}

/** A code the server will send. [index] is the server's, passed back unchanged. */
internal data class SignupChallenge(val index: Int)

/** What the server asks for next in a sign-up. It is read the same way after opening one and after a code. */
internal sealed interface SignupNext {

    /** Enter this kind. Includes one still being verified: entering it again corrects it. */
    data class AskFor(val kind: SignupKind) : SignupNext

    /** Every required identifier is verified: the account can be created. */
    data object ReadyToCreate : SignupNext

    /** The server asks for nothing this flow can give. */
    data object NothingToDo : SignupNext
}

/** Where a sign-up stands after a code was accepted. */
internal sealed interface SignupProgress {

    /** The email already had an account, so the server signed that user in. */
    data class SignedIn(val credentials: Credentials) : SignupProgress

    /** The sign-up goes on; [next] says how. */
    data class Continue(val next: SignupNext) : SignupProgress
}
