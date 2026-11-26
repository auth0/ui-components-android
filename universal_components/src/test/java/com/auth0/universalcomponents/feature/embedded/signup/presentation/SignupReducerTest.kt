package com.auth0.universalcomponents.feature.embedded.signup.presentation

import com.auth0.android.embedded.authorize.PhoneDeliveryMethod
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
        assertThat(update.commands).containsExactly(SignupCommand.SendCode(IDENTIFIER, CHALLENGE, delivery = null))
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
            SignupOutcome.CodeSent(IDENTIFIER, CHALLENGE, delivery = null),
        )

        assertThat(update.state).isEqualTo(SignupState.Code(IDENTIFIER, CHALLENGE, delivery = null))
        assertThat((update.state as SignupState.Code).resendSecondsLeft).isEqualTo(RESEND_WAIT_SECONDS)
        assertThat(update.commands).containsExactly(SignupCommand.CountDownResend)
    }

    @Test
    fun `code sent - a resend - stays on the code screen with the error gone and the wait started again`() {
        val resending = code(phase = CodePhase.Resending, error = SignupError.InvalidCode, secondsLeft = 0)

        val update = reducer.handleOutcome(resending, SignupOutcome.CodeSent(IDENTIFIER, CHALLENGE, delivery = null))

        assertThat(update.state).isEqualTo(code(secondsLeft = RESEND_WAIT_SECONDS))
        assertThat(update.commands).containsExactly(SignupCommand.CountDownResend)
    }

    @Test
    fun `code sent - when no code was being sent - is dropped`() {
        listOf(entry(), code(), SignupState.Loading, SignupState.Idle, SignupState.Authenticated).forEach { state ->
            val update = reducer.handleOutcome(state, SignupOutcome.CodeSent(IDENTIFIER, CHALLENGE, delivery = null))

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
        assertThat(update.commands).containsExactly(SignupCommand.SendCode(IDENTIFIER, CHALLENGE, delivery = null))
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
    fun `a second passes - on the error screen over the code screen - the wait goes on under it`() {
        val failed = SignupState.Failed(SignupError.Network, SignupRetry.ReturnTo(code(secondsLeft = 10)))

        val update = reducer.handleSecondsPassed(failed, seconds = 1)

        assertThat(update.state)
            .isEqualTo(SignupState.Failed(SignupError.Network, SignupRetry.ReturnTo(code(secondsLeft = 9))))
        assertThat(update.commands).containsExactly(SignupCommand.CountDownResend)
    }

    @Test
    fun `seconds pass - on the error screen, past the wait - try again returns to a code screen that can resend`() {
        val failed = SignupState.Failed(SignupError.Network, SignupRetry.ReturnTo(code(secondsLeft = 3)))

        val counted = reducer.handleSecondsPassed(failed, seconds = 30)
        val returned = reducer.handleIntent(counted.state, SignupIntent.Retry)

        assertThat(counted.commands).isEmpty()
        assertThat(returned.state).isEqualTo(code(secondsLeft = 0))
        assertThat(returned.commands).isEmpty()
    }

    @Test
    fun `a second passes - once the code screen is left - nothing goes on`() {
        listOf(
            entry(),
            SignupState.Idle,
            SignupState.Failed(SignupError.Ended),
            SignupState.Failed(SignupError.Network, SignupRetry.CreateAccount),
            SignupState.Failed(SignupError.Network, SignupRetry.ReturnTo(entry(value = ADDRESS))),
        ).forEach { state ->
            val update = reducer.handleSecondsPassed(state, seconds = 1)

            assertThat(update.state).isEqualTo(state)
            assertThat(update.commands).isEmpty()
        }
    }

    // endregion

    // region phone: how to get the code

    @Test
    fun `identified - a number that takes a text and a call - asks how, and sends nothing yet`() {
        val update = reducer.handleOutcome(
            phoneEntry(isSubmitting = true),
            SignupOutcome.Identified(PHONE, BOTH_WAYS),
        )

        assertThat(update.state).isEqualTo(SignupState.Delivery(PHONE, BOTH_WAYS))
        assertThat(update.commands).isEmpty()
    }

    @Test
    fun `identified - a number that only takes texts - is not asked how, and the code goes at once`() {
        val textOnly = SignupChallenge(1, MASKED_NUMBER, listOf(PhoneDeliveryMethod.TEXT))
        val submitting = phoneEntry(isSubmitting = true)

        val update = reducer.handleOutcome(submitting, SignupOutcome.Identified(PHONE, textOnly))

        assertThat(update.state).isEqualTo(submitting)
        assertThat(update.commands)
            .containsExactly(SignupCommand.SendCode(PHONE, textOnly, PhoneDeliveryMethod.TEXT))
    }

    @Test
    fun `identified - a number that only takes calls - is called without being asked`() {
        val callsOnly = SignupChallenge(1, MASKED_NUMBER, listOf(PhoneDeliveryMethod.VOICE))

        val update = reducer.handleOutcome(phoneEntry(isSubmitting = true), SignupOutcome.Identified(PHONE, callsOnly))

        assertThat(update.commands)
            .containsExactly(SignupCommand.SendCode(PHONE, callsOnly, PhoneDeliveryMethod.VOICE))
    }

    @Test
    fun `identified - a phone that was not being identified - is dropped`() {
        listOf(phoneEntry(), entry(isSubmitting = true), delivery(), SignupState.Idle).forEach { state ->
            val update = reducer.handleOutcome(state, SignupOutcome.Identified(PHONE, BOTH_WAYS))

            assertThat(update.state).isEqualTo(state)
            assertThat(update.commands).isEmpty()
        }
    }

    @Test
    fun `choose delivery - a way the number takes - sends the code that way, with Continue spinning`() {
        val update = reducer.handleIntent(delivery(), SignupIntent.ChooseDelivery(PhoneDeliveryMethod.VOICE))

        assertThat(update.state).isEqualTo(delivery(isSubmitting = true))
        assertThat(update.commands)
            .containsExactly(SignupCommand.SendCode(PHONE, BOTH_WAYS, PhoneDeliveryMethod.VOICE))
    }

    @Test
    fun `choose delivery - the error from an earlier try - is cleared`() {
        val failed = delivery(error = SignupError.CodeNotSent)

        val update = reducer.handleIntent(failed, SignupIntent.ChooseDelivery(PhoneDeliveryMethod.TEXT))

        assertThat(update.state).isEqualTo(delivery(isSubmitting = true))
    }

    @Test
    fun `choose delivery - a way the number doesn't take, another screen, or while sending - is dropped`() {
        val textOnly = delivery(SignupChallenge(1, MASKED_NUMBER, listOf(PhoneDeliveryMethod.TEXT)))
        listOf(
            textOnly to PhoneDeliveryMethod.VOICE,
            phoneEntry() to PhoneDeliveryMethod.TEXT,
            phoneCode() to PhoneDeliveryMethod.TEXT,
            delivery(isSubmitting = true) to PhoneDeliveryMethod.TEXT,
            SignupState.Idle to PhoneDeliveryMethod.TEXT,
        ).forEach { (state, way) ->
            val update = reducer.handleIntent(state, SignupIntent.ChooseDelivery(way))

            assertThat(update.state).isEqualTo(state)
            assertThat(update.commands).isEmpty()
        }
    }

    @Test
    fun `code sent - the way the user chose - shows the code screen for it and starts the wait`() {
        val sending = delivery(isSubmitting = true)

        val update = reducer.handleOutcome(
            sending,
            SignupOutcome.CodeSent(PHONE, BOTH_WAYS, PhoneDeliveryMethod.VOICE),
        )

        assertThat(update.state).isEqualTo(phoneCode(delivery = PhoneDeliveryMethod.VOICE))
        assertThat(update.commands).containsExactly(SignupCommand.CountDownResend)
    }

    @Test
    fun `code sent - at once for a number with one way - shows the code screen for that way`() {
        val update = reducer.handleOutcome(
            phoneEntry(isSubmitting = true),
            SignupOutcome.CodeSent(PHONE, BOTH_WAYS, PhoneDeliveryMethod.TEXT),
        )

        assertThat(update.state).isEqualTo(phoneCode(delivery = PhoneDeliveryMethod.TEXT))
    }

    @Test
    fun `code sent - when the choice was not sending - is dropped`() {
        listOf(delivery(), phoneEntry(), SignupState.Idle).forEach { state ->
            val sent = SignupOutcome.CodeSent(PHONE, BOTH_WAYS, PhoneDeliveryMethod.TEXT)

            val update = reducer.handleOutcome(state, sent)

            assertThat(update.state).isEqualTo(state)
            assertThat(update.commands).isEmpty()
        }
    }

    @Test
    fun `back - from the choice - returns to the entry with the number, sending nothing`() {
        val update = reducer.handleIntent(delivery(), SignupIntent.Back)

        assertThat(update.state).isEqualTo(phoneEntry(value = NUMBER))
        assertThat(update.commands).isEmpty()
        assertThat(update.effect).isNull()
    }

    @Test
    fun `back - from the phone's code screen - returns to the entry with the number`() {
        val update = reducer.handleIntent(phoneCode(), SignupIntent.Back)

        assertThat(update.state).isEqualTo(phoneEntry(value = NUMBER))
    }

    @Test
    fun `back - while the code is being sent from the choice - is ignored`() {
        val sending = delivery(isSubmitting = true)

        val update = reducer.handleIntent(sending, SignupIntent.Back)

        assertThat(update.state).isEqualTo(sending)
        assertThat(update.effect).isNull()
    }

    @Test
    fun `request failed - the choice can't be sent - stays with the message under the choices`() {
        val update = reducer.handleOutcome(
            delivery(isSubmitting = true),
            SignupOutcome.RequestFailed(SignupError.CodeNotSent),
        )

        assertThat(update.state).isEqualTo(delivery(error = SignupError.CodeNotSent))
    }

    @Test
    fun `request failed - the connection drops sending from the choice - try again returns to the choice`() {
        val failed = reducer.handleOutcome(
            delivery(isSubmitting = true),
            SignupOutcome.RequestFailed(SignupError.Network),
        )

        assertThat(failed.state).isEqualTo(
            SignupState.Failed(SignupError.Network, SignupRetry.ReturnTo(delivery()), SignupKind.Phone),
        )
        assertThat(reducer.handleIntent(failed.state, SignupIntent.Retry).state).isEqualTo(delivery())
    }

    @Test
    fun `request failed - a number the server calls invalid - stays on the entry with the message`() {
        val update = reducer.handleOutcome(
            phoneEntry(value = NUMBER, isSubmitting = true),
            SignupOutcome.RequestFailed(SignupError.InvalidPhone),
        )

        assertThat(update.state).isEqualTo(phoneEntry(value = NUMBER, error = SignupError.InvalidPhone))
    }

    @Test
    fun `request failed - the sign-up ends from a phone screen - the error screen uses the phone's words`() {
        val update = reducer.handleOutcome(
            phoneEntry(isSubmitting = true),
            SignupOutcome.RequestFailed(SignupError.TooManyAttempts),
        )

        assertThat(update.state).isEqualTo(SignupState.Failed(SignupError.TooManyAttempts, kind = SignupKind.Phone))
    }

    @Test
    fun `verified - a phone verified and the email still wanted - asks for the email, not keeping the number`() {
        val verifying = phoneCode(phase = CodePhase.Verifying)

        val update = reducer.handleOutcome(
            verifying,
            SignupOutcome.Verified(SignupProgress.Continue(SignupNext.AskFor(SignupKind.Email))),
        )

        assertThat(update.state).isEqualTo(SignupState.Entry(SignupKind.Email))
    }

    // endregion

    // region phone: a code that timed out

    @Test
    fun `request failed - a phone's code expired on verify - stays, with Verify off and Resend open at once`() {
        val update = reducer.handleOutcome(
            phoneCode(phase = CodePhase.Verifying),
            SignupOutcome.RequestFailed(SignupError.CodeExpired),
        )

        assertThat(update.state).isEqualTo(phoneCode(isExpired = true, secondsLeft = 0))
        assertThat(update.commands).isEmpty()
    }

    @Test
    fun `request failed - an email's code expired - ends the sign-up, as on iOS`() {
        val update = reducer.handleOutcome(
            code(phase = CodePhase.Verifying),
            SignupOutcome.RequestFailed(SignupError.CodeExpired),
        )

        assertThat(update.state).isEqualTo(SignupState.Failed(SignupError.CodeExpired))
    }

    @Test
    fun `submit code - while the code is expired - is dropped`() {
        val expired = phoneCode(isExpired = true, secondsLeft = 0)

        val update = reducer.handleIntent(expired, SignupIntent.SubmitCode(CODE))

        assertThat(update.state).isEqualTo(expired)
        assertThat(update.commands).isEmpty()
    }

    @Test
    fun `resend - after the code expired - starts the sign-up over for the same number, the way the user chose`() {
        val expired = phoneCode(delivery = PhoneDeliveryMethod.VOICE, isExpired = true, secondsLeft = 0)
        val expiredAndResending = expired.copy(phase = CodePhase.Resending)

        val update = reducer.handleIntent(expired, SignupIntent.Resend)

        assertThat(update.state).isEqualTo(expiredAndResending)
        assertThat(update.commands)
            .containsExactly(SignupCommand.ReplaceExpiredCode(PHONE, PhoneDeliveryMethod.VOICE))
    }

    @Test
    fun `code sent - a replacement for the expired code - is a new challenge, no longer expired, with the wait started`() {
        val fresh = SignupChallenge(7, MASKED_NUMBER, listOf(PhoneDeliveryMethod.TEXT, PhoneDeliveryMethod.VOICE))
        val resending = phoneCode(delivery = PhoneDeliveryMethod.VOICE, isExpired = true, secondsLeft = 0)
            .copy(phase = CodePhase.Resending)

        val update = reducer.handleOutcome(resending, SignupOutcome.CodeSent(PHONE, fresh, PhoneDeliveryMethod.VOICE))

        assertThat(update.state).isEqualTo(
            SignupState.Code(PHONE, fresh, PhoneDeliveryMethod.VOICE, resendSecondsLeft = RESEND_WAIT_SECONDS),
        )
        assertThat(update.commands).containsExactly(SignupCommand.CountDownResend)
    }

    @Test
    fun `request failed - the replacement can't be made - try again returns to the expired code screen`() {
        val resending = phoneCode(phase = CodePhase.Resending, isExpired = true, secondsLeft = 0)

        val failed = reducer.handleOutcome(resending, SignupOutcome.RequestFailed(SignupError.Network))

        val expired = phoneCode(isExpired = true, secondsLeft = 0)
        assertThat(failed.state)
            .isEqualTo(SignupState.Failed(SignupError.Network, SignupRetry.ReturnTo(expired), SignupKind.Phone))
        assertThat(reducer.handleIntent(failed.state, SignupIntent.Retry).state).isEqualTo(expired)
    }

    @Test
    fun `seconds pass - on an expired code, or the error screen over it - nothing counts down`() {
        val expired = phoneCode(isExpired = true, secondsLeft = 0)
        val failed = SignupState.Failed(SignupError.Network, SignupRetry.ReturnTo(expired), SignupKind.Phone)

        listOf(expired, failed).forEach { state ->
            val update = reducer.handleSecondsPassed(state, seconds = 1)

            assertThat(update.state).isEqualTo(state)
            assertThat(update.commands).isEmpty()
        }
    }

    @Test
    fun `request failed - the sign-up ends from the choice - the error screen uses the phone's words`() {
        val update = reducer.handleOutcome(
            delivery(isSubmitting = true),
            SignupOutcome.RequestFailed(SignupError.TooManyAttempts),
        )

        assertThat(update.state).isEqualTo(SignupState.Failed(SignupError.TooManyAttempts, kind = SignupKind.Phone))
    }

    @Test
    fun `shown error - an expired code says so, until a new one is on its way`() {
        val expired = phoneCode(isExpired = true, secondsLeft = 0)

        assertThat(expired.shownError).isEqualTo(SignupError.CodeExpired)
        assertThat(expired.copy(phase = CodePhase.Resending).shownError).isNull()
        assertThat(expired.copy(error = SignupError.CodeNotSent).shownError).isEqualTo(SignupError.CodeNotSent)
        assertThat(phoneCode().shownError).isNull()
    }

    @Test
    fun `shown for - a number the server hid - is drawn with every hidden digit alike, else what was typed`() {
        assertThat(BOTH_WAYS.shownFor(PHONE)).isEqualTo(MASKED_NUMBER)
        assertThat(SignupChallenge(1, "+1XXXXXX4567", listOf(PhoneDeliveryMethod.TEXT)).shownFor(PHONE))
            .isEqualTo("+1******4567")
        assertThat(SignupChallenge(1, null, listOf(PhoneDeliveryMethod.TEXT)).shownFor(PHONE)).isEqualTo(NUMBER)
        assertThat(CHALLENGE.shownFor(IDENTIFIER)).isEqualTo(ADDRESS)
    }

    @Test
    fun `resend - a code that is still good - goes the way the last one went, not as a replacement`() {
        val update = reducer.handleIntent(
            phoneCode(delivery = PhoneDeliveryMethod.VOICE, secondsLeft = 0),
            SignupIntent.Resend,
        )

        assertThat(update.commands)
            .containsExactly(SignupCommand.SendCode(PHONE, BOTH_WAYS, PhoneDeliveryMethod.VOICE))
    }

    // endregion

    @Test
    fun `a typed code and the credentials - are never written out`() {
        assertThat(SignupIntent.SubmitCode("123456").toString()).doesNotContain("123456")
        assertThat(SignupCommand.Verify("123456").toString()).doesNotContain("123456")
    }

    @Test
    fun `the email the user typed - is never written out`() {
        val typed = entry(value = ADDRESS)

        listOf(
            IDENTIFIER,
            typed,
            code(),
            SignupIntent.SubmitIdentifier(IDENTIFIER),
            SignupCommand.Identify(IDENTIFIER),
            SignupCommand.SendCode(IDENTIFIER, CHALLENGE, delivery = null),
            SignupOutcome.Identified(IDENTIFIER, CHALLENGE),
            SignupOutcome.CodeSent(IDENTIFIER, CHALLENGE, delivery = null),
            SignupState.Failed(SignupError.Network, SignupRetry.ReturnTo(typed)),
        ).forEach { assertThat(it.toString()).doesNotContain(ADDRESS) }
    }

    @Test
    fun `the number the server masked - is never written out either`() {
        val shown = listOf(
            BOTH_WAYS,
            delivery(),
            phoneCode(),
            SignupOutcome.Identified(PHONE, BOTH_WAYS),
            SignupCommand.SendCode(PHONE, BOTH_WAYS, PhoneDeliveryMethod.TEXT),
            SignupOutcome.CodeSent(PHONE, BOTH_WAYS, PhoneDeliveryMethod.TEXT),
        )

        shown.forEach { assertThat(it.toString()).doesNotContain("4567") }
        assertThat(BOTH_WAYS.toString()).contains("TEXT")
    }

    @Test
    fun `the number the user typed - is never written out`() {
        val typed = phoneEntry(value = NUMBER)

        listOf(
            PHONE,
            typed,
            delivery(),
            phoneCode(),
            SignupIntent.SubmitIdentifier(PHONE),
            SignupCommand.Identify(PHONE),
            SignupCommand.SendCode(PHONE, BOTH_WAYS, PhoneDeliveryMethod.TEXT),
            SignupCommand.ReplaceExpiredCode(PHONE, PhoneDeliveryMethod.TEXT),
            SignupOutcome.Identified(PHONE, BOTH_WAYS),
            SignupState.Failed(SignupError.Network, SignupRetry.ReturnTo(typed), SignupKind.Phone),
        ).forEach { assertThat(it.toString()).doesNotContain(NUMBER) }
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
    ) = SignupState.Code(
        IDENTIFIER,
        CHALLENGE,
        delivery = null,
        phase = phase,
        error = error,
        resendSecondsLeft = secondsLeft,
    )

    private fun phoneEntry(
        value: String = "",
        isSubmitting: Boolean = false,
        error: SignupError.Fixable? = null,
    ) = SignupState.Entry(SignupKind.Phone, value, isSubmitting, error)

    private fun delivery(
        challenge: SignupChallenge = BOTH_WAYS,
        isSubmitting: Boolean = false,
        error: SignupError.Fixable? = null,
    ) = SignupState.Delivery(PHONE, challenge, isSubmitting, error)

    private fun phoneCode(
        delivery: PhoneDeliveryMethod? = PhoneDeliveryMethod.TEXT,
        phase: CodePhase = CodePhase.Ready,
        isExpired: Boolean = false,
        secondsLeft: Int = RESEND_WAIT_SECONDS,
    ) = SignupState.Code(
        PHONE,
        BOTH_WAYS,
        delivery,
        phase,
        resendSecondsLeft = secondsLeft,
        isExpired = isExpired,
    )

    private companion object {
        const val ADDRESS = "jane@example.com"
        const val CODE = "123456"
        val IDENTIFIER = SignupIdentifier(SignupKind.Email, ADDRESS)
        val CHALLENGE = SignupChallenge(index = 1)
        const val NUMBER = "+15551234567"
        const val MASKED_NUMBER = "+1******4567"
        val PHONE = SignupIdentifier(SignupKind.Phone, NUMBER)
        val BOTH_WAYS = SignupChallenge(1, MASKED_NUMBER, listOf(PhoneDeliveryMethod.TEXT, PhoneDeliveryMethod.VOICE))
    }
}
