package com.auth0.universalcomponents.utils.logging

import android.util.Log

/**
 * Lightweight, SDK-internal logging facade.
 *
 * All diagnostic logging inside the SDK should go through this object instead of calling
 * [android.util.Log] directly. Routing every log statement through a single choke point lets us
 * decide *in one place* whether logs are emitted, which is important because this is a published
 * library: consumers ship our compiled artifact inside their own apps and we must not leak internal
 * diagnostics (tokens, sessions, enrollment details) into their production Logcat by default.
 *
 * ### Usage
 * ```
 * Logger.d(TAG, "Fetching new token for audience: $audience")
 * Logger.e(TAG, "Verification failed", throwable)
 * ```
 * ### Enabling output
 * Logging is **off by default** so a released SDK is silent. The switch is flipped only through the
 * SDK-internal [setEnabled]; it is intentionally **not yet wired to any public initialization
 * option** — the seam exists so a future change can decide how diagnostics get turned on without
 * touching call sites.
 *
 */
public object Logger {

    /**
     * Master switch for all SDK logging.
     *
     * When `false` (the default) every logging method below is a no-op and nothing reaches Logcat.
     * Mutable only from within the SDK via [setEnabled].
     */
    private var enabled: Boolean = false

    /**
     * Turns SDK logging on or off. Intended to be called once during SDK initialization.
     */
    internal fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    /** Logs a debug message. No-op unless [enabled] is `true`. */
    @JvmStatic
    public fun d(tag: String, message: String) {
        if (enabled) Log.d(tag, message)
    }

    /** Logs an info message. No-op unless [enabled] is `true`. */
    @JvmStatic
    public fun i(tag: String, message: String) {
        if (enabled) Log.i(tag, message)
    }

    /** Logs a verbose message. No-op unless [enabled] is `true`. */
    @JvmStatic
    public fun v(tag: String, message: String) {
        if (enabled) Log.v(tag, message)
    }

    /**
     * Logs a warning, optionally with a [throwable]. No-op unless [enabled] is `true`.
     */
    @JvmStatic
    public fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (enabled) Log.w(tag, message, throwable)
    }

    /**
     * Logs an error, optionally with a [throwable]. No-op unless [enabled] is `true`.
     */
    @JvmStatic
    public fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (enabled) Log.e(tag, message, throwable)
    }
}
