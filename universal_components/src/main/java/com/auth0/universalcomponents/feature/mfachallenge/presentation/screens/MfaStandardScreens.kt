package com.auth0.universalcomponents.feature.mfachallenge.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.auth0.universalcomponents.feature.mfachallenge.AuthenticatorListHeader
import com.auth0.universalcomponents.feature.mfachallenge.AuthenticatorListModel
import com.auth0.universalcomponents.feature.mfachallenge.EmailChallengeModel
import com.auth0.universalcomponents.feature.mfachallenge.ErrorModel
import com.auth0.universalcomponents.feature.mfachallenge.LoadingModel
import com.auth0.universalcomponents.feature.mfachallenge.MfaError
import com.auth0.universalcomponents.feature.mfachallenge.MfaExit
import com.auth0.universalcomponents.feature.mfachallenge.MfaFactorId
import com.auth0.universalcomponents.feature.mfachallenge.PhoneChallengeModel
import com.auth0.universalcomponents.feature.mfachallenge.PhoneDelivery
import com.auth0.universalcomponents.feature.mfachallenge.PushChallengeModel
import com.auth0.universalcomponents.feature.mfachallenge.ResolvedMfaMessages
import com.auth0.universalcomponents.feature.mfachallenge.presentation.forDisplay
import com.auth0.universalcomponents.feature.mfachallenge.presentation.hasDigits
import com.auth0.universalcomponents.presentation.ui.components.CircularLoader
import com.auth0.universalcomponents.presentation.ui.components.OTPInputField
import com.auth0.universalcomponents.presentation.ui.flow.FlowErrorScreen
import com.auth0.universalcomponents.presentation.ui.flow.FlowLoadingScreen
import com.auth0.universalcomponents.presentation.ui.flow.FlowPrimaryButton
import com.auth0.universalcomponents.presentation.ui.flow.FlowScreenHeader
import com.auth0.universalcomponents.presentation.ui.flow.FlowStepLayout
import com.auth0.universalcomponents.presentation.ui.flow.FlowTextButton
import com.auth0.universalcomponents.theme.Auth0Theme

/*
 * The standard MFA screens. Each draws a public model with the resolved words, exactly what a screen the app replaced
 * receives, so the two behave the same. They draw no frame: [MfaHost] places them inside the login frame.
 */

/**
 * A short wait with nothing to ask. It is there from the first frame, since the user caused it and a blank screen
 * would look broken; [messages] gives screen readers a label. It offers no Cancel: leaving is the host's job (Back), so
 * [model] is only there to match the other screens.
 */
@Composable
@Suppress("UnusedParameter")
internal fun MfaLoadingScreen(
    model: LoadingModel,
    messages: ResolvedMfaMessages,
    modifier: Modifier = Modifier,
) {
    val label = messages.loading.accessibilityLabel
    FlowLoadingScreen(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = label },
        showAtOnce = true,
    )
}

/**
 * Every factor the round offers, one row each. From round 2 on, the server has asked for one more factor, so the
 * header says so. While [AuthenticatorListModel.pendingRowId]'s challenge goes out, its row is busy and no row can be
 * tapped.
 */
@Composable
internal fun MfaAuthenticatorListScreen(
    model: AuthenticatorListModel,
    messages: ResolvedMfaMessages,
    modifier: Modifier = Modifier,
) {
    val dimensions = Auth0Theme.dimensions
    val header = when (model.header) {
        AuthenticatorListHeader.Challenge -> messages.authenticatorList.headers.challenge
        AuthenticatorListHeader.ChallengeAdditional -> messages.authenticatorList.headers.challengeAdditional
    }
    FlowStepLayout(
        modifier = modifier,
        pageDescription = {
            FlowScreenHeader(
                title = header.title,
                subtitle = header.description,
                bottomSpacing = dimensions.spacingLg,
            )
        },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(dimensions.spacingXs)) {
                model.rows.forEach { row ->
                    val words = row.words(messages)
                    MfaFactorRow(
                        icon = row.icon?.let { painterResource(it) },
                        label = words.title,
                        subtitle = words.description,
                        onClick = { model.select(row.id) },
                        isBusy = row.id == model.pendingRowId,
                        enabled = model.pendingRowId == null,
                    )
                }
            }
        },
        primaryAction = {},
    )
}

/**
 * Waiting for the user to approve the push on [PushChallengeModel.deviceName], which may be blank. Resend shows only
 * while [PushChallengeModel.canResend] (the server allows another push) and opens once the wait is over; while the push
 * is being sent again it can't be tapped twice, and "Verify another way" is off until the push is out. That action
 * shows only when [PushChallengeModel.canSwitch].
 */
@Composable
internal fun MfaPushChallengeScreen(
    model: PushChallengeModel,
    messages: ResolvedMfaMessages,
    modifier: Modifier = Modifier,
) {
    val dimensions = Auth0Theme.dimensions
    val words = messages.pushChallenge
    FlowStepLayout(
        modifier = modifier,
        pageDescription = {
            FlowScreenHeader(
                title = words.title,
                subtitle = if (model.deviceName.isBlank()) {
                    words.descriptionUnnamed
                } else {
                    words.description(model.deviceName)
                },
                bottomSpacing = dimensions.spacingXxl + dimensions.spacingMd,
            )
        },
        content = { MfaWaitingForApproval(words.waiting, device = model.deviceName) },
        primaryAction = {
            Column(verticalArrangement = Arrangement.spacedBy(dimensions.spacingMd)) {
                if (model.canResend) {
                    FlowPrimaryButton(
                        title = if (model.resendAvailableIn > 0) {
                            words.resend.cooldown(model.resendAvailableIn)
                        } else {
                            words.resend.button
                        },
                        onClick = { model.resend() },
                        enabled = model.resendAvailableIn == 0,
                    )
                }
                if (model.canSwitch) {
                    FlowTextButton(
                        title = words.verifyAnotherWay,
                        onClick = { model.switchFactor() },
                        enabled = !model.isResending,
                    )
                }
            }
        },
        pinActionsToBottom = false,
    )
}

/**
 * Typing the code that went to [PhoneChallengeModel.phoneNumber], by text or by a call. Verify turns on once every digit
 * is in and nothing is on its way; the wrong-code line hides once the user types again. Resend and "Verify another
 * way" follow the push screen's rules.
 */
@Composable
internal fun MfaPhoneChallengeScreen(
    model: PhoneChallengeModel,
    messages: ResolvedMfaMessages,
    modifier: Modifier = Modifier,
) {
    val words = messages.phoneChallenge
    MfaCodeEntry(
        words = CodeEntryWords(
            title = words.title,
            subtitle = when (model.delivery) {
                PhoneDelivery.Text -> words.description(model.phoneNumber)
                PhoneDelivery.Voice -> words.descriptionVoice(model.phoneNumber)
            },
            codeLabel = words.codeLabel,
            verifyButton = words.verifyButton,
            wrongCode = words.wrongCode,
            resendButton = words.resend.button,
            resendCooldown = words.resend.cooldown,
            verifyAnotherWay = words.verifyAnotherWay,
        ),
        controls = CodeEntryControls(
            isWrongCode = model.isWrongCode,
            isVerifying = model.isVerifying,
            isResending = model.isResending,
            canResend = model.canResend,
            resendAvailableIn = model.resendAvailableIn,
            canSwitch = model.canSwitch,
            submit = model.submit,
            resend = model.resend,
            switchFactor = model.switchFactor,
        ),
        modifier = modifier,
    )
}

/**
 * Typing the code that went to [EmailChallengeModel.address]. It follows the phone code screen's rules; when the server
 * gave no address, the line under the title says the code went by email without naming it.
 */
@Composable
internal fun MfaEmailChallengeScreen(
    model: EmailChallengeModel,
    messages: ResolvedMfaMessages,
    modifier: Modifier = Modifier,
) {
    val words = messages.emailChallenge
    MfaCodeEntry(
        words = CodeEntryWords(
            title = words.title,
            subtitle = words.subtitleFor(model.address),
            codeLabel = words.codeLabel,
            verifyButton = words.verifyButton,
            wrongCode = words.wrongCode,
            resendButton = words.resend.button,
            resendCooldown = words.resend.cooldown,
            verifyAnotherWay = words.verifyAnotherWay,
        ),
        controls = CodeEntryControls(
            isWrongCode = model.isWrongCode,
            isVerifying = model.isVerifying,
            isResending = model.isResending,
            canResend = model.canResend,
            resendAvailableIn = model.resendAvailableIn,
            canSwitch = model.canSwitch,
            submit = model.submit,
            resend = model.resend,
            switchFactor = model.switchFactor,
        ),
        modifier = modifier,
    )
}

/** The line under the title: the address the code went to, or, when the server gave none, that it went by email. */
internal fun ResolvedMfaMessages.EmailChallenge.subtitleFor(address: String): String =
    if (address.isBlank()) descriptionUnnamed else description(address)

/** The words of a code screen; each factor's screen fills them from its own messages. */
private class CodeEntryWords(
    val title: String,
    val subtitle: String,
    val codeLabel: String,
    val verifyButton: String,
    val wrongCode: String,
    val resendButton: String,
    val resendCooldown: (seconds: Int) -> String,
    val verifyAnotherWay: String,
)

/** What a code screen shows and does, whichever factor's code it takes. */
private class CodeEntryControls(
    val isWrongCode: Boolean,
    val isVerifying: Boolean,
    val isResending: Boolean,
    val canResend: Boolean,
    val resendAvailableIn: Int,
    val canSwitch: Boolean,
    val submit: (code: String) -> Unit,
    val resend: () -> Unit,
    val switchFactor: () -> Unit,
)

/**
 * The body every code screen shares: the six boxes, a wrong-code line, Verify, and Resend and "Verify another way" when
 * [CodeEntryControls.canResend] and [CodeEntryControls.canSwitch] allow them. The title's second line turns off the
 * font's contextual alternates, since it may show a hidden number or address (`*********0128`, `s***@example.com`).
 */
@Composable
private fun MfaCodeEntry(
    words: CodeEntryWords,
    controls: CodeEntryControls,
    modifier: Modifier = Modifier,
) {
    val dimensions = Auth0Theme.dimensions
    var code by rememberSaveable { mutableStateOf("") }
    // Hidden once the user types after a refused code, shown again by the next Verify. Kept across a rotation, so a
    // line the user already dismissed does not come back.
    var hideError by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val isBusy = controls.isVerifying || controls.isResending
    val showsError = controls.isWrongCode && !hideError
    val maxWidth = Auth0Theme.sizes.otpFieldWidth * CODE_LENGTH + dimensions.spacingXs * (CODE_LENGTH - 1)

    FlowStepLayout(
        modifier = modifier,
        pageDescription = {
            FlowScreenHeader(
                title = words.title,
                subtitle = words.subtitle,
                subtitleFontFeatures = NO_CONTEXTUAL_ALTERNATES,
            )
        },
        content = {
            Text(text = words.codeLabel, style = Auth0Theme.typography.label, color = Auth0Theme.colors.textBold)
            Spacer(Modifier.height(dimensions.spacingMd))
            // Held to phone-sized boxes: they grow with the width, and would balloon in landscape.
            Box(modifier = Modifier.widthIn(max = maxWidth)) {
                OTPInputField(
                    value = code,
                    length = CODE_LENGTH,
                    isError = showsError,
                    // The digits on show are the ones being checked, and a wrong-code answer should still be seen.
                    onValueChange = {
                        if (!isBusy) {
                            code = it.filter(Char::isDigit).take(CODE_LENGTH)
                            hideError = true
                        }
                    },
                    focusRequester = focusRequester,
                    // The visible label is a separate node, so TalkBack would land on an unnamed edit box.
                    modifier = Modifier.semantics { contentDescription = words.codeLabel },
                )
            }
            if (showsError) {
                Text(
                    text = words.wrongCode,
                    style = Auth0Theme.typography.bodySmall,
                    color = Auth0Theme.colors.textOnError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dimensions.spacingXs)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        },
        primaryAction = {
            FlowPrimaryButton(
                title = words.verifyButton,
                onClick = {
                    hideError = false
                    controls.submit(code)
                },
                isLoading = controls.isVerifying,
                enabled = code.length == CODE_LENGTH && !isBusy,
            )
        },
        secondaryAction = if (controls.canResend || controls.canSwitch) {
            {
                Column(verticalArrangement = Arrangement.spacedBy(dimensions.spacingMd)) {
                    if (controls.canResend) {
                        FlowTextButton(
                            title = if (controls.resendAvailableIn > 0) {
                                words.resendCooldown(controls.resendAvailableIn)
                            } else {
                                words.resendButton
                            },
                            onClick = {
                                // The old code is no use once a new one is on its way.
                                code = ""
                                controls.resend()
                            },
                            enabled = controls.resendAvailableIn == 0 && !isBusy,
                            style = Auth0Theme.typography.body,
                        )
                    }
                    if (controls.canSwitch) {
                        FlowTextButton(
                            title = words.verifyAnotherWay,
                            onClick = { controls.switchFactor() },
                            enabled = !isBusy,
                        )
                    }
                }
            }
        } else {
            null
        },
        pinActionsToBottom = false,
    )
}

/**
 * MFA ended, and this says why: the server's own words for an unexpected error when it gave some (cut at
 * [MAX_SERVER_EXPLANATION] characters, since they come untranslated and unchecked). The way out is "Start over" or
 * "Close".
 */
@Composable
internal fun MfaErrorScreen(
    model: ErrorModel,
    messages: ResolvedMfaMessages,
    modifier: Modifier = Modifier,
) {
    val words = messages.errors.messageFor(model.error)
    FlowErrorScreen(
        modifier = modifier,
        title = words.title,
        message = model.error.descriptionToShow(words),
        actionTitle = when (model.exit) {
            MfaExit.StartOver -> messages.errors.startOver
            MfaExit.Close -> messages.errors.close
        },
        onRetry = { model.leave() },
    )
}

/**
 * Inter swaps a `*` or a bullet next to digits for a raised, larger "case" form, but only for the few nearest the
 * digits, so a hidden number (`*********0128`) would show two sizes. Lines that show one turn that feature off.
 */
internal const val NO_CONTEXTUAL_ALTERNATES = "'calt' 0"

/** How many digits the code has. */
private const val CODE_LENGTH = 6

/** The longest explanation from the server the error screen shows; it is untranslated text we did not write. */
internal const val MAX_SERVER_EXPLANATION = 200

/** What the error screen says under its title: the server's explanation, cut short, or else our words for the error. */
internal fun MfaError.descriptionToShow(words: ResolvedMfaMessages.Message): String {
    val explanation = (this as? MfaError.Unexpected)?.serverMessage?.trim().orEmpty()
    return when {
        explanation.isEmpty() -> words.description
        explanation.length <= MAX_SERVER_EXPLANATION -> explanation
        else -> explanation.take(MAX_SERVER_EXPLANATION).trimEnd() + "…"
    }
}

/** The spinner with its caption, announced once to screen readers, with the device's name when it has one. */
@Composable
private fun MfaWaitingForApproval(caption: String, device: String) {
    val dimensions = Auth0Theme.dimensions
    val label = if (device.isBlank()) caption else "$caption, $device"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensions.spacingXl + dimensions.spacingXxs)
            .semantics(mergeDescendants = true) { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingSm),
    ) {
        CircularLoader(modifier = Modifier.size(dimensions.spacingXxl))
        Text(
            text = caption,
            style = Auth0Theme.typography.body,
            color = Auth0Theme.colors.textDefault,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/**
 * What a row says. A named device leads, with the factor's name under it; a device with no name shows the factor's name
 * with its description under it. A phone leads with how its code is sent, with its number under it (left out when the
 * server hid every digit); an email leads with its name, with its address under it. A factor this version has no words
 * for shows just its own title.
 */
internal fun AuthenticatorListModel.Row.words(messages: ResolvedMfaMessages): ResolvedMfaMessages.Message {
    if (factor == MfaFactorId.Phone && delivery != null) {
        val phone = messages.phoneChallenge
        val label = if (delivery == PhoneDelivery.Voice) phone.rowVoice(title) else phone.rowText(title)
        return ResolvedMfaMessages.Message(label, if (title.hasDigits()) title.forDisplay() else "")
    }
    if (factor == MfaFactorId.Email) {
        // The factor's name leads, with the address under it; with no address, its description takes that place.
        val kind = messages.emailChallenge.row
        return if (title.isBlank()) kind else ResolvedMfaMessages.Message(kind.title, title)
    }
    if (factor != MfaFactorId.Push) return ResolvedMfaMessages.Message(title, subtitle.orEmpty())
    val kind = messages.pushChallenge.row
    return if (title.isBlank()) kind else ResolvedMfaMessages.Message(title, kind.title)
}
