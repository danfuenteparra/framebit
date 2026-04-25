package com.example.moviebox.ui.screens.tvshows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviebox.auth.AuthManager
import com.example.moviebox.data.remote.dto.TvShowDto
import com.example.moviebox.data.remote.model.FriendActivity
import com.example.moviebox.data.repository.SocialRepository
import com.example.moviebox.data.repository.TvShowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TvShowsViewModel @Inject constructor(
    private val tvShowRepository: TvShowRepository,
    private val socialRepository: SocialRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<TvShowsUiState>(TvShowsUiState.Loading)
    val uiState: StateFlow<TvShowsUiState> = _uiState

    private val _friendsTvShows = MutableStateFlow<List<FriendActivity>>(emptyList())
    val friendsTvShows: StateFlow<List<FriendActivity>> = _friendsTvShows

    private val apiKey = "3ec3dbb22f2043fa67e0ddf84266ad61"

    init {
        loadContent()
    }

    fun loadContent() {
        viewModelScope.launch {
            _uiState.value = TvShowsUiState.Loading
            try {
                val popularTvShows = tvShowRepository.getPopularTvShowsFromApi(apiKey)
                val topRatedTvShows = tvShowRepository.getTopRatedTvShowsFromApi(apiKey)

                _uiState.value = TvShowsUiState.Success(
                    popularTvShows = popularTvShows.getOrDefault(emptyList()),
                    topRatedTvShows = topRatedTvShows.getOrDefault(emptyList())
                )
            } catch (e: Exception) {
                _uiState.value = TvShowsUiState.Error(e.message ?: "Error desconocido")
            }
        }
        loadFriendsActivity()
    }

    fun loadFriendsActivity() {
        val userId = authManager.getCachedUserId() ?: return
        viewModelScope.launch {
            try {
                _friendsTvShows.value = socialRepository.getFriendsTvShows(userId)
            } catch (_: Exception) {
                _friendsTvShows.value = emptyList()
            }
        }
    }
}

sealed class TvShowsUiState {
    object Loading : TvShowsUiState()
    data class Success(
        val popularTvShows: List<TvShowDto>,
        val topRatedTvShows: List<TvShowDto>
    ) : TvShowsUiState()
    data class Error(val message: String) : TvShowsUiState()
}