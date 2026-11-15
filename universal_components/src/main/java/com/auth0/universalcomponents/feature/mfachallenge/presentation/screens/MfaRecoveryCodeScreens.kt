package com.auth0.universalcomponents.feature.mfachallenge.presentation.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginFieldLabel
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginInputField
import com.auth0.universalcomponents.feature.mfachallenge.RecoveryCodeChallengeModel
import com.auth0.universalcomponents.feature.mfachallenge.ResolvedMfaMessages
import com.auth0.universalcomponents.feature.mfachallenge.presentation.cleanedRecoveryCode
import com.auth0.universalcomponents.presentation.ui.flow.FlowPrimaryButton
import com.auth0.universalcomponents.presentation.ui.flow.FlowScreenHeader
import com.auth0.universalcomponents.presentation.ui.flow.FlowStepLayout
import com.auth0.universalcomponents.presentation.ui.flow.FlowTextButton
import com.auth0.universalcomponents.presentation.ui.flow.flowSurface
import com.auth0.universalcomponents.theme.Auth0Theme
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Typing a recovery code, and then saving the new one that replaces it, on one screen of the factor: the model's
 * [RecoveryCodeChallengeModel.newCode] says which. The typed code is kept only in memory: a rotation starts the field
 * again, so a secret is never put in the saved state.
 */
@Composable
internal fun MfaRecoveryCodeChallengeScreen(
    model: RecoveryCodeChallengeModel,
    messages: ResolvedMfaMessages,
    modifier: Modifier = Modifier,
) {
    val words = messages.recoveryCodeChallenge
    val newCode = model.newCode
    if (newCode == null) {
        MfaRecoveryCodeEntry(model, words, modifier)
    } else {
        MfaRecoveryCodeSave(model, newCode, words, modifier)
    }
}

/**
 * The field for the code, hidden by default with an eye to show what was typed. Continue turns on once something is
 * typed and nothing is on its way; the line under the field goes away as soon as the user edits the code.
 */
@Composable
private fun MfaRecoveryCodeEntry(
    model: RecoveryCodeChallengeModel,
    words: ResolvedMfaMessages.RecoveryCodeChallenge,
    modifier: Modifier,
) {
    val colors = Auth0Theme.colors
    val sizes = Auth0Theme.sizes
    // Neither the typed code nor what it answered is saved, so a rotation starts the field again, without the old line.
    var code by remember { mutableStateOf("") }
    var isShown by rememberSaveable { mutableStateOf(false) }
    // True once the user edits the code after the last answer: the line about that answer goes away, and comes back with
    // the next one.
    var hasEdited by remember { mutableStateOf(false) }
    val showsAnswer = !hasEdited && code.isNotEmpty()
    val showsWrongCode = showsAnswer && model.isWrongCode
    val problemText = when {
        !showsAnswer -> null
        model.requestError != null -> words.noConnection
        showsWrongCode -> words.wrongCode
        else -> null
    }
    val hiddenText = remember { PasswordVisualTransformation() }
    val canSubmit = code.cleanedRecoveryCode().isNotEmpty() && !model.isVerifying
    val submit = {
        if (canSubmit) {
            hasEdited = false
            model.submit(code)
        }
    }

    FlowStepLayout(
        modifier = modifier,
        pageDescription = { FlowScreenHeader(title = words.title, subtitle = words.description) },
        content = {
            LoginFieldLabel(words.codeLabel)
            LoginInputField(
                value = code,
                // Not turned off while checking, as the digit boxes are not: a disabled field loses focus, and the user
                // would have to tap it again to retype after a wrong code. What is typed meanwhile is ignored.
                onValueChange = {
                    if (!model.isVerifying) {
                        code = it
                        hasEdited = true
                    }
                },
                // The visible label is a separate node, so TalkBack would land on an unnamed edit box.
                modifier = Modifier.semantics { contentDescription = words.codeLabel },
                isInvalid = showsWrongCode,
                keyboardType = KeyboardType.Password,
                placeholder = words.placeholder,
                trailing = {
                    IconButton(onClick = { isShown = !isShown }) {
                        Icon(
                            imageVector = if (isShown) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (isShown) words.hideCode else words.showCode,
                            tint = colors.textBold,
                            modifier = Modifier.size(sizes.iconMedium),
                        )
                    }
                },
                visualTransformation = if (isShown) VisualTransformation.None else hiddenText,
                onDone = { submit() },
            )
            if (problemText != null) MfaProblemLine(problemText)
        },
        primaryAction = {
            FlowPrimaryButton(
                title = words.continueButton,
                onClick = { submit() },
                isLoading = model.isVerifying,
                enabled = canSubmit,
            )
        },
        secondaryAction = if (model.canSwitch) {
            {
                FlowTextButton(
                    title = words.verifyAnotherWay,
                    onClick = { model.switchFactor() },
                    enabled = !model.isVerifying,
                )
            }
        } else {
            null
        },
        pinActionsToBottom = false,
    )
}

/**
 * The new code, shown with a copy button, and a box the user ticks to say they saved it: Continue turns on once it is
 * ticked. The tick is this screen's own state, so a screen the app draws in its place decides that for itself.
 */
@Composable
private fun MfaRecoveryCodeSave(
    model: RecoveryCodeChallengeModel,
    newCode: String,
    words: ResolvedMfaMessages.RecoveryCodeChallenge,
    modifier: Modifier,
) {
    val colors = Auth0Theme.colors
    val dimensions = Auth0Theme.dimensions
    // Not saved: after a rotation or a restart the user ticks it again, so the tick always means "I saved it just now".
    var hasSaved by remember { mutableStateOf(false) }

    FlowStepLayout(
        modifier = modifier,
        pageDescription = { FlowScreenHeader(title = words.saveTitle, subtitle = words.saveDescription) },
        content = {
            LoginFieldLabel(words.codeLabel)
            MfaCopyableCode(code = newCode, words = words)
            Text(
                text = words.saveHelper,
                style = Auth0Theme.typography.bodySmall,
                color = colors.textDefault,
                modifier = Modifier.padding(top = dimensions.spacingXs),
            )
            // A dropped connection keeps the new code on show, so Continue can be tapped again.
            if (model.requestError != null) MfaProblemLine(words.noConnection)
        },
        // The box sits with the action, so the layout's own gap above the actions is the gap above it. The row is a 48 dp
        // target with the 20 dp box in its middle, so most of the space under the box is its own.
        primaryAction = {
            MfaSavedCheckbox(
                label = words.savedConfirmation,
                checked = hasSaved,
                enabled = !model.isVerifying,
                onCheckedChange = { hasSaved = it },
            )
            Spacer(Modifier.height(dimensions.spacingXxs))
            FlowPrimaryButton(
                title = words.continueButton,
                onClick = { model.confirm() },
                isLoading = model.isVerifying,
                enabled = hasSaved && !model.isVerifying,
            )
        },
        pinActionsToBottom = false,
    )
}

/** The line under a field or a code about the last request, announced to screen readers when it appears. */
@Composable
private fun MfaProblemLine(text: String) {
    Text(
        text = text,
        style = Auth0Theme.typography.bodySmall,
        color = Auth0Theme.colors.textOnError,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Auth0Theme.dimensions.spacingXs)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/**
 * The code in a box with a copy button. It copies as a sensitive item, so the system keeps it out of the clipboard
 * preview, and the copy is taken back after two minutes ([SecretClipboard]). From Android 13 the system confirms the
 * copy itself; before that a short line under the box does.
 */
@Composable
private fun MfaCopyableCode(code: String, words: ResolvedMfaMessages.RecoveryCodeChallenge) {
    val context = LocalContext.current
    val colors = Auth0Theme.colors
    val dimensions = Auth0Theme.dimensions
    val sizes = Auth0Theme.sizes
    // From Android 13 the system confirms a copy itself; before that, a short line under the box does.
    val showsOwnConfirmation = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
    var copies by remember { mutableIntStateOf(0) }
    var isCopiedShown by remember { mutableStateOf(false) }
    LaunchedEffect(copies) {
        if (copies > 0) {
            isCopiedShown = true
            delay(COPIED_SHOWN_MILLIS)
            isCopiedShown = false
        }
    }
    // Spelled out, so a screen reader does not try to read a string of letters and digits as a word.
    val spelledOut = remember(code) { code.toList().joinToString(" ") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = sizes.codeContainerHeight)
            .flowSurface(Auth0Theme.shapes.medium, isFocused = false, isError = false)
            .padding(start = sizes.padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSm),
    ) {
        Text(
            text = code,
            style = Auth0Theme.typography.body.copy(fontFamily = FontFamily.Monospace),
            color = colors.textBold,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = spelledOut },
        )
        IconButton(
            onClick = {
                SecretClipboard.copy(context, label = words.codeLabel, text = code)
                if (showsOwnConfirmation) copies++
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_copy),
                contentDescription = words.copyCode,
                tint = colors.textBold,
                modifier = Modifier.size(sizes.iconMedium),
            )
        }
    }
    if (isCopiedShown) {
        Text(
            text = words.copied,
            style = Auth0Theme.typography.bodySmall,
            color = colors.textDefault,
            modifier = Modifier
                .padding(top = dimensions.spacingXs)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/** A box and its words that toggle together, so the whole row is the tap target. */
@Composable
private fun MfaSavedCheckbox(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = Auth0Theme.colors
    val dimensions = Auth0Theme.dimensions
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange)
            // Without a tap of its own the box is only 24 dp, so the row asks for the usual 48 dp target.
            .minimumInteractiveComponentSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            // The row takes the tap, so the box and its words act as one control.
            onCheckedChange = null,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = colors.backgroundPrimary,
                uncheckedColor = colors.borderDefault,
                checkmarkColor = colors.textOnPrimary,
                disabledCheckedColor = colors.textDisabled,
                disabledUncheckedColor = colors.textDisabled,
            ),
        )
        Spacer(modifier = Modifier.width(dimensions.spacingSm))
        Text(text = label, style = Auth0Theme.typography.body, color = colors.textBold, modifier = Modifier.weight(1f))
    }
}

private const val COPIED_SHOWN_MILLIS = 1_500L

/**
 * Copies the new recovery code to the clipboard in the careful way: the system is told to keep it out of the clipboard
 * preview, and it is taken back after [LIFETIME_MILLIS], as iOS does.
 *
 * Unlike iOS, where the system removes the copy, the app does it here, so it only happens while the app's process is
 * alive. A later copy, by this app or another, is left alone when the system lets the app see it; when it doesn't (the
 * app is in the background), the clipboard is cleared all the same, since a secret left on it matters more.
 */
private object SecretClipboard {

    /** How long a copied secret stays on the clipboard. */
    const val LIFETIME_MILLIS = 2 * 60 * 1_000L

    /** The clip's extra that tells the system the text is a secret, and its preview should be left out. */
    private const val EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"

    /** Tells this app's own clip from one copied since. */
    private const val EXTRA_COPY_ID = "com.auth0.universalcomponents.extra.SECRET_COPY_ID"

    private val handler = Handler(Looper.getMainLooper())
    private var pendingTakeBack: Runnable? = null

    /** Copies [text] under [label], replacing any copy this app took before and its wait to be taken back. */
    fun copy(context: Context, label: String, text: String) {
        val clipboard = context.applicationContext.getSystemService(ClipboardManager::class.java) ?: return
        val copyId = UUID.randomUUID().toString()
        val clip = ClipData.newPlainText(label, text)
        clip.description.extras = PersistableBundle().apply {
            putBoolean(EXTRA_IS_SENSITIVE, true)
            putString(EXTRA_COPY_ID, copyId)
        }
        clipboard.setPrimaryClip(clip)

        pendingTakeBack?.let(handler::removeCallbacks)
        val takeBack = Runnable {
            pendingTakeBack = null
            val current = clipboard.primaryClipDescription
            // Nothing readable is cleared too: from the background the system shows an app no clipboard at all.
            if (current == null || current.extras?.getString(EXTRA_COPY_ID) == copyId) clipboard.clearPrimaryClip()
        }
        pendingTakeBack = takeBack
        handler.postDelayed(takeBack, LIFETIME_MILLIS)
    }
}
