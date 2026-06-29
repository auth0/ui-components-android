package com.auth0.universalcomponents.utils.logging

import android.util.Log

/**
 * Lightweight, SDK-internal logging facade.
 *
 * All diagnostic logging inside the SDK goes through this object instead of calling
 * [android.util.Log] directly. Routing every log statement through a single choke point gives us
 * one consistent entry point — useful for keeping tags uniform and as the natural place to add
 * redaction or gating later should a privacy requirement call for it.
 *
 * Logged messages must never contain sensitive values (tokens, sessions, PII); callers are
 * responsible for logging only non-sensitive, debug-friendly context.
 *
 * ### Usage
 * ```
 * Logger.d(TAG, "Fetching new token for audience: $audience")
 * Logger.e(TAG, "Verification failed", throwable)
 * ```
 */
internal object Logger {

    /** Logs a debug message. */
    internal fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    /** Logs an info message. */
    internal fun i(tag: String, message: String) {
        Log.i(tag, message)
    }

    /** Logs a verbose message. */
    internal fun v(tag: String, message: String) {
        Log.v(tag, message)
    }

    /** Logs a warning, optionally with a [throwable]. */
    internal fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
    }

    /** Logs an error, optionally with a [throwable]. */
    internal fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
    }
}
