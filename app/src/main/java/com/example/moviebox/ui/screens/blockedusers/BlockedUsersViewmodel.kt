package com.example.moviebox.ui.screens.blockedusers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviebox.auth.AuthManager
import com.example.moviebox.data.remote.model.PublicUser
import com.example.moviebox.data.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la pantalla "Usuarios bloqueados".
 * Carga la lista de bloqueados del usuario actual y permite desbloquear.
 */
@HiltViewModel
class BlockedUsersViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _users = MutableStateFlow<List<PublicUser>>(emptyList())
    val users: StateFlow<List<PublicUser>> = _users

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    init {
        load()
    }

    /** Carga la lista de bloqueados del usuario actual. */
    fun load() {
        val myId = authManager.getCachedUserId() ?: return
        viewModelScope.launch {
            _loading.value = true
            try {
                socialRepository.ensureSignedIn()
                _users.value = socialRepository.getBlockedUsers(myId)
            } catch (_: Exception) {
                _users.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Desbloquea al usuario [otherUserId] y refresca la lista.
     * No restaura los follows previos: si el usuario quiere reseguir,
     * tendrá que hacerlo manualmente desde el perfil del otro.
     */
    fun unblock(otherUserId: String) {
        val myId = authManager.getCachedUserId() ?: return
        viewModelScope.launch {
            try {
                socialRepository.ensureSignedIn()
                socialRepository.unblockUser(myId, otherUserId)
                // Optimista: quitar de la lista al instante
                _users.value = _users.value.filterNot { it.userId == otherUserId }
            } catch (_: Exception) {
                // Si falla, recargamos para volver al estado real
                load()
            }
        }
    }
}