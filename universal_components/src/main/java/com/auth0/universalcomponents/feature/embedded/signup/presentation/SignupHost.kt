package com.auth0.universalcomponents.feature.embedded.signup.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auth0.android.embedded.authorize.PhoneDeliveryMethod
import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.presentation.components.EntryRole
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginScreenFrame
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginCodeScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginEmailScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginPhoneScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginSuccessScreen
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupChallenge
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupIdentifier
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupKind
import com.auth0.universalcomponents.feature.mfachallenge.presentation.forDisplay
import com.auth0.universalcomponents.feature.mfachallenge.presentation.screens.NO_CONTEXTUAL_ALTERNATES
import com.auth0.universalcomponents.presentation.ui.flow.FlowErrorScreen
import com.auth0.universalcomponents.presentation.ui.flow.FlowLoadingScreen
import com.auth0.universalcomponents.presentation.ui.mfa.phone.Country
import com.auth0.universalcomponents.presentation.ui.utils.ObserveAsEvents
import com.auth0.universalcomponents.presentation.ui.utils.stepTransition
import com.auth0.universalcomponents.utils.findActivity

/**
 * Draws the sign-up's state with the login's own screens and sign-up's words, animates screen changes, and owns the
 * navigation bar, the one Back handler and the effects. It only draws: what each tap means is [SignupReducer]'s.
 *
 * [onAuthenticated] gets the credentials, once. [onExit] is the user leaving sign-up: Back from any screen but the code
 * screen and the choice of how to get it (which return to the entry), or "Go back"; Back waits while a request is
 * running. [onLogIn], when sign-up was opened from the login, adds "Already have an account? Log in" to the first
 * screen; it is what returns there. The sign-up is left, so the next visit opens a new one, when this leaves the
 * composition, unless it only leaves to be drawn again (a rotation).
 */
@Composable
internal fun SignupHost(
    viewModel: SignupViewModel,
    onAuthenticated: (Credentials) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    onLogIn: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnAuthenticated by rememberUpdatedState(onAuthenticated)
    val currentOnExit by rememberUpdatedState(onExit)

    LaunchedEffect(viewModel) { viewModel.send(SignupIntent.Start) }
    // Keyed on the ViewModel: a new sign-up (another connection) has effects of its own.
    ObserveAsEvents(viewModel.effects, key1 = viewModel) { effect ->
        when (effect) {
            is SignupEffect.Authenticated -> currentOnAuthenticated(effect.credentials)
            SignupEffect.Exit -> currentOnExit()
        }
    }
    LeaveWhenDisposed(viewModel)

    val send: (SignupIntent) -> Unit = remember(viewModel) { viewModel::send }
    val onBack: () -> Unit = remember(viewModel) { { viewModel.send(SignupIntent.Back) } }
    BackHandler(onBack = onBack)

    // The state keeps only the number, and several countries share a dial code (+1), so on Edit or Try again the phone
    // screen would pick one of them itself. The host remembers the country the number was typed with, as the login does.
    var typedCountry by rememberSaveable { mutableIntStateOf(NO_COUNTRY) }

    LoginScreenFrame(
        onBack = onBack,
        modifier = modifier,
        title = stringResource(R.string.embedded_signup_title),
        isBackEnabled = !state.isBusy,
    ) {
        AnimatedContent(
            targetState = state,
            contentKey = { it.transitionIdentity },
            transitionSpec = {
                stepTransition(isForward = targetState.navigationDepth >= initialState.navigationDepth)
            },
            modifier = Modifier.fillMaxSize(),
            label = "signupState",
        ) { shown ->
            SignupScreen(
                state = shown,
                send = send,
                onBack = onBack,
                onLogIn = onLogIn,
                typedCountry = Country.countries.getOrNull(typedCountry),
                onCountryTyped = { typedCountry = Country.countries.indexOf(it) },
            )
        }
    }
}

/**
 * The sign-up is left when this leaves the composition, so the next visit starts a new one. A rotation also leaves it,
 * but the same sign-up is drawn again, so it is not left then.
 */
@Composable
private fun LeaveWhenDisposed(viewModel: SignupViewModel) {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(viewModel) {
        onDispose {
            if (activity?.isChangingConfigurations != true) viewModel.send(SignupIntent.Leave)
        }
    }
}

/** The screen for [state]; input goes back through [send]. */
@Composable
private fun SignupScreen(
    state: SignupState,
    send: (SignupIntent) -> Unit,
    onBack: () -> Unit,
    onLogIn: (() -> Unit)?,
    typedCountry: Country?,
    onCountryTyped: (Country) -> Unit,
) {
    when (state) {
        // Drawn alike, so the first frame, before Start, is already the spinner.
        SignupState.Idle, SignupState.Loading, SignupState.Creating -> FlowLoadingScreen()

        is SignupState.Entry -> when (state.kind) {
            SignupKind.Email -> LoginEmailScreen(
                email = state.value,
                isLoading = state.isSubmitting,
                errorMessage = state.error?.text(state.kind),
                isEntry = true,
                role = EntryRole.SignUp(onLogIn),
                onContinue = { send(SignupIntent.SubmitIdentifier(SignupIdentifier(state.kind, it))) },
            )

            SignupKind.Phone -> LoginPhoneScreen(
                phone = state.value,
                country = typedCountry.keptFor(state.value),
                isLoading = state.isSubmitting,
                errorMessage = state.error?.text(state.kind),
                isEntry = true,
                role = EntryRole.SignUp(onLogIn),
                onContinue = { number, country ->
                    onCountryTyped(country)
                    send(SignupIntent.SubmitIdentifier(SignupIdentifier(state.kind, number)))
                },
            )
        }

        is SignupState.Delivery -> SignupDeliveryScreen(
            sentTo = state.challenge.shownFor(state.identifier),
            ways = state.challenge.deliveries,
            onContinue = { send(SignupIntent.ChooseDelivery(it)) },
            sentToFontFeatures = state.challenge.fontFeatures,
            isLoading = state.isSubmitting,
            errorMessage = state.error?.text(state.identifier.kind),
            onEdit = onBack,
        )

        is SignupState.Code -> LoginCodeScreen(
            subtitle = stringResource(
                if (state.delivery == PhoneDeliveryMethod.VOICE) {
                    R.string.embedded_signup_code_called
                } else {
                    R.string.embedded_signup_code_sent_to
                },
                state.challenge.shownFor(state.identifier),
            ),
            subtitleFontFeatures = state.challenge.fontFeatures,
            isLoading = state.phase == CodePhase.Verifying,
            isResending = state.phase == CodePhase.Resending,
            resendSecondsLeft = state.resendSecondsLeft,
            errorMessage = if (state.showsExpired) {
                stringResource(R.string.embedded_signup_error_code_expired_phone)
            } else {
                state.error?.text(state.identifier.kind)
            },
            needsNewCode = state.isExpired,
            onVerify = { send(SignupIntent.SubmitCode(it)) },
            onEdit = onBack,
            onResend = { send(SignupIntent.Resend) },
        )

        SignupState.Authenticated -> LoginSuccessScreen()

        SignupState.Unavailable -> FlowErrorScreen(
            onRetry = onBack,
            message = stringResource(R.string.embedded_signup_unavailable_message),
            title = stringResource(R.string.embedded_signup_unavailable_title),
            actionTitle = stringResource(R.string.embedded_login_go_back),
        )

        is SignupState.Failed -> FlowErrorScreen(
            onRetry = { send(SignupIntent.Retry) },
            message = state.error.text(),
            title = stringResource(R.string.embedded_signup_error_title),
            // With nothing to go back to the sign-up is over, and only a new one helps.
            actionTitle = stringResource(
                if (state.retry == null) R.string.embedded_signup_start_over else R.string.try_again,
            ),
        )
    }
}

/** No country has been typed with yet. */
private const val NO_COUNTRY = -1

/**
 * The country the number was typed with, while the field holds that number (it starts with the country's dial code);
 * otherwise null, and the phone screen picks one from the number itself.
 */
internal fun Country?.keptFor(number: String): Country? =
    this?.takeIf { number.isNotEmpty() && number.startsWith(it.phoneCode) }

/**
 * Where the code goes, to show: the number as the server hid it, with every hidden digit drawn alike, else what the user
 * typed (an email always, since the server hides nothing there).
 */
internal fun SignupChallenge.shownFor(typed: SignupIdentifier): String = sentTo?.forDisplay() ?: typed.value

/**
 * The code screen says the code expired, when no other error is shown. The line is gone while a new code is on its way,
 * as on iOS, and back if that fails and the user tries again.
 */
internal val SignupState.Code.showsExpired: Boolean
    get() = isExpired && error == null && phase == CodePhase.Ready

/** The hidden digits of a number the server hid are drawn alike only with the font's contextual alternates off. */
private val SignupChallenge.fontFeatures: String?
    get() = NO_CONTEXTUAL_ALTERNATES.takeIf { sentTo != null }

/** Names the screen and ignores its data, so a data-only change doesn't animate. */
private val SignupState.transitionIdentity: String
    get() = when (this) {
        SignupState.Idle, SignupState.Loading, SignupState.Creating -> "loading"
        is SignupState.Entry -> "entry:$kind"
        is SignupState.Delivery -> "delivery"
        is SignupState.Code -> "code"
        SignupState.Authenticated -> "authenticated"
        SignupState.Unavailable -> "unavailable"
        is SignupState.Failed -> "failed"
    }

/** Compared across a change: deeper pushes, shallower pops. */
private val SignupState.navigationDepth: Int
    get() = when (this) {
        SignupState.Idle, SignupState.Loading, SignupState.Creating -> 0
        is SignupState.Entry -> 1
        is SignupState.Delivery -> 2
        is SignupState.Code -> 3
        SignupState.Authenticated, SignupState.Unavailable, is SignupState.Failed -> 4
    }
