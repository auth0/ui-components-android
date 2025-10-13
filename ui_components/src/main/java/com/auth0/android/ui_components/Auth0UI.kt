package com.auth0.android.ui_components

import android.util.Log
import com.auth0.android.Auth0
import com.auth0.android.ui_components.token.TokenProvider
import java.util.concurrent.atomic.AtomicBoolean


public object Auth0UI {

    private var initialized = AtomicBoolean(false)
    private lateinit var _account: Auth0
    private lateinit var _tokenProvider: TokenProvider

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

    public fun initialize(
        account: Auth0,
        tokenProvider: TokenProvider,
    ) {
        if (initialized.get()) {
            Log.d("Auth0UI", "Auth0UI is already initialized.")
            return
        }
        _account = account
        _tokenProvider = tokenProvider
        initialized.set(true)
    }

    private fun assertInitialized() {
        if (!initialized.get()) {
            throw IllegalStateException("Auth0UI must be initialized first.")
        }
    }

}