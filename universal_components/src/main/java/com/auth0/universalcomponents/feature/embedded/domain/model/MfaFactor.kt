package com.auth0.universalcomponents.feature.embedded.domain.model

import com.auth0.android.result.Credentials

/**
 * A second way to prove who the user is, offered by the server once the first factor passed.
 * Only push is supported for now; more factors are added here one at a time.
 */
internal sealed interface MfaFactor {

    /** [index] is the server's handle for that device; [deviceName] is what the user called it. */
    data class Push(val index: Int, val deviceName: String) : MfaFactor
}

/** How a correct first-factor code ended: signed in, or the server wants a second factor. */
internal sealed interface VerifyOutcome {

    data class SignedIn(val credentials: Credentials) : VerifyOutcome

    data class MfaRequired(val factors: List<MfaFactor>) : VerifyOutcome
}

/** Where a push approval stands after one check. */
internal sealed interface PushStatus {

    data class Approved(val credentials: Credentials) : PushStatus

    /** Not answered yet; check again in [retryInMs], or keep the last wait when null. */
    data class Pending(val retryInMs: Long?) : PushStatus
}
