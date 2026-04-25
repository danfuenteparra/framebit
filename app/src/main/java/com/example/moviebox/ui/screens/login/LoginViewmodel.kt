package com.example.moviebox.ui.screens.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviebox.auth.AuthManager
import com.example.moviebox.data.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la pantalla de Login.
 * Flujo: Auth0 login → Firebase signInAnonymously → upsert usuario en Firestore.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val socialRepository: SocialRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(activityContext: Context) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val credentials = authManager.login(activityContext)
                val accessToken = credentials.accessToken

                // Perfil de Auth0 (nombre, foto, email, sub)
                val profile = authManager.getUserProfile(accessToken)
                val userId = profile.getId() ?: ""
                val name = profile.name ?: profile.nickname ?: "Usuario"
                val email = profile.email ?: ""
                val pictureUrl = profile.pictureURL

                if (userId.isNotBlank()) {
                    // 1. Autenticación Firebase (anónima) para pasar reglas de Firestore
                    socialRepository.ensureSignedIn()
                    // 2. Guardar/actualizar al usuario en users/{userId}
                    socialRepository.upsertCurrentUser(userId, name, email, pictureUrl)
                }

                _uiState.value = LoginUiState.Success
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
