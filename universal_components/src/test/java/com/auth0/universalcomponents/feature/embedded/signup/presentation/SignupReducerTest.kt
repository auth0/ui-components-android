package com.auth0.universalcomponents.feature.embedded.signup.presentation

import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupChallenge
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupError
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupIdentifier
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupKind
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupNext
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupProgress
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.junit.Test

class SignupReducerTest {

    private val reducer = SignupReducer()

    // region start

    @Test
    fun `start - nothing has run - shows loading and opens the sign-up`() {
        val update = reducer.handleIntent(SignupState.Idle, SignupIntent.Start)

        assertThat(update.state).isEqualTo(SignupState.Loading)
        assertThat(update.commands).containsExactly(SignupCommand.Open)
    }

    @Test
    fun `start - already started - changes nothing, however far it got`() {
        listOf(SignupState.Loading, SignupState.Creating, entry(), code(), SignupState.Authenticated).forEach { state ->
            val update = reducer.handleIntent(state, SignupIntent.Start)

            assertThat(update.state).isEqualTo(state)
            assertThat(update.commands).isEmpty()
        }
    }

    @Test
    fun `start - after the sign-up was left - opens a new one`() {
        val left = reducer.handleIntent(code(), SignupIntent.Leave).state

        assertThat(reducer.handleIntent(left, SignupIntent.Start).commands).containsExactly(SignupCommand.Open)
    }

    // endregion

    // region opened

    @Test
    fun `opened - the email is asked for - shows the entry screen for it`() {
        val opened = SignupOutcome.Opened(SignupNext.AskFor(SignupKind.Email))

        val update = reducer.handleOutcome(SignupState.Loading, opened)

        assertThat(update.state).isEqualTo(entry())
        assertThat(update.commands).isEmpty()
    }

    @Test
    fun `opened - every identifier already verified - creates the account at once`() {
        val update = reducer.handleOutcome(SignupState.Loading, SignupOutcome.Opened(SignupNext.ReadyToCreate))

        assertThat(update.state).isEqualTo(SignupState.Creating)
        assertThat(update.commands).containsExactly(SignupCommand.CreateAccount)
    }

    @Test
    fun `opened - the server asks for nothing this flow can give - fails with only start over`() {
        val update = reducer.handleOutcome(SignupState.Loading, SignupOutcome.Opened(SignupNext.NothingToDo))

        val failed = update.state as SignupState.Failed
        assertThat(failed.error).isInstanceOf(SignupError.Unknown::class.java)
        assertThat(failed.retry).isNull()
    }

    @Test
    fun `opened - when nothing is opening - is dropped`() {
        listOf(SignupState.Idle, entry(), code()).forEach { state ->
            val update = reducer.handleOutcome(state, SignupOutcome.Opened(SignupNext.AskFor(SignupKind.Email)))

            assertThat(update.state).isEqualTo(state)
        }
    }

    @Test
    fun `open failed - the connection offers no sign-up - says it is unavailable`() {
        val update = reducer.handleOutcome(SignupState.Loading, SignupOutcome.OpenFailed(SignupError.Unavailable))

        assertThat(update.state).isEqualTo(SignupState.Unavailable)
    }

    @Test
    fun `open failed - the sign-up ended or there is no connection - offers only start over`() {
        listOf(SignupError.Denied, SignupError.Ended, SignupError.ConnectionMissing, SignupError.TooManyAttempts)
            .forEach { error ->
                val update = reducer.handleOutcome(SignupState.Loading, SignupOutcome.OpenFailed(error))

                assertThat(update.state).isEqualTo(SignupState.Failed(error, retry = null))
            }
    }

    @Test
    fun `open failed - the request never arrived - offers to try again by opening again`() {
        val update = reducer.handleOutcome(SignupState.Loading, SignupOutcome.OpenFailed(SignupError.Network))

        assertThat(update.state).isEqualTo(SignupState.Failed(SignupError.Network, SignupRetry.Reopen))
    }

    @Test
    fun `open failed - when nothing is opening - is dropped`() {
        val update = reducer.handleOutcome(entry(), SignupOutcome.OpenFailed(SignupError.Network))

        assertThat(update.state).isEqualTo(entry())
    }

    // endregion

    // region the email

    @Test
    fun `submit email - on the entry screen - spins Continue and identifies it`() {
        val update = reducer.handleIntent(entry(), SignupIntent.SubmitIdentifier(IDENTIFIER))

        assertThat(update.state).isEqualTo(entry(value = ADDRESS, isSubmitting = true))
        assertThat(update.commands).containsExactly(SignupCommand.Identify(IDENTIFIER))
    }

    @Test
    fun `submit email - clears the error that was showing`() {
        val update = reducer.handleIntent(
            entry(error = SignupError.InvalidEmail),
            SignupIntent.SubmitIdentifier(IDENTIFIER),
        )

        assertThat((update.state as SignupState.Entry).error).isNull()
    }

    @Test
    fun `submit email - while one is on its way, or on any other screen - is dropped`() {
        listOf(entry(isSubmitting = true), SignupState.Loading, code(), SignupState.Idle).forEach { state ->
            val update = reducer.handleIntent(state, SignupIntent.SubmitIdentifier(IDENTIFIER))

            assertThat(update.state).isEqualTo(state)
            assertThat(update.commands).isEmpty()
        }
    }

    @Test
    fun `identified - the code goes out at once, and Continue keeps spinning`() {
        val submitting = entry(value = ADDRESS, isSubmitting = true)

        val update = reducer.handleOutcome(submitting, SignupOutcome.Identified(IDENTIFIER, CHALLENGE))

        assertThat(update.state).isEqualTo(submitting)
        assertThat(update.commands).containsExactly(SignupCommand.SendCode(IDENTIFIER, CHALLENGE))
    }

    @Test
    fun `identified - when no email is being identified - is dropped`() {
        listOf(entry(), code(), SignupState.Idle).forEach { state ->
            val update = reducer.handleOutcome(state, SignupOutcome.Identified(IDENTIFIER, CHALLENGE))

            assertThat(update.state).isEqualTo(state)
            assertThat(update.commands).isEmpty()
        }
    }

    @Test
    fun `code sent - the first - shows the code screen and starts the wait`() {
        val update = reducer.handleOutcome(
            entry(value = ADDRESS, isSubmitting = true),
            SignupOutcome.CodeSent(IDENTIFIER, CHALLENGE),
        )

        assertThat(update.state).isEqualTo(SignupState.Code(IDENTIFIER, CHALLENGE))
        assertThat((update.state as SignupState.Code).resendSecondsLeft).isEqualTo(RESEND_WAIT_SECONDS)
        assertThat(update.commands).containsExactly(SignupCommand.CountDownResend)
    }

    @Test
    fun `code sent - a resend - stays on the code screen with the error gone and the wait started again`() {
        val resending = code(phase = CodePhase.Resending, error = SignupError.InvalidCode, secondsLeft = 0)

        val update = reducer.handleOutcome(resending, SignupOutcome.CodeSent(IDENTIFIER, CHALLENGE))

        assertThat(update.state).isEqualTo(code(secondsLeft = RESEND_WAIT_SECONDS))
        assertThat(update.commands).containsExactly(SignupCommand.CountDownResend)
    }

    @Test
    fun `code sent - when no code was being sent - is dropped`() {
        listOf(entry(), code(), SignupState.Loading, SignupState.Idle, SignupState.Authenticated).forEach { state ->
            val update = reducer.handleOutcome(state, SignupOutcome.CodeSent(IDENTIFIER, CHALLENGE))

            assertThat(update.state).isEqualTo(state)
            assertThat(update.commands).isEmpty()
        }
    }

    // endregion

    // region a request from the entry screen fails

    @Test
    fun `entry failed - a wrong email or no way to send a code - stays, with the message under the field`() {
        listOf(SignupError.InvalidEmail, SignupError.CodeNotSent).forEach { error ->
            val update = reducer.handleOutcome(
                entry(value = ADDRESS, isSubmitting = true),
                SignupOutcome.RequestFailed(error),
            )

            assertThat(update.state).isEqualTo(entry(value = ADDRESS, error = error))
        }
    }

    @Test
    fun `entry failed - the sign-up ended - offers only start over`() {
        val update = reducer.handleOutcome(
            entry(value = ADDRESS, isSubmitting = true),
            SignupOutcome.RequestFailed(SignupError.TooManyAttempts),
        )

        assertThat(update.state).isEqualTo(SignupState.Failed(SignupError.TooManyAttempts, retry = null))
    }

    @Test
    fun `entry failed - the request never arrived - try again returns to the entry with what was typed`() {
        val update = reducer.handleOutcome(
            entry(value = ADDRESS, isSubmitting = true),
            SignupOutcome.RequestFailed(SignupError.Network),
        )

        assertThat(update.state)
            .isEqualTo(SignupState.Failed(SignupError.Network, SignupRetry.ReturnTo(entry(value = ADDRESS))))
    }

    @Test
    fun `entry failed - when nothing is on its way - is dropped`() {
        listOf(entry(), SignupState.Idle, SignupState.Loading).forEach { state ->
            val update = reducer.handleOutcome(state, SignupOutcome.RequestFailed(SignupError.InvalidEmail))

            assertThat(update.state).isEqualTo(state)
        }
    }

    // endregion

    // region the code

    @Test
    fun `submit code - on the code screen - spins Verify and sends it`() {
        val update = reducer.handleIntent(code(error = SignupError.InvalidCode), SignupIntent.SubmitCode(CODE))

        assertThat(update.state).isEqualTo(code(phase = CodePhase.Verifying))
        assertThat(update.commands).containsExactly(SignupCommand.Verify(CODE))
    }

    @Test
    fun `submit code - blank, busy or on another screen - is dropped`() {
        listOf(
            code() to " ",
            code(phase = CodePhase.Verifying) to CODE,
            code(phase = CodePhase.Resending) to CODE,
            entry() to CODE,
            SignupState.Loading to CODE,
        ).forEach { (state, typed) ->
            val update = reducer.handleIntent(state, SignupIntent.SubmitCode(typed))

            assertThat(update.state).isEqualTo(state)
            assertThat(update.commands).isEmpty()
        }
    }

    @Test
    fun `verified - the email already had an account - signs in, once, with the credentials`() {
        val credentials = mockk<Credentials>()

        val update = reducer.handleOutcome(
            code(phase = CodePhase.Verifying),
            SignupOutcome.Verified(SignupProgress.SignedIn(credentials)),
        )

        assertThat(update.state).isEqualTo(SignupState.Authenticated)
        assertThat(update.effect).isEqualTo(SignupEffect.Authenticated(credentials))
    }

    @Test
    fun `verified - every required identifier verified - creates the account with no extra screen`() {
        val verifying = code(phase = CodePhase.Verifying)

        val update = reducer.handleOutcome(
            verifying,
            SignupOutcome.Verified(SignupProgress.Continue(SignupNext.ReadyToCreate)),
        )

        assertThat(update.state).isEqualTo(verifying)
        assertThat(update.commands).containsExactly(SignupCommand.CreateAccount)
    }

    @Test
    fun `verified - the email is asked for again - back to its entry, with what was typed`() {
        val update = reducer.handleOutcome(
            code(phase = CodePhase.Verifying),
            SignupOutcome.Verified(SignupProgress.Continue(SignupNext.AskFor(SignupKind.Email))),
        )

        assertThat(update.state).isEqualTo(entry(value = ADDRESS))
    }

    @Test
    fun `verified - the server asks for nothing - fails with only start over`() {
        val update = reducer.handleOutcome(
            code(phase = CodePhase.Verifying),
            SignupOutcome.Verified(SignupProgress.Continue(SignupNext.NothingToDo)),
        )

        assertThat((update.state as SignupState.Failed).retry).isNull()
    }

    @Test
    fun `verified - when no code is being checked - is dropped`() {
        listOf(code(), entry(), SignupState.Idle, SignupState.Loading).forEach { state ->
            val update = reducer.handleOutcome(
                state,
                SignupOutcome.Verified(SignupProgress.SignedIn(mockk<Credentials>())),
            )

            assertThat(update.state).isEqualTo(state)
            assertThat(update.effect).isNull()
        }
    }

    @Test
    fun `code failed - a wrong code - stays, with the message under the field`() {
        val update = reducer.handleOutcome(
            code(phase = CodePhase.Verifying),
            SignupOutcome.RequestFailed(SignupError.InvalidCode),
        )

        assertThat(update.state).isEqualTo(code(error = SignupError.InvalidCode))
    }

    @Test
    fun `code failed - too many wrong codes, or an expired email code - offers only start over`() {
        listOf(SignupError.TooManyWrongCodes, SignupError.CodeExpired, SignupError.TooManyCodesRequested)
            .forEach { error ->
                val update = reducer.handleOutcome(
                    code(phase = CodePhase.Verifying),
                    SignupOutcome.RequestFailed(error),
                )

                assertThat(update.state).isEqualTo(SignupState.Failed(error, retry = null))
            }
    }

    @Test
    fun `code failed - the request never arrived - try again returns to the code screen as it was`() {
        val update = reducer.handleOutcome(
            code(phase = CodePhase.Verifying, error = SignupError.InvalidCode),
            SignupOutcome.RequestFailed(SignupError.Network),
        )

        assertThat(update.state)
            .isEqualTo(SignupState.Failed(SignupError.Network, SignupRetry.ReturnTo(code())))
    }

    @Test
    fun `code failed - when nothing is on its way - is dropped`() {
        val update = reducer.handleOutcome(code(), SignupOutcome.RequestFailed(SignupError.InvalidCode))

        assertThat(update.state).isEqualTo(code())
    }

    // endregion

    // region resend

    @Test
    fun `resend - the wait is over - sends a new code, keeping the screen`() {
        val update = reducer.handleIntent(code(secondsLeft = 0, error = SignupError.InvalidCode), SignupIntent.Resend)

        assertThat(update.state).isEqualTo(code(phase = CodePhase.Resending, secondsLeft = 0))
        assertThat(update.commands).containsExactly(SignupCommand.SendCode(IDENTIFIER, CHALLENGE))
    }

    @Test
    fun `resend - the wait is not over, or something is on its way - is dropped`() {
        listOf(
            code(secondsLeft = 12),
            code(phase = CodePhase.Verifying, secondsLeft = 0),
            code(phase = CodePhase.Resending, secondsLeft = 0),
            entry(),
        ).forEach { state ->
            val update = reducer.handleIntent(state, SignupIntent.Resend)

            assertThat(update.state).isEqualTo(state)
            assertThat(update.commands).isEmpty()
        }
    }

    @Test
    fun `resend failed - a request that never arrived - try again returns to the code screen`() {
        val update = reducer.handleOutcome(
            code(phase = CodePhase.Resending, secondsLeft = 0),
            SignupOutcome.RequestFailed(SignupError.Network),
        )

        assertThat(update.state)
            .isEqualTo(SignupState.Failed(SignupError.Network, SignupRetry.ReturnTo(code(secondsLeft = 0))))
    }

    @Test
    fun `resend failed - too many codes requested - offers only start over`() {
        val update = reducer.handleOutcome(
            code(phase = CodePhase.Resending, secondsLeft = 0),
            SignupOutcome.RequestFailed(SignupError.TooManyCodesRequested),
        )

        assertThat(update.state).isEqualTo(SignupState.Failed(SignupError.TooManyCodesRequested))
    }

    // endregion

    // region the account

    @Test
    fun `account created - after the code - signs in, once, with the credentials`() {
        val credentials = mockk<Credentials>()

        val update = reducer.handleOutcome(
            code(phase = CodePhase.Verifying),
            SignupOutcome.AccountCreated(credentials),
        )

        assertThat(update.state).isEqualTo(SignupState.Authenticated)
        assertThat(update.effect).isEqualTo(SignupEffect.Authenticated(credentials))
    }

    @Test
    fun `account created - while creating it with no code screen - signs in`() {
        val credentials = mockk<Credentials>()

        val update = reducer.handleOutcome(SignupState.Creating, SignupOutcome.AccountCreated(credentials))

        assertThat(update.effect).isEqualTo(SignupEffect.Authenticated(credentials))
    }

    @Test
    fun `account created - when no account was being created - is dropped`() {
        listOf(SignupState.Idle, SignupState.Loading, entry(), code(), SignupState.Authenticated).forEach { state ->
            val update = reducer.handleOutcome(state, SignupOutcome.AccountCreated(mockk<Credentials>()))

            assertThat(update.state).isEqualTo(state)
            assertThat(update.effect).isNull()
        }
    }

    @Test
    fun `create failed - the request never arrived - try again creates it without asking for the code`() {
        val update = reducer.handleOutcome(
            code(phase = CodePhase.Verifying),
            SignupOutcome.CreateFailed(SignupError.Network),
        )

        assertThat(update.state).isEqualTo(SignupState.Failed(SignupError.Network, SignupRetry.CreateAccount))
    }

    @Test
    fun `create failed - refused, or the tenant wants more - offers only start over`() {
        listOf(SignupError.Denied, SignupError.MoreStepsRequired, SignupError.Ended).forEach { error ->
            val update = reducer.handleOutcome(SignupState.Creating, SignupOutcome.CreateFailed(error))

            assertThat(update.state).isEqualTo(SignupState.Failed(error, retry = null))
        }
    }

    @Test
    fun `create failed - when no account was being created - is dropped`() {
        listOf(entry(), SignupState.Loading).forEach { state ->
            val update = reducer.handleOutcome(state, SignupOutcome.CreateFailed(SignupError.Network))

            assertThat(update.state).isEqualTo(state)
        }
    }

    // endregion

    // region try again

    @Test
    fun `try again - to a screen - returns to it as it was`() {
        val update = reducer.handleIntent(
            SignupState.Failed(SignupError.Network, SignupRetry.ReturnTo(entry(value = ADDRESS))),
            SignupIntent.Retry,
        )

        assertThat(update.state).isEqualTo(entry(value = ADDRESS))
        assertThat(update.commands).isEmpty()
    }

    @Test
    fun `try again - to a code screen still waiting - goes on counting`() {
        val update = reducer.handleIntent(
            SignupState.Failed(SignupError.Network, SignupRetry.ReturnTo(code(secondsLeft = 30))),
            SignupIntent.Retry,
        )

        assertThat(update.state).isEqualTo(code(secondsLeft = 30))
        assertThat(update.commands).containsExactly(SignupCommand.CountDownResend)
    }

    @Test
    fun `try again - to creating the account - shows the busy spinner and creates it`() {
        val update = reducer.handleIntent(
            SignupState.Failed(SignupError.Network, SignupRetry.CreateAccount),
            SignupIntent.Retry,
        )

        assertThat(update.state).isEqualTo(SignupState.Creating)
        assertThat(update.commands).containsExactly(SignupCommand.CreateAccount)
    }

    @Test
    fun `try again - to opening again, or start over - shows loading and opens a new sign-up`() {
        listOf(
            SignupState.Failed(SignupError.Network, SignupRetry.Reopen),
            SignupState.Failed(SignupError.Ended, retry = null),
        ).forEach { failed ->
            val update = reducer.handleIntent(failed, SignupIntent.Retry)

            assertThat(update.state).isEqualTo(SignupState.Loading)
            assertThat(update.commands).containsExactly(SignupCommand.Open)
        }
    }

    @Test
    fun `try again - from any other screen - is dropped`() {
        listOf(entry(), code(), SignupState.Loading, SignupState.Creating, SignupState.Unavailable).forEach { state ->
            val update = reducer.handleIntent(state, SignupIntent.Retry)

            assertThat(update.state).isEqualTo(state)
            assertThat(update.commands).isEmpty()
        }
    }

    // endregion

    // region back

    @Test
    fun `back - on the code screen - returns to the entry with what was typed, sending nothing`() {
        val update = reducer.handleIntent(code(error = SignupError.InvalidCode), SignupIntent.Back)

        assertThat(update.state).isEqualTo(entry(value = ADDRESS))
        assertThat(update.commands).isEmpty()
        assertThat(update.effect).isNull()
    }

    @Test
    fun `back - while something is on its way - is ignored`() {
        listOf(
            entry(isSubmitting = true),
            code(phase = CodePhase.Verifying),
            code(phase = CodePhase.Resending),
            SignupState.Creating,
        ).forEach { state ->
            val update = reducer.handleIntent(state, SignupIntent.Back)

            assertThat(update.state).isEqualTo(state)
            assertThat(update.effect).isNull()
        }
    }

    @Test
    fun `back - while the account is created again after try again - is ignored, so its credentials aren't dropped`() {
        val failed = SignupState.Failed(SignupError.Network, SignupRetry.CreateAccount)
        val creating = reducer.handleIntent(failed, SignupIntent.Retry).state

        val update = reducer.handleIntent(creating, SignupIntent.Back)

        assertThat(update.state).isEqualTo(creating)
        assertThat(update.effect).isNull()
        assertThat(creating.isBusy).isTrue()
    }

    @Test
    fun `back - anywhere else - leaves`() {
        listOf(
            entry(),
            SignupState.Loading,
            SignupState.Unavailable,
            SignupState.Failed(SignupError.Ended),
            SignupState.Authenticated,
        ).forEach { state ->
            val update = reducer.handleIntent(state, SignupIntent.Back)

            assertThat(update.state).isEqualTo(state)
            assertThat(update.effect).isEqualTo(SignupEffect.Exit)
        }
    }

    // endregion

    // region leave, and the wait

    @Test
    fun `leave - from anywhere - goes back to the start and stops the work`() {
        listOf(
            entry(isSubmitting = true),
            code(),
            SignupState.Authenticated,
            SignupState.Loading,
            SignupState.Creating,
        ).forEach { state ->
            val update = reducer.handleIntent(state, SignupIntent.Leave)

            assertThat(update.state).isEqualTo(SignupState.Idle)
            assertThat(update.commands).containsExactly(SignupCommand.CancelWork)
        }
    }

    @Test
    fun `a second passes - on the code screen - counts down and asks for the next one`() {
        val update = reducer.handleSecondsPassed(code(secondsLeft = 10), seconds = 1)

        assertThat(update.state).isEqualTo(code(secondsLeft = 9))
        assertThat(update.commands).containsExactly(SignupCommand.CountDownResend)
    }

    @Test
    fun `seconds pass - the whole wait - opens resend and stops counting`() {
        val update = reducer.handleSecondsPassed(code(secondsLeft = 3), seconds = 5)

        assertThat(update.state).isEqualTo(code(secondsLeft = 0))
        assertThat(update.commands).isEmpty()
    }

    @Test
    fun `a second passes - once the code screen is left - nothing goes on`() {
        listOf(entry(), SignupState.Idle, SignupState.Failed(SignupError.Ended)).forEach { state ->
            val update = reducer.handleSecondsPassed(state, seconds = 1)

            assertThat(update.state).isEqualTo(state)
            assertThat(update.commands).isEmpty()
        }
    }

    // endregion

    @Test
    fun `a typed code and the credentials - are never written out`() {
        assertThat(SignupIntent.SubmitCode("123456").toString()).doesNotContain("123456")
        assertThat(SignupCommand.Verify("123456").toString()).doesNotContain("123456")
    }

    private fun entry(
        value: String = "",
        isSubmitting: Boolean = false,
        error: SignupError.Fixable? = null,
    ) = SignupState.Entry(SignupKind.Email, value, isSubmitting, error)

    private fun code(
        phase: CodePhase = CodePhase.Ready,
        error: SignupError.Fixable? = null,
        secondsLeft: Int = RESEND_WAIT_SECONDS,
    ) = SignupState.Code(IDENTIFIER, CHALLENGE, phase, error, secondsLeft)

    private companion object {
        const val ADDRESS = "jane@example.com"
        const val CODE = "123456"
        val IDENTIFIER = SignupIdentifier(SignupKind.Email, ADDRESS)
        val CHALLENGE = SignupChallenge(index = 1)
    }
}
