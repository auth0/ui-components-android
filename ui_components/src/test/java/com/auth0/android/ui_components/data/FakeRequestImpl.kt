package com.auth0.android.ui_components.data

import com.auth0.android.Auth0Exception
import com.auth0.android.callback.Callback
import com.auth0.android.request.Request

/**
 * Fake implementation of the [com.auth0.android.request.Request] interface for testing purpose
 * Can be configured to return a success result or throw an exception.
 */
class FakeRequestImpl<T, U : Auth0Exception>(
    private val response: T? = null,
    private val exception: U? = null
) : Request<T, U> {

    override fun addHeader(
        name: String,
        value: String
    ): Request<T, U> = this

    override fun addParameter(
        name: String,
        value: Any
    ): Request<T, U> = this

    override suspend fun await(): T {
        return when {
            exception != null -> throw exception
            else -> response as T
        }
    }

    override fun execute(): T {
        return when {
            exception != null -> throw exception
            else -> response as T
        }
    }

    override fun start(callback: Callback<T, U>) {
        when {
            exception != null -> callback.onFailure(exception)
            response != null -> callback.onSuccess(response)
        }
    }

    override fun addParameter(
        name: String,
        value: String
    ): Request<T, U> = this

    override fun addParameters(parameters: Map<String, String>): Request<T, U> = this
}