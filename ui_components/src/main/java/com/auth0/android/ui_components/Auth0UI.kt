package com.auth0.android.ui_components

import android.util.Log
import androidx.credentials.CredentialManager
import com.auth0.android.Auth0
import com.auth0.android.ui_components.token.TokenProvider
import java.util.concurrent.atomic.AtomicBoolean


data class PasskeyConfiguration(
    val credentialManager: CredentialManager? = null,
    val connection: String? = null
)

public object Auth0UI {

    private var initialized = AtomicBoolean(false)
    private lateinit var _account: Auth0
    private lateinit var _tokenProvider: TokenProvider
    private lateinit var _scheme: String
    private lateinit var _passkeyConfiguration: PasskeyConfiguration

    internal val account: Auth0
        get() {
            assertInitialized()
            return _account
        }

    internal val tokenProvider: TokenProvider
        get() {
            assertInitialized()
            return _tokenProvider
        }

    internal val scheme: String
        get() {
            assertInitialized()
            return _scheme
        }

    internal val passkeyConfiguration: PasskeyConfiguration
        get() {
            assertInitialized()
            return _passkeyConfiguration
        }

    public fun initialize(
        account: Auth0,
        tokenProvider: TokenProvider,
        scheme: String,
        passkeyConfiguration: PasskeyConfiguration = PasskeyConfiguration()
    ) {
        if (initialized.get()) {
            Log.d("Auth0UI", "Auth0UI is already initialized.")
            return
        }
        _account = account
        _tokenProvider = tokenProvider
        _scheme = scheme
        _passkeyConfiguration = passkeyConfiguration
        initialized.set(true)
    }

    private fun assertInitialized() {
        if (!initialized.get()) {
            throw IllegalStateException("Auth0UI must be initialized first.")
        }
    }

}