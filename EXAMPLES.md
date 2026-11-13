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

> **Note:** Embedded Login is in beta and requires **Auth0.Android `5.0.0-beta.1` or later**, a pre-release that replaces the earlier Auth0.Android 3.x dependency. Your tenant must have Embedded Login enabled for your application, with Email OTP, and optionally Phone (SMS) OTP with a phone provider, on the connection you pass in.

### Display the Embedded Login Screen

`EmbeddedLoginScreen` runs the full native sign-in flow inside your app: email entry (with "Continue with Phone" when the tenant also offers a phone number), then the one-time code, then multi-factor authentication when the tenant's policy asks for more. It signs in with the `Auth0` account you passed to `Auth0UniversalComponents.initialize(...)`, so initialize the library first. The library stores nothing: when the user is signed in it hands the credentials to `onAuthenticated`, once, and your app keeps them where it keeps its session:

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

On code entry, Back returns to the email or phone entry it came from, and when the tenant offers email too, phone entry returns to email entry; during MFA, Back works as described below. Anywhere else, Back calls `onExit`. To style the screens, pass `themeConfiguration` as you would to the other components (see [Theme Customization](#theme-customization)).

### Multi-Factor Authentication

When the tenant's MFA policy asks for more than the emailed code, `EmbeddedLoginScreen` moves into MFA in place; nothing extra is needed in your app. Today it runs a push notification, or a code sent to a phone (by text message or voice call) or to an email address. With several factors, the user picks one from a list first. Other factors aren't supported in this flow yet.

- **Approved, or the code is right:** the credentials go to `onAuthenticated`. If the policy asks for another factor, the list returns headed **One more step**, with no limit on the number of steps.
- **A wrong phone or email code** stays on the screen, inline, so the user can type another. **Resend** opens 60 seconds after each send.
- **Declined, expired, session ended, too many attempts or no connection:** a screen with its own message and **Start over**, which returns to email entry with the email kept.
- **Verify another way** (several factors) returns to the list. **Back** does the same from a factor screen, and returns to email entry from the list, or from a factor screen in the first step when there is only one factor. While a code or a push is being sent again, or a typed code is being checked, it does nothing.
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

Every word has a default string resource named `auth0_mfa_<group>_<key>`, for example `auth0_mfa_push_challenge_title`. To change it for the whole app (and translate it), declare a string with the same name in your app. To change one place, or to build text from a value such as the device name, use `MfaMessages`; anything you leave unset keeps its default. The groups are `authenticatorList`, `loading`, `errors`, `pushChallenge`, `phoneChallenge` and `emailChallenge`.

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

`canResend` says whether the server still allows another send. `resendAvailableIn` is the seconds left before it may be sent, and 0 when it can be tapped now. The flow owns that countdown, so calling `resend()` early does nothing; a replacement only has to show it (the model is published again each second while it counts down). On a code screen (phone or email), `submit(code)` does nothing unless the code is exactly 6 digits, so a replacement may enable its button on any input.

The factor list (`authenticatorList`), the phone code screen (`phoneChallenge`), the email code screen (`emailChallenge`) and the error screen (`error`) are replaced the same way. There is no screen to replace for loading. Models hold data only, and every action defaults to doing nothing, so they are easy to build for a preview or a test of your own screen. Build the words with `ResolvedMfaMessages.from(context)`, passing `MfaMessages { ... }` as the second argument to include your own.

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
- The login still draws its own background, navigation bar and Back; only the MFA steps are yours. Every control is an action on a model; on the error step, `leave()` returns to email entry.

Adding `onMfaError` or `mfa` is source-compatible, but your app must be recompiled against this version.

### Flow Behaviour

- **A wrong or expired code appears inline,** under the code field, and the user stays on code entry. After an expired code, **Resend** opens at once.
- **Resend opens 60 seconds after each code is sent;** until then the button counts down.
- **Other failures go full screen:** for example network or server errors, or too many attempts. **Try again** returns to code entry if the code can still be checked, and to email or phone entry (with what was typed kept) if not. If the first load failed, it loads again.
- **While a request is in flight,** Back and **Edit** are ignored and repeated taps are dropped. This stops a sign-in from finishing after the user has left the screen. During MFA, Back is ignored only while a push or code is being sent again or a typed code is being checked.
- **If the connection offers neither Email OTP nor Phone OTP,** the flow shows a screen saying no sign-in methods are available.
- **Phone sign-in:** the user picks the country and types the number, which is sent in international form (for example `+14233530900`). The code arrives by text message, or by a call when that is all the server can do for the number. The country starts as the one the device is set to (the United States if the list has none like it), and the one the user picked is kept for Edit and Try again. When the tenant offers email too, email comes first: the phone screen is reached from "Continue with Phone" and Back returns to email. When phone is the only method, it is the first screen and Back calls `onExit`.
- **The sign-in session lives in memory.** Rotation is safe, but if Android ends the app while the user is signing in, the flow starts again from email entry when the app reopens.

## Support

For issues and questions:

- [GitHub Issues](https://github.com/auth0/universal-components-android/issues)
- [Auth0 Community](https://community.auth0.com/)
- [Auth0 Support](https://support.auth0.com/)
