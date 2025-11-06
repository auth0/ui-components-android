package com.auth0.android.ui_components.presentation.ui.mfa.authenticator_methods

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.auth0.android.ui_components.theme.sectionHeading2
import com.auth0.android.ui_components.theme.sectionHeading1


/**
 * Screen displaying the list of enabled authenticators
 */
@Composable
fun AuthenticatorListScreen(
    authenticatorMethodList: List<AuthenticatorUiData>,
    onAuthenticatorClick: (AuthenticatorUiData) -> Unit
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            key = { method -> method.type },
            items = authenticatorMethodList
        ) { authenticatorMethod ->
            AuthenticatorItem(
                title = authenticatorMethod.title,
                leadingIcon = getMFAMethodIcon(authenticatorMethod.type),
                showActiveTag = authenticatorMethod.confirmed,
                onClick = { onAuthenticatorClick(authenticatorMethod) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
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