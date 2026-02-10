# Auth0 Android UI Components - Code Examples

This guide provides practical code examples demonstrating how to use the Auth0 Android UI Components
SDK in your application.

## Table of Contents

- [Initialization](#initialization)
- [Using UI Components](#using-ui-components)
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

// Initialize Auth0 UI Components
Auth0UI.initialize(
    account = account,
    tokenProvider = DefaultTokenProvider(credentialsManager),
    scheme = getString(R.string.com_auth0_scheme)
)
```

### CustomTokenProvider

User can provide their own instance of `TokenProvider` to handle token management.

```Kotlin

class CustomTokenProvider : TokenProvider {

    suspend fun fetchCredentials(): Credentials {
        //  Implementation
    }

    suspend fun fetchApiCredentials(audience: String, scope: String? = null): APICredentials {
        // Implementation
    }

    suspend fun saveApiCredentials(audience: String, credentials: APICredentials) {
        // Implementation
    }
}


// Initialize Auth0 UI Components
Auth0UI.initialize(
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
    credentialManager = credentialManager, // User's instance of credentials manager
    connection = connection, // Your custom Auth0 DB connection
    userIdentity = userIdentity // userIdentity if the user is logged in with a linked account
)

// Initialize Auth0 UI Components
Auth0UI.initialize(
    account = account,
    tokenProvider = CustomTokenProvider(),
    scheme = getString(R.string.com_auth0_scheme),
    passkeyConfiguration = passkeyConfiguration
)

```

## Using UI Components

The simplest way to add UI components to your app is to call the `AuthenticatorSettingsComponent` method from your application. The SDK will handle all the internal navigation.

```kotlin
@Composable
fun MFASettingsScreen() {
    // User application screen composable
    AuthenticatorSettingsComponent()
}
```

## Support

For issues and questions:

- [GitHub Issues](https://github.com/auth0/ui-components-android/issues)
- [Auth0 Community](https://community.auth0.com/)
- [Auth0 Support](https://support.auth0.com/)
