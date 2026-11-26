package com.auth0.universalcomponents.feature.embedded.signup.data.repository

import com.auth0.android.embedded.EmbeddedAuthClient
import com.auth0.android.embedded.EmbeddedAuthException
import com.auth0.android.embedded.authorize.EmbeddedCapability
import com.auth0.android.embedded.authorize.NextAction
import com.auth0.android.embedded.authorize.OtpType
import com.auth0.android.embedded.authorize.PhoneDeliveryMethod
import com.auth0.android.request.Request
import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.data.embeddedauth.EmbeddedAuthSteps
import com.auth0.universalcomponents.data.embeddedauth.StepAnswer
import com.auth0.universalcomponents.domain.network.EmptyResult
import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.embedded.signup.data.mapper.capabilities
import com.auth0.universalcomponents.feature.embedded.signup.data.mapper.identifierType
import com.auth0.universalcomponents.feature.embedded.signup.data.mapper.toSignupFailure
import com.auth0.universalcomponents.feature.embedded.signup.data.mapper.toSignupMenu
import com.auth0.universalcomponents.feature.embedded.signup.data.mapper.toSignupOffer
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupChallenge
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupError
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupFailure
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupIdentifier
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupKind
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupNext
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupProgress
import com.auth0.universalcomponents.feature.embedded.signup.domain.repository.EmbeddedSignupRepository
import com.auth0.universalcomponents.utils.logging.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A sign-up against the SDK: authorize → signup → identify → challengeEmail or challengePhone → verifyOtp →
 * confirmSignup. Every step before the last answers with `insufficient_authorization` and the next actions to take,
 * which is read as the menu.
 * [steps] holds a client of its own, because a sign-up is a session of its own and must not touch the login's.
 * [connection] is fixed for the whole flow; opening again starts a new session with it.
 */
internal class EmbeddedSignupRepositoryImpl(
    private val steps: EmbeddedAuthSteps,
    private val connection: String,
) : EmbeddedSignupRepository {

    /** One call from the screens at a time, from its first SDK step to its last. A public function never calls another. */
    private val oneAtATime = Mutex()

    /** The kinds the sign-up being run requires, so each menu asks for those first. Read and set only under [oneAtATime]. */
    private var required: List<SignupKind> = emptyList()

    override suspend fun open(): Result<SignupNext, SignupFailure> = oneAtATime.withLock { openSignup() }

    override suspend fun identify(identifier: SignupIdentifier): Result<SignupChallenge, SignupFailure> =
        oneAtATime.withLock { identifyAs(identifier) }

    override suspend fun sendCode(
        challenge: SignupChallenge,
        delivery: PhoneDeliveryMethod?,
    ): EmptyResult<SignupFailure> = oneAtATime.withLock { sendTo(challenge, delivery) }

    override suspend fun replaceExpiredCode(
        identifier: SignupIdentifier,
        delivery: PhoneDeliveryMethod?,
    ): Result<SignupChallenge, SignupFailure> = oneAtATime.withLock {
        // The server ended the sign-up with the code, so a new one starts it over for the same identifier.
        val opened = openSignup()
        if (opened is Result.Error) return@withLock opened
        val challenge = when (val identified = identifyAs(identifier)) {
            is Result.Error -> return@withLock identified
            is Result.Success -> identified.data
        }
        when (val sent = sendTo(challenge, delivery)) {
            is Result.Error -> sent
            is Result.Success -> Result.Success(challenge)
        }
    }

    override suspend fun verify(code: String): Result<SignupProgress, SignupFailure> = oneAtATime.withLock {
        when (val answer = steps.run { verifyOtp(code, OtpType.OOB) }) {
            // Credentials here mean the identifier already had an account: the server signed that user in.
            is StepAnswer.Done -> Result.Success(SignupProgress.SignedIn(answer.value))
            is StepAnswer.Next -> answer.toSignupFailure()?.let(::failed)
                ?: Result.Success(SignupProgress.Continue(answer.actions.toSignupMenu(required).next))

            is StepAnswer.Failed -> failed(answer.toSignupFailure())
        }
    }

    override suspend fun createAccount(): Result<Credentials, SignupFailure> = oneAtATime.withLock {
        when (val answer = steps.run { confirmSignup() }) {
            is StepAnswer.Done -> Result.Success(answer.value)
            // Anything but a failure of its own means the tenant wants more than a sign-up gives (a second factor,
            // say). Sending the confirmation again would get the same answer.
            is StepAnswer.Next -> failed(answer.toSignupFailure() ?: SignupFailure(SignupError.MoreStepsRequired))
            is StepAnswer.Failed -> failed(answer.toSignupFailure())
        }
    }

    // The steps below run under [oneAtATime], held by the public function that called them.

    private suspend fun openSignup(): Result<SignupNext, SignupFailure> {
        if (connection.isBlank()) return failed(SignupFailure(SignupError.ConnectionMissing))
        val offered = when (val authorized = menuAfter { authorize(connection, capabilities = CAPABILITIES) }) {
            is Result.Error -> return authorized
            is Result.Success -> authorized.data.toSignupOffer()
        }
        if (offered?.isSupported != true) {
            Logger.w(TAG, "No sign-up this flow can finish is offered; required: ${offered?.required}")
            return Result.Error(SignupFailure(SignupError.Unavailable))
        }
        required = offered.required
        return when (val menu = menuAfter { signup() }) {
            is Result.Error -> menu
            is Result.Success -> Result.Success(menu.data.toSignupMenu(required).next)
        }
    }

    private suspend fun identifyAs(identifier: SignupIdentifier): Result<SignupChallenge, SignupFailure> {
        val type = identifier.kind.identifierType
        return when (val menu = menuAfter { identify(identifier.value, type) }) {
            is Result.Error -> menu
            is Result.Success -> menu.data.toSignupMenu().challenge?.let { Result.Success(it) }
                ?: failed(SignupFailure(SignupError.CodeNotSent))
        }
    }

    private suspend fun sendTo(challenge: SignupChallenge, delivery: PhoneDeliveryMethod?): EmptyResult<SignupFailure> {
        // An email's code has no way to choose; a phone's goes the way given.
        val sent = menuAfter {
            if (delivery == null) challengeEmail(challenge.index) else challengePhone(challenge.index, delivery)
        }
        return when (sent) {
            is Result.Error -> sent
            is Result.Success -> Result.Success(Unit)
        }
    }

    /**
     * Runs one step that always answers with the next menu, and returns the actions in it. A step that finishes
     * without one, or answers with something the user must fix or that ends the sign-up, is the failure it stands for.
     */
    private suspend fun <T> menuAfter(
        step: EmbeddedAuthClient.() -> Request<T, EmbeddedAuthException>,
    ): Result<List<NextAction>, SignupFailure> = when (val answer = steps.run(step)) {
        is StepAnswer.Next -> answer.toSignupFailure()?.let(::failed) ?: Result.Success(answer.actions)
        is StepAnswer.Failed -> failed(answer.toSignupFailure())
        is StepAnswer.Done -> failed(SignupFailure(SignupError.Unknown("A sign-up step ended without a next step")))
    }

    /** Every failure is logged with its cause, then handed on. */
    private fun failed(failure: SignupFailure): Result.Error<SignupFailure> {
        Logger.w(TAG, "Sign-up step failed: ${failure.error}", failure.cause)
        return Result.Error(failure)
    }

    private companion object {
        const val TAG = "EmbeddedSignup"

        /**
         * What these screens can drive, told to the server so it only offers those: sign-up's own steps, then each
         * kind's, once each. One fixed set, sent once when the sign-up opens, for the whole sign-up.
         */
        val CAPABILITIES: Set<EmbeddedCapability> = buildSet {
            add(EmbeddedCapability.SIGNUP)
            add(EmbeddedCapability.CONFIRM_SIGNUP)
            SignupKind.entries.forEach { addAll(it.capabilities) }
        }
    }
}
