package com.auth0.universalcomponents.feature.embedded.signup.data.mapper

import com.auth0.android.embedded.authorize.EmbeddedCapability
import com.auth0.android.embedded.authorize.IdentifierType
import com.auth0.android.embedded.authorize.NextAction
import com.auth0.android.embedded.authorize.PhoneDeliveryMethod
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupChallenge
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupKind
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupNext

// How the server's menu (`nextActions`) is read for sign-up. This is the only file that looks inside a `NextAction`
// for it, so a new kind of identifier changes it and the screens, and nothing in between.

internal val SignupKind.identifierType: IdentifierType
    get() = when (this) {
        SignupKind.Email -> IdentifierType.EMAIL
        SignupKind.Phone -> IdentifierType.PHONE
    }

/** The steps the server must be able to offer for this kind, so `authorize` never advertises one with no screen. */
internal val SignupKind.capabilities: Set<EmbeddedCapability>
    get() = when (this) {
        SignupKind.Email -> setOf(
            EmbeddedCapability.IDENTIFY_EMAIL,
            EmbeddedCapability.CHALLENGE_EMAIL,
            EmbeddedCapability.VERIFY_OTP,
        )

        SignupKind.Phone -> setOf(
            EmbeddedCapability.IDENTIFY_PHONE,
            EmbeddedCapability.CHALLENGE_PHONE,
            EmbeddedCapability.VERIFY_OTP,
        )
    }

/** The kind of a server identifier; null for one this flow has no screens for. */
private fun IdentifierType.toSignupKind(): SignupKind? = when (this) {
    IdentifierType.EMAIL -> SignupKind.Email
    IdentifierType.PHONE -> SignupKind.Phone
    IdentifierType.USERNAME -> null
}

/** Whether this action asks for [kind] to be entered, or entered again to correct it. */
private fun NextAction.asksFor(kind: SignupKind): Boolean = when (kind) {
    SignupKind.Email -> this is NextAction.IdentifyEmail
    SignupKind.Phone -> this is NextAction.IdentifyPhone
}

/**
 * The server's offer to send a code, when this action is one. A phone that lists no way to send it is no offer, as on
 * iOS: there is nothing to send. (The login treats that as a text.)
 */
private fun NextAction.toSignupChallenge(): SignupChallenge? = when (this) {
    is NextAction.ChallengeEmail -> SignupChallenge(index)
    is NextAction.ChallengePhone -> {
        val ways = PhoneDeliveryMethod.entries.filter { it in deliveryMethods }
        // A number the server hid completely (`*********`) tells the user nothing, so the screen shows what was typed.
        val named = identifier.takeIf { shown -> shown.any(Char::isDigit) }
        if (ways.isEmpty()) null else SignupChallenge(index, sentTo = named, deliveries = ways)
    }
    else -> null
}

/** What one sign-up requires, from `NextAction.Signup`. */
internal data class SignupOffer(
    /** The required kinds this flow can render. */
    val required: List<SignupKind>,
    /** The server requires a kind this flow has no screens for, so the sign-up can't be finished here. */
    val requiresUnsupportedKind: Boolean,
) {
    /**
     * Whether this flow can take the user to a created account. Exactly one identifier may be required until several
     * are supported. An optional one is left out: the server lets the account be created without it.
     */
    val isSupported: Boolean get() = !requiresUnsupportedKind && required.size == 1
}

/** The sign-up the server's menu offers, or null when it offers none. */
internal fun List<NextAction>.toSignupOffer(): SignupOffer? {
    val signup = filterIsInstance<NextAction.Signup>().firstOrNull() ?: return null
    val required = signup.identifiers.filter { it.required }.map { it.type.toSignupKind() }
    return SignupOffer(required.filterNotNull(), requiresUnsupportedKind = null in required)
}

/** The server's menu, read for sign-up. */
internal data class SignupMenu(
    /**
     * Kinds the server will take an entry for, the required ones first. Includes one still being verified: entering it
     * again corrects it.
     */
    val askedFor: List<SignupKind>,
    /** A code the server will send now, or again. */
    val challenge: SignupChallenge?,
    /** Every required identifier is verified: the account can be created. */
    val canConfirm: Boolean,
) {
    /** One rule for every menu: create the account once it can be, else ask for what is still to be entered. */
    val next: SignupNext
        get() = when {
            canConfirm -> SignupNext.ReadyToCreate
            askedFor.isNotEmpty() -> SignupNext.AskFor(askedFor.first())
            else -> SignupNext.NothingToDo
        }
}

/**
 * [required] are the kinds this sign-up requires: the ones asked for first, since a connection that requires a phone and
 * allows an email must not start on the email.
 */
internal fun List<NextAction>.toSignupMenu(required: List<SignupKind> = emptyList()): SignupMenu = SignupMenu(
    askedFor = SignupKind.entries.filter { kind -> any { it.asksFor(kind) } }.sortedBy { it !in required },
    challenge = firstNotNullOfOrNull { it.toSignupChallenge() },
    canConfirm = any { it is NextAction.ConfirmSignup },
)
