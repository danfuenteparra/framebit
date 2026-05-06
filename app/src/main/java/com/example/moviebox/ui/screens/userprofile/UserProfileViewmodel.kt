package com.example.moviebox.ui.screens.userprofile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviebox.auth.AuthManager
import com.example.moviebox.data.remote.model.BlockRelation
import com.example.moviebox.data.remote.model.LibraryEntry
import com.example.moviebox.data.remote.model.PublicUser
import com.example.moviebox.data.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Item de Top 3 para mostrar (mediaType, posición, mediaId, título, póster).
 */
data class TopItemDisplay(
    val mediaType: String,
    val position: Int,
    val mediaId: Int,
    val title: String,
    val posterPath: String?
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val authManager: AuthManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val targetUserId: String = savedStateHandle["userId"] ?: ""

    // ----- Estado del perfil -----

    private val _user = MutableStateFlow<PublicUser?>(null)
    val user: StateFlow<PublicUser?> = _user

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing

    private val _topMovies = MutableStateFlow<List<TopItemDisplay?>>(listOf(null, null, null))
    val topMovies: StateFlow<List<TopItemDisplay?>> = _topMovies

    private val _topTvShows = MutableStateFlow<List<TopItemDisplay?>>(listOf(null, null, null))
    val topTvShows: StateFlow<List<TopItemDisplay?>> = _topTvShows

    private val _topGames = MutableStateFlow<List<TopItemDisplay?>>(listOf(null, null, null))
    val topGames: StateFlow<List<TopItemDisplay?>> = _topGames

    private val _watchedMovies = MutableStateFlow<List<LibraryEntry>>(emptyList())
    val watchedMovies: StateFlow<List<LibraryEntry>> = _watchedMovies

    private val _watchedTvShows = MutableStateFlow<List<LibraryEntry>>(emptyList())
    val watchedTvShows: StateFlow<List<LibraryEntry>> = _watchedTvShows

    private val _watchedGames = MutableStateFlow<List<LibraryEntry>>(emptyList())
    val watchedGames: StateFlow<List<LibraryEntry>> = _watchedGames

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    // ----- Estado de bloqueo -----

    private val _blockRelation = MutableStateFlow<BlockRelation>(BlockRelation.NotBlocked)
    val blockRelation: StateFlow<BlockRelation> = _blockRelation

    val isOwnProfile: Boolean
        get() = targetUserId == authManager.getCachedUserId()

    init {
        load()
    }

    /**
     * Carga inicial del perfil.
     * Primero comprueba la relación de bloqueo: si hay bloqueo activo en cualquier
     * dirección, NO carga top items, library ni estado de follow para evitar
     * mostrar nada del otro usuario.
     */
    private fun load() {
        if (targetUserId.isBlank()) {
            _loading.value = false
            return
        }
        viewModelScope.launch {
            _loading.value = true
            try {
                socialRepository.ensureSignedIn()

                // Datos básicos del usuario (nombre, foto, contadores)
                _user.value = socialRepository.getUser(targetUserId)

                val myId = authManager.getCachedUserId()

                // Relación de bloqueo (solo aplica si miramos un perfil ajeno)
                if (myId != null && myId != targetUserId) {
                    _blockRelation.value = socialRepository.getBlockRelation(myId, targetUserId)
                }

                // Si hay bloqueo activo, no cargamos nada más del otro usuario
                if (_blockRelation.value !is BlockRelation.NotBlocked) {
                    return@launch
                }

                // Estado de follow (solo si no es nuestro perfil y no hay bloqueo)
                if (myId != null && myId != targetUserId) {
                    _isFollowing.value = socialRepository.isFollowing(myId, targetUserId)
                }

                loadTopItems()
                loadLibrary()
            } catch (_: Exception) { }
            finally {
                _loading.value = false
            }
        }
    }

    private suspend fun loadTopItems() {
        val items = try {
            socialRepository.getTopItems(targetUserId)
        } catch (_: Exception) {
            emptyList()
        }

        val movies = MutableList<TopItemDisplay?>(3) { null }
        val tvShows = MutableList<TopItemDisplay?>(3) { null }
        val games = MutableList<TopItemDisplay?>(3) { null }

        items.forEach { data ->
            val mediaType = data["mediaType"] as? String ?: return@forEach
            val pos = (data["position"] as? Long)?.toInt() ?: return@forEach
            if (pos !in 0..2) return@forEach
            val display = TopItemDisplay(
                mediaType = mediaType,
                position = pos,
                mediaId = (data["mediaId"] as? Long)?.toInt() ?: 0,
                title = data["title"] as? String ?: "",
                posterPath = data["posterPath"] as? String
            )
            when (mediaType) {
                "movie" -> movies[pos] = display
                "tv" -> tvShows[pos] = display
                "game" -> games[pos] = display
            }
        }

        _topMovies.value = movies
        _topTvShows.value = tvShows
        _topGames.value = games
    }

    private suspend fun loadLibrary() {
        val all = try {
            socialRepository.getLibrary(targetUserId)
        } catch (_: Exception) {
            emptyList()
        }
        val watched = all.filter { it.status == "watched" }
        _watchedMovies.value = watched.filter { it.mediaType == "movie" }
        _watchedTvShows.value = watched.filter { it.mediaType == "tv" }
        _watchedGames.value = watched.filter { it.mediaType == "game" }
    }

    /**
     * Alterna seguir / dejar de seguir. Si hay bloqueo activo, no hace nada
     * (la UI ya oculta el botón en ese caso, pero defendemos la lógica igual).
     */
    fun toggleFollow() {
        val myId = authManager.getCachedUserId() ?: return
        if (myId == targetUserId) return
        if (_blockRelation.value !is BlockRelation.NotBlocked) return
        viewModelScope.launch {
            try {
                if (_isFollowing.value) {
                    socialRepository.unfollow(myId, targetUserId)
                    _isFollowing.value = false
                    _user.value = _user.value?.let {
                        it.copy(followersCount = (it.followersCount - 1).coerceAtLeast(0))
                    }
                } else {
                    socialRepository.follow(myId, targetUserId)
                    _isFollowing.value = true
                    _user.value = _user.value?.let {
                        it.copy(followersCount = it.followersCount + 1)
                    }
                }
            } catch (_: Exception) { }
        }
    }

    /**
     * Bloquea al usuario del perfil actual. Tras bloquear:
     *  - El estado pasa a IBlockedThem.
     *  - Se limpian top items y library del UI (no se mostraban ya, pero por si acaso).
     *  - El isFollowing queda en false (se rompe el follow en el backend).
     *  - Se actualiza el contador de seguidores en el modelo en memoria.
     */
    fun blockUser() {
        val myId = authManager.getCachedUserId() ?: return
        if (myId == targetUserId) return
        viewModelScope.launch {
            try {
                socialRepository.ensureSignedIn()
                socialRepository.blockUser(myId, targetUserId)
                _blockRelation.value = BlockRelation.IBlockedThem
                _isFollowing.value = false
                _topMovies.value = listOf(null, null, null)
                _topTvShows.value = listOf(null, null, null)
                _topGames.value = listOf(null, null, null)
                _watchedMovies.value = emptyList()
                _watchedTvShows.value = emptyList()
                _watchedGames.value = emptyList()
            } catch (_: Exception) { }
        }
    }

    /**
     * Desbloquea al usuario y recarga el perfil completo.
     * No restaura los follows previos (es decisión consciente: si te bloqueé
     * y te desbloqueo, vuelves a empezar de cero).
     */
    fun unblockUser() {
        val myId = authManager.getCachedUserId() ?: return
        if (myId == targetUserId) return
        viewModelScope.launch {
            try {
                socialRepository.ensureSignedIn()
                socialRepository.unblockUser(myId, targetUserId)
                _blockRelation.value = BlockRelation.NotBlocked
                // Recargamos todo para repoblar top items y library
                load()
            } catch (_: Exception) { }
        }
    }
}