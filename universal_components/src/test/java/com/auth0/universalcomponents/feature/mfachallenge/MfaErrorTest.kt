package com.auth0.universalcomponents.feature.mfachallenge

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MfaErrorTest {

    @Test
    fun `cause - failure behind the error - is kept for an app that wants the details`() {
        val sdkError = IllegalStateException("sdk")

        val errors = listOf(
            MfaError.TokenExpired(sdkError),
            MfaError.TokenInvalid(sdkError),
            MfaError.Rejected(sdkError),
            MfaError.TooManyAttempts(sdkError),
            MfaError.NoEligibleFactors(sdkError),
            MfaError.Network(sdkError),
            MfaError.Unexpected("Your account is blocked.", sdkError),
        )

        errors.forEach { assertThat(it.cause).isSameInstanceAs(sdkError) }
    }

    @Test
    fun `cause - errors MFA decides on its own - has none`() {
        assertThat(MfaError.Cancelled().cause).isNull()
        assertThat(MfaError.PresentationUnavailable().cause).isNull()
        assertThat(MfaError.NoEligibleFactors().cause).isNull()
    }

    @Test
    fun `toString - any error - names the case only, never the server's words or the cause`() {
        val error = MfaError.Unexpected("Your account is blocked.", IllegalStateException("device Jane's phone"))

        assertThat(error.toString()).isEqualTo("Unexpected")
        assertThat(MfaError.NoEligibleFactors().toString()).isEqualTo("NoEligibleFactors")
    }

    @Test
    fun `toString - every case - is its own name, written out so R8 can't shorten it`() {
        val names = listOf(
            MfaError.Cancelled() to "Cancelled",
            MfaError.TokenExpired() to "TokenExpired",
            MfaError.TokenInvalid() to "TokenInvalid",
            MfaError.Rejected() to "Rejected",
            MfaError.TooManyAttempts() to "TooManyAttempts",
            MfaError.NoEligibleFactors() to "NoEligibleFactors",
            MfaError.PresentationUnavailable() to "PresentationUnavailable",
            MfaError.Network() to "Network",
            MfaError.Unexpected() to "Unexpected",
        )

        names.forEach { (error, name) -> assertThat(error.toString()).isEqualTo(name) }
    }

    @Test
    fun `equality - two errors of one case - are different, as exceptions are`() {
        assertThat(MfaError.Rejected()).isNotEqualTo(MfaError.Rejected())
    }

    @Test
    fun `serverMessage - unexpected error - carries the server's own explanation`() {
        assertThat(MfaError.Unexpected("Your account is blocked.").serverMessage).isEqualTo("Your account is blocked.")
        assertThat(MfaError.Unexpected().serverMessage).isNull()
    }

    @Test
    fun `error - any case - is an exception an app can catch or log`() {
        val error: Exception = MfaError.Network()

        assertThat(error).isInstanceOf(MfaError::class.java)
    }
}
