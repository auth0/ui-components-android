package com.auth0.android.ui_components.utils

import android.app.Activity
import androidx.credentials.CredentialManager


fun getCredentialManager(
    activity: Activity
): CredentialManager {
    return CredentialManager.create(activity)
}

