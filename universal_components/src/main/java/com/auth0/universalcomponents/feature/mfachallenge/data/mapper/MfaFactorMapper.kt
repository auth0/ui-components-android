package com.auth0.universalcomponents.feature.mfachallenge.data.mapper

import com.auth0.android.embedded.authorize.NextAction
import com.auth0.android.embedded.authorize.PhoneDeliveryMethod
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.MfaFactor

/**
 * The second factors in a server menu that these screens support; the rest is left out. They are listed as iOS lists
 * them: a push first (the more phishing-resistant), then a code texted to a phone, a code spoken by a call, and a code
 * sent to an email; within each the server's order is kept. A phone gives one factor for each way the server can reach
 * it, and a text when it names none, as the SDK does. An email address is listed even when the first factor used it:
 * the server decides what is on offer.
 */
internal fun List<NextAction>.toMfaFactors(): List<MfaFactor> = flatMap { action ->
    when (action) {
        is NextAction.ChallengePush -> listOf(MfaFactor.Push(action.index, action.name))
        is NextAction.ChallengePhone -> action.ways.map { delivery ->
            MfaFactor.Phone(action.index, action.identifier, delivery)
        }
        is NextAction.ChallengeEmail -> listOf(MfaFactor.Email(action.index, action.identifier))
        else -> emptyList()
    }
}.sortedBy { it.listRank }

/** The ways the server can reach this phone: a text when it names none, as the SDK does. */
internal val NextAction.ChallengePhone.ways: List<PhoneDeliveryMethod>
    get() = deliveryMethods.ifEmpty { listOf(PhoneDeliveryMethod.TEXT) }.distinct()

/** Where a factor sits in the list: lower first. */
private val MfaFactor.listRank: Int
    get() = when (this) {
        is MfaFactor.Push -> 0
        is MfaFactor.Phone -> if (delivery == PhoneDeliveryMethod.VOICE) 2 else 1
        is MfaFactor.Email -> 3
    }
