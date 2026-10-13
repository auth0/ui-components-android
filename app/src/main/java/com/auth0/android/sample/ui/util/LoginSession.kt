package com.auth0.android.sample.ui.util

import android.content.Context
import androidx.core.content.edit

/** How the current session was signed in, so logout can pick the right teardown. */
enum class LoginMethod { UNIVERSAL, EMBEDDED }

/** SharedPreferences record of the last login method. */
class LoginSession(context: Context) {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /** Null (unknown) is treated as Universal at logout. */
    val method: LoginMethod?
        get() = preferences.getString(KEY, null)?.let { saved -> LoginMethod.entries.find { it.name == saved } }

    fun record(method: LoginMethod) {
        preferences.edit { putString(KEY, method.name) }
    }

    fun clear() {
        preferences.edit { remove(KEY) }
    }

    private companion object {
        const val PREFERENCES_NAME = "com.auth0.uicomponents.sample"
        const val KEY = "com.auth0.uicomponents.lastLoginMethod"
    }
}
