package com.auth0.universalcomponents.feature.mfachallenge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.auth0.universalcomponents.feature.mfachallenge.presentation.components.MfaFactorRow
import com.auth0.universalcomponents.presentation.ui.components.CircularLoader
import com.auth0.universalcomponents.presentation.ui.flow.LoginErrorScreen
import com.auth0.universalcomponents.presentation.ui.flow.LoginLoadingScreen
import com.auth0.universalcomponents.presentation.ui.flow.LoginPrimaryButton
import com.auth0.universalcomponents.presentation.ui.flow.LoginScreenHeader
import com.auth0.universalcomponents.presentation.ui.flow.LoginStepLayout
import com.auth0.universalcomponents.presentation.ui.flow.LoginTextButton
import com.auth0.universalcomponents.theme.Auth0Theme

/*
 * The standard MFA screens. Each draws a public model with the resolved words, exactly what a screen the app replaced
 * receives, so the two behave the same. They are public so an app can use one on its own, to keep the look for a step
 * it doesn't redraw, or to drive its own flow with a model it builds. They draw no frame: place them inside your own,
 * and inside [Auth0Theme] (`EmbeddedLoginScreen` already provides it); without a theme they throw.
 */

/**
 * A short wait with nothing to ask. It is there from the first frame, since the user caused it and a blank screen
 * would look broken; [messages] gives screen readers a label. It offers no Cancel: leaving is the host's job (Back), so
 * [model] is only there to match the other screens. Draw it inside [Auth0Theme].
 */
@Composable
@Suppress("UnusedParameter")
fun MfaLoadingScreen(
    model: LoadingModel,
    modifier: Modifier = Modifier,
    messages: ResolvedMfaMessages = MfaMessages.Default.resolve(),
) {
    val label = messages.loading.accessibilityLabel
    LoginLoadingScreen(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = label },
        showAtOnce = true,
    )
}

/**
 * Every factor the round offers, one row each. From round 2 on, the server has asked for one more factor, so the
 * header says so. While [AuthenticatorListModel.pendingRowId]'s challenge goes out, its row is busy and no row can be
 * tapped. Draw it inside [Auth0Theme].
 */
@Composable
fun MfaAuthenticatorListScreen(
    model: AuthenticatorListModel,
    modifier: Modifier = Modifier,
    messages: ResolvedMfaMessages = MfaMessages.Default.resolve(),
) {
    val dimensions = Auth0Theme.dimensions
    val header = when (model.header) {
        AuthenticatorListHeader.Challenge -> messages.authenticatorList.headers.challenge
        AuthenticatorListHeader.ChallengeAdditional -> messages.authenticatorList.headers.challengeAdditional
    }
    LoginStepLayout(
        modifier = modifier,
        pageDescription = {
            LoginScreenHeader(
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
 * shows only when [PushChallengeModel.canSwitch]. Draw it inside [Auth0Theme].
 */
@Composable
fun MfaPushChallengeScreen(
    model: PushChallengeModel,
    modifier: Modifier = Modifier,
    messages: ResolvedMfaMessages = MfaMessages.Default.resolve(),
) {
    val dimensions = Auth0Theme.dimensions
    val words = messages.pushChallenge
    LoginStepLayout(
        modifier = modifier,
        pageDescription = {
            LoginScreenHeader(
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
                    LoginPrimaryButton(
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
                    LoginTextButton(
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
 * MFA ended, and this says why: the server's own words for an unexpected error when it gave some. The way out is
 * "Start over" or "Close". Draw it inside [Auth0Theme].
 */
@Composable
fun MfaErrorScreen(
    model: ErrorModel,
    modifier: Modifier = Modifier,
    messages: ResolvedMfaMessages = MfaMessages.Default.resolve(),
) {
    val words = messages.errors.textFor(model.error)
    LoginErrorScreen(
        modifier = modifier,
        title = words.title,
        message = (model.error as? MfaError.Unexpected)?.serverMessage ?: words.description,
        actionTitle = when (model.exit) {
            MfaExit.StartOver -> messages.errors.startOver
            MfaExit.Close -> messages.errors.close
        },
        onRetry = { model.leave() },
    )
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
 * with its description under it. A factor this version has no words for shows just its own title.
 */
private fun AuthenticatorListModel.Row.words(messages: ResolvedMfaMessages): ResolvedMfaMessages.Text {
    if (factor != MfaFactorId.Push) return ResolvedMfaMessages.Text(title, subtitle.orEmpty())
    val kind = messages.pushChallenge.row
    return if (title.isBlank()) kind else ResolvedMfaMessages.Text(title, kind.title)
}
