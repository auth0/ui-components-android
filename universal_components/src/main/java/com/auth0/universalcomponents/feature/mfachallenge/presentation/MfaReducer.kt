package com.auth0.universalcomponents.feature.mfachallenge.presentation

import com.auth0.universalcomponents.feature.mfachallenge.MfaError
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.PushStatus

/**
 * Every MFA rule in one place: the state on show plus an input gives the next state, maybe with work to run and an
 * effect for the host. [factors] are what the server offered for the first round. Pure, so it needs no Android,
 * coroutines or SDK to test.
 *
 * Answers that arrive after the user moved on are handled as iOS handles them: an approval, or a request for
 * another factor, still counts while MFA runs, because the server has already moved on; anything else from a
 * screen the user left is dropped, so it can't fail their next step.
 */
internal class MfaReducer(private val factors: List<MfaFactor>) {

    fun start(): MfaUpdate = beginRound(MfaRound(number = 1, factors = factors))

    fun handleIntent(state: MfaState, intent: MfaIntent): MfaUpdate {
        val unchanged = MfaUpdate(state)
        return when (intent) {
            is MfaIntent.SelectFactor ->
                if (state is MfaState.ChooseFactor && intent.factor in state.round.factors) {
                    select(state.round, intent.factor)
                } else {
                    unchanged
                }

            // Not while a resend is going out: the push can't be taken back, and a second pick would send another.
            MfaIntent.ChooseAnotherFactor ->
                if (state is MfaState.PushWaiting && state.round.canSwitch && state.phase != PushPhase.Resending) {
                    showList(state.round)
                } else {
                    unchanged
                }

            MfaIntent.Back -> goBack(state)

            // Out of MFA from wherever it is; once MFA has ended, leaving is the host's.
            MfaIntent.Cancel -> if (state is MfaState.Failed) unchanged else restart(state)

            MfaIntent.Resend -> if (state is MfaState.PushWaiting && state.canResendNow) resend(state) else unchanged
        }
    }

    fun handleOutcome(state: MfaState, outcome: MfaOutcome): MfaUpdate = when (outcome) {
        is MfaOutcome.PushSent -> pushSent(state, outcome.pending)
        is MfaOutcome.PushPending -> pushPending(state, outcome.pending)
        // Even after the user moved on: they did approve, so they're signed in. The host shows the success screen.
        is MfaOutcome.SignedIn -> MfaUpdate(state, effect = MfaEffect.Authenticated(outcome.credentials))
        is MfaOutcome.AnotherFactor -> nextRound(state, outcome.factors)
        // Even after the user moved on: the server has closed the session, whichever device asked, so MFA ends with
        // the real reason rather than a later "session ended".
        is MfaOutcome.PushRejected -> fail(MfaError.Rejected(outcome.cause))
        is MfaOutcome.SendFailed -> sendFailed(state, outcome.error)
        is MfaOutcome.CheckFailed -> checkFailed(state, outcome)
    }

    /**
     * The app is back in the foreground: the user may have approved in another app meanwhile, so a waiting push is
     * checked now instead of after the wait, as on iOS. A check already on its way is left to answer.
     */
    fun handleAppForegrounded(state: MfaState): MfaUpdate =
        if (state is MfaState.PushWaiting && state.phase == PushPhase.Waiting) {
            MfaUpdate(state.copy(phase = PushPhase.Checking), listOf(MfaCommand.CancelPushWork, MfaCommand.CheckPush))
        } else {
            MfaUpdate(state)
        }

    /** The wait before the next check is over. It checks only if the push is still waiting and nothing is on its way. */
    fun handlePushCheckDue(state: MfaState): MfaUpdate =
        if (state is MfaState.PushWaiting && state.phase == PushPhase.Waiting) {
            MfaUpdate(state.copy(phase = PushPhase.Checking), listOf(MfaCommand.CheckPush))
        } else {
            MfaUpdate(state)
        }

    /**
     * [seconds] of the resend wait have passed: one, or more when the app was frozen meanwhile and catches up, as on
     * iOS, instead of making the user wait the countdown out again. It stops at zero, or once the push screen is left.
     */
    fun handleSecondPassed(state: MfaState, seconds: Int = 1): MfaUpdate {
        if (state !is MfaState.PushWaiting || state.resendSecondsLeft == 0) return MfaUpdate(state)
        val left = (state.resendSecondsLeft - maxOf(1, seconds)).coerceAtLeast(0)
        return MfaUpdate(
            state.copy(resendSecondsLeft = left),
            if (left > 0) listOf(MfaCommand.CountDownResend) else emptyList(),
        )
    }

    /**
     * No factor that can be used here ends MFA. In the first round a single factor goes straight to it. From round 2
     * on, the list comes back even with one factor, headed "One more step", so the user sees why they're asked again
     * (the PRD's additional challenge).
     */
    private fun beginRound(round: MfaRound): MfaUpdate = when {
        round.factors.isEmpty() -> fail(MfaError.NoEligibleFactors())
        round.factors.size == 1 && !round.isAdditional -> select(round, round.factors.single())
        else -> MfaUpdate(MfaState.ChooseFactor(round))
    }

    /** The challenge goes out first; its screen shows once it's out. */
    private fun select(round: MfaRound, factor: MfaFactor): MfaUpdate = when (factor) {
        is MfaFactor.Push -> MfaUpdate(MfaState.SendingChallenge(round, factor), listOf(MfaCommand.SendPush(factor)))
    }

    /** A first push, or a resend, is out: the checks and the resend wait start. */
    private fun pushSent(state: MfaState, pending: PushStatus.Pending): MfaUpdate {
        val everyMs = checkInterval(pending.retryInMs)
        val waiting = when {
            state is MfaState.SendingChallenge && state.factor is MfaFactor.Push -> MfaState.PushWaiting(
                round = state.round,
                factor = state.factor,
                phase = PushPhase.Waiting,
                everyMs = everyMs,
                canResend = pending.canResend,
                resendSecondsLeft = RESEND_COOLDOWN_SECONDS,
            )

            state is MfaState.PushWaiting && state.phase == PushPhase.Resending -> state.copy(
                phase = PushPhase.Waiting,
                everyMs = everyMs,
                canResend = pending.canResend,
                resendSecondsLeft = RESEND_COOLDOWN_SECONDS,
                failedChecks = 0,
            )

            // A push for a screen the user has left.
            else -> return MfaUpdate(state)
        }
        return MfaUpdate(waiting, listOf(MfaCommand.SchedulePushCheck(everyMs), MfaCommand.CountDownResend))
    }

    /** Still waiting: check again at the server's pace, a little slower when it said we asked too soon. */
    private fun pushPending(state: MfaState, pending: PushStatus.Pending): MfaUpdate {
        if (state !is MfaState.PushWaiting || state.phase != PushPhase.Checking) return MfaUpdate(state)
        val everyMs = if (pending.isSlowDown) {
            checkInterval(maxOf(pending.retryInMs ?: 0, state.everyMs + SLOW_DOWN_STEP_MS))
        } else {
            checkInterval(pending.retryInMs ?: state.everyMs)
        }
        val waiting = state.copy(
            phase = PushPhase.Waiting,
            everyMs = everyMs,
            canResend = pending.canResend,
            failedChecks = 0,
        )
        return MfaUpdate(waiting, listOf(MfaCommand.SchedulePushCheck(everyMs)))
    }

    /**
     * The user approved and the sign-in needs one more factor: the next round starts with the server's new menu,
     * even when the approval came from a device the user switched away from.
     */
    private fun nextRound(state: MfaState, factors: List<MfaFactor>): MfaUpdate {
        val round = state.roundOrNull ?: return MfaUpdate(state)
        val next = beginRound(MfaRound(round.number + 1, factors))
        return next.copy(commands = listOf(MfaCommand.CancelPushWork) + next.commands)
    }

    /** A send fails the flow only while it's the one on show: the first push, or a resend. */
    private fun sendFailed(state: MfaState, error: MfaError): MfaUpdate {
        val isOnShow = state is MfaState.SendingChallenge ||
            (state is MfaState.PushWaiting && state.phase == PushPhase.Resending)
        return if (isOnShow) fail(error) else MfaUpdate(state)
    }

    /**
     * Any error but a network one means the sign-in session is gone, whichever device asked, so MFA ends with that
     * real error, even if the check came from a screen the user has left: otherwise the next push would fail and
     * the user would be told "session ended" instead of why. A network blip keeps the session, so the push the user
     * may already have approved is checked again quietly, at the same pace; after [QUIET_RETRIES] of those in a row
     * MFA ends. A blip from a check the user has left behind is dropped. As on iOS, a blip while the app is in the
     * background doesn't count: the user is approving in another app, where Android may cut this one's network.
     */
    private fun checkFailed(state: MfaState, failed: MfaOutcome.CheckFailed): MfaUpdate {
        val error = failed.error
        if (error !is MfaError.Network) return fail(error)
        if (state !is MfaState.PushWaiting || state.phase != PushPhase.Checking) return MfaUpdate(state)
        if (!failed.whileInBackground && state.failedChecks >= QUIET_RETRIES) return fail(error)
        val failedChecks = if (failed.whileInBackground) state.failedChecks else state.failedChecks + 1
        val waiting = state.copy(phase = PushPhase.Waiting, failedChecks = failedChecks)
        return MfaUpdate(waiting, listOf(MfaCommand.SchedulePushCheck(state.everyMs)))
    }

    private fun showList(round: MfaRound): MfaUpdate =
        MfaUpdate(MfaState.ChooseFactor(round), listOf(MfaCommand.CancelPushWork))

    /** A resend replaces the checks until it's out; an answer to a check still on its way is handled as it comes. */
    private fun resend(state: MfaState.PushWaiting): MfaUpdate = MfaUpdate(
        state.copy(phase = PushPhase.Resending, canResend = false),
        listOf(MfaCommand.CancelPushWork, MfaCommand.SendPush(state.factor)),
    )

    /**
     * The server can't step back, so Back stays on our side: from the push to the list when there's a choice,
     * otherwise out of MFA to start over. A challenge still going out finishes on its own, and its answer is dropped.
     * Not while a resend is going out, for the reason "Verify another way" is not either: the push can't be taken back,
     * and picking the device again from the list would send another. Cancel still leaves. Once MFA has ended, Back is
     * the host's.
     */
    private fun goBack(state: MfaState): MfaUpdate = when (state) {
        is MfaState.PushWaiting -> when {
            state.phase == PushPhase.Resending -> MfaUpdate(state)
            state.round.showsList -> showList(state.round)
            else -> restart(state)
        }
        is MfaState.ChooseFactor, is MfaState.SendingChallenge -> restart(state)
        is MfaState.Failed -> MfaUpdate(state)
    }

    private fun restart(state: MfaState) = MfaUpdate(state, effect = MfaEffect.Restart)

    /** MFA is over: its screen says why, and the host is told. */
    private fun fail(error: MfaError) = MfaUpdate(MfaState.Failed(error), effect = MfaEffect.Failed(error))

    /** The server's wait between checks, or [DEFAULT_CHECK_MS] when it gave none, kept within [CHECK_MS_RANGE]. */
    private fun checkInterval(ms: Long?): Long = (ms ?: DEFAULT_CHECK_MS).coerceIn(CHECK_MS_RANGE)

    /** The push is out, the server still offers another, and the resend wait is over. */
    private val MfaState.PushWaiting.canResendNow: Boolean
        get() = phase != PushPhase.Resending && canResend && resendSecondsLeft == 0

    private val MfaState.roundOrNull: MfaRound?
        get() = when (this) {
            is MfaState.ChooseFactor -> round
            is MfaState.SendingChallenge -> round
            is MfaState.PushWaiting -> round
            is MfaState.Failed -> null
        }

    companion object {
        /** The same wait as resending an emailed code. */
        const val RESEND_COOLDOWN_SECONDS = 60

        /** Checks in a row that may fail on the network before MFA ends, as on iOS. */
        const val QUIET_RETRIES = 3

        /** The wait between checks when the server gives none, and the bounds on the one it gives, as on iOS. */
        const val DEFAULT_CHECK_MS = 2_000L
        val CHECK_MS_RANGE = 1_000L..60_000L

        /** How much longer each wait gets when the server says we asked too soon, as on iOS. */
        const val SLOW_DOWN_STEP_MS = 2_000L
    }
}
