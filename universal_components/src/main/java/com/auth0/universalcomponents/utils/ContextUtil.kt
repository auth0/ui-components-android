package com.auth0.universalcomponents.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** The activity this context belongs to, however many wrappers are around it; null when there is none. */
internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
