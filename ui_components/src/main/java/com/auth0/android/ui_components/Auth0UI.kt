package com.auth0.android.ui_components

import android.app.Application
import android.content.Context
import com.auth0.android.Auth0
import com.auth0.android.ui_components.token.TokenProvider
import java.util.concurrent.atomic.AtomicBoolean


public object Auth0UI {

    @Volatile
    private var initialized = AtomicBoolean(false)
    private lateinit var _account: Auth0
    private lateinit var _tokenProvider: TokenProvider
    private lateinit var _context: Application
    private lateinit var _authorizationParams: Map<String, String>

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

    internal val authorizationParams: Map<String, String>
        get() {
            assertInitialized()
            return _authorizationParams
        }

    internal val context: Context
        get() {
            assertInitialized()
            return _context
        }


    public fun initialize(
        account: Auth0,
        tokenProvider: TokenProvider,
        authorizationParams: Map<String, String>
    ) {
        if (initialized.get()) return
        _account = account
        _tokenProvider = tokenProvider
        _authorizationParams = authorizationParams
        initialized.set(true)
    }

    private fun assertInitialized() {
        if (!initialized.get()) {
            throw IllegalStateException("Auth0UI must be initialized first.")
        }
    }

}