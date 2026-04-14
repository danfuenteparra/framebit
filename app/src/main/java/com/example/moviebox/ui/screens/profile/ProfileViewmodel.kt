package com.example.moviebox.ui.screens.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviebox.auth.AuthManager
import com.example.moviebox.data.local.dao.TopItemDao
import com.example.moviebox.data.local.entity.TopItemEntity
import com.example.moviebox.data.remote.dto.GameDto
import com.example.moviebox.data.remote.dto.MovieDto
import com.example.moviebox.data.remote.dto.TvShowDto
import com.example.moviebox.data.repository.GameRepository
import com.example.moviebox.data.repository.MovieRepository
import com.example.moviebox.data.repository.TvShowRepository
import com.auth0.android.result.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val topItemDao: TopItemDao,
    private val movieRepository: MovieRepository,
    private val tvShowRepository: TvShowRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState

    // Top items por tipo
    private val _topMovies = MutableStateFlow<List<TopItemEntity?>>(listOf(null, null, null))
    val topMovies: StateFlow<List<TopItemEntity?>> = _topMovies

    private val _topTvShows = MutableStateFlow<List<TopItemEntity?>>(listOf(null, null, null))
    val topTvShows: StateFlow<List<TopItemEntity?>> = _topTvShows

    private val _topGames = MutableStateFlow<List<TopItemEntity?>>(listOf(null, null, null))
    val topGames: StateFlow<List<TopItemEntity?>> = _topGames

    // Búsqueda
    private val _searchResults = MutableStateFlow<SearchResults>(SearchResults.Empty)
    val searchResults: StateFlow<SearchResults> = _searchResults

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val tmdbApiKey = "3ec3dbb22f2043fa67e0ddf84266ad61"
    private val rawgApiKey = "3391dac64bae44c1bed7a142c6008538"

    // ID del usuario actual (se obtiene de Auth0)
    private var currentUserId: String = ""

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val accessToken = authManager.getAccessToken()
                if (accessToken != null) {
                    val profile = authManager.getUserProfile(accessToken)
                    currentUserId = profile.getId() ?: ""
                    _uiState.value = ProfileUiState.Success(profile)
                    loadTopItems()
                } else {
                    _uiState.value = ProfileUiState.Error("No hay sesión activa")
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    private fun loadTopItems() {
        if (currentUserId.isBlank()) return
        viewModelScope.launch {
            topItemDao.getAllTopItems(currentUserId).collect { items ->
                val movies = MutableList<TopItemEntity?>(3) { null }
                val tvShows = MutableList<TopItemEntity?>(3) { null }
                val games = MutableList<TopItemEntity?>(3) { null }

                items.forEach { item ->
                    when (item.mediaType) {
                        "movie" -> if (item.position in 0..2) movies[item.position] = item
                        "tv" -> if (item.position in 0..2) tvShows[item.position] = item
                        "game" -> if (item.position in 0..2) games[item.position] = item
                    }
                }

                _topMovies.value = movies
                _topTvShows.value = tvShows
                _topGames.value = games
            }
        }
    }

    fun searchForType(query: String, mediaType: String) {
        _searchQuery.value = query
        if (query.length < 2) {
            _searchResults.value = SearchResults.Empty
            return
        }

        viewModelScope.launch {
            _searchResults.value = SearchResults.Loading
            try {
                when (mediaType) {
                    "movie" -> {
                        val result = movieRepository.searchMoviesFromApi(tmdbApiKey, query)
                        _searchResults.value = SearchResults.Movies(result.getOrDefault(emptyList()))
                    }
                    "tv" -> {
                        val result = tvShowRepository.searchTvShowsFromApi(tmdbApiKey, query)
                        _searchResults.value = SearchResults.TvShows(result.getOrDefault(emptyList()))
                    }
                    "game" -> {
                        val result = gameRepository.searchGamesFromApi(rawgApiKey, query)
                        _searchResults.value = SearchResults.Games(result.getOrDefault(emptyList()))
                    }
                }
            } catch (e: Exception) {
                _searchResults.value = SearchResults.Empty
            }
        }
    }

    fun setTopItem(mediaType: String, position: Int, mediaId: Int, title: String, posterPath: String?) {
        viewModelScope.launch {
            topItemDao.deleteTopItem(currentUserId, mediaType, position)
            topItemDao.insertTopItem(
                TopItemEntity(
                    userId = currentUserId,
                    mediaType = mediaType,
                    position = position,
                    mediaId = mediaId,
                    title = title,
                    posterPath = posterPath
                )
            )
        }
    }

    fun removeTopItem(mediaType: String, position: Int) {
        viewModelScope.launch {
            topItemDao.deleteTopItem(currentUserId, mediaType, position)
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = SearchResults.Empty
    }

    fun logout(activityContext: Context) {
        viewModelScope.launch {
            try {
                authManager.logout(activityContext)
                _uiState.value = ProfileUiState.LoggedOut
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Error al cerrar sesión")
            }
        }
    }
}

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    object LoggedOut : ProfileUiState()
    data class Success(val userProfile: UserProfile) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

sealed class SearchResults {
    object Empty : SearchResults()
    object Loading : SearchResults()
    data class Movies(val movies: List<MovieDto>) : SearchResults()
    data class TvShows(val tvShows: List<TvShowDto>) : SearchResults()
    data class Games(val games: List<GameDto>) : SearchResults()
}
