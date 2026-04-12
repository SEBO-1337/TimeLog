package com.sebo.timelog.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sebo.timelog.data.remote.AuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val email: String? = null
)

class AuthViewModel(
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(
            isAuthenticated = authService.currentUser() != null,
            email = authService.currentUser()?.email
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authService.authState.collect { user ->
                _uiState.value = _uiState.value.copy(
                    isAuthenticated = user != null,
                    email = user?.email,
                    isLoading = false,
                    errorMessage = null
                )
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "E-Mail und Passwort sind erforderlich")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                authService.login(email.trim(), password)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Login fehlgeschlagen"
                )
            }
        }
    }

    fun register(email: String, password: String, repeatPassword: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "E-Mail und Passwort sind erforderlich")
            return
        }
        if (password != repeatPassword) {
            _uiState.value = _uiState.value.copy(errorMessage = "Passwoerter stimmen nicht ueberein")
            return
        }
        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "Passwort muss mindestens 6 Zeichen haben")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                authService.register(email.trim(), password)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Registrierung fehlgeschlagen"
                )
            }
        }
    }

    fun logout() {
        authService.logout()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    companion object {
        fun factory(authService: AuthService): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(authService) as T
                }
            }
        }
    }
}

