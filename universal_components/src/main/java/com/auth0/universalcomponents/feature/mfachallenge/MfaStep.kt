package com.auth0.universalcomponents.feature.mfachallenge

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

/*
 * What MFA shows at one moment, as data: the same for the standard screens, a screen the app replaced, and an app that
 * draws everything itself (headless). The models hold data only; the words come from the app's MfaMessages, and a
 * replaced screen receives the resolved words together with its model.
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
data class LoadingModel(val cancel: MfaAction? = null)

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
data class AuthenticatorListModel(
    val header: AuthenticatorListHeader,
    val rows: List<Row>,
    val round: Int = 1,
    val pendingRowId: String? = null,
    val select: MfaSelectAction = MfaSelectAction { },
    val cancel: MfaAction = MfaAction.None,
) {

    /**
     * One way to verify. [title] is what the user named the factor, such as a device name, and may be blank: show the
     * factor's own name then (`messages.pushChallenge.row`). [subtitle] is null from this library: the standard list
     * words its second line from the messages too. [icon] is a drawable of this library; null draws none.
     */
    @Immutable
    data class Row(
        val id: String,
        val factor: MfaFactorId,
        val title: String,
        val subtitle: String? = null,
        @DrawableRes val icon: Int? = null,
    )
}

/**
 * The screen of one factor. [model] is the factor's own model, picked by [factor] (a push gets a
 * [PushChallengeModel]).
 */
@Immutable
data class MfaFactorStep(val factor: MfaFactorId, val model: MfaFactorModel, val round: Int = 1)

/** What a factor's screen shows and does. */
sealed interface MfaFactorModel

/**
 * A sign-in request was sent to [deviceName] (maybe blank) and waits for approval. Resend is offered while [canResend]
 * and opens once [resendAvailableIn] seconds have passed; while it goes out, [isResending] is true. [switchFactor]
 * returns to the list when [canSwitch]. [cancel] leaves MFA.
 */
@Immutable
data class PushChallengeModel(
    val deviceName: String,
    val canResend: Boolean = false,
    val resendAvailableIn: Int = 0,
    val canSwitch: Boolean = false,
    val resend: MfaAction = MfaAction.None,
    val switchFactor: MfaAction = MfaAction.None,
    val cancel: MfaAction = MfaAction.None,
    val isResending: Boolean = false,
) : MfaFactorModel

/** Why MFA ended is [error]; [exit] says where [leave] goes. */
@Immutable
data class ErrorModel(
    val error: MfaError,
    val exit: MfaExit = MfaExit.StartOver,
    val leave: MfaAction = MfaAction.None,
)

/** Where the error screen's way out leads. */
enum class MfaExit {

    /** Back to the first factor. */
    StartOver,

    /** Close MFA. */
    Close,
}
