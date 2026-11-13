package com.auth0.universalcomponents.feature.mfachallenge.domain.model

import com.auth0.android.embedded.authorize.PhoneDeliveryMethod
import com.auth0.android.result.Credentials

/**
 * A second way to prove who the user is, offered by the server once the first factor passed.
 * Push and a code sent to a phone or an email are supported for now; more factors are added here one at a time.
 */
internal sealed interface MfaFactor {

    /** [index] is the server's handle for that device; [deviceName] is what the user called it, maybe blank. */
    data class Push(val index: Int, val deviceName: String) : MfaFactor

    /**
     * A factor whose proof is a code the user types. They share one screen state, so the rules for a wrong code, Back,
     * "Verify another way" and Resend are written once.
     */
    sealed interface Code : MfaFactor

    /**
     * A code sent to a phone, by [delivery]. [index] is the server's handle for the phone and [maskedNumber] is the number
     * as the server hides it. A phone that can be texted and called is two factors, one per way, so each is its own row.
     */
    data class Phone(val index: Int, val maskedNumber: String, val delivery: PhoneDeliveryMethod) : Code

    /**
     * A code sent to an email address. [index] is the server's handle for the address and [address] is the address as the
     * server hides it, maybe blank.
     */
    data class Email(val index: Int, val address: String) : Code
}

/** Where a push stands after the server answered: sending it, or one check of it. */
internal sealed interface PushStatus {

    data class Approved(val credentials: Credentials) : PushStatus

    /**
     * Not answered yet. [retryInMs] is the server's wait before the next check, when it gave one; [isSlowDown] means
     * we checked too soon. [canResend] is true while the server still offers to send the push again.
     */
    data class Pending(
        val retryInMs: Long?,
        val canResend: Boolean,
        val isSlowDown: Boolean = false,
    ) : PushStatus

    /** Approved, and the sign-in needs one more factor: [factors] are the next round's that these screens support. */
    data class AnotherFactor(val factors: List<MfaFactor>) : PushStatus

    /** The user denied the push: their answer, not a failure. [cause] is the SDK reply behind it, kept for the app. */
    data class Rejected(val cause: Throwable?) : PushStatus
}

/** Where a code stands after the user typed it. */
internal sealed interface CodeStatus {

    data class Verified(val credentials: Credentials) : CodeStatus

    /** The server did not accept the code; the user may try another. */
    data object WrongCode : CodeStatus

    /** The code was right and the sign-in needs one more factor: [factors] are the next round's that these screens support. */
    data class AnotherFactor(val factors: List<MfaFactor>) : CodeStatus
}
