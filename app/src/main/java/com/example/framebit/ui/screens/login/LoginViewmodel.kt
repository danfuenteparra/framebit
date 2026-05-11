package com.example.framebit.ui.screens.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.framebit.auth.AuthManager
import com.example.framebit.data.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val socialRepository: SocialRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    // ===================== AUTH0 =====================

    fun login(activityContext: Context) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val credentials = authManager.login(activityContext)
                val accessToken = credentials.accessToken
                val profile = authManager.getUserProfile(accessToken)
                val userId = profile.getId() ?: ""
                val name = profile.name ?: profile.nickname ?: "Usuario"
                val email = profile.email ?: ""
                val pictureUrl = profile.pictureURL

                if (userId.isNotBlank()) {
                    socialRepository.ensureSignedIn()
                    socialRepository.upsertCurrentUser(userId, name, email, pictureUrl)
                }

                _uiState.value = LoginUiState.Success
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    // ===================== EMAIL / PASSWORD =====================

    fun loginEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Introduce email y contraseña")
            return
        }
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                authManager.loginWithEmail(email, password)
                val userId = authManager.getCachedUserId().orEmpty()
                if (userId.isNotBlank()) {
                    socialRepository.upsertCurrentUser(
                        userId = userId,
                        name = authManager.getCachedName().orEmpty().ifBlank { email.substringBefore("@") },
                        email = authManager.getCachedEmail().orEmpty(),
                        pictureUrl = authManager.getCachedPictureUrl()
                    )
                }
                _uiState.value = LoginUiState.Success
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(mapAuthError(e))
            }
        }
    }

    fun registerEmail(email: String, password: String, displayName: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Introduce email y contraseña")
            return
        }
        if (password.length < 6) {
            _uiState.value = LoginUiState.Error("La contraseña debe tener al menos 6 caracteres")
            return
        }
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                // 1) Asegurar sesión Firebase para poder leer Firestore con las reglas actuales
                socialRepository.ensureSignedIn()

                // 2) Comprobar duplicado de email en Firestore (incluye cuentas Auth0)
                val normalized = email.trim().lowercase()
                if (socialRepository.existsUserWithEmail(normalized)) {
                    _uiState.value = LoginUiState.Error(
                        "Ese email ya está registrado. Entra con Google o usa otro email."
                    )
                    return@launch
                }

                // 3) Crear cuenta Firebase + perfil
                authManager.registerWithEmail(email, password, displayName.trim())
                val userId = authManager.getCachedUserId().orEmpty()
                if (userId.isNotBlank()) {
                    socialRepository.upsertCurrentUser(
                        userId = userId,
                        name = authManager.getCachedName().orEmpty().ifBlank {
                            displayName.trim().ifBlank { email.substringBefore("@") }
                        },
                        email = authManager.getCachedEmail().orEmpty(),
                        pictureUrl = null
                    )
                }
                _uiState.value = LoginUiState.Success
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(mapAuthError(e))
            }
        }
    }

    private fun mapAuthError(e: Exception): String {
        val msg = e.message.orEmpty().lowercase()
        return when {
            "password is invalid" in msg || "wrong-password" in msg -> "Contraseña incorrecta"
            "no user record" in msg || "user-not-found" in msg -> "No existe ninguna cuenta con ese email"
            "email address is already in use" in msg || "email-already-in-use" in msg -> "Ese email ya está registrado"
            "badly formatted" in msg || "invalid-email" in msg -> "Email no válido"
            "network error" in msg -> "Error de red"
            "weak-password" in msg -> "Contraseña demasiado débil"
            else -> e.message ?: "Error desconocido"
        }
    }

    fun clearError() {
        if (_uiState.value is LoginUiState.Error) _uiState.value = LoginUiState.Idle
    }
}

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}