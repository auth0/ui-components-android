package com.auth0.universalcomponents.feature.embedded.data.mapper

import com.auth0.android.embedded.authorize.NextAction
import com.auth0.android.embedded.authorize.OtpChannel
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption

/** Maps a server action to a sign-in method. */
internal fun NextAction.toLoginOption(): LoginOption = when (this) {
    NextAction.IdentifyEmail -> LoginOption.EmailOtp
    is NextAction.VerifyOtp if channel == OtpChannel.EMAIL -> LoginOption.EmailOtp
    else -> LoginOption.Unsupported(capability.value)
}
