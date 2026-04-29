package com.example.moviebox.ui.screens.moviedetail.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviebox.auth.AuthManager
import com.example.moviebox.data.remote.dto.MovieDto
import com.example.moviebox.data.remote.model.FriendActivity
import com.example.moviebox.data.repository.MovieRepository
import com.example.moviebox.data.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val socialRepository: SocialRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    private val _friendsMovies = MutableStateFlow<List<FriendActivity>>(emptyList())
    val friendsMovies: StateFlow<List<FriendActivity>> = _friendsMovies

    private val apiKey = "3ec3dbb22f2043fa67e0ddf84266ad61"

    init {
        loadContent()
    }

    fun loadContent() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val popularMovies = movieRepository.getPopularMoviesFromApi(apiKey)
                val topRatedMovies = movieRepository.getTopRatedMoviesFromApi(apiKey)

                _uiState.value = HomeUiState.Success(
                    popularMovies = popularMovies.getOrDefault(emptyList()),
                    topRatedMovies = topRatedMovies.getOrDefault(emptyList())
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Error desconocido")
            }
        }
        loadFriendsActivity()
    }

    /** Recarga la actividad de amigos (también llamable desde la UI al volver a la pantalla). */
    fun loadFriendsActivity() {
        val userId = authManager.getCachedUserId() ?: return
        viewModelScope.launch {
            try {
                _friendsMovies.value = socialRepository.getFriendsMovies(userId)
            } catch (_: Exception) {
                _friendsMovies.value = emptyList()
            }
        }
    }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val popularMovies: List<MovieDto>,
        val topRatedMovies: List<MovieDto>
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}