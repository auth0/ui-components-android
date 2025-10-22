package com.auth0.android.sample.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val credentials: Credentials) : AuthState()
    data class Error(val message: String) : AuthState()
}


class AuthViewModel : ViewModel() {

    private val scope = "openid profile email offline_access"

    private val _authState: MutableStateFlow<AuthState> = MutableStateFlow(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun login(context: Context, webAuthProvider: WebAuthProvider.Builder) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val credentials = webAuthProvider
                    .withScope(scope)
                    .await(context)
                _authState.value = AuthState.Authenticated(credentials)
            } catch (e: AuthenticationException) {
                Log.e("TAG", "login: ${e.printStackTrace()}")
                _authState.value = AuthState.Error(e.message ?: "An unknown error occurred")
            }
        }

    }

}