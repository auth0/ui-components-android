package com.auth0.universalcomponents.feature.embedded.domain.model

import com.auth0.android.result.Credentials

/**
 * A second way to prove who the user is, offered by the server once the first factor passed.
 * Only push is supported for now; more factors are added here one at a time.
 */
internal sealed interface MfaFactor {

    /** [index] is the server's handle for that device; [deviceName] is what the user called it, maybe blank. */
    data class Push(val index: Int, val deviceName: String) : MfaFactor
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

    /** The user denied the push. It's their answer, not a failure, so it isn't an error. */
    data object Rejected : PushStatus
}
