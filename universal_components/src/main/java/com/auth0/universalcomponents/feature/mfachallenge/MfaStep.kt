package com.auth0.universalcomponents.feature.mfachallenge

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

/*
 * What MFA shows at one moment, as data: the same for the standard screens, a screen the app replaced, and an app that
 * draws everything itself (headless). A model holds data and one function per control; the words come from the app's
 * MfaMessages, and a replaced screen receives the resolved words together with its model.
 *
 * Functions only equal themselves, so two models are equal when they hold the same data and the very same functions:
 * to check what a model shows, compare its fields.
 *
 * A later release may add a field to a model. It goes last, with a default, and the constructors keep every shorter
 * form (`@JvmOverloads`), so an app that is already compiled keeps working. `copy` and destructuring are the exception:
 * recompile if you use them.
 *
 * A model can change often (the push model every second, while the resend wait counts down), so draw from the latest
 * one. A later release may add steps, factors and error cases: give every `when` over these types an `else` branch.
 */

/** The screen MFA is on. A new factor adds no case here: its screen is a [Factor]. */
sealed interface MfaStep {

    /** Something is going out or coming in, and there is nothing to ask the user. */
    @Immutable
    data class Loading(val model: LoadingModel) : MfaStep

    /** The factors on offer, to pick one from. */
    @Immutable
    data class AuthenticatorList(val model: AuthenticatorListModel) : MfaStep

    /** One factor's own screen, such as waiting for a push to be approved. */
    @Immutable
    data class Factor(val step: MfaFactorStep) : MfaStep

    /** MFA ended without signing in, and says why. */
    @Immutable
    data class Error(val model: ErrorModel) : MfaStep
}

/** [cancel] leaves MFA, when the user may; null when there is nothing to cancel. */
@Immutable
data class LoadingModel @JvmOverloads constructor(val cancel: (() -> Unit)? = null)

/** Which words head the list. */
enum class AuthenticatorListHeader {

    /** The first factor. */
    Challenge,

    /** The server asked for one more factor after the first one passed. */
    ChallengeAdditional,
}

/**
 * The factors one round offers. [round] counts from 1; it is above 1 when the server asked for one more factor.
 * While [pendingRowId]'s challenge goes out, that row is busy and the others should not be tapped. [select] picks a
 * row; [cancel] leaves MFA.
 */
@Immutable
data class AuthenticatorListModel @JvmOverloads constructor(
    val header: AuthenticatorListHeader,
    val rows: List<Row>,
    val round: Int = 1,
    val pendingRowId: String? = null,
    val select: (rowId: String) -> Unit = {},
    val cancel: () -> Unit = {},
) {

    /**
     * One way to verify. [title] is what the user named the factor, such as a device name, and may be blank: show the
     * factor's own name then (`messages.pushChallenge.row`); for a phone it is the number as the server hides it, and
     * [delivery] says how its code is sent (the standard list words the row from both); for an email it is the address
     * as the server hides it, and blank when the server gave none (`messages.emailChallenge.row`); for an authenticator
     * app (`messages.totpChallenge.row`) and a recovery code (`messages.recoveryCodeChallenge.row`) it is always blank.
     * [subtitle] is null from this library: the standard list words its second line from the messages too. [icon] is a
     * drawable of this library; null draws none.
     */
    @Immutable
    data class Row @JvmOverloads constructor(
        val id: String,
        val factor: MfaFactorId,
        val title: String,
        val subtitle: String? = null,
        @param:DrawableRes val icon: Int? = null,
        val delivery: PhoneDelivery? = null,
    )
}

/**
 * The screen of one factor. [model] is the factor's own model, picked by [factor] (a push gets a
 * [PushChallengeModel], a phone a [PhoneChallengeModel], an email an [EmailChallengeModel], an authenticator app a
 * [TotpChallengeModel], a recovery code a [RecoveryCodeChallengeModel]).
 */
@Immutable
data class MfaFactorStep @JvmOverloads constructor(
    val factor: MfaFactorId,
    val model: MfaFactorModel,
    val round: Int = 1,
)

/** What a factor's screen shows and does. */
sealed interface MfaFactorModel

/**
 * A sign-in request was sent to [deviceName] (maybe blank) and waits for approval. Resend is offered while [canResend]
 * and opens once [resendAvailableIn] seconds have passed; while it goes out, [isResending] is true. [switchFactor]
 * returns to the list when [canSwitch]. [cancel] leaves MFA.
 */
@Immutable
data class PushChallengeModel @JvmOverloads constructor(
    val deviceName: String,
    val canResend: Boolean = false,
    val resendAvailableIn: Int = 0,
    val canSwitch: Boolean = false,
    val resend: () -> Unit = {},
    val switchFactor: () -> Unit = {},
    val cancel: () -> Unit = {},
    val isResending: Boolean = false,
) : MfaFactorModel

/**
 * What every screen that takes a typed code shows and does, whichever factor's code it is, so one screen of your own
 * can serve them all. [submit] checks a typed code, unless [isVerifying]; [isWrongCode] is true after the server
 * refused the last one. [switchFactor] returns to the list when [canSwitch]. [cancel] leaves MFA.
 *
 * [requestError] says why the last check, or the last resend, got no answer while the screen stays up: a
 * [MfaError.Network] (the request may or may not have reached the server, so the same code may be tried again), or on a
 * resend a [MfaError.TooManyAttempts]. It is null otherwise, and cleared when the next request starts; the standard
 * screens also hide its line as soon as the user edits the code. Show `requestError?.let { messages.errors.messageFor(it) }`,
 * or the factor's own `noConnection` and `tooManyRequests` words.
 */
sealed interface CodeChallengeModel : MfaFactorModel {
    val isWrongCode: Boolean
    val isVerifying: Boolean
    val canSwitch: Boolean
    val submit: (code: String) -> Unit
    val switchFactor: () -> Unit
    val cancel: () -> Unit
    val requestError: MfaError?
}

/**
 * A code the server sends, so it can be sent again. Resend is offered while [canResend] and opens once
 * [resendAvailableIn] seconds have passed; while it goes out, [isResending] is true. [isCodeExpired] is true when the
 * server says the code timed out but offers another: [submit] does nothing until Resend brings a new one.
 */
sealed interface SentCodeChallengeModel : CodeChallengeModel {
    val isResending: Boolean
    val canResend: Boolean
    val resendAvailableIn: Int
    val resend: () -> Unit
    val isCodeExpired: Boolean
}

/**
 * A code was sent to [phoneNumber] (as the server hides it) by [delivery], and waits to be typed. See
 * [SentCodeChallengeModel] and [CodeChallengeModel] for what the other fields mean.
 */
@Immutable
data class PhoneChallengeModel @JvmOverloads constructor(
    val phoneNumber: String,
    val delivery: PhoneDelivery,
    override val isWrongCode: Boolean = false,
    override val isVerifying: Boolean = false,
    override val isResending: Boolean = false,
    override val canResend: Boolean = false,
    override val resendAvailableIn: Int = 0,
    override val canSwitch: Boolean = false,
    override val submit: (code: String) -> Unit = {},
    override val resend: () -> Unit = {},
    override val switchFactor: () -> Unit = {},
    override val cancel: () -> Unit = {},
    override val requestError: MfaError? = null,
    override val isCodeExpired: Boolean = false,
) : SentCodeChallengeModel

/** How a phone gets its code. */
enum class PhoneDelivery {

    /** A text message. */
    Text,

    /** A phone call that speaks the code. */
    Voice,
}

/**
 * A code was sent to [address] (as the server hides it, maybe blank) and waits to be typed. See
 * [SentCodeChallengeModel] and [CodeChallengeModel] for what the other fields mean.
 */
@Immutable
data class EmailChallengeModel @JvmOverloads constructor(
    val address: String,
    override val isWrongCode: Boolean = false,
    override val isVerifying: Boolean = false,
    override val isResending: Boolean = false,
    override val canResend: Boolean = false,
    override val resendAvailableIn: Int = 0,
    override val canSwitch: Boolean = false,
    override val submit: (code: String) -> Unit = {},
    override val resend: () -> Unit = {},
    override val switchFactor: () -> Unit = {},
    override val cancel: () -> Unit = {},
    override val requestError: MfaError? = null,
    override val isCodeExpired: Boolean = false,
) : SentCodeChallengeModel

/**
 * The code an authenticator app shows waits to be typed. Nothing is sent, so there is no destination to name and no
 * Resend. See [CodeChallengeModel] for what the fields mean.
 */
@Immutable
data class TotpChallengeModel @JvmOverloads constructor(
    override val isWrongCode: Boolean = false,
    override val isVerifying: Boolean = false,
    override val canSwitch: Boolean = false,
    override val submit: (code: String) -> Unit = {},
    override val switchFactor: () -> Unit = {},
    override val cancel: () -> Unit = {},
    override val requestError: MfaError? = null,
) : CodeChallengeModel

/**
 * A recovery code is typed, and then the new one that replaces it is saved. Nothing is sent to the user, so there is
 * nothing to name and no Resend. It is not a [CodeChallengeModel]: a recovery code is text of no fixed length, not six
 * digits, so a screen of your own for typed codes should not draw it as one.
 *
 * While [newCode] is null the user types a code: [submit] checks it, unless [isVerifying], and [isWrongCode] is true after
 * the server refused the last one. Spaces around it, and the invisible characters a paste brings along, are dropped
 * before it is sent; a blank code is ignored.
 *
 * Once the server accepts the code it is used up, and [newCode] is set: show it so the user can save it, and call
 * [confirm] when they have, which signs them in (it does nothing while [isVerifying]). The standard screen holds Continue
 * back until the user ticks that they saved it; a screen of your own decides that for itself. On this step [switchFactor]
 * does nothing, as there is no other factor to go to. [cancel] and Back leave, and [newCode] is lost with them, as it is
 * the only copy there is.
 *
 * [requestError] is [MfaError.Network] when the last request got no answer and the screen stays up: type the code again
 * or call [confirm] again; it is null otherwise, and cleared when the next request starts. [newCode] is a live secret:
 * never log it, and don't print or dump the model, whose description leaves it out.
 */
@Immutable
data class RecoveryCodeChallengeModel @JvmOverloads constructor(
    val newCode: String? = null,
    val isWrongCode: Boolean = false,
    val isVerifying: Boolean = false,
    val requestError: MfaError? = null,
    val canSwitch: Boolean = false,
    val submit: (code: String) -> Unit = {},
    val confirm: () -> Unit = {},
    val switchFactor: () -> Unit = {},
    val cancel: () -> Unit = {},
) : MfaFactorModel {

    override fun toString(): String =
        "RecoveryCodeChallengeModel(hasNewCode=${newCode != null}, isWrongCode=$isWrongCode, isVerifying=$isVerifying, " +
            "requestError=$requestError, canSwitch=$canSwitch)"
}

/** Why MFA ended is [error]; [exit] says where [leave] goes. */
@Immutable
data class ErrorModel @JvmOverloads constructor(
    val error: MfaError,
    val exit: MfaExit = MfaExit.StartOver,
    val leave: () -> Unit = {},
)

/** Where the error screen's way out leads. */
enum class MfaExit {

    /** Back to the first factor. */
    StartOver,

    /** Close MFA. */
    Close,
}

/**
 * Which kind of second factor a row or a screen is about. Open, so a later release adds a factor without breaking an
 * app's `when`: give it an `else` branch.
 */
@JvmInline
value class MfaFactorId(val value: String) {

    companion object {
        /** Approve a sign-in request on another device. */
        val Push = MfaFactorId("push")

        /** Type a code that was sent to a phone, by text or by a call. */
        val Phone = MfaFactorId("phone")

        /** Type a code that was sent to an email address. */
        val Email = MfaFactorId("email")

        /** Type the code an authenticator app shows. */
        val Totp = MfaFactorId("totp")

        /** Type a recovery code, then save the new one that replaces it. */
        val RecoveryCode = MfaFactorId("recovery_code")
    }
}
