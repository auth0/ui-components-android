
![Auth0 Android UI Components](https://cdn.auth0.com/website/sdks/banners/ui-components-android.png) 

[![Maven Central](https://img.shields.io/maven-central/v/com.auth0.android/ui-components.svg?style=flat-square)](https://search.maven.org/artifact/com.auth0.android/ui-components)
[![License](https://img.shields.io/:license-Apache%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)

📚 [Documentation](#documentation) • 🚀 [Getting Started](#getting-started) • 💻 [Sample App](#run-the-sample-app) • 💬 [Feedback](#feedback)

## Documentation

Composable UI building blocks for MFA enrollment and verification on Android, built with Jetpack Compose. This library provides ready-to-use components that integrate seamlessly with Auth0's authentication flows.

- [Sample App](https://github.com/atko-cic/ui-components-android/tree/main/app)
- [API Documentation](#) <!-- Add link when available -->

## Features

This library provides ready-to-use UI components for multi-factor authentication:

- 🔐 **TOTP (Time-based One-Time Password)** - Authenticator app support with QR code enrollment
- 📱 **Push Notifications** - Secure push-based authentication
- 💬 **SMS OTP** - Phone number verification via one-time codes
- 📧 **Email OTP** - Email-based verification
- 🔑 **Recovery Codes** - Backup authentication codes for account recovery

All components are built on top of the [Auth0 Android SDK](https://github.com/auth0/Auth0.Android) and integrate with Auth0's My Account APIs.

> ⚠️ **My Account APIs Required** - This SDK requires [My Account APIs](https://auth0.com/docs/manage-users/my-account-api) which are currently in early access. Please reach out to Auth0 support to enable My Account APIs for your tenant.

> ⚠️ **BETA RELEASE** - This SDK is currently in beta. APIs may change before the stable release.

## Getting Started

### Requirements

Android API version 30 or later and Java 17+.

Here's what you need in `build.gradle` to target Java 17 byte code for Android and Kotlin plugins respectively:

```groovy
android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = '17'
    }
}
```

**Build Configuration:**
- Gradle 8.13+ with AGP 8.11+
- Kotlin 2.2.20
- Jetpack Compose 

### Installation

Add the dependency to your `build.gradle` file:

```gradle
dependencies {
    implementation 'com.auth0.android:ui-components:1.0.0'
}
```

<details>
  <summary>Using Version Catalog</summary>

Add to your `gradle/libs.versions.toml`:

```toml
[versions]
auth0-ui-components = "1.0.0"

[libraries]
auth0-ui-components = { module = "com.auth0.android:ui-components", version.ref = "auth0-ui-components" }
```

Then in your `build.gradle`:

```gradle
dependencies {
    implementation(libs.auth0.ui.components)
}
```
</details>

#### Permissions

Open your app's `AndroidManifest.xml` file and add the following permission:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### Configure the SDK

First, create an instance of `Auth0` with your Application information:

```kotlin
val account = Auth0.getInstance("{YOUR_CLIENT_ID}", "{YOUR_DOMAIN}")
```

<details>
  <summary>Configure using Android Context</summary>

Alternatively, you can save your Application information in the `strings.xml` file using the following names:

```xml
<resources>
    <string name="com_auth0_client_id">YOUR_CLIENT_ID</string>
    <string name="com_auth0_domain">YOUR_DOMAIN</string>
</resources>
```

You can then create a new Auth0 instance by passing an Android Context:

```kotlin
val account = Auth0.getInstance(context)
```
</details>

### Setup for Authentication

Go to the [Auth0 Dashboard](https://manage.auth0.com/#/applications) and navigate to your application's settings. Make sure you have the following:

1. **Application Type:** Set to **Native**
2. **Allowed Callback URLs:** Add a URL with the following format:

```
https://{YOUR_AUTH0_DOMAIN}/android/{YOUR_APP_PACKAGE_NAME}/callback
```

Replace `{YOUR_APP_PACKAGE_NAME}` with your actual application's package name, available in your `app/build.gradle` file as the `applicationId` value.

Next, define the Manifest Placeholders for the Auth0 Domain and Scheme. Go to your application's `build.gradle` file and add the `manifestPlaceholders` line:

```groovy
android {
    defaultConfig {
        applicationId "com.auth0.android.sample"
        minSdk 30
        targetSdk 36
        
        // Add these manifest placeholders
        manifestPlaceholders = [
            auth0Domain: "@string/com_auth0_domain",
            auth0Scheme: "@string/com_auth0_scheme"
        ]
    }
}
```

> **Note:** The scheme value can be either `https` or a custom one. See [App Deep Linking](#a-note-about-app-deep-linking) for more details.

### Initialize the Library

Initialize the Auth0 UI Components library in your Application class or main Activity:

```kotlin
// Create Auth0 account instance
val account = Auth0.getInstance(clientId, domain)

// Setup credentials manager
val credentialsManager = CredentialsManager(
    AuthenticationAPIClient(account),
    SharedPreferencesStorage(context)
)

// Initialize the UI library
Auth0UI.initialize(
    account = account,
    tokenProvider = DefaultTokenProvider(credentialsManager),
    scheme = "https"
)
```

### Use MFA Components

In your Compose UI, simply add the MFA component where you want users to manage their multi-factor authentication:

```kotlin
@Composable
fun SettingsScreen() {
    MFAComponent()
}
```

Navigation between different MFA enrollment and verification flows is handled internally by the library.

<details>
  <summary>Advanced Configuration</summary>

For advanced configurations:

```kotlin
// Custom token provider
class CustomTokenProvider : TokenProvider {
    override suspend fun getAccessToken(): String {
        // Your custom logic to retrieve access token
        return credentialsManager.awaitCredentials().accessToken
    }
}

// Initialize with custom configuration
Auth0UI.initialize(
    account = account,
    tokenProvider = CustomTokenProvider(),
    scheme = "customscheme"
)
```

Ensure you're using:
- Kotlin 2.2.20
- Jetpack Compose
- Java 17
</details>

##### A note about App Deep Linking:

> Whenever possible, Auth0 recommends using [Android App Links](https://auth0.com/docs/applications/enable-android-app-links) as a secure way to link directly to content within your app. Custom URL schemes can be subject to [client impersonation attacks](https://datatracker.ietf.org/doc/html/rfc8252#section-8.6).

If you followed the configuration steps above, the default scheme is `https`. This works best for Android API 23 or newer with [Android App Links](https://auth0.com/docs/applications/enable-android-app-links), but on older Android versions, this may show an intent chooser dialog. You can use a custom unique scheme instead:

1. Update the `auth0Scheme` Manifest Placeholder in your `app/build.gradle` file
2. Update the **Allowed Callback URLs** in your [Auth0 Dashboard](https://manage.auth0.com/#/applications)
3. Pass your custom scheme when initializing:

```kotlin
Auth0UI.initialize(
    account = account,
    tokenProvider = tokenProvider,
    scheme = "customscheme"
)
```

> Note that schemes [can only have lowercase letters](https://developer.android.com/guide/topics/manifest/data-element).

---

## Run the Sample App

This repository includes a complete sample application demonstrating the MFA UI components in action.

### Setup

1. **Create a Native application** in your Auth0 tenant and note the Client ID and Domain.

2. **Configure Allowed Callback URLs** in your Auth0 Dashboard. Add:
   ```
   {scheme}://{domain}/android/com.auth0.android.sample/callback
   ```
   Replace `{scheme}` and `{domain}` with your values.

3. **Set your Auth0 credentials** in `app/src/main/res/values/strings.xml`:
   ```xml
   <resources>
       <string name="com_auth0_client_id">YOUR_CLIENT_ID</string>
       <string name="com_auth0_domain">YOUR_DOMAIN</string>
       <string name="com_auth0_scheme">demo</string>
   </resources>
   ```

### Running the Sample

**From Android Studio:**
1. Open the project folder
2. Let Gradle sync finish
3. Select the `app` run configuration
4. Click Run on a device/emulator (API 30+)

**From Terminal:**

```sh
./gradlew :app:installDebug
```

This installs the debug build on a connected device/emulator. You can also assemble APKs:

```sh
./gradlew :app:assembleDebug
```

### What You'll See

1. **Login Screen** - Launch the app to see the login interface
2. **Universal Login** - Tap Login to authenticate via Auth0's Universal Login in the browser
3. **MFA Settings** - After successful login, you'll see the MFA management interface where you can:
   - View all available MFA methods
   - Enroll TOTP authenticators via QR code
   - Configure Push notifications
   - Set up SMS or Email verification
   - Generate and manage Recovery Codes

---

## Troubleshooting

<details>
  <summary><b>Browser returns to the app but nothing happens</b></summary>

- Verify your scheme/domain in `strings.xml` match the Auth0 **Allowed Callback URL** exactly
- Example: `demo://your-tenant.us.auth0.com/android/com.auth0.android.sample/callback`
- Check that the `manifestPlaceholders` in `build.gradle` are correctly configured
- Ensure the `auth0Scheme` matches what you're using in the initialization
</details>

<details>
  <summary><b>Login completes but API calls fail (401/403)</b></summary>

- Confirm the audience and scopes are correctly configured
- Verify your application is authorized to call the MyAccount APIs
- Check that you're requesting the `offline_access` scope for refresh tokens
- Ensure the access token is being properly stored and retrieved
</details>

<details>
  <summary><b>SMS/Email OTP not received</b></summary>

- Verify that SMS/Email factors are enabled in your Auth0 tenant Dashboard
- Check that you've configured the appropriate SMS/Email providers in Auth0
- Confirm the test device phone number or email is valid and reachable
- Check spam folders for email OTPs
</details>

<details>
  <summary><b>Build errors or dependency conflicts</b></summary>

- Ensure you're using the correct versions:
  - JDK 17
  - AGP 8.11+
  - Gradle 8.13+
  - Kotlin 2.2.20
- Clear Gradle cache: `./gradlew clean`
- Invalidate caches in Android Studio: File → Invalidate Caches / Restart
</details>

---

## Feedback

### Contributing

We appreciate feedback and contribution to this repo! Before you get started, please see the following:

- [Auth0's general contribution guidelines](https://github.com/auth0/open-source-template/blob/master/GENERAL-CONTRIBUTING.md)
- [Auth0's code of conduct guidelines](https://github.com/auth0/open-source-template/blob/master/CODE-OF-CONDUCT.md)

### Raise an Issue

To provide feedback or report a bug, [please raise an issue on our issue tracker](https://github.com/atko-cic/ui-components-android/issues).

### Vulnerability Reporting

Please do not report security vulnerabilities on the public GitHub issue tracker. The [Responsible Disclosure Program](https://auth0.com/whitehat) details the procedure for disclosing security issues.

### Important Note

Portions of this SDK may have AI-assisted or generated code.

---


<p align="center">
  <picture>
    <source media="(prefers-color-scheme: light)" srcset="https://cdn.auth0.com/website/sdks/logos/auth0_light_mode.png" width="150">
    <source media="(prefers-color-scheme: dark)" srcset="https://cdn.auth0.com/website/sdks/logos/auth0_dark_mode.png" width="150">
    <img alt="Auth0 Logo" src="https://cdn.auth0.com/website/sdks/logos/auth0_light_mode.png" width="150">
  </picture>
</p>
<p align="center">Auth0 is an easy-to-implement, adaptable authentication and authorization platform. To learn more check out <a href="https://auth0.com/why-auth0">Why Auth0?</a></p>
<p align="center">
This project is licensed under the Apache License 2.0. See the <a href="./LICENSE">LICENSE</a> file for more info.<br>
Copyright 2025 Okta, Inc.<br>
Licensed under the Apache License, Version 2.0 (the "<a href="./LICENSE">License</a>");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at<br>
&nbsp;&nbsp;&nbsp;&nbsp;<a href="http://www.apache.org/licenses/LICENSE-2.0">http://www.apache.org/licenses/LICENSE-2.0</a><br>
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the <a href="./LICENSE">License</a> for the specific language governing permissions and
limitations under the License.
</p>
