package com.auth0.universalcomponents.feature.mfachallenge

import androidx.compose.runtime.Immutable

/**
 * What a control on an MFA screen does when tapped. Every action equals every other, so two models are the same when
 * their data is, and a [MfaStepStream] does not announce a step again just because its actions were rebuilt.
 */
@Immutable
class MfaAction(private val action: () -> Unit) {

    operator fun invoke() = action()

    override fun equals(other: Any?): Boolean = other is MfaAction

    override fun hashCode(): Int = HASH

    override fun toString(): String = "MfaAction"

    companion object {
        private const val HASH = 1

        /** Does nothing. For previews and tests. */
        val None = MfaAction { }
    }
}

/** Like [MfaAction], for picking a row of the list: it is given the row's [AuthenticatorListModel.Row.id]. */
@Immutable
class MfaSelectAction(private val action: (rowId: String) -> Unit) {

    operator fun invoke(rowId: String) = action(rowId)

    override fun equals(other: Any?): Boolean = other is MfaSelectAction

    override fun hashCode(): Int = HASH

    override fun toString(): String = "MfaSelectAction"

    private companion object {
        const val HASH = 2
    }
}
