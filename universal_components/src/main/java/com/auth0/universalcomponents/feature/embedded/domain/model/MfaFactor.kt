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

/** Where a push approval stands after one check. */
internal sealed interface PushStatus {

    data class Approved(val credentials: Credentials) : PushStatus

    /**
     * Not answered yet; check again in [retryInMs], or keep the last wait when null.
     * [isSlowDown] means the server said we checked too soon.
     */
    data class Pending(val retryInMs: Long?, val isSlowDown: Boolean = false) : PushStatus
}

/** How often a push may be checked, whatever the server asks: never back-to-back, never longer than a minute. */
internal object PushPolling {
    const val MIN_WAIT_MS = 1_000L
    const val MAX_WAIT_MS = 60_000L
}
