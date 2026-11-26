package com.auth0.universalcomponents.feature.embedded.signup.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.auth0.android.embedded.authorize.PhoneDeliveryMethod
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginChoiceButton
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginFieldLabel
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginInlineError
import com.auth0.universalcomponents.presentation.ui.flow.FlowPrimaryButton
import com.auth0.universalcomponents.presentation.ui.flow.FlowScreenHeader
import com.auth0.universalcomponents.presentation.ui.flow.FlowStepLayout
import com.auth0.universalcomponents.theme.Auth0Theme

/**
 * Asked once a phone number is accepted and takes both a text and a call: how does the user want the code? The first of
 * [ways] is chosen to begin with; [onContinue] gets the one chosen. [sentTo] is the number as the server hid it, or what
 * was typed, and [sentToFontFeatures] sets how it is drawn. [errorMessage] sits under the choices. While [isLoading]
 * the code is on its way: the choices and Edit are off and Continue spins. A null [onEdit] hides Edit.
 */
@Composable
internal fun SignupDeliveryScreen(
    sentTo: String,
    ways: List<PhoneDeliveryMethod>,
    onContinue: (PhoneDeliveryMethod) -> Unit,
    modifier: Modifier = Modifier,
    sentToFontFeatures: String? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onEdit: (() -> Unit)? = null,
) {
    var chosen by rememberSaveable { mutableStateOf(ways.firstOrNull() ?: PhoneDeliveryMethod.TEXT) }
    val question = stringResource(R.string.embedded_signup_delivery_label)

    FlowStepLayout(
        modifier = modifier,
        pageDescription = {
            FlowScreenHeader(
                title = stringResource(R.string.embedded_signup_delivery_title),
                subtitle = stringResource(R.string.embedded_signup_delivery_subtitle, sentTo),
                subtitleFontFeatures = sentToFontFeatures,
                editLabel = stringResource(R.string.embedded_login_code_edit),
                onEdit = onEdit,
                isEditEnabled = !isLoading,
            )
        },
        content = {
            LoginFieldLabel(question)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .selectableGroup()
                    // The question above is plain text: on the group, a screen reader says what the choices answer.
                    .semantics { contentDescription = question },
                horizontalArrangement = Arrangement.spacedBy(Auth0Theme.dimensions.spacingMd),
            ) {
                ways.forEach { way ->
                    LoginChoiceButton(
                        title = stringResource(way.titleRes),
                        isSelected = way == chosen,
                        onClick = { chosen = way },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        enabled = !isLoading,
                    )
                }
            }
            errorMessage?.let { LoginInlineError(it) }
        },
        primaryAction = {
            FlowPrimaryButton(
                title = stringResource(R.string.embedded_login_continue),
                onClick = { onContinue(chosen) },
                isLoading = isLoading,
                enabled = !isLoading,
            )
        },
    )
}

/** The choice's name on its button. */
private val PhoneDeliveryMethod.titleRes: Int
    get() = when (this) {
        PhoneDeliveryMethod.TEXT -> R.string.embedded_signup_delivery_text
        PhoneDeliveryMethod.VOICE -> R.string.embedded_signup_delivery_voice
    }
