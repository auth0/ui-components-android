package com.auth0.universalcomponents.feature.embedded.data.mapper

import com.auth0.android.embedded.authorize.NextAction
import com.auth0.universalcomponents.data.embeddedauth.sdkInstance
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NextActionMapperTest {

    @Test
    fun `the server offers sign-up - it is the sign-up option`() {
        val signup = sdkInstance<NextAction.Signup>(List::class.java to emptyList<Any>())

        assertThat(signup.toLoginOption()).isEqualTo(LoginOption.Signup)
    }

    @Test
    fun `the first factors - are still what they were`() {
        assertThat(NextAction.IdentifyEmail.toLoginOption()).isEqualTo(LoginOption.EmailOtp)
        assertThat(NextAction.IdentifyPhone.toLoginOption()).isEqualTo(LoginOption.PhoneOtp)
        assertThat(NextAction.IdentifyUsername.toLoginOption()).isEqualTo(LoginOption.Username)
    }

    @Test
    fun `the step that creates the account - is not a way to sign in`() {
        assertThat(NextAction.ConfirmSignup.toLoginOption()).isInstanceOf(LoginOption.Unsupported::class.java)
    }
}
