package com.auth0.universalcomponents.feature.embedded.presentation

import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class CodeCooldownTest {

    private val clock = TestTimeSource()
    private val cooldown = CodeCooldown(clock)

    @Test
    fun `no code sent - no wait, and nothing counting`() {
        assertThat(cooldown.secondsLeft(LoginOption.EmailOtp, EMAIL)).isEqualTo(0)
        assertThat(cooldown.isRunning).isFalse()
    }

    @Test
    fun `a code just sent - the whole wait is left`() {
        cooldown.start(LoginOption.EmailOtp, EMAIL)

        assertThat(cooldown.secondsLeft(LoginOption.EmailOtp, EMAIL)).isEqualTo(60)
        assertThat(cooldown.isRunning).isTrue()
    }

    @Test
    fun `time passes - counts down in whole seconds, rounding up`() {
        cooldown.start(LoginOption.EmailOtp, EMAIL)

        clock += 1500.milliseconds
        assertThat(cooldown.secondsLeft(LoginOption.EmailOtp, EMAIL)).isEqualTo(59)

        clock += 58_499.milliseconds
        assertThat(cooldown.secondsLeft(LoginOption.EmailOtp, EMAIL)).isEqualTo(1)
    }

    @Test
    fun `the wait is over - zero left, and nothing counting`() {
        cooldown.start(LoginOption.EmailOtp, EMAIL)

        clock += 60.seconds

        assertThat(cooldown.secondsLeft(LoginOption.EmailOtp, EMAIL)).isEqualTo(0)
        assertThat(cooldown.isRunning).isFalse()
    }

    @Test
    fun `a long sleep - reads the time that really passed`() {
        cooldown.start(LoginOption.EmailOtp, EMAIL)

        clock += 45.seconds

        assertThat(cooldown.secondsLeft(LoginOption.EmailOtp, EMAIL)).isEqualTo(15)
    }

    @Test
    fun `someone else - has no wait of the last code's`() {
        cooldown.start(LoginOption.EmailOtp, EMAIL)

        assertThat(cooldown.secondsLeft(LoginOption.EmailOtp, "john@example.com")).isEqualTo(0)
        assertThat(cooldown.secondsLeft(LoginOption.PhoneOtp, EMAIL)).isEqualTo(0)
    }

    @Test
    fun `the code can't be used any more - another may be asked for at once`() {
        cooldown.start(LoginOption.EmailOtp, EMAIL)

        cooldown.end(LoginOption.EmailOtp, EMAIL)

        assertThat(cooldown.secondsLeft(LoginOption.EmailOtp, EMAIL)).isEqualTo(0)
        assertThat(cooldown.isRunning).isFalse()
    }

    @Test
    fun `ending the code of someone else - leaves the wait as it is`() {
        cooldown.start(LoginOption.EmailOtp, EMAIL)

        cooldown.end(LoginOption.EmailOtp, "john@example.com")

        assertThat(cooldown.secondsLeft(LoginOption.EmailOtp, EMAIL)).isEqualTo(60)
    }

    @Test
    fun `another code sent - the wait starts again`() {
        cooldown.start(LoginOption.EmailOtp, EMAIL)
        clock += 50.seconds

        cooldown.start(LoginOption.EmailOtp, EMAIL)

        assertThat(cooldown.secondsLeft(LoginOption.EmailOtp, EMAIL)).isEqualTo(60)
    }

    private companion object {
        const val EMAIL = "jane@example.com"
    }
}
