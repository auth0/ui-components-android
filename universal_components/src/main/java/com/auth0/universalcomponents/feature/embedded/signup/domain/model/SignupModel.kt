package com.auth0.universalcomponents.feature.embedded.signup.domain.model

import com.auth0.android.embedded.authorize.PhoneDeliveryMethod
import com.auth0.android.result.Credentials

/**
 * What a new account can be identified by. A kind the server offers that isn't here is left out, so the flow never
 * shows a screen it can't finish. Adding one is a new case: the compiler then lists each exhaustive `when` that needs it,
 * but not the rules written as plain comparisons, such as [recoversExpiredCode].
 */
internal enum class SignupKind {
    Email,
    Phone;

    /**
     * Whether a code that timed out can be replaced from the code screen, which starts the sign-up over for the same
     * identifier. A kind without it ends the sign-up instead.
     */
    val recoversExpiredCode: Boolean get() = this == Phone
}

/**
 * What the user typed for one [kind]; a phone number is in E.164 form. It is personal data, so [toString] leaves
 * [value] out, as it does a code.
 */
internal data class SignupIdentifier(val kind: SignupKind, val value: String) {
    override fun toString(): String = "SignupIdentifier(kind=$kind)"
}

/**
 * A code the server will send. [index] is the server's, passed back unchanged. [sentTo] is the number where the server
 * says the code goes, as it hid it; null for an email, and for a number it hid completely, so the screen shows what was
 * typed. [deliveries] are the ways it will send the code, text first; empty for an email, which is only ever emailed.
 * A phone number is personal data even masked, so [toString] leaves [sentTo] out, as it does a typed number.
 */
internal data class SignupChallenge(
    val index: Int,
    val sentTo: String? = null,
    val deliveries: List<PhoneDeliveryMethod> = emptyList(),
) {
    override fun toString(): String = "SignupChallenge(index=$index, deliveries=$deliveries)"

    /** The way the first code goes: a text when the number takes one, else a call; null for an email. */
    val firstDelivery: PhoneDeliveryMethod?
        get() = if (PhoneDeliveryMethod.TEXT in deliveries) PhoneDeliveryMethod.TEXT else deliveries.firstOrNull()
}

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

    /** An account already existed for the identifier, so the server signed that user in. */
    data class SignedIn(val credentials: Credentials) : SignupProgress

    /** The sign-up goes on; [next] says how. */
    data class Continue(val next: SignupNext) : SignupProgress
}
