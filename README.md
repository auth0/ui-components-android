
## Auth0 Android UI Components + Sample App

Composable UI building blocks for MFA enrollment and verification on Android, plus a runnable sample app that shows how to integrate them in a real app.

This repo contains:
- `ui_components`: a reusable Android library (Kotlin + Jetpack Compose) that implements MFA flows (TOTP, Push, SMS, Email, Recovery Codes) on top of the Auth0 Android SDK and My Account APIs.
- `app`: a minimal sample application that initializes Auth0, logs users in via Universal Login, and embeds the MFA UI components.

---

## Requirements

- Android Studio (2024.x or newer) with Gradle support for AGP 8.11+
- JDK 17 (project is configured with Java/Kotlin 17)
- Android SDK
	- compileSdk: 36
	- targetSdk: 36
	- minSdk: 30
- Kotlin: 2.2.20


---

## Project structure

```
ui_components_android/
├─ app/                # Sample app (package: com.auth0.android.sample)
└─ ui_components/      # Reusable MFA UI library
```

---

## Configure Auth0 for the sample app

1) Create a Native application in your Auth0 tenant and note the Client ID and Domain.

2) Configure Allowed Callback URLs for Android. The sample uses an intent-filter callback of the form:

	 {scheme}://{domain}/android/com.auth0.android.sample/callback

	 If you change the applicationId or scheme/domain values, update the callback accordingly.

3) Set your Auth0 values in the sample app resources:

	 File: `app/src/main/res/values/strings.xml`
	 - `com_auth0_client_id` → your application’s Client ID
	 - `com_auth0_domain` → your tenant domain (e.g., your-tenant.us.auth0.com)
	 - `com_auth0_scheme` → a custom scheme you’ll use for the callback (e.g., demo)

	 The Android Manifest uses these via Gradle manifest placeholders:
	 - `auth0Domain` = `@string/com_auth0_domain`
	 - `auth0Scheme` = `@string/com_auth0_scheme`

4)  Audience configuration

	 The sample sets the audience to your tenant’s Management API v2 endpoint:
	 `https://{domain}/api/v2/`

	 Ensure your application is configured to allow this audience if you plan to request tokens for APIs that back My Account operations.

---

## Run the sample app

From Android Studio:
- Open the project folder.
- Let Gradle sync finish.
- Select the `app` run configuration and click Run on a device/emulator (API 26+).

From a terminal (macOS zsh):

```sh
./gradlew :app:installDebug
```

This installs the debug build on a connected device/emulator. You can also assemble APKs/AABs:

```sh
./gradlew :app:assembleDebug
```

---

## What you’ll see in the sample

1) Launch the app → you’ll land on a simple Login screen.
2) Tap Login → Universal Login opens in the browser; complete authentication.
3) After success, you’re navigated to Settings, which embeds the MFA UI Components. From here you can:
	 - View available MFA methods
	 - Enroll TOTP or Push via QR
	 - Enroll SMS or Email and verify via OTP
	 - Generate and copy Recovery Codes

---

## Using the library in your app

The core initialization happens once at app startup. Provide:
- an `Auth0` instance,
- a `TokenProvider` (the sample uses `DefaultTokenProvider(CredentialsManager)`), and
- a configured `WebAuthProvider.Builder` for login.

Minimal setup (Kotlin):

```kotlin
// In your Application or first Activity
val account = Auth0.getInstance(clientId, domain)
val credentialsManager = CredentialsManager(AuthenticationAPIClient(account), SharedPreferencesStorage(context))

Auth0UI.initialize(
		account,
		DefaultTokenProvider(credentialsManager),
		WebAuthProvider.login(account).withScheme(yourScheme)
)

// In your Compose UI where you want MFA
MFAComponent()
```

Navigation inside MFA is handled internally by the library.

If you consume this module from a multi-module project, add a dependency on `:ui_components` and ensure you’re on Kotlin 2.2.20, Compose BOM 2024.09.00, and Java 17.

---

## Troubleshooting

- Browser returns to the app but nothing happens:
	- Verify your scheme/domain in `strings.xml` match the Auth0 Allowed Callback URL.
	- Example: `demo://your-tenant.us.auth0.com/android/com.auth0.android.sample/callback`.

- Login completes but API calls fail (401/403):
	- Confirm the audience/scopes and that your application is authorized to call the APIs backing My Account flows.

- SMS/Email OTP not received:
	- Ensure those factors are enabled and configured in your Auth0 tenant and the test device/number/email is reachable.

---

## Notes

- Build tooling pinned via Version Catalog (`gradle/libs.versions.toml`).
- AGP 8.11.2 + Gradle 8.13 + JDK 17 are required.
- Modules target `compileSdk = 36`, `minSdk = 26`.

---
