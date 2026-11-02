package com.auth0.universalcomponents.feature.embedded.data.mapper

import com.auth0.android.embedded.authorize.NextAction
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor

/** The second factors in a server menu that these screens support, in its order; the rest is left out. */
internal fun List<NextAction>.toMfaFactors(): List<MfaFactor> = mapNotNull { action ->
    when (action) {
        is NextAction.ChallengePush -> MfaFactor.Push(action.index, action.name)
        else -> null
    }
}
