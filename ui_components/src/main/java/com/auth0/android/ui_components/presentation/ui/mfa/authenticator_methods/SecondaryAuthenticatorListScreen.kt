package com.auth0.android.ui_components.presentation.ui.mfa.authenticator_methods

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.auth0.android.ui_components.R
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.presentation.ui.components.EmptyAuthenticatorItem
import com.auth0.android.ui_components.presentation.viewmodel.AuthenticatorUiData
import com.auth0.android.ui_components.theme.sectionHeading1
import com.auth0.android.ui_components.theme.sectionHeading2


/**
 * Screen displaying the list of enabled authenticators
 */
@Composable
fun SecondaryAuthenticatorListScreen(
    authenticatorMethodList: List<AuthenticatorUiData>,
    onAuthenticatorItemClick: (AuthenticatorUiData) -> Unit
) {
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.verification_methods),
        modifier = Modifier.height(24.dp),
        style = sectionHeading1,
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = stringResource(R.string.manage_2fa_methods),
        modifier = Modifier.height(17.dp),
        style = sectionHeading2,
    )

    Spacer(modifier = Modifier.height(24.dp))

    if (authenticatorMethodList.isEmpty()) {
        EmptyAuthenticatorItem(emptyMessage = stringResource(R.string.no_authenticators_enabled))
        return
    }

    for (item in authenticatorMethodList) {
        AuthenticatorItem(
            title = item.title,
            leadingIcon = getMFAMethodIcon(item.type),
            showActiveTag = item.confirmed,
            onClick = { onAuthenticatorItemClick(item) }
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun getMFAMethodIcon(authenticatorType: AuthenticatorType): Painter {
    return painterResource(
        when (authenticatorType) {
            AuthenticatorType.TOTP -> R.drawable.ic_authenticator
            AuthenticatorType.PHONE -> R.drawable.ic_sms_otp
            AuthenticatorType.EMAIL -> R.drawable.ic_email
            AuthenticatorType.PUSH -> R.drawable.ic_authenticator
            AuthenticatorType.RECOVERY_CODE -> R.drawable.ic_recovery_code
        }
    )
}