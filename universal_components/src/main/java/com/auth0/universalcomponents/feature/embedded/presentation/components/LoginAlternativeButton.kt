package com.auth0.universalcomponents.feature.embedded.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption
import com.auth0.universalcomponents.presentation.ui.components.CircularLoader
import com.auth0.universalcomponents.presentation.ui.flow.flowSurface
import com.auth0.universalcomponents.theme.Auth0Theme

private const val DISABLED_ALPHA = 0.4f

/** The "OR" between a screen's own way and the others. It only separates, so a screen reader skips it. */
@Composable
internal fun LoginMethodDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clearAndSetSemantics { },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Auth0Theme.dimensions.spacingSm),
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Auth0Theme.colors.borderSubtle)
        Text(
            text = stringResource(R.string.embedded_login_or),
            style = Auth0Theme.typography.bodySmall,
            color = Auth0Theme.colors.textDefault,
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Auth0Theme.colors.borderSubtle)
    }
}

/**
 * The white button for another way to sign in. While [isLoading] it shows a spinner in place of its words, and it is
 * off, so only the button that was tapped answers the request on its way.
 */
@Composable
internal fun LoginAlternativeButton(
    title: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val dimensions = Auth0Theme.dimensions
    val shape = Auth0Theme.shapes.medium
    val color = Auth0Theme.colors.textBold
    Box(
        modifier = modifier
            .fillMaxWidth()
            // Off, the whole button fades, border and all; while it spins it is the button that was tapped, so it stays.
            .alpha(if (enabled || isLoading) 1f else DISABLED_ALPHA)
            .height(Auth0Theme.sizes.buttonHeight)
            .flowSurface(shape, isFocused = false, isError = false)
            .clip(shape)
            .clickable(enabled = enabled && !isLoading, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularLoader(modifier = Modifier.size(16.dp), color = color, strokeWidth = 2.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(dimensions.spacingMd),
                    tint = color,
                )
                Spacer(Modifier.width(dimensions.spacingSm))
                Text(text = title, style = Auth0Theme.typography.label, color = color)
            }
        }
    }
}

/**
 * "OR", then one button for each of [methods], in the order given. Every button is off while a request is on its way
 * ([enabled] is false), and a tap on one opens that method's own screen.
 */
@Composable
internal fun LoginAlternativeMethods(
    methods: List<LoginOption>,
    onChoose: (LoginOption) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val spacing = Auth0Theme.dimensions.spacingSm
    Column(modifier = modifier) {
        LoginMethodDivider()
        methods.forEach { method ->
            Spacer(Modifier.height(spacing))
            LoginAlternativeButton(
                title = stringResource(method.continueWithTitle),
                icon = method.continueWithIcon,
                onClick = { onChoose(method) },
                enabled = enabled,
            )
        }
    }
}

/** What "Continue with …" says for this method, on the first screen. */
private val LoginOption.continueWithTitle: Int
    get() = when (this) {
        LoginOption.PhoneOtp -> R.string.embedded_login_continue_with_phone
        LoginOption.Username, LoginOption.Password -> R.string.embedded_login_continue_with_password
        else -> R.string.embedded_login_continue_with_email
    }

/** That button's icon: the envelope, the phone, or the masked password. */
@get:DrawableRes
internal val LoginOption.continueWithIcon: Int
    get() = when (this) {
        LoginOption.PhoneOtp -> R.drawable.ic_device
        LoginOption.Username, LoginOption.Password -> R.drawable.ic_password
        else -> R.drawable.ic_email
    }
