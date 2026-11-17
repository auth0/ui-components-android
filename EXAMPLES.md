# Auth0 Android Universal Components - Code Examples

This guide provides practical code examples demonstrating how to use the Auth0 Android Universal Components
SDK in your application.

## Table of Contents

- [Initialization](#initialization)
- [Using Universal Components](#using-universal-components)
- [Theme Customization](#theme-customization)
- [Embedded Login](#embedded-login)
- [Support](#support)


## Initialization

The simplest way to initialise the SDK is by providing the default values:

```kotlin

private val account: Auth0 by lazy {
    Auth0.getInstance(
        getString(R.string.com_auth0_client_id),
        getString(R.string.com_auth0_domain)
    )
}

private val credentialsManager: CredentialsManager by lazy {
    CredentialsManager(
        AuthenticationAPIClient(account),
        SharedPreferencesStorage(this)
    )
}

// Initialize Auth0 Universal Components
Auth0UniversalComponents.initialize(
    context = context,
    account = account,
    tokenProvider = DefaultTokenProvider(credentialsManager),
    scheme = getString(R.string.com_auth0_scheme)
)
```

### CustomTokenProvider

User can provide their own instance of `TokenProvider` to handle token management.

```Kotlin

class CustomTokenProvider : TokenProvider {

    override suspend fun fetchCredentials(): Credentials {
        //  Implementation
    }

    override suspend fun fetchApiCredentials(audience: String, scope: String? = null): APICredentials {
        // Implementation
    }

    override suspend fun saveApiCredentials(audience: String, credentials: APICredentials) {
        // Implementation
    }
}


// Initialize Auth0 Universal Components
Auth0UniversalComponents.initialize(
    context = context,
    account = account,
    tokenProvider = CustomTokenProvider(),
    scheme = getString(R.string.com_auth0_scheme)
)

```

### PasskeyConfiguration

Configuring the `PasskeyConfiguration` class while initializing the SDK will let the user control
the passkey management within the SDK

```Kotlin

val passkeyConfiguration = PasskeyConfiguration(
    credentialManager = credentialManager, // User's instance of AndroidX Credentials manager
    connection = connection, // Your custom Auth0 DB connection
    userIdentity = userIdentity // userIdentity if the user is logged in with a linked account
)

// Initialize Auth0 Universal Components
Auth0UniversalComponents.initialize(
    context = context,
    account = account,
    tokenProvider = CustomTokenProvider(),
    scheme = getString(R.string.com_auth0_scheme),
    passkeyConfiguration = passkeyConfiguration
)

```

## Using Universal Components

The simplest way to add UI components to your app is to call the `AuthenticatorSettingsComponent` composable function from your application. The SDK will handle all the internal navigation.

```kotlin
@Composable
fun MFASettingsScreen() {
    // User application screen composable
    AuthenticatorSettingsComponent()
}
```

## Theme Customization

The SDK supports full theme customization through `Auth0ThemeConfiguration`. You can customize colors, typography, shapes, dimensions, and sizes to match your brand.

The theming system follows the same pattern as Material3's `MaterialTheme`:
- `Auth0Theme { }` — composable that provides theme tokens to all descendants
- `Auth0Theme.colors`, `Auth0Theme.typography`, etc. — accessors for reading tokens inside composables

### Default Theme

When no configuration is provided, the component uses the default Auth0 theme:

```kotlin
@Composable
fun MFASettingsScreen() {
    AuthenticatorSettingsComponent()
}
```

### Custom Brand Colors

Override specific color tokens using `Auth0Color.light().copy(...)`:

```kotlin
@Composable
fun MFASettingsScreen() {
    AuthenticatorSettingsComponent(
        themeConfiguration = Auth0ThemeConfiguration(
            color = Auth0Color.light().copy(
                backgroundPrimary = Color(0xFFFF6B00),       // Brand orange
                textOnPrimary = Color.White
            )
        )
    )
}
```

### Dark Mode

Force dark mode via the `Auth0Theme` composable's `darkTheme` parameter, or apply the dark color scheme explicitly:

```kotlin
// Option 1: Force dark mode via Auth0Theme
@Composable
fun MFASettingsScreen() {
    Auth0Theme(darkTheme = true) {
        AuthenticatorSettingsComponent()
    }
}

// Option 2: Explicit dark color scheme
@Composable
fun MFASettingsScreen() {
    AuthenticatorSettingsComponent(
        themeConfiguration = Auth0ThemeConfiguration(
            color = Auth0Color.dark()
        )
    )
}
```

### Custom Shapes

Customize corner radii across all components:

```kotlin
@Composable
fun MFASettingsScreen() {
    AuthenticatorSettingsComponent(
        themeConfiguration = Auth0ThemeConfiguration(
            shapes = Auth0Shapes(
                none = RoundedCornerShape(0.dp),
                extraSmall = RoundedCornerShape(8.dp),
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(18.dp),
                large = RoundedCornerShape(24.dp),
                extraLarge = RoundedCornerShape(32.dp),
                full = RoundedCornerShape(100.dp)
            )
        )
    )
}
```

### Full Customization

Combine colors, typography, and shapes for complete brand control:

```kotlin
@Composable
fun MFASettingsScreen() {
    AuthenticatorSettingsComponent(
        themeConfiguration = Auth0ThemeConfiguration(
            color = Auth0Color.light().copy(
                backgroundPrimary = Color(0xFF0066CC),
                textOnPrimary = Color.White,
                backgroundLayerBase = Color(0xFFF5F5F5),
                backgroundLayerMedium = Color.White,
                textBold = Color(0xFF1F1F1F),
                textDefault = Color(0xFF636363),
                backgroundError = Color(0xFFFF4444),
                backgroundSuccess = Color(0xFF00CC66),
                borderDefault = Color(0xFFE0E0E0)
            ),
            typography = Auth0Typography.default().copy(
                displayMedium = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                ),
                body = TextStyle(
                    fontSize = 18.sp
                )
            ),
            shapes = Auth0Shapes(
                none = RoundedCornerShape(0.dp),
                extraSmall = RoundedCornerShape(4.dp),
                small = RoundedCornerShape(8.dp),
                medium = RoundedCornerShape(12.dp),
                large = RoundedCornerShape(16.dp),
                extraLarge = RoundedCornerShape(24.dp),
                full = RoundedCornerShape(100.dp)
            )
        )
    )
}
```

### Accessing Theme Tokens in Custom Composables

Use `Auth0Theme.colors`, `Auth0Theme.typography`, `Auth0Theme.shapes`, `Auth0Theme.dimensions`, and `Auth0Theme.sizes` to read theme tokens inside any composable wrapped by `Auth0Theme`:

```kotlin
@Composable
fun CustomAuthCard() {
    Card(
        shape = Auth0Theme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = Auth0Theme.colors.backgroundLayerMedium
        )
    ) {
        Column(modifier = Modifier.padding(Auth0Theme.dimensions.spacingMd)) {
            Text(
                text = "Authenticator",
                style = Auth0Theme.typography.title,
                color = Auth0Theme.colors.textBold
            )
            Text(
                text = "Enabled",
                style = Auth0Theme.typography.bodySmall,
                color = Auth0Theme.colors.textDefault
            )
        }
    }
}
```

### Using Auth0Theme Directly

Wrap custom composables with `Auth0Theme` to provide theme tokens without using `AuthenticatorSettingsComponent`:

```kotlin
@Composable
fun BrandedScreen() {
    Auth0Theme(
        configuration = Auth0ThemeConfiguration(
            color = Auth0Color.light().copy(
                backgroundPrimary = Color(0xFFFF6B00)
            )
        )
    ) {
        // All descendants can access Auth0Theme.colors, Auth0Theme.typography, etc.
        CustomAuthCard()
    }
}
```

### Dynamic Theme Switching

Hold the theme configuration in state to switch themes at runtime:

```kotlin
@Composable
fun MFASettingsScreen() {
    var isDarkMode by remember { mutableStateOf(false) }

    val themeConfig = Auth0ThemeConfiguration(
        color = if (isDarkMode) Auth0Color.dark() else Auth0Color.light()
    )

    Column {
        Switch(
            checked = isDarkMode,
            onCheckedChange = { isDarkMode = it }
        )
        AuthenticatorSettingsComponent(themeConfiguration = themeConfig)
    }
}
```

## Embedded Login

> **Note:** Embedded Login is in beta and requires **Auth0.Android `5.0.0-beta.2` or later**, a pre-release that replaces the earlier Auth0.Android 3.x dependency. Your tenant must have Embedded Login enabled for your application, with Email OTP, and optionally Phone (SMS) OTP with a phone provider, a password or a username on the connection you pass in.

### Display the Embedded Login Screen

`EmbeddedLoginScreen` runs the full native sign-in flow inside your app: the first method the tenant lists (an email, a phone number or a username, with a button for each other method), then the one-time code or the password, then multi-factor authentication when the tenant's policy asks for more. It signs in with the `Auth0` account you passed to `Auth0UniversalComponents.initialize(...)`, so initialize the library first. The library stores nothing: when the user is signed in it hands the credentials to `onAuthenticated`, once, and your app keeps them where it keeps its session:

```kotlin
@Composable
fun LoginScreen(
    credentialsManager: CredentialsManager,
    onSignedIn: () -> Unit,
    onBack: () -> Unit
) {
    EmbeddedLoginScreen(
        connection = "Username-Password-Authentication",
        onAuthenticated = { credentials ->
            credentialsManager.saveCredentials(credentials)
            onSignedIn()
        },
        onExit = onBack
    )
}
```

On code or password entry, Back returns to the entry it came from, and a method opened from the first screen returns to the first screen; during MFA, Back works as described below. Anywhere else, Back calls `onExit`. To style the screens, pass `themeConfiguration` as you would to the other components (see [Theme Customization](#theme-customization)).

### Sign in with a Password

`EmbeddedLoginScreen` also signs users in with a password, with nothing to configure in your app. What the user sees follows what the tenant offers on the connection you pass in:

- **The first screen is the first method the tenant lists**: email, phone number or username. Every other method is a button under **OR**, in the tenant's order ("Continue with Email", "Continue with Phone", "Continue with Password"), and it opens that method's own screen. **Back** returns to the first screen.
- **A username** goes straight to the password screen. A tenant that offers a username but has no password step for it shows "Incorrect username or password. Please try again." under the field, the same words as a wrong password, so the screen never says whether the account exists. A username the server refuses outright shows the server's own words on an error screen, as on iOS, and **Try again** returns to the username screen.
- **An email** is sent to the server first, and whichever of a code and a password the server lists first leads (unless you pass `preference`, below). With a code first, it is sent at once and the code screen offers **Continue with a password** under **Resend**. With the password first, nothing is sent and the password screen offers **Email me a code** under **Continue**.
- **A phone number** is offered its code only, whatever the server lists: a password for a phone number isn't supported yet. It is sent to the server first, and the code goes out at once by text message (or a call, when that is all the server can do for the number). A number the server offers no code for shows "We couldn't send your code. Please confirm the phone number is correct and try again." under the field.
- **The password screen** shows who is signing in, with **Edit**, and a password field that hides what is typed (an eye shows it; it is hidden again when a request starts and as soon as the app is paused). While the password is readable, the Activity's window is kept out of screenshots, screen recordings and the recent-apps preview (`FLAG_SECURE`; a Dialog or bottom sheet that holds the login has a window of its own, so set the flag there yourself). The password is sent exactly as typed, with nothing trimmed, and the field is emptied at every **Continue**. A rotation keeps what is typed; it is held in memory only, never in the saved instance state, so it is gone if Android ends the app. A wrong password stays on the screen, inline, and the same sign-in is used for the next try.
- **A code asked for from the password screen** waits as long as **Resend** does, counted from the last code sent to the same person, even when they went back and came forward again: the button reads "Email me a code in 42s" until it opens.
- **Too many attempts** stays on the screen the user was on, with "Too many attempts. Please try again later." under the field. The next try opens a new sign-in quietly. If that new sign-in no longer offers a password, an email returns to its screen with "A password isn't available for this account. Please use a code instead."; a username reads as a wrong password.
- **A password found in a data breach** shows a screen with its own message; **Try again** returns to the password screen.
- **A second factor after the password** moves into MFA in place, as after a code. **Start over** returns to the first screen.

**To choose instead of following the server,** pass `preference`. `EmbeddedLoginPreference.Server` (the default) follows the order above; `Code` always leads with the code and `Password` with the password, when the server offers both for an email. The other is still one tap away, and nothing changes when only one of the two is offered, or for a username (password only) or a phone number (code only). Changing `preference` starts the sign-in again.

```kotlin
EmbeddedLoginScreen(
    connection = "Username-Password-Authentication",
    onAuthenticated = { credentials -> /* save them */ },
    onExit = onBack,
    preference = EmbeddedLoginPreference.Password
)
```

Not included: a password for a phone number, **Forgot password**, and password-manager autofill.

### Multi-Factor Authentication

When the tenant's MFA policy asks for more than the emailed code, `EmbeddedLoginScreen` moves into MFA in place; nothing extra is needed in your app. Today it runs a push notification, a code sent to a phone (by text message or voice call) or to an email address, the code from an authenticator app, or a recovery code. With several factors, the user picks one from a list first, and the recovery code is listed last. Other factors aren't supported in this flow yet.

- **Approved, or the code is right:** the credentials go to `onAuthenticated`. If the policy asks for another factor, the list returns headed **One more step**, with no limit on the number of steps.
- **A code sent to a phone** says where it went by the end of the number, not the masked number: **Sent to the phone number ending in 900** for a text and **Calling your phone number ending in 900** for a call. The digits are the ones the server shows (up to four); when it shows none, the screen says **We texted a code to your phone.** or **We're calling your phone with a code.** `phoneChallenge { description = … }` and `descriptionVoice` reword them, and still receive the number as the server hides it. To change them with a string resource instead, declare `auth0_mfa_phone_challenge_description_ending_in` and `auth0_mfa_phone_challenge_description_voice_ending_in`, where `%1$s` is those digits (these replace `auth0_mfa_phone_challenge_description` and `_voice`, whose `%1$s` was the masked number). The factor list is unchanged.
- **The sign-in code screen for a phone** names the number as the server hides it, taken from its answer once the code is sent ("From ***-***-**0128"); when the server names none it shows the number the user typed. An email is shown as typed.
- **A wrong code** (phone, email or authenticator app) stays on the screen, inline, so the user can type another. **Resend** opens 60 seconds after each send. The authenticator app has nothing to send, so its screen opens at once and has no **Resend**.
- **No answer while a typed code is being checked** (phone, email or authenticator app) stays on the screen with a "No connection" line under the boxes, and the same code can be sent again. It may be a dropped connection or a timeout after the server already had the code, so a second try can also be refused as wrong. For a recovery code that is worse: the server may already have used the code up, so the user is told it is wrong and never sees the new one. The line goes away as soon as the user edits the code, and comes back only with the next answer.
- **A recovery code** opens its screen at once, with an empty field that hides what is typed (an eye shows it); nothing is sent until the user taps **Continue** (or the keyboard's Done). Before it is sent the code is cleaned: invisible characters that come with pasted text (zero-width spaces, direction marks) and the spaces around it are removed, while case, hyphens and spaces inside it are sent as typed. A wrong code stays on the screen with its own line, which goes away as soon as the field changes. Using a code replaces it, so the screen then shows the new one, with a copy button and a note to store it somewhere safe. **Continue** stays off until the user ticks **I have saved this recovery code**, then confirms the new code and signs in (or asks for one more factor). A dropped connection while checking or confirming keeps the screen, and the same step can be tried again; any other failure ends MFA.
- **The screen with the new recovery code** is kept out of screenshots, screen recordings and the recent-apps preview (`FLAG_SECURE`, set on the Activity's window, also when your app replaced the screen or draws MFA itself; a Dialog or bottom sheet that holds the login has a window of its own, so set the flag there yourself). The user is not held there: Back and Cancel leave and start the sign-in over, and the new code is lost with them, since it is the only copy. Leaving while a typed recovery code is being checked loses it the same way: the check uses the code up, and one that has already gone out can't be recalled, so its answer is dropped. Back waits while the new code is being confirmed. If Android ends your app while the new code is on screen, it is lost with the sign-in session.
- **Copying the new recovery code** marks the copy as sensitive, so the system keeps it out of the clipboard preview, and takes it off the clipboard after two minutes. The app does the removing, so it only happens while the app's process is alive; a screen you draw yourself gets neither, so copy with the same care.
- **A code that expired** (phone or email), when the server offers another, stays on the screen with "Your code has expired. Resend another code and try again." Verify waits and **Resend** is open at once.
- **A resend that gets no answer, or is refused as too many,** keeps the code already on the screen, with "No connection" or "Too many attempts" under the boxes. **Resend** opens again after the usual wait.
- **Declined, session ended, too many attempts, a first code that could not be sent, or no connection at any other time:** a screen with its own message and **Start over**, which returns to the first screen, with what was typed kept when it is the same method.
- **Verify another way** (several factors) returns to the list. **Back** does the same from a factor screen, and returns to the first screen from the list, or from a factor screen in the first step when there is only one factor. While a code or a push is being sent again, a typed code is being checked, or the new recovery code is being confirmed, it does nothing. On the screen with the new recovery code it leaves, as Back does.
- **No factor this flow can run:** a **Verification method not available** screen shows, unless your app takes over with `onMfaError`.
- **Push checks right away** when the app returns to the foreground, so approving on the same phone signs in without waiting for the next check.

#### Handle MFA Errors

`onMfaError` is called once when MFA ends without signing the user in, except when the user leaves. It returns an `MfaErrorHandling`, which matters for `NoEligibleFactors` only: return `Handled` when your app takes over, and the user sees a spinner instead of the error; return `NotHandled` (or set no handler) and the screen shows. Every other error shows its own screen, and your answer is ignored.

A good use is falling back to Universal Login when none of the tenant's factors can be run in the app:

```kotlin
EmbeddedLoginScreen(
    connection = "Username-Password-Authentication",
    onAuthenticated = { credentials -> /* save them */ },
    onExit = onBack,
    onMfaError = { error ->
        when (error) {
            is MfaError.NoEligibleFactors -> {
                startUniversalLogin()
                MfaErrorHandling.Handled
            }
            else -> MfaErrorHandling.NotHandled
        }
    },
)
```

If you return `Handled`, your app owns what happens next, including when what you opened ends without signing the user in: route them somewhere, or they stay on the spinner until they press Back.

**Always add an `else` branch** when you switch over `MfaError`: a later release can add cases. The cases are `TokenExpired`, `TokenInvalid`, `Rejected`, `TooManyAttempts`, `NoEligibleFactors`, `Network` and `Unexpected(serverMessage)`; `Cancelled` and `PresentationUnavailable` are never sent to `onMfaError`. `MfaError` is an `Exception` whose `cause` holds the SDK's own error when there is one. That cause can carry the server's words, so don't show it or send it to analytics as it is.

#### Change the words

Every word has a default string resource named `auth0_mfa_<group>_<key>`, for example `auth0_mfa_push_challenge_title`. To change it for the whole app (and translate it), declare a string with the same name in your app. To change one place, or to build text from a value such as the device name, use `MfaMessages`; anything you leave unset keeps its default. The groups are `authenticatorList`, `loading`, `errors`, `pushChallenge`, `phoneChallenge`, `emailChallenge`, `totpChallenge` and `recoveryCodeChallenge`.

```kotlin
// Built once: a new MfaMessages on every recomposition would make the words resolve again.
val mfa = remember {
    MfaConfiguration(
        messages = MfaMessages {
            pushChallenge {
                title = "Approve on your phone"
                description = { deviceName -> "We sent a request to $deviceName." }
            }
            phoneChallenge {
                resend { cooldown = { seconds -> "Send again in $seconds s" } }
            }
            emailChallenge {
                // The address as the server hides it, such as j***@example.com. When it gives none, the line
                // under the title is `descriptionUnnamed`.
                description = { address -> "We emailed a code to $address." }
            }
            totpChallenge {
                // It names no app: the user's may be any.
                description = "Open your authenticator app"
            }
            recoveryCodeChallenge {
                saveDescription = "This code works only once. Keep it for when you lose your device."
                savedConfirmation = "I have stored this code"
            }
            errors { rejected { title = "Sign-in denied" } }
        },
    )
}

EmbeddedLoginScreen(
    connection = "Username-Password-Authentication",
    onAuthenticated = { credentials -> /* save them */ },
    onExit = onBack,
    mfa = mfa,
)
```

#### Replace one screen

A replaced screen only renders its model and calls its actions, so the countdowns, checks and cancel keep working underneath. It receives the **model** (data and actions) and the **resolved words**, so you need not look anything up:

```kotlin
mfa = MfaConfiguration(
    screens = MfaScreens {
        pushChallenge { model, messages ->
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                Text(messages.pushChallenge.title, style = MaterialTheme.typography.headlineSmall)
                if (model.canResend) {
                    Button(onClick = { model.resend() }, enabled = model.resendAvailableIn == 0 && !model.isResending) {
                        Text(messages.pushChallenge.resend.button)
                    }
                }
                if (model.canSwitch) {
                    TextButton(onClick = { model.switchFactor() }) { Text(messages.pushChallenge.verifyAnotherWay) }
                }
                TextButton(onClick = { model.cancel() }) { Text("Cancel") }
            }
        }
    },
)
```

`canResend` says whether the server still allows another send. `resendAvailableIn` is the seconds left before it may be sent, and 0 when it can be tapped now. The flow owns that countdown, so calling `resend()` early does nothing; a replacement only has to show it (the model is published again each second while it counts down). On a code screen (phone, email or authenticator app), `submit(code)` does nothing unless the code is exactly 6 digits, so a replacement may enable its button on any input. The authenticator app's model (`TotpChallengeModel`) has no destination and no resend, since nothing is sent. The three code models share `CodeChallengeModel`, and the two that send a code (phone, email) share `SentCodeChallengeModel`, so one screen of your own can serve them all. `requestError` is `MfaError.Network` when the last check or resend got no answer (the code is untouched and `submit` may send it again), or `MfaError.TooManyAttempts` when a resend was refused; draw the factor's `noConnection` or `tooManyRequests` words (or `requestError?.let { messages.errors.messageFor(it) }`). `isCodeExpired` is true when the code timed out and another can be sent: `submit` does nothing until Resend brings it. Both are cleared by the next request. The standard screens also hide the wrong-code, no-connection and too-many lines as soon as the user edits the code (an expired line stays, because Verify waits for a new code whatever is typed); a screen of your own can do the same.

The recovery code screen (`recoveryCodeChallenge`) has one model for both of its steps, and it is not a `CodeChallengeModel`: a recovery code is text of any length, not six digits. While `newCode` is null it takes a code through `submit(code)` (it is cleaned before it is sent, and a blank one is ignored); once the server accepts it, `newCode` is set: show it so the user can save it, and call `confirm()` when they have, which signs them in. The standard screen holds Continue back until its "I have saved this recovery code" box is ticked; a replacement decides that for itself. On that step `switchFactor()` does nothing, and `cancel()` leaves, which loses the new code. `isWrongCode` and `requestError` (`MfaError.Network`, when the last request got no answer and the same step can be tried again) say what to show, and the words are under `recoveryCodeChallenge`. `newCode` is the live recovery code: never log it, and don't print or dump the model or the step, whose descriptions leave it out. The SDK client's request logging would write the typed code and the new one to the log, so keep it off in release builds.

The factor list (`authenticatorList`), the phone code screen (`phoneChallenge`), the email code screen (`emailChallenge`), the authenticator app screen (`totpChallenge`), the recovery code screen (`recoveryCodeChallenge`) and the error screen (`error`) are replaced the same way. There is no screen to replace for loading. Models hold data only, and every action defaults to doing nothing, so they are easy to build for a preview or a test of your own screen. Build the words with `ResolvedMfaMessages.from(context)`, passing `MfaMessages { ... }` as the second argument to include your own.

#### Draw every MFA screen yourself (headless)

With `MfaPresentation.Headless`, the flow still decides everything: which factors to offer, when to check, when to give up, and when to hand the credentials to your app. It only publishes each step into a `MfaStepStream` that **you create and keep** (`remember`), and your own UI draws it over the login:

```kotlin
val stream = remember { MfaStepStream() }
val step by stream.step.collectAsStateWithLifecycle()

Box {
    EmbeddedLoginScreen(
        connection = "Username-Password-Authentication",
        onAuthenticated = { credentials -> /* save them */ },
        onExit = onBack,
        mfa = MfaConfiguration(presentation = MfaPresentation.Headless(stream)),
    )
    when (val current = step) {
        is MfaStep.Loading -> MyLoading()
        is MfaStep.AuthenticatorList -> MyList(current.model)
        is MfaStep.Factor -> when (val model = current.step.model) {
            is PushChallengeModel -> MyPush(model)
            is PhoneChallengeModel -> MyPhone(model)
            is EmailChallengeModel -> MyEmail(model)
            is TotpChallengeModel -> MyTotp(model)
            is RecoveryCodeChallengeModel -> MyRecoveryCode(model)
            else -> Unit
        }
        is MfaStep.Error -> MyError(current.model)
        null -> Unit
        else -> Unit
    }
}
```

- An `MfaStep` is `Loading`, `AuthenticatorList`, `Error`, or `Factor(step)` for a factor's own screen: read `step.factor` (for example `MfaFactorId.Push`) and check `step.model` for that factor's model. New factors add no case, so keep the `else` branch.
- `stream.step` is `null` when there is nothing to draw: before MFA, after it ends, or when no factor can be used. Your overlay goes away and the login's own screen shows.
- `stream.result` is `MfaResult.SignedIn` or `MfaResult.Ended(error)` when MFA ends, and goes back to `null` when the next MFA starts.
- On the recovery code step the login still sets `FLAG_SECURE` on the Activity's window while `RecoveryCodeChallengeModel.newCode` is not null, so your own screen is kept out of screenshots too.
- The login still draws its own background, navigation bar and Back; only the MFA steps are yours. Every control is an action on a model; on the error step, `leave()` returns to the first screen.

Adding `onMfaError`, `mfa` or `preference` is source-compatible, but your app must be recompiled against this version.

### Flow Behaviour

- **A wrong or expired code appears inline,** under the code field, and the user stays on code entry. After an expired code, **Resend** opens at once.
- **Resend opens 60 seconds after each code is sent;** until then the button counts down.
- **Other failures go full screen:** for example network or server errors. **Try again** returns to where the failure came from: code or password entry after a failed check, and email, phone or username entry (with what was typed kept) after a request that did not go through. If the first load failed, it loads again. **Too many attempts** is the exception: it stays on the screen the user was on, with its message under the field.
- **While a request is in flight,** Back and **Edit** are ignored and repeated taps are dropped. This stops a sign-in from finishing after the user has left the screen. During MFA, Back is ignored only while a push or code is being sent again or a typed code is being checked.
- **If the connection offers no way to start with an email, a phone number or a username,** the flow shows a screen saying no sign-in methods are available.
- **Phone sign-in:** the user picks the country and types the number, which is sent in international form (for example `+14233530900`). The code arrives by text message, or by a call when that is all the server can do for the number. With several phones listed, the first that can take a text gets it. The country starts as the one the device is set to (the United States if the list has none like it), and the one the user picked is kept for Edit and Try again. When the tenant offers other methods too, the one it lists first comes first: the phone screen is reached from "Continue with Phone" and Back returns to the first screen. When phone is the only method, it is the first screen and Back calls `onExit`.
- **The sign-in session lives in memory.** Rotation is safe, but if Android ends the app while the user is signing in, the flow starts again from the first screen when the app reopens.

## Support

For issues and questions:

- [GitHub Issues](https://github.com/auth0/universal-components-android/issues)
- [Auth0 Community](https://community.auth0.com/)
- [Auth0 Support](https://support.auth0.com/)
