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

> **Note:** Embedded Login is in beta and requires **Auth0.Android `5.0.0-beta.0` or later**, a pre-release that replaces the earlier Auth0.Android 3.x dependency. Your tenant must have Embedded Login enabled for your application, with Email OTP on the connection you pass in.

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

On code entry, Back returns to email entry. Anywhere else, Back calls `onExit`. To style the screens, pass `themeConfiguration` as you would to the other components (see [Theme Customization](#theme-customization)).

### Flow Behaviour

- **A wrong or expired code appears inline,** under the code field, and the user stays on code entry. After an expired code, **Resend** opens at once.
- **Resend opens 60 seconds after each code is sent;** until then the button counts down.
- **Other failures go full screen:** for example network or server errors, or too many attempts. **Try again** returns to code entry if the code can still be checked, and to email entry (with the email kept) if not. If the first load failed, it loads again.
- **If no code can be sent to the email,** for example because the connection only offers another method for that user, the flow shows its error screen. **Try again** returns to email entry with the email kept.
- **While a request is in flight,** Back and **Edit** are ignored and repeated taps are dropped. This stops a sign-in from finishing after the user has left the screen.
- **If the connection doesn't offer Email OTP,** the flow shows a screen saying no sign-in methods are available.

### Multi-Factor Authentication

When the code is right but the account needs a second factor, the same screen carries on with MFA. Nothing extra is needed in your app; `onAuthenticated` is called once MFA passes.

- **Supported factor: push notification.** Other second factors aren't supported yet; if the tenant offers only those, the flow shows its error screen.
- **With one push device,** the push is sent straight away. **With several,** the user picks one from a list, in the order the tenant offers them.
- **While the push waits,** the screen checks with the server at the interval it asks for. **Resend Code** sends the push again after a 60-second wait. **Choose another device** returns to the list.
- **Back** returns from the push to the list. From the list, or when there's only one device, it returns to email entry with the email kept, and the next code starts a new sign-in.
- **A declined push, an expired request, or an account with no usable factor** goes full screen. **Try again** starts the sign-in over, unless the failure was a network or server error, in which case it sends the push again.

## Support

For issues and questions:

- [GitHub Issues](https://github.com/auth0/universal-components-android/issues)
- [Auth0 Community](https://community.auth0.com/)
- [Auth0 Support](https://support.auth0.com/)
