package com.auth0.universalcomponents.feature.embedded.data.repository

import com.auth0.android.embedded.EmbeddedAuthError.InsufficientAuthorization.Reason
import com.auth0.android.embedded.authorize.NextAction
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure

/** How one SDK step answered: finished, "not finished yet, do one of [Next.actions]", or failed. */
internal sealed interface StepAnswer<out T> {
    data class Done<T>(val value: T) : StepAnswer<T>
    data class Next(val actions: List<NextAction>, val reason: Reason) : StepAnswer<Nothing>
    data class Failed(val failure: LoginFailure) : StepAnswer<Nothing>
}

/** The reasons the server gives when the code (or identifier) the user typed was wrong. */
internal val INVALID_CODE_REASONS = setOf(Reason.INVALID_CODE, Reason.INVALID_IDENTIFIER_OR_CODE)

/** The actions a step asks for next; none if it finished. */
internal val StepAnswer<*>.nextActions: List<NextAction>
    get() = (this as? StepAnswer.Next)?.actions.orEmpty()
