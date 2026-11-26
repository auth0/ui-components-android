package com.auth0.universalcomponents.feature.embedded.signup.data.mapper

import com.auth0.android.embedded.EmbeddedAuthError
import com.auth0.android.embedded.EmbeddedAuthError.InsufficientAuthorization.Reason
import com.auth0.android.embedded.EmbeddedAuthException
import com.auth0.android.embedded.authorize.NextAction
import com.auth0.universalcomponents.data.embeddedauth.StepAnswer
import com.auth0.universalcomponents.data.embeddedauth.sdkException
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupError
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupFailure
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SignupErrorMapperTest {

    // region an answer that isn't finished yet

    @Test
    fun `a continuation with no reason and the description invalid_email - is a wrong email`() {
        assertThat(next(Reason.NONE, "invalid_email").toSignupFailure()?.error).isEqualTo(SignupError.InvalidEmail)
    }

    @Test
    fun `a continuation with no reason and the description invalid_phone - is a wrong phone number`() {
        assertThat(next(Reason.NONE, "invalid_phone").toSignupFailure()?.error).isEqualTo(SignupError.InvalidPhone)
    }

    @Test
    fun `invalid_phone with a reason of its own - is not taken for a wrong phone number`() {
        assertThat(next(Reason.SLOW_DOWN, "invalid_phone").toSignupFailure()).isNull()
    }

    @Test
    fun `a continuation with no reason and no description - is just the next menu`() {
        assertThat(next(Reason.NONE, null).toSignupFailure()).isNull()
        assertThat(next(Reason.NONE, "").toSignupFailure()).isNull()
    }

    @Test
    fun `a continuation with no reason and the SDK's own sentence - is just the next menu`() {
        val sentence = "An error occurred when trying to authenticate with the server."

        assertThat(next(Reason.NONE, sentence).toSignupFailure()).isNull()
    }

    @Test
    fun `invalid_email with a reason of its own - is not taken for a wrong email`() {
        assertThat(next(Reason.SLOW_DOWN, "invalid_email").toSignupFailure()).isNull()
    }

    @Test
    fun `a continuation saying the code was wrong - is an invalid code, whichever of the two reasons`() {
        assertThat(next(Reason.INVALID_CODE).toSignupFailure()?.error).isEqualTo(SignupError.InvalidCode)
        assertThat(next(Reason.INVALID_IDENTIFIER_OR_CODE).toSignupFailure()?.error).isEqualTo(SignupError.InvalidCode)
    }

    @Test
    fun `a continuation for another reason - is just the next menu`() {
        assertThat(next(Reason.AUTHORIZATION_PENDING).toSignupFailure()).isNull()
        assertThat(next(Reason.INVALID_IDENTIFIER_OR_PASSWORD).toSignupFailure()).isNull()
    }

    // endregion

    // region a failure

    @Test
    fun `too many attempts, or too many logins - are too many attempts`() {
        assertThat(failed(EmbeddedAuthError.TooManyAttempts).error).isEqualTo(SignupError.TooManyAttempts)
        assertThat(failed(EmbeddedAuthError.TooManyLogins).error).isEqualTo(SignupError.TooManyAttempts)
    }

    @Test
    fun `too many wrong codes - are too many wrong codes`() {
        assertThat(failed(EmbeddedAuthError.TooManyWrongOtpAttempts).error).isEqualTo(SignupError.TooManyWrongCodes)
    }

    @Test
    fun `an expired challenge - is an expired code`() {
        assertThat(failed(EmbeddedAuthError.ChallengeExpired).error).isEqualTo(SignupError.CodeExpired)
    }

    @Test
    fun `access denied with too_many_resend_attempts - is too many codes requested`() {
        val failure = failed(EmbeddedAuthError.AccessDenied, description = "too_many_resend_attempts")

        assertThat(failure.error).isEqualTo(SignupError.TooManyCodesRequested)
    }

    @Test
    fun `any other access denied - is denied`() {
        assertThat(failed(EmbeddedAuthError.AccessDenied).error).isEqualTo(SignupError.Denied)
        assertThat(failed(EmbeddedAuthError.AccessDenied, description = "pre_registration_blocked").error)
            .isEqualTo(SignupError.Denied)
    }

    @Test
    fun `a request that never arrived - is a network error, which the session survives`() {
        assertThat(failed(EmbeddedAuthError.Network).error).isEqualTo(SignupError.Network)
    }

    @Test
    fun `any other error - ends the sign-up`() {
        listOf(
            EmbeddedAuthError.InvalidRequest,
            EmbeddedAuthError.Unknown,
            EmbeddedAuthError.IpBlocked,
            EmbeddedAuthError.ConsentRequired,
            EmbeddedAuthError.PasswordLeaked,
            EmbeddedAuthError.NoNextSteps,
            EmbeddedAuthError.NoActiveSession,
            EmbeddedAuthError.SessionExpired,
            EmbeddedAuthError.NoEligibleFactors,
        ).forEach { assertThat(failed(it).error).isEqualTo(SignupError.Ended) }
    }

    @Test
    fun `a server error - can be tried again, because the SDK keeps its session`() {
        listOf(500, 503, 599).forEach { status ->
            val failure = failed(EmbeddedAuthError.Unknown, statusCode = status)

            assertThat(failure.error).isEqualTo(SignupError.ServerError)
            assertThat(failure.error).isInstanceOf(SignupError.Transient::class.java)
            assertThat(failure.cause).isInstanceOf(EmbeddedAuthException::class.java)
        }
    }

    @Test
    fun `a server error - wins over what the error would be named, since the session is kept either way`() {
        assertThat(failed(EmbeddedAuthError.AccessDenied, statusCode = 502).error).isEqualTo(SignupError.ServerError)
    }

    @Test
    fun `a status just outside 5xx - still ends the sign-up`() {
        assertThat(failed(EmbeddedAuthError.Unknown, statusCode = 499).error).isEqualTo(SignupError.Ended)
        assertThat(failed(EmbeddedAuthError.Unknown, statusCode = 600).error).isEqualTo(SignupError.Ended)
    }

    @Test
    fun `a failure - keeps the SDK's error as its cause`() {
        val failure = failed(EmbeddedAuthError.TooManyAttempts)

        assertThat(failure.cause).isInstanceOf(EmbeddedAuthException::class.java)
    }

    @Test
    fun `an exception that isn't the SDK's - is unknown and ends the sign-up, with its cause kept`() {
        val cause = IllegalStateException("broken")

        val failure = StepAnswer.Failed(cause).toSignupFailure()

        assertThat(failure.error).isEqualTo(SignupError.Unknown("broken"))
        assertThat(failure.error).isInstanceOf(SignupError.Fatal::class.java)
        assertThat(failure.cause).isSameInstanceAs(cause)
    }

    // endregion

    @Test
    fun `the groups - say what the user can do`() {
        assertThat(SignupError.InvalidEmail).isInstanceOf(SignupError.Fixable::class.java)
        assertThat(SignupError.InvalidPhone).isInstanceOf(SignupError.Fixable::class.java)
        assertThat(SignupError.InvalidCode).isInstanceOf(SignupError.Fixable::class.java)
        assertThat(SignupError.CodeNotSent).isInstanceOf(SignupError.Fixable::class.java)
        assertThat(SignupError.CodeExpired).isInstanceOf(SignupError.Fatal::class.java)
        assertThat(SignupError.Unavailable).isInstanceOf(SignupError.Fatal::class.java)
        assertThat(SignupError.Network).isInstanceOf(SignupError.Transient::class.java)
        assertThat(SignupError.ServerError).isInstanceOf(SignupError.Transient::class.java)
        assertThat(SignupError.Unknown("x")).isInstanceOf(SignupError.Fatal::class.java)
    }

    private fun next(reason: Reason, description: String? = null): StepAnswer.Next =
        StepAnswer.Next(listOf(NextAction.IdentifyEmail), reason, description)

    private fun failed(
        error: EmbeddedAuthError,
        description: String = "",
        statusCode: Int = 400,
    ): SignupFailure =
        StepAnswer.Failed(sdkException(error, description = description, statusCode = statusCode)).toSignupFailure()
}
