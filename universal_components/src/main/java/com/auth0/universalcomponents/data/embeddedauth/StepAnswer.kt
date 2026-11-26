package com.auth0.universalcomponents.data.embeddedauth

import com.auth0.android.embedded.EmbeddedAuthError.InsufficientAuthorization.Reason
import com.auth0.android.embedded.authorize.NextAction

/**
 * How one SDK step answered: finished, "not finished yet, do one of [Next.actions]", or failed. A failure keeps the
 * SDK's [Failed.error] as it came, so the login and MFA can each name it in their own terms.
 */
internal sealed interface StepAnswer<out T> {
    data class Done<T>(val value: T) : StepAnswer<T>

    /**
     * [description] is the server's own `error_description`, such as `invalid_email`, which the SDK has no [reason] for;
     * null when none is given. A step run through the SDK always gives one: when the server sent none, it is the SDK's
     * generic sentence, so compare it only with words the server is known to use, and never show it.
     */
    data class Next(
        val actions: List<NextAction>,
        val reason: Reason,
        val description: String? = null,
    ) : StepAnswer<Nothing>

    data class Failed(val error: Exception) : StepAnswer<Nothing>
}

/** The reasons the server gives when the code (or identifier) the user typed was wrong. */
internal val INVALID_CODE_REASONS = setOf(Reason.INVALID_CODE, Reason.INVALID_IDENTIFIER_OR_CODE)

/** The actions a step asks for next; none if it finished. */
internal val StepAnswer<*>.nextActions: List<NextAction>
    get() = (this as? StepAnswer.Next)?.actions.orEmpty()
