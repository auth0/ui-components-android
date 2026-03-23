package com.auth0.android.sample.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.authentication.storage.CredentialsManager
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch


sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val credentials: Credentials) : AuthState()
    data class Error(val message: String) : AuthState()
}

data class UserProfile(
    val name: String = "User",
    val email: String = "",
    val pictureUrl: String = ""
)

class AuthViewModel : ViewModel() {

    private val scope = "openid profile email offline_access"

    private val _authState: MutableStateFlow<AuthState> = MutableStateFlow(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _userProfile: MutableStateFlow<UserProfile> = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _loginError = Channel<String>(Channel.BUFFERED)
    val loginError = _loginError.receiveAsFlow()

    fun sendLoginError(message: String) {
        viewModelScope.launch {
            _loginError.send(message)
        }
    }

    fun login(context: Context, webAuthProvider: WebAuthProvider.Builder) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val credentials = webAuthProvider
                    .withScope(scope)
                    .await(context)
                extractUserProfile(credentials)
                _authState.value = AuthState.Authenticated(credentials)
            } catch (e: AuthenticationException) {
                if (e.isBrowserAppNotAvailable || e.isCanceled) {
                    Log.e("TAG", "login: User cancelled or browser not available")
                    _authState.value = AuthState.Idle
                } else {
                    Log.e("TAG", "login: ${e.printStackTrace()}")
                    _authState.value = AuthState.Error(e.message ?: "An unknown error occurred")
                }
            }
        }
    }

    fun loginWithPassword(
        email: String,
        password: String,
        authClient: AuthenticationAPIClient,
        audience: String
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val credentials = authClient
                    .login(email, password, "Username-Password-Authentication")
                    .setScope(scope)
                    .setAudience(audience)
                    .validateClaims()
                    .await()
                extractUserProfile(credentials)
                _authState.value = AuthState.Authenticated(credentials)
            } catch (e: AuthenticationException) {
                Log.e("TAG", "loginWithPassword: ${e.printStackTrace()}")
                _loginError.send(e.getDescription())
                _authState.value = AuthState.Idle
            }
        }
    }

    fun logout(
        context: Context,
        credentialsManager: CredentialsManager,
        logoutBuilder: WebAuthProvider.LogoutBuilder
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                logoutBuilder.await(context)
                credentialsManager.clearCredentials()
                _userProfile.value = UserProfile()
                _authState.value = AuthState.Idle
            } catch (e: AuthenticationException) {
                Log.e("TAG", "logout: ${e.stackTraceToString()}")
                _authState.value = AuthState.Error(e.message ?: "Logout failed")
            }
        }
    }

    /**
     * Extract user profile from the ID token claims in the credentials.
     */
    private fun extractUserProfile(credentials: Credentials) {
        try {
            val user = credentials.user
            _userProfile.value = UserProfile(
                name = user.name ?: user.nickname ?: "User",
                email = user.email ?: "",
                pictureUrl = user.pictureURL ?: ""
            )
        } catch (e: Exception) {
            Log.w("AuthViewModel", "Could not extract user profile", e)
        }
    }

    /**
     * Load user profile from existing credentials (when app resumes with valid session).
     */
    fun loadProfileFromCredentials(credentialsManager: CredentialsManager) {
        viewModelScope.launch {
            try {
                val credentials = credentialsManager.awaitCredentials()
                extractUserProfile(credentials)
            } catch (e: Exception) {
                Log.w("AuthViewModel", "Could not load profile from credentials", e)
            }
        }
    }
}
