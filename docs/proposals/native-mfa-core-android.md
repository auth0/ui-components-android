# Native MFA Core on Android: proposal

Status: draft for discussion. Branch reviewed: `feature/embedded_mfa_push_support` (HEAD `7c54768`).
Inputs: [Native MFA UI Components](https://oktainc.atlassian.net/wiki/spaces/UCT/pages/1176535300/Native+MFA+UI+Components) (requirement, v3) and [Embedded Authorization SDK Design](https://oktainc.atlassian.net/wiki/spaces/DXSDK/pages/1094550391/Embedded+Authorization+SDK+Design) (SDK, v25).

---

## 1. Summary

The push MFA on this branch already uses the shape the requirement asks for. `MfaIntentHandler` is a pure reducer. `MfaViewModel` only runs commands and emits effects. One SDK session is shared between the first factor and MFA. Because of that, **we evolve this code into the MFA Core rather than rewrite it.**

The current code is push-shaped in four places:

1. `MfaRepository`
2. `MfaStep.WaitForPush`
3. `MfaFactor` (one variant)
4. The error model, which lives in `EmbeddedLoginError`

It also has no rounds, no lifecycle handling, no public customisation surface and no public error type.

The proposal has four parts:

1. A factor-agnostic reducer that understands three interaction kinds, not N factors (section 3).
2. Factor registry descriptors plus a normalised `MfaOutcome` from an `MfaTransport` (one per entry point).
3. A public surface (`MfaConfiguration`, `MfaStep`, slot models, `MfaException`) frozen in Milestone 1 for all six screens. Factors that ship later then add no public API.
4. A PR sequence that lands the push work and refactors it in place (section 8).

The claim "adding a factor doesn't touch the Core" holds for every factor in the requirement. It does **not** hold for a factor with a new interaction kind, such as passkeys or WebAuthn. The requirement should say so explicitly (section 10).

---

## 2. Gap analysis: current branch vs requirement

| Requirement | Current branch | Gap |
|---|---|---|
| UI-free state machine | `MfaIntentHandler` pure reducer + `MfaViewModel` command runner | Logic is in place, but the engine is a `ViewModel`. Step-up needs it without a ViewModel. |
| Factor registry, phishing-resistance order | `MfaFactor.Push` only; `toMfaFactors()` keeps server order | Missing |
| Rounds (max 3, `maxChallengesExceeded` on the 4th) | `checkPush` treats a further `next` menu as `ExtraStepUnsupported` | Missing; today that path is an error |
| "Verify another way" cancels timers and polls | `Back` → `ChooseFactor` + `StopPush` | Logic exists, push only. Server legality is unconfirmed (Q1). |
| Injectable clock | `delay()` in the ViewModel; countdown ticks once a second | Countdown drifts and is wrong after backgrounding |
| App lifecycle signal | none | Missing. Push keeps polling in background, and there is no immediate poll on return. |
| Normalised server outcome | `StepAnswer` (SDK-shaped) + `MfaResult` (push-shaped) | Needs a factor-agnostic `MfaOutcome` |
| `MfaTransport` per entry point | `MfaRepository` (push methods) | Generalise |
| Typed public errors (`MfaException`) | `EmbeddedLoginError` (internal, mixed first factor + MFA) | Split and expose |
| `onMfaError`; `noEligibleFactors` goes to the app | `NoEligibleFactors` renders the error screen | Behaviour change |
| `MfaConfiguration`: messages, screens, headless | none; strings are `embedded_login_mfa_*` | Missing. Copy keys must be frozen now. |
| Six slots with typed models + actions | 2 internal screens | Missing |
| Recovery code display: no dismiss, no screenshots | n/a | New (`FLAG_SECURE` + sheet window) |
| `auth_session` in memory only; clean start after process death | SDK keeps it in memory ✓; no `SavedStateHandle` ✓ | `LoginCodeScreen` keeps the typed code in `rememberSaveable` (Bundle). See section 7. |

---

## 3. The core idea: reduce on interaction kinds, not factors

Every factor in the requirement fits one of three interaction shapes. The reducer only knows these shapes. Descriptors map a factor to its shape.

| Interaction | Factors | Flow |
|---|---|---|
| `EnterCode` | authenticator app (TOTP), recovery code | select → code entry → `verify` |
| `SendThenEnterCode` | SMS, voice, email | select → `challenge` → code entry (resend cooldown) → `verify` |
| `SendThenPoll` | push | select → `challenge` → waiting (poll `verifyOob`, resend cooldown) |

After any successful verify, the server answers one of three ways:

- **Authenticated:** the flow completes.
- **NextRound(menu):** "One more step"; round + 1.
- **NewRecoveryCode(code):** display, then acknowledge, then `confirmRecoveryCode()`, then Authenticated (or NextRound).

So the reducer handles one generic `Challenge` step:

```kotlin
internal sealed interface CoreStep {
    data object Loading : CoreStep
    data object ChooseFactor : CoreStep
    data class Challenge(
        val factor: MfaFactor,
        val interaction: Interaction,
        val phase: Phase,                     // Sending | Ready | Submitting
        val codeRejected: Boolean = false,    // wrong code: stay, keep input
        val resendAvailableAtMs: Long? = null,// deadline, not a ticking counter
        val pollInMs: Long? = null,
    ) : CoreStep
    data class ShowRecoveryCode(val code: Secret, val isConfirming: Boolean = false) : CoreStep
    data class Failed(val error: MfaError, val retry: MfaFactor?) : CoreStep
    data object Done : CoreStep
}

internal sealed interface Interaction {
    data class EnterCode(val format: CodeFormat) : Interaction
    data class SendThenEnterCode(val format: CodeFormat, val resendCooldownSec: Int) : Interaction
    data class SendThenPoll(val resendCooldownSec: Int) : Interaction
}
```

Today's `WaitForPush` becomes `Challenge(factor = Push, interaction = SendThenPoll)`. Every existing push rule in `MfaIntentHandler` carries over:

- send before the screen appears
- resend cooldown
- the in-flight poll finishes before the next step
- Back while sending is ignored

These rules move from "if push" to "if `SendThen*`".

A new interaction kind (for example passkey or WebAuthn through Credential Manager) is the only change that touches the Core. That is a deliberate, reviewable exception.

---

## 4. Proposed architecture

### 4.1 Packages

MFA Core moves out of `feature/embedded`, because step-up (Milestone 3) consumes it too.

```
feature/mfa/
  core/                    // no androidx.*, no compose.*  (enforced, see 9.3)
    MfaState, CoreStep, Interaction, MfaIntent, MfaCommand, MfaEffect
    MfaReducer             // evolution of MfaIntentHandler: pure
    MfaEngine              // evolution of MfaViewModel's command runner; takes a CoroutineScope
    FactorRegistry, FactorDescriptor, FactorKind
    MfaTransport, MfaOutcome, MfaError
    MfaClock, Secret
  transport/
    EmbeddedMfaTransport   // EmbeddedAuthClient (Milestone 1); absorbs MfaRepositoryImpl
    MfaApiTransport        // mfa_token + MFA API (Milestone 3)
  factor/
    push/  otp/  phone/  email/  recovery/   // descriptor + default screen + copy, one folder per factor
  ui/
    MfaHost                // renders the projected MfaStep through the slots
    MfaViewModel           // thin: owns MfaEngine in viewModelScope (embedded login)
    screens/               // default slot implementations
  api/                     // the only public package
    MfaConfiguration, MfaMessages (+ DSL), MfaScreens, MfaPresentation
    MfaStep + slot models, MfaController, MfaException
feature/embedded/          // first factor only; hands over to feature/mfa
```

### 4.2 Factor registry

```kotlin
// All five kinds are declared in Milestone 1, because copy keys and slots are frozen then too.
internal enum class FactorKind(val rank: Int, val slot: MfaSlot) {
    Otp(0, MfaSlot.OtpChallenge),
    Push(1, MfaSlot.PushChallenge),
    Phone(2, MfaSlot.PhoneChallenge),
    Email(3, MfaSlot.EmailChallenge),
    RecoveryCode(4, MfaSlot.RecoveryCodeChallenge),
}

internal interface FactorDescriptor {
    val kind: FactorKind
    fun interaction(factor: MfaFactor): Interaction
}

internal class FactorRegistry(private val descriptors: Map<FactorKind, FactorDescriptor>) {
    /** Supported only, phishing-resistance order, stable within a kind (one row per device/number/email). */
    fun eligible(offered: List<MfaFactor>): List<MfaFactor> =
        offered.filter { it.kind in descriptors }.sortedBy { it.kind.rank }
    fun interaction(factor: MfaFactor): Interaction = descriptors.getValue(factor.kind).interaction(factor)
}
```

A factor ships when its descriptor is registered. A factor that the transport parses but that isn't registered yet is filtered out. If nothing remains, the result is `NoEligibleFactors`. This is what makes "one factor per release" safe: the transport can learn to parse SMS before the SMS screen ships.

### 4.3 Transport and normalised outcome

The transport is wire-specific: SDK `NextAction` for embedded login, MFA API for step-up. It returns our own types only, so the Core never sees `NextAction`.

```kotlin
internal interface MfaTransport {
    suspend fun challenge(factor: MfaFactor, delivery: PhoneDelivery? = null): MfaOutcome
    suspend fun verifyCode(factor: MfaFactor, code: Secret): MfaOutcome
    suspend fun poll(): MfaOutcome
    suspend fun confirmRecoveryCode(): MfaOutcome
}

internal sealed interface MfaOutcome {
    data class ChallengeSent(val pollInMs: Long? = null) : MfaOutcome
    data class Pending(val pollInMs: Long?) : MfaOutcome           // authorization_pending / slow_down
    data object CodeRejected : MfaOutcome                           // invalid_code: stay on screen
    data class NextRound(val offered: List<MfaFactor>) : MfaOutcome // a fresh challenge menu after a verify
    data class NewRecoveryCode(val code: Secret) : MfaOutcome       // NextAction.ConfirmRecoveryCode(newCode)
    data class Authenticated(val credentials: Credentials) : MfaOutcome
    data class Failed(val error: MfaError, val canRetry: Boolean) : MfaOutcome
}
```

Mapping in `EmbeddedMfaTransport`. It reuses `runStep` and the error mapper:

| SDK call | SDK answer | `MfaOutcome` |
|---|---|---|
| `challengePush(i)` | `Next[VerifyOob(pollInMs)]` | `ChallengeSent(pollInMs)` |
| `challengePhone(i, method)` / `challengeEmail(i)` | `Next[VerifyOtp(channel, identifier)]` | `ChallengeSent()` |
| `verifyOob()` | `Next`, reason `AUTHORIZATION_PENDING` / `SLOW_DOWN` | `Pending(pollInMs)` |
| `verifyOtp(code, TOTP \| OOB)` | `Next`, reason `INVALID_CODE` | `CodeRejected` |
| `verifyRecoveryCode(code)` | `Next[ConfirmRecoveryCode(newCode)]` | `NewRecoveryCode` |
| any verify, `confirmRecoveryCode()` | `Next`, reason `NONE`, with challenge actions | `NextRound(parse(actions))` |
| any verify | `Done(credentials)` | `Authenticated` |
| any | `Failed` | `Failed(map(error), canRetryStep)` |

`OtpType` follows the factor: `TOTP` for the authenticator app, `OOB` for SMS, voice and email.

### 4.4 Engine, clock and lifecycle

`MfaEngine` is today's `MfaViewModel` body (`applyUpdate`, `runCommand`, the `NonCancellable` poll with `cancelAndJoin`), with the `ViewModel` superclass removed:

```kotlin
internal class MfaEngine(
    private val scope: CoroutineScope,
    private val transport: MfaTransport,
    private val registry: FactorRegistry,
    private val clock: MfaClock,            // fun interface { fun nowMs(): Long }
    offered: List<MfaFactor>,
) : MfaController {
    override val step: StateFlow<MfaStep>  // projection of internal MfaState; see 5.2
    val effects: Flow<MfaEffect>
    fun onIntent(intent: MfaIntent)
}
```

- **Rounds.** `MfaState.round` starts at 1. `NextRound` increments it. On round 4 the step becomes `Failed(MaxChallengesExceeded)`. If the round has no eligible factors, the result is the `NoEligibleFactors` effect. With one eligible factor, the engine goes straight to it; with several, it shows `ChooseFactor` with the `challengeAdditional` header.
- **Countdown.** State stores `resendAvailableAtMs`. A ticker command recomputes `secondsLeft` from `clock.nowMs()`. After backgrounding, the value is correct on the first tick, with no drift. Tests use `clock = { testScheduler.currentTime }`.
- **Lifecycle.** The intents are `Backgrounded` and `Foregrounded`. Backgrounded stops polling but keeps the step. Foregrounded, while waiting on a push, polls at once. That covers "approve on the same phone advances on return". For embedded login, the host sends both intents from `LifecycleEventEffect(ON_STOP/ON_START)`. That needs `lifecycle-runtime-compose` 2.7+ (section 9.4).
- **Switching factor.** `VerifyAnotherWay` emits `StopPoll` and `StopCountdown` before going to `ChooseFactor`. It follows the same "finish the in-flight call first" rule as today.

`MfaViewModel` shrinks to ownership only:

```kotlin
internal class MfaViewModel(engineFactory: (CoroutineScope) -> MfaEngine) : ViewModel() {
    val engine = engineFactory(viewModelScope)
}
```

Step-up (Milestone 3) hosts the same engine in its sheet's scope. No logic is duplicated.

---

## 5. Public API (frozen in Milestone 1)

### 5.1 Entry point changes

```kotlin
@Composable
public fun EmbeddedLoginScreen(
    connection: String,
    onAuthenticated: (Credentials) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    themeConfiguration: Auth0ThemeConfiguration = Auth0ThemeConfiguration.Default,
    mfa: MfaConfiguration = MfaConfiguration.Default,      // new
    onMfaError: (MfaException) -> Unit = {},               // new
)
```

`EmbeddedLoginScreen` shipped in `1.0.0-beta.1`. Adding defaulted parameters keeps source compatibility but **breaks binary compatibility**, because Compose and Kotlin generate a new synthetic signature. That's acceptable in beta. Land it once, with both parameters, before `1.0.0` GA.

### 5.2 `MfaStep`: one case per screen, all six declared now

```kotlin
public sealed interface MfaStep {
    public data object Loading : MfaStep
    public class FactorList internal constructor(public val model: FactorListModel) : MfaStep
    public class PushWaiting internal constructor(public val model: PushChallengeModel) : MfaStep
    public class CodeEntry internal constructor(public val model: CodeChallengeModel) : MfaStep  // otp | phone | email
    public class RecoveryCodeEntry internal constructor(public val model: RecoveryCodeChallengeModel) : MfaStep
    public class RecoveryCodeDisplay internal constructor(public val model: RecoveryCodeDisplayModel) : MfaStep
    public class Error internal constructor(public val model: ErrorModel) : MfaStep
}
```

All six cases are declared in Milestone 1, even though only push renders on device. A headless app's `when (step)` then never breaks when a factor ships. Internal `MfaState` stays private; `MfaStep` is a projection. Internal rules can change without an API change.

Each model carries data plus one action per control. A custom screen can only render data and call actions, so it can't break the flow:

```kotlin
@Stable
public class CodeChallengeModel internal constructor(
    public val kind: MfaFactorKind,            // Otp | Phone | Email
    public val maskedTarget: String?,          // null for the authenticator app
    public val code: String,                   // held in the engine, not rememberSaveable (see 7)
    public val isSubmitting: Boolean,
    public val error: MfaInlineError?,         // wrong code
    public val resend: ResendState?,           // null for the authenticator app
    public val onCodeChange: (String) -> Unit,
    public val onSubmit: () -> Unit,
    public val onResend: () -> Unit,
    public val onVerifyAnotherWay: (() -> Unit)?, // null when only one factor
    public val onCancel: () -> Unit,
)
```

Rules for the public API:

- **No `data class` in the public API.** `copy` and `componentN` lock the constructor and the field order.
- **Internal constructors.** Only the engine creates models.
- **Compose stability.** Use `@Stable` or `@Immutable`.

### 5.3 `MfaConfiguration`

```kotlin
@Immutable
public class MfaConfiguration(
    public val messages: MfaMessages = MfaMessages.Default,   // overrides only; defaults resolve from string resources
    public val screens: MfaScreens = MfaScreens(),            // null slot = default screen
    public val presentation: MfaPresentation = MfaPresentation.Default,
)

public class MfaScreens(
    public val authenticatorList: (@Composable (FactorListModel) -> Unit)? = null,
    public val pushChallenge: (@Composable (PushChallengeModel) -> Unit)? = null,
    public val otpChallenge: (@Composable (CodeChallengeModel) -> Unit)? = null,
    public val phoneChallenge: (@Composable (CodeChallengeModel) -> Unit)? = null,
    public val emailChallenge: (@Composable (CodeChallengeModel) -> Unit)? = null,
    public val recoveryCodeChallenge: (@Composable (RecoveryCodeChallengeModel) -> Unit)? = null,
    public val recoveryCodeDisplay: (@Composable (RecoveryCodeDisplayModel) -> Unit)? = null,
    public val error: (@Composable (ErrorModel) -> Unit)? = null,
)

public sealed interface MfaPresentation {
    public data object Default : MfaPresentation
    /** Renders nothing; the app draws every step from the controller. */
    public class Headless(public val content: @Composable (MfaController) -> Unit) : MfaPresentation
}
```

`MfaMessages`:

- It is a typed tree that matches the requirement's key list exactly, with a `mfaMessages { }` DSL.
- Every leaf is nullable. Null means the string-resource default, resolved in composition with `stringResource`.
- Dynamic copy is a function: `description: ((device: String) -> String)?`.
- Rename the existing resources now to follow the frozen tree, for example `embedded_login_mfa_push_title` → `auth0_mfa_push_challenge_title`. This is free before GA and costly after.

### 5.4 `MfaException`

```kotlin
public sealed class MfaException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    public class Cancelled internal constructor() : MfaException("cancelled")
    public class TokenExpired internal constructor(cause: Throwable?) : MfaException("token_expired", cause)
    public class TokenInvalid internal constructor(cause: Throwable?) : MfaException("token_invalid", cause)
    public class MaxChallengesExceeded internal constructor() : MfaException("max_challenges_exceeded")
    public class Rejected internal constructor(cause: Throwable?) : MfaException("rejected", cause)
    public class TooManyAttempts internal constructor(cause: Throwable?) : MfaException("too_many_attempts", cause)
    public class NoEligibleFactors internal constructor(cause: Throwable?) : MfaException("no_eligible_factors", cause)
    public class PresentationUnavailable internal constructor() : MfaException("presentation_unavailable")
    /** Proposed addition: the SDK has terminal errors the requirement doesn't list (see table). */
    public class Unknown internal constructor(public val code: String, cause: Throwable?) : MfaException(code, cause)
}
```

The subtypes are classes, not `object`s, because a singleton exception shares one stack trace across every throw. That breaks crash reporting and leaks state between flows.

SDK → `MfaException` mapping:

| `EmbeddedAuthError` | `MfaException` | UI |
|---|---|---|
| `AuthorizationRejected` | `Rejected` | error screen |
| `ChallengeExpired`, `SessionExpired` | `TokenExpired` | error screen |
| `InvalidRequest`, `NoActiveSession` | `TokenInvalid` | error screen |
| `TooManyWrongOtpAttempts`, `TooManyAttempts` | `TooManyAttempts` | error screen |
| `NoEligibleFactors`, or a menu with no registered factor | `NoEligibleFactors` | **no screen**; `onMfaError`, the app falls back to Universal Login |
| client-side round 4 | `MaxChallengesExceeded` | error screen |
| user cancel | `Cancelled` | **no screen**; login returns to "Sign in" |
| `Network`, 5xx | not terminal | error screen with Retry (`canRetryStep`) |
| `TooManyLogins`, `IpBlocked`, `AccessDenied`, `ConsentRequired`, `NoNextSteps`, `Unknown` | `Unknown(code)` | error screen (generic, or server text) |

---

## 6. Impact on the current branch, file by file

| File | Action | Notes |
|---|---|---|
| `MfaIntentHandler` | **Evolve** into `core/MfaReducer` | Push rules generalise to `SendThen*`. Add rounds, lifecycle intents, verify-another-way, code entry, recovery display. `MfaIntentHandlerTest` ports almost as-is. |
| `MfaViewModel` | **Split** | Command runner → `MfaEngine` (no ViewModel). The ViewModel only owns it. The `NonCancellable` + `cancelAndJoin` pattern is kept. |
| `MfaContract` | **Split** | `MfaState` and the intents, commands and effects stay internal. `MfaStep` is replaced by the internal `CoreStep` plus the public `MfaStep` projection. `MfaResult` is replaced by `MfaOutcome`. |
| `MfaRepository` / `MfaRepositoryImpl` | **Replace** with `MfaTransport` / `EmbeddedMfaTransport` | `checkPush`'s `furtherStep → ExtraStepUnsupported` becomes `NextRound`. |
| `MfaFactor` | **Extend** | Add `kind`, plus `Otp`, `Phone(index, masked, methods)`, `Email(index, masked)`, `RecoveryCode`. Parsing moves into the transport. |
| `MfaFactorMapper` | **Move** into the transport | Map every `NextAction` the SDK models. The registry decides what's shown. |
| `EmbeddedLoginError` | **Split** | First-factor cases stay. `PushDenied`, `NoEligibleFactors` and `MfaRequired` leave. |
| `EmbeddedLoginRepository.verifyOtp` | **Change return type** | `Result<VerifyOutcome, LoginFailure>` with `SignedIn \| MfaRequired(factors)`. MFA isn't a failure; it is modelled as one today only because the SDK signals it with an exception. |
| `LoginStep.Mfa`, `LoginRenderingHost` | **Keep** | The hand-off, the per-attempt ViewModel key and the shared `EmbeddedAuthSession` are correct. Pass `MfaConfiguration` through and route `onMfaError`. |
| `MfaFlowHost` | **Evolve** into `ui/MfaHost` | Slot resolution, headless, `collectAsStateWithLifecycle`, `LifecycleEventEffect`, and `paneTitle` per step. |
| `MfaScreens.kt` | **Move** into `factor/push` + `ui/screens` | They become the default slot implementations, driven by the models. |
| `strings.xml` (`embedded_login_mfa_*`) | **Rename** to the frozen key tree | Before GA |
| `LoginFlowViewModel` | **No change** in Milestone 1 | Moving it to the reducer pattern later is consistent, but out of scope |

---

## 7. Security

1. **Typed codes stay out of the Bundle.** `LoginCodeScreen` holds the code in `rememberSaveable`. That writes it to the saved-instance Bundle, which outlives the process, while the flow itself restarts clean, so it is inconsistent too. For MFA, the code and the recovery code live in engine state, in memory only. Apply the same fix to `LoginCodeScreen`. Brief backgrounding still keeps digits, because the ViewModel survives.
2. **No secret ends up in `toString`.** `data class` state that holds a code, a new recovery code or `Credentials` prints them in logs, crash reports and the debugger. Wrap them in `@JvmInline value class Secret(private val value: String) { override fun toString() = "Secret(***)" }`. Keep `Credentials` out of `MfaState` entirely; it only travels in the `Authenticated` effect. Add a unit test that asserts `toString()` of every state contains no fixture secret.
3. **Recovery code display:**
   - Set `FLAG_SECURE` through `DisposableEffect` on the host window, cleared on dispose.
   - In step-up, the `ModalBottomSheet` has **its own window**, so the activity flag doesn't cover it. Use `ModalBottomSheetProperties(securePolicy = SecureFlagPolicy.SecureOn)`.
   - Mark "Copy code" with `ClipDescription.EXTRA_IS_SENSITIVE` (API 33+), so it is hidden from the clipboard preview.
   - The screen can't be dismissed: Back is consumed and the only exit is "I've saved this code".
4. **Recovery code field.** Turn autocorrect and autocapitalisation off (`KeyboardOptions(autoCorrectEnabled = false, capitalization = None, keyboardType = Ascii)`). Check whether we can also disable IME learning (`IME_FLAG_NO_PERSONALIZED_LEARNING`). If Compose doesn't expose it, wrap it.
5. **Masked targets.** Show the server's masked `identifier` as-is and never log it. Polling logs (`Logger.d`) must stay free of identifiers.
6. **Session tokens.** `auth_session` stays in the SDK. `mfa_token` (Milestone 3) stays in the engine's memory only. There is no `SavedStateHandle` anywhere in MFA, by design: after process death the flow starts clean.

---

## 8. Delivery plan (Android)

Every PR leaves `main` releasable. Nothing public is exposed until PR 3.

| PR | Scope | Public API |
|---|---|---|
| **0** (this branch) | Land push MFA as it is, with small fixes: `collectAsStateWithLifecycle`, the resend-cooldown comment (it says it matches email, but it is 5 s against 60 s; settle the value with UX) | none (all `internal`) |
| **1** | Add `explicitApi()` and the binary-compatibility-validator (`apiDump`) to `universal_components`; bump dependencies (9.4) | baseline dump only |
| **2** | Core extraction: `feature/mfa/core`, `MfaReducer` (rounds, lifecycle, clock, verify-another-way), `MfaEngine`, `MfaTransport` + `EmbeddedMfaTransport`, registry with the push descriptor. `NextRound` replaces `ExtraStepUnsupported`. Shared scenario fixtures (9.1). | none |
| **3** | Public surface: `MfaStep` (six cases), models, `MfaConfiguration`, `MfaMessages`, `MfaScreens`, `Headless`, `MfaException`, `onMfaError`. String rename. | **frozen here** |
| **4** | Factor list ordering and masking, the "One more step" header, error-screen rules (`Cancelled` and `NoEligibleFactors` bypass the screen), accessibility (9.2) | none |
| M2-a | **Authenticator app (TOTP)**: builds the code entry screen with no send/resend | none |
| M2-b | **SMS / voice**: adds `SendThenEnterCode`, resend, delivery method | none |
| M2-c | **Email**: reuses M2-b | none |
| M2-d | **Recovery code**: entry, display (`FLAG_SECURE`), confirm | none |
| M3 | `MfaApiTransport`, `MfaStepUpHost`, `withMfaRequired`, coordinator, credentials sink | additive |

TOTP comes first in Milestone 2 because it builds the shared code entry screen without the send/resend complexity. SMS, voice and email then only add the "send first" step.

---

## 9. Quality gates

### 9.1 Shared scenario fixtures (same on iOS and Android)

The requirement wants the same test cases on both platforms, run without UI or network. Write them once as data, and run them against each platform's engine with a fake transport:

```json
{
  "name": "push approved after two pending checks, second round asks for TOTP",
  "offered": [{ "kind": "push", "index": 0, "label": "Pixel 8" }],
  "script": [
    { "intent": "start" },
    { "expect": { "step": "pushWaiting", "sending": true } },
    { "transport": "challenge", "answer": { "challengeSent": { "pollInMs": 2000 } } },
    { "advanceMs": 2000 },
    { "transport": "poll", "answer": { "pending": {} } },
    { "advanceMs": 2000 },
    { "transport": "poll", "answer": { "nextRound": [{ "kind": "otp" }] } },
    { "expect": { "step": "codeEntry", "kind": "otp", "round": 2 } }
  ]
}
```

They live in one place: a small shared repo, consumed as a git submodule or a published test artifact. Android runs them with a parameterised JUnit runner on `runTest` virtual time; iOS runs them with XCTest. This is also the "MFA Core is done when" check.

### 9.2 Accessibility and layout

- On each step change, set `semantics { paneTitle = title }` on the screen container so TalkBack announces it, and mark the title as `heading()`. Moving input focus doesn't move accessibility focus.
- Mark inline errors with `liveRegion = Polite` on the error text, which is today's pattern. Focus stays on the field.
- Countdowns update their content description only every 10 s and at 0.
- Run screenshot tests (Roborazzi or Paparazzi) at 360 dp × `fontScale = 2f` for every default screen.

### 9.3 Architecture guard

Add a Konsist (or ArchUnit) test that `feature.mfa.core..` imports nothing from `androidx..` or `compose..`. That makes "UI-free" checkable. A separate Gradle module isn't practical, because `Credentials` comes from an Android library (AAR).

### 9.4 Dependency bumps needed

| Dependency | Now | Needed for |
|---|---|---|
| lifecycle | 2.6.1 | `LifecycleEventEffect`, `collectAsStateWithLifecycle` (add `lifecycle-runtime-compose`, 2.8.x) |
| compose BOM | 2024.09.00 | SMS OTP autofill (`ContentType.SmsOtpCode`, Compose 1.8+) |
| kotlinx-coroutines | 1.7.3 | aligns with Kotlin 2.2 (1.10.x); test-scheduler fixes |

---

## 10. Open questions

For the SDK and server team:

1. **Factor switching.** After `challengePush(0)`, the `auth_session` rotates. Is `challengeEmail(1)` from the **round's original menu** still accepted on the new session? Or does the post-challenge `next` repeat the other challenges? "Verify another way" depends on this, and so do today's Back → `ChooseFactor` → `SelectFactor` path and the "server can't step back" comment.
2. **Round signal.** Is a new round always `insufficient_authorization` with reason `NONE` and a challenge menu, straight after a verify? Is there any server hint for the "One more step" copy, or is it client-only?
3. **The round cap of 3** is client-side only, because the server has `NoNextSteps` but no round count. Confirm that iOS uses the same cap.
4. **Phone.** `ChallengePhone(index, identifier, methods)` is one action per number with a list of methods. Is that one row with an SMS/voice choice on the code screen ("Call me instead"), or two rows? This needs a UX decision.
5. **Recovery code.** Does `verifyRecoveryCode` always lead to `ConfirmRecoveryCode(newCode)`? Can `confirmRecoveryCode()` itself return `NextRound`?
6. **TOTP** is offered directly as `VerifyOtp(channel = TOTP)` in the menu, with no challenge step. Confirm that it needs no index for users with several authenticator apps.

For the requirement and the iOS team:

7. **The error list is incomplete.** `TooManyLogins`, `IpBlocked`, `AccessDenied`, `ConsentRequired` and `NoNextSteps` exist in the SDK but are missing from `MfaException`. I propose `Unknown(code)`. Also, adding a case to a public sealed type later breaks consumers' exhaustive `when`, so all cases should be fixed now.
8. **`onMfaError` scope.** Does it fire only for `NoEligibleFactors`, or for every terminal MFA failure, so apps can record analytics? I propose every terminal failure, with only `NoEligibleFactors` leaving the component.
9. **Round 2 with one factor.** The list is skipped, so where does "One more step" show? I propose the factor screen's header in that case.
10. **"Adding a factor never touches the Core"** holds only within the three interaction kinds. Passkeys or WebAuthn would add a fourth. Say so in the requirement, so it isn't read as an absolute guarantee.
11. **Copy keys and slot names.** Keep them in one shared keys file that both platforms test against (or generate from), so parity is enforced by CI rather than by review.
