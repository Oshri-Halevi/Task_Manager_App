package com.example.taskmanagerapp.auth

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskmanagerapp.R
import kotlinx.coroutines.launch

class AuthViewModel(private val repo: AuthRepository) : ViewModel() {

    private val _state = MutableLiveData<AuthState>(AuthState.Unauthenticated)
    val state: LiveData<AuthState> = _state

    companion object {
        private const val TAG = "AuthViewModel"
    }

    init {
        checkSession()
    }

    fun checkSession() = viewModelScope.launch {
        _state.value = AuthState.Loading
        val session = repo.restoreSession()
        
        _state.value = if (session != null) {
            Log.d(TAG, "Session restored for user: $session")
            AuthState.Authenticated(session.userId)
        } else {
            Log.d(TAG, "No session found, staying unauthenticated")
            AuthState.Unauthenticated
        }
    }

    fun signInWithGoogle(idToken: String) = viewModelScope.launch {
        if (!AppConfig.hasGoogleClientId()) {
            Log.e(TAG, "Cannot sign in with Google: client ID is missing")
            _state.value = AuthState.Error(R.string.error_missing_google_client_id)
            return@launch
        }
        
        _state.value = AuthState.Loading
        runCatching { repo.loginWithGoogleIdToken(idToken) }
            .onSuccess { session ->
                Log.d(TAG, "Google sign-in successful")
                _state.value = AuthState.Authenticated(session.userId) 
            }
            .onFailure { 
                Log.e(TAG, "Google sign-in failed", it)
                _state.value = AuthState.Error(R.string.error_auth_failed) 
            }
    }

    fun signInFake() = viewModelScope.launch {
        if (!AppConfig.isDebugBuild) {
            Log.w(TAG, "Demo sign-in is only available in debug builds")
            return@launch
        }
        
        _state.value = AuthState.Loading
        runCatching { repo.signInFake() }
            .onSuccess { session ->
                Log.d(TAG, "Demo sign-in successful")
                _state.value = AuthState.Authenticated(session.userId) 
            }
            .onFailure { 
                Log.e(TAG, "Demo sign-in failed", it)
                _state.value = AuthState.Error(R.string.error_auth_failed) 
            }
    }

    fun signOut() {
        _state.value = AuthState.Unauthenticated
        viewModelScope.launch {
            Log.d(TAG, "Signing out...")
            runCatching { repo.signOut() }
                .onSuccess { Log.d(TAG, "Sign-out successful") }
                .onFailure { Log.e(TAG, "Sign-out failed", it) }
        }
    }
}
