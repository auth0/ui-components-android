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

> **Note:** Embedded Login is in beta and requires **Auth0.Android `5.0.0-beta.1` or later** (beta.0 has no MFA calls, so an app pinned to it fails once MFA starts), a pre-release that replaces the earlier Auth0.Android 3.x dependency. Your tenant must have Embedded Login enabled for your application, with Email OTP on the connection you pass in.

### Display the Embedded Login Screen

`EmbeddedLoginScreen` runs the full native sign-in flow inside your app: email entry, then the one-time code, then a second factor when the account needs one (see [Multi-Factor Authentication](#multi-factor-authentication)). The MFA enrollment components above are unaffected.

It signs in with the `Auth0` account you passed to `Auth0UniversalComponents.initialize(...)`, so initialize the library first. On success it calls `onAuthenticated` with the credentials. It doesn't store them, so save them yourself:

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

On code entry, Back returns to email entry; during MFA, Back works as described in [Multi-Factor Authentication](#multi-factor-authentication). Anywhere else, Back calls `onExit`. To style the screens, pass `themeConfiguration` as you would to the other components (see [Theme Customization](#theme-customization)).

### Flow Behaviour

- **A wrong or expired code appears inline,** under the code field, and the user stays on code entry. After an expired code, **Resend** opens at once.
- **Resend opens 60 seconds after each code is sent;** until then the button counts down.
- **Other failures go full screen:** for example network or server errors, or too many attempts. **Try again** returns to code entry if the code can still be checked, and to email entry (with the email kept) if not. If the first load failed, it loads again.
- **If no code can be sent to the email,** for example because the connection only offers another method for that user, the flow shows its error screen. **Try again** returns to email entry with the email kept.
- **While a request is in flight,** Back and **Edit** are ignored and repeated taps are dropped. This stops a sign-in from finishing after the user has left the screen. (MFA is the exception: see below.)
- **If the connection doesn't offer Email OTP,** the flow shows a screen saying no sign-in methods are available.

### Multi-Factor Authentication

When the code is right but the account needs a second factor, the same screen carries on with MFA. Nothing extra is needed in your app; `onAuthenticated` is called once MFA passes.

- **Supported factor: push notification.** The flow tells the server it can run the emailed code and push only. If none of the factors the tenant offers can be used, the flow shows a "Verification method not available" screen, unless your app takes over (see [Handle MFA Errors](#handle-mfa-errors)).
- **With one push device,** the push is sent straight away, and its screen shows once it's out. **With several,** the user picks one from a list, in the order the tenant offers them; the row spins while its push is sent.
- **While the push waits,** the screen checks with the server at the interval it asks for, kept between 1 and 60 seconds (2 seconds if it gives none), and 2 seconds slower each time the server says it checked too soon. **Resend notification** shows while the server allows another push, and opens after a 60-second wait. **Verify another way** returns to the list; if the user had already approved on the first device, they are signed in. The checks carry on while the app is in the background; a network failure there isn't held against the push, and when the user returns a waiting push is checked at once.
- **When the sign-in policy needs another factor,** the list comes back headed "One more step", with the factors the server offers for that step. Even with just one factor the list shows, so the user sees why they're asked again. There's no limit on the number of steps: the server decides.
- **If a check hits a network error,** it is retried quietly, up to three times. After that, or on any other error (a declined push, an expired request, an ended session, too many attempts), MFA ends on an error screen that says why, and **Start over** returns to email entry with the email kept.
- **Back** returns from the push to the list. From the list, while a push is being sent, when there's only one device, or on the error screen, it returns to email entry with the email kept, and the next code starts a new sign-in. A push that has already left the app can't be recalled, so its notification may still reach the device after the user went back.
- **The sign-in session lives in memory.** If Android ends the app while the user is approving the push, for example in the background, the flow starts again from email entry when the app reopens.

### Handle MFA Errors

When MFA ends without signing the user in, `EmbeddedLoginScreen` calls `onMfaError` once with an `MfaError`, and shows the matching error screen. The callback is optional. It returns an `MfaErrorHandling`, which matters for one error only:

| MFA ends with… | The user sees | `onMfaError` |
| --- | --- | --- |
| `NoEligibleFactors`, and you return `Handled` | A spinner, while your app takes over (nothing flashes by) | Called once |
| `NoEligibleFactors`, and you return `NotHandled` (or set no handler) | The "Verification method not available" screen | Called once, if set |
| Any other error | The MFA error screen; your answer is ignored | Called once, if set |
| The user leaves (Back, **Start over**) | Email entry | **Not called**: leaving isn't a failure |
| Signed in | The success screen | Not called |

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

The `Handled` spinner has no button of its own; **Back** returns to email entry. If you return `Handled`, your app owns what happens next, including when what you opened ends without signing the user in (for example they back out of the browser for Universal Login, or it fails): route them somewhere, or they stay on the spinner until they press Back.

**Always add an `else` branch** when you switch over `MfaError`: a later release can add cases, and Kotlin can't warn about a case it doesn't know yet. The compiler may call the branch redundant today; it is what keeps your app working once a case is added.

The cases are `TokenExpired`, `TokenInvalid`, `Rejected`, `TooManyAttempts`, `NoEligibleFactors`, `Network` and `Unexpected(serverMessage)`. `PresentationUnavailable` exists for a later release and embedded login never produces it. `Cancelled` is never sent to `onMfaError` (leaving isn't a failure); a headless app sees it only as `MfaResult.Ended(Cancelled)`. `MfaError` is an `Exception`, and its `cause` holds the SDK's own error when there is one (for example `error.cause as? EmbeddedAuthException`). Its `message` is null and its `toString()` names the case only, so logging the error itself never writes a server message or a device name. Its `cause` can hold the server's words, so don't show it or send it to analytics as it is. Adding `onMfaError` is source-compatible, but your app must be recompiled against this version.

### Customize MFA

`EmbeddedLoginScreen` takes an optional `mfa = MfaConfiguration(messages, screens, presentation)`. Each part is independent, and you use only what you need:

| Level | You want to… | Use |
| --- | --- | --- |
| 1 | Nothing: the default screens | no `mfa` argument |
| 2 | Know when MFA fails | `onMfaError` ([Handle MFA Errors](#handle-mfa-errors)) |
| 3 | Change the words | `MfaMessages` |
| 4 | Replace one screen | `MfaScreens` |
| 5 | Draw every MFA screen yourself | `MfaPresentation.Headless` |
| 6 | Handle "One more step" | the `round` and `header` on the models |

Adding `mfa` is source-compatible, but your app must be recompiled against this version.

#### Change the words (level 3)

Every word has a default string resource named `auth0_mfa_<group>_<key>`. To change it for the whole app (and translate it), declare a string with the same name in your app. To change one place, or to build text from a value, use `MfaMessages`; anything you leave unset keeps its default:

```kotlin
// Built once: a new MfaMessages on every recomposition would make the words resolve again.
val mfa = remember {
    MfaConfiguration(
        messages = MfaMessages {
            pushChallenge {
                title = "Approve on your phone"
                description = { deviceName -> "We sent a request to $deviceName." }
                resend { cooldown = { seconds -> "Send again in $seconds s" } }
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

The keys follow the same paths as on iOS:

| Path | Takes | Resource name |
| --- | --- | --- |
| `authenticatorList.headers.challenge.{title, description}` | | `auth0_mfa_authenticator_list_headers_challenge_title` |
| `authenticatorList.headers.challengeAdditional.{title, description}` | | `…_challenge_additional_title` |
| `loading.accessibilityLabel` | | `auth0_mfa_loading_accessibility_label` |
| `errors.<case>.{title, description}` for `cancelled`, `tokenExpired`, `tokenInvalid`, `rejected`, `tooManyAttempts`, `noEligibleFactors`, `network`, `unexpected` | | `auth0_mfa_errors_rejected_title` |
| `errors.startOver`, `errors.close` | | `auth0_mfa_errors_start_over` |
| `pushChallenge.row.{title, description}` (the factor in the list: a named device leads, with the title under it; a device with no name shows the title with the description under it) | | `auth0_mfa_push_challenge_row_title` |
| `pushChallenge.title`, `.waiting`, `.verifyAnotherWay`, `.descriptionUnnamed` | | `auth0_mfa_push_challenge_title` |
| `pushChallenge.description` | the device name | `auth0_mfa_push_challenge_description` |
| `pushChallenge.resend.button` | | `auth0_mfa_push_challenge_resend_button` |
| `pushChallenge.resend.cooldown` | the seconds left | `auth0_mfa_push_challenge_resend_cooldown` |

For an unexpected error, the server's own explanation is shown when it gave one, instead of `errors.unexpected.description`.

#### Replace one screen (level 4)

`MfaScreens` replaces the list of factors, the push screen or the error screen; any you leave unset stays standard. Your screen receives the **model** (data and actions) and the **resolved words**, so you need not look anything up:

```kotlin
mfa = MfaConfiguration(
    screens = MfaScreens {
        pushChallenge { model, messages ->
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                Text(messages.pushChallenge.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (model.deviceName.isBlank()) {
                        messages.pushChallenge.descriptionUnnamed
                    } else {
                        messages.pushChallenge.description(model.deviceName)
                    },
                )
                if (model.canResend) {
                    Button(onClick = { model.resend() }, enabled = model.resendAvailableIn == 0 && !model.isResending) {
                        Text(
                            if (model.resendAvailableIn > 0) {
                                messages.pushChallenge.resend.cooldown(model.resendAvailableIn)
                            } else {
                                messages.pushChallenge.resend.button
                            },
                        )
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

- **A replaced screen is drawn inside the login frame** (below its navigation bar), in place of the standard one.
- **Models hold data only.** The push model changes every second while Resend counts down (`resendAvailableIn`), so always draw from the model you are given.
- **Actions are plain functions on the model:** call them (`model.resend()`). Functions only equal themselves, so to check what a model shows in a test, compare its fields rather than whole models.
- **Timers keep running** while your screen is shown: the countdown, the checks and the quiet retries belong to MFA, not to the screen.
- **There is no screen to replace for loading.** `loading.accessibilityLabel` is its only text.
- **The models need only their data**, so they are easy to build for a preview or a test of your own screen; every action defaults to doing nothing.

MFA itself only runs inside `EmbeddedLoginScreen` for now. A standalone MFA component, for an API that answers `mfa_required` (step-up), is planned and not available yet.

#### Draw every MFA screen yourself (level 5)

With `MfaPresentation.Headless`, the library draws no MFA screen. It publishes what to draw, and how MFA ended, in a `MfaStepStream` that **you create and keep** (`remember`), and your own UI draws it over the login:

```kotlin
val stream = remember { MfaStepStream() }
val step by stream.step.collectAsStateWithLifecycle()
val result by stream.result.collectAsStateWithLifecycle()

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
            else -> Unit
        }
        is MfaStep.Error -> MyError(current.model)
        null -> Unit
    }
}
```

- **`step`** is the screen to draw, or `null` when there is nothing to draw: before MFA, after it ended, or when MFA ended because no factor can be used (your `onMfaError` is told; with `Handled` your app takes over, otherwise the login draws its own "not available" screen, so you need not). A new factor adds no `MfaStep` case; it is another `MfaStep.Factor` model, so keep the `else` branch.
- **`result`** is `null` while MFA runs and is cleared when the next MFA starts. It becomes `MfaResult.SignedIn`, or `MfaResult.Ended(error)`: `MfaError.Cancelled` when the user left, otherwise the error. `onAuthenticated` and `onMfaError` are called as usual.
- **Every control is an action on a model.** To leave MFA, call `cancel()`; on the error step, `leave()` returns to email entry.
- **The login frame stays:** its navigation bar and system Back are still there, and Back behaves as in the standard screens. A `BackHandler` you add inside your own UI takes precedence.
- **Rotation is safe:** the flow lives in a ViewModel, and the new stream your `remember` makes after a rotation is filled with the current step and result again. **Process death** starts again from email entry; the stream isn't saved.

#### More than one step (level 6)

When the sign-in policy needs another factor, the list comes back with `header = AuthenticatorListHeader.ChallengeAdditional` and `round` above 1, even with a single factor. `MfaFactorStep.round` tells the same for a factor's own screen. The server decides how many rounds there are; there is no limit.

## Support

For issues and questions:

- [GitHub Issues](https://github.com/auth0/universal-components-android/issues)
- [Auth0 Community](https://community.auth0.com/)
- [Auth0 Support](https://support.auth0.com/)
