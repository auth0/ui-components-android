package com.auth0.android.ui_components.domain

import kotlinx.coroutines.CoroutineDispatcher


interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val unconfined: CoroutineDispatcher
}