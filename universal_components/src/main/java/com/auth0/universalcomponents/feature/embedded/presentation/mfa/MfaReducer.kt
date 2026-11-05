package com.auth0.universalcomponents.feature.embedded.presentation.mfa

import com.auth0.universalcomponents.feature.embedded.domain.model.MfaError
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.embedded.domain.model.PushStatus

/**
 * Every MFA rule in one place: the step on show plus an input gives the next step, maybe with work to run and an
 * effect for the host. [factors] are what the server offered for the first round. Pure, so it needs no Android,
 * coroutines or SDK to test.
 *
 * Answers that arrive after the user moved on are handled as iOS handles them: an approval, or a request for
 * another factor, still counts while MFA runs, because the server has already moved on; anything else from a
 * screen the user left is dropped, so it can't fail their next step.
 */
internal class MfaReducer(private val factors: List<MfaFactor>) {

    fun start(): MfaUpdate = beginRound(MfaRound(number = 1, factors = factors))

    fun handleIntent(step: MfaStep, intent: MfaIntent): MfaUpdate {
        val unchanged = MfaUpdate(step)
        return when (intent) {
            is MfaIntent.SelectFactor ->
                if (step is MfaStep.ChooseFactor && intent.factor in step.round.factors) {
                    select(step.round, intent.factor)
                } else {
                    unchanged
                }

            // Not while a resend is going out: the push can't be taken back, and a second pick would send another.
            MfaIntent.ChooseAnotherFactor ->
                if (step is MfaStep.PushWaiting && step.round.canSwitch && step.phase != PushPhase.Resending) {
                    showList(step.round)
                } else {
                    unchanged
                }

            MfaIntent.Back -> goBack(step)

            MfaIntent.Resend -> if (step is MfaStep.PushWaiting && step.canResendNow) resend(step) else unchanged
        }
    }

    fun handleOutcome(step: MfaStep, outcome: MfaOutcome): MfaUpdate = when (outcome) {
        is MfaOutcome.PushSent -> pushSent(step, outcome.pending)
        is MfaOutcome.PushPending -> pushPending(step, outcome.pending)
        // Even after the user moved on: they did approve, so they're signed in. The host shows the success screen.
        is MfaOutcome.SignedIn -> MfaUpdate(step, effect = MfaEffect.Authenticated(outcome.credentials))
        is MfaOutcome.AnotherFactor -> nextRound(step, outcome.factors)
        // Even after the user moved on: the server has closed the session, whichever device asked, so MFA ends with
        // the real reason rather than a later "session ended".
        MfaOutcome.PushRejected -> end(MfaError.Rejected(null))
        is MfaOutcome.SendFailed -> sendFailed(step, outcome.error)
        is MfaOutcome.CheckFailed -> checkFailed(step, outcome)
    }

    /**
     * The app is back in the foreground: the user may have approved in another app meanwhile, so a waiting push is
     * checked now instead of after the wait, as on iOS. A check already on its way is left to answer.
     */
    fun handleAppForegrounded(step: MfaStep): MfaUpdate =
        if (step is MfaStep.PushWaiting && step.phase == PushPhase.Waiting) {
            MfaUpdate(step.copy(phase = PushPhase.Checking), listOf(MfaCommand.CancelPushWork, MfaCommand.CheckPush))
        } else {
            MfaUpdate(step)
        }

    /** The wait before the next check is over. It checks only if the push is still waiting and nothing is on its way. */
    fun handlePushCheckDue(step: MfaStep): MfaUpdate =
        if (step is MfaStep.PushWaiting && step.phase == PushPhase.Waiting) {
            MfaUpdate(step.copy(phase = PushPhase.Checking), listOf(MfaCommand.CheckPush))
        } else {
            MfaUpdate(step)
        }

    /** One second of the resend wait has passed; it stops at zero, or once the push screen is left. */
    fun handleSecondPassed(step: MfaStep): MfaUpdate {
        if (step !is MfaStep.PushWaiting || step.resendSecondsLeft == 0) return MfaUpdate(step)
        val left = step.resendSecondsLeft - 1
        return MfaUpdate(
            step.copy(resendSecondsLeft = left),
            if (left > 0) listOf(MfaCommand.CountDownResend) else emptyList(),
        )
    }

    /**
     * No factor that can be used here ends MFA. In the first round a single factor goes straight to it. From round 2
     * on, the list comes back even with one factor, headed "One more step", so the user sees why they're asked again
     * (the PRD's additional challenge).
     */
    private fun beginRound(round: MfaRound): MfaUpdate = when {
        round.factors.isEmpty() -> end(MfaError.NoEligibleFactors())
        round.factors.size == 1 && !round.isAdditional -> select(round, round.factors.single())
        else -> MfaUpdate(MfaStep.ChooseFactor(round))
    }

    /** The challenge goes out first; its screen shows once it's out. */
    private fun select(round: MfaRound, factor: MfaFactor): MfaUpdate = when (factor) {
        is MfaFactor.Push -> MfaUpdate(MfaStep.SendingChallenge(round, factor), listOf(MfaCommand.SendPush(factor)))
    }

    /** A first push, or a resend, is out: the checks and the resend wait start. */
    private fun pushSent(step: MfaStep, pending: PushStatus.Pending): MfaUpdate {
        val everyMs = checkInterval(pending.retryInMs)
        val waiting = when {
            step is MfaStep.SendingChallenge && step.factor is MfaFactor.Push -> MfaStep.PushWaiting(
                round = step.round,
                factor = step.factor,
                phase = PushPhase.Waiting,
                everyMs = everyMs,
                canResend = pending.canResend,
                resendSecondsLeft = RESEND_COOLDOWN_SECONDS,
            )

            step is MfaStep.PushWaiting && step.phase == PushPhase.Resending -> step.copy(
                phase = PushPhase.Waiting,
                everyMs = everyMs,
                canResend = pending.canResend,
                resendSecondsLeft = RESEND_COOLDOWN_SECONDS,
                failedChecks = 0,
            )

            // A push for a screen the user has left.
            else -> return MfaUpdate(step)
        }
        return MfaUpdate(waiting, listOf(MfaCommand.SchedulePushCheck(everyMs), MfaCommand.CountDownResend))
    }

    /** Still waiting: check again at the server's pace, a little slower when it said we asked too soon. */
    private fun pushPending(step: MfaStep, pending: PushStatus.Pending): MfaUpdate {
        if (step !is MfaStep.PushWaiting || step.phase != PushPhase.Checking) return MfaUpdate(step)
        val everyMs = if (pending.isSlowDown) {
            checkInterval(maxOf(pending.retryInMs ?: 0, step.everyMs + SLOW_DOWN_STEP_MS))
        } else {
            checkInterval(pending.retryInMs ?: step.everyMs)
        }
        val waiting = step.copy(
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
    private fun nextRound(step: MfaStep, factors: List<MfaFactor>): MfaUpdate {
        val round = step.roundOrNull ?: return MfaUpdate(step)
        val next = beginRound(MfaRound(round.number + 1, factors))
        return next.copy(commands = listOf(MfaCommand.CancelPushWork) + next.commands)
    }

    /** A send fails the flow only while it's the one on show: the first push, or a resend. */
    private fun sendFailed(step: MfaStep, error: MfaError): MfaUpdate {
        val isOnShow = step is MfaStep.SendingChallenge ||
            (step is MfaStep.PushWaiting && step.phase == PushPhase.Resending)
        return if (isOnShow) end(error) else MfaUpdate(step)
    }

    /**
     * Any error but a network one means the sign-in session is gone, whichever device asked, so MFA ends with that
     * real error, even if the check came from a screen the user has left: otherwise the next push would fail and
     * the user would be told "session ended" instead of why. A network blip keeps the session, so the push the user
     * may already have approved is checked again quietly, at the same pace; after [QUIET_RETRIES] of those in a row
     * MFA ends. A blip from a check the user has left behind is dropped. As on iOS, a blip while the app is in the
     * background doesn't count: the user is approving in another app, where Android may cut this one's network.
     */
    private fun checkFailed(step: MfaStep, failed: MfaOutcome.CheckFailed): MfaUpdate {
        val error = failed.error
        if (error !is MfaError.Network) return end(error)
        if (step !is MfaStep.PushWaiting || step.phase != PushPhase.Checking) return MfaUpdate(step)
        if (!failed.whileInBackground && step.failedChecks >= QUIET_RETRIES) return end(error)
        val failedChecks = if (failed.whileInBackground) step.failedChecks else step.failedChecks + 1
        val waiting = step.copy(phase = PushPhase.Waiting, failedChecks = failedChecks)
        return MfaUpdate(waiting, listOf(MfaCommand.SchedulePushCheck(step.everyMs)))
    }

    private fun showList(round: MfaRound): MfaUpdate =
        MfaUpdate(MfaStep.ChooseFactor(round), listOf(MfaCommand.CancelPushWork))

    /** A resend replaces the checks until it's out; an answer to a check still on its way is handled as it comes. */
    private fun resend(step: MfaStep.PushWaiting): MfaUpdate = MfaUpdate(
        step.copy(phase = PushPhase.Resending, canResend = false),
        listOf(MfaCommand.CancelPushWork, MfaCommand.SendPush(step.factor)),
    )

    /**
     * The server can't step back, so Back stays on our side: from the push to the list when there's a choice,
     * otherwise out of MFA to start over. A challenge still going out finishes on its own, and its answer is dropped.
     * Once MFA has ended, Back is the host's.
     */
    private fun goBack(step: MfaStep): MfaUpdate = when (step) {
        is MfaStep.PushWaiting -> if (step.round.showsList) showList(step.round) else leave(step)
        is MfaStep.ChooseFactor, is MfaStep.SendingChallenge -> leave(step)
        is MfaStep.Failed -> MfaUpdate(step)
    }

    private fun leave(step: MfaStep) = MfaUpdate(step, effect = MfaEffect.Restart)

    /** MFA is over: its screen says why, and the host is told. */
    private fun end(error: MfaError) = MfaUpdate(MfaStep.Failed(error), effect = MfaEffect.Failed(error))

    /** The server's wait between checks, or [DEFAULT_CHECK_MS] when it gave none, kept within [CHECK_MS_RANGE]. */
    private fun checkInterval(ms: Long?): Long = (ms ?: DEFAULT_CHECK_MS).coerceIn(CHECK_MS_RANGE)

    /** The push is out, the server still offers another, and the resend wait is over. */
    private val MfaStep.PushWaiting.canResendNow: Boolean
        get() = phase != PushPhase.Resending && canResend && resendSecondsLeft == 0

    private val MfaStep.roundOrNull: MfaRound?
        get() = when (this) {
            is MfaStep.ChooseFactor -> round
            is MfaStep.SendingChallenge -> round
            is MfaStep.PushWaiting -> round
            is MfaStep.Failed -> null
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
