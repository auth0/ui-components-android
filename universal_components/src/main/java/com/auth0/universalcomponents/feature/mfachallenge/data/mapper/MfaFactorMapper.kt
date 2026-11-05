package com.auth0.universalcomponents.feature.mfachallenge.data.mapper

import com.auth0.android.embedded.authorize.NextAction
import com.auth0.android.embedded.authorize.PhoneDeliveryMethod
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.MfaFactor

/**
 * The second factors in a server menu that these screens support, in its order; the rest is left out. A phone gives one
 * factor for each way the server can reach it, and a text when it names none, as the SDK does.
 */
internal fun List<NextAction>.toMfaFactors(): List<MfaFactor> = flatMap { action ->
    when (action) {
        is NextAction.ChallengePush -> listOf(MfaFactor.Push(action.index, action.name))
        is NextAction.ChallengePhone ->
            action.deliveryMethods.ifEmpty { listOf(PhoneDeliveryMethod.TEXT) }.distinct().map { delivery ->
                MfaFactor.Phone(action.index, action.identifier, delivery)
            }
        else -> emptyList()
    }
}
