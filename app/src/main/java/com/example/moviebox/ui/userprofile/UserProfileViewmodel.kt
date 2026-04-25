package com.example.moviebox.ui.screens.userprofile

import androidx.lifecycle.SavedStateHandle
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

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    val isOwnProfile: Boolean
        get() = targetUserId == authManager.getCachedUserId()

    init {
        load()
    }

    private fun load() {
        if (targetUserId.isBlank()) {
            _loading.value = false
            return
        }
        viewModelScope.launch {
            _loading.value = true
            try {
                _user.value = socialRepository.getUser(targetUserId)
                val myId = authManager.getCachedUserId()
                if (myId != null && myId != targetUserId) {
                    _isFollowing.value = socialRepository.isFollowing(myId, targetUserId)
                }
                loadTopItems()
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

    fun toggleFollow() {
        val myId = authManager.getCachedUserId() ?: return
        if (myId == targetUserId) return
        viewModelScope.launch {
            try {
                if (_isFollowing.value) {
                    socialRepository.unfollow(myId, targetUserId)
                    _isFollowing.value = false
                    _user.value = _user.value?.let { it.copy(followersCount = (it.followersCount - 1).coerceAtLeast(0)) }
                } else {
                    socialRepository.follow(myId, targetUserId)
                    _isFollowing.value = true
                    _user.value = _user.value?.let { it.copy(followersCount = it.followersCount + 1) }
                }
            } catch (_: Exception) { }
        }
    }
}
