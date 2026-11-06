package com.auth0.universalcomponents.feature.embedded.data.mapper

import com.auth0.android.embedded.authorize.NextAction
import com.auth0.android.embedded.authorize.OtpChannel
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption

/** Maps a server action to a sign-in method. A code that arrives as a voice call belongs to the phone method too. */
internal fun NextAction.toLoginOption(): LoginOption = when (this) {
    NextAction.IdentifyEmail -> LoginOption.EmailOtp
    NextAction.IdentifyPhone -> LoginOption.PhoneOtp
    is NextAction.VerifyOtp if channel == OtpChannel.EMAIL -> LoginOption.EmailOtp
    is NextAction.VerifyOtp if channel == OtpChannel.SMS || channel == OtpChannel.VOICE -> LoginOption.PhoneOtp
    else -> LoginOption.Unsupported(capability.value)
}
