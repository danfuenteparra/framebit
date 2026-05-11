package com.example.framebit.ui.screens.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.framebit.data.local.entity.GameEntity
import com.example.framebit.data.local.entity.MovieEntity
import com.example.framebit.data.local.entity.TvShowEntity
import com.example.framebit.data.repository.GameRepository
import com.example.framebit.data.repository.MovieRepository
import com.example.framebit.data.repository.TvShowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val tvShowRepository: TvShowRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<WatchlistUiState>(WatchlistUiState.Loading)
    val uiState: StateFlow<WatchlistUiState> = _uiState

    init { loadWatchlist() }

    private fun loadWatchlist() {
        viewModelScope.launch {
            combine(
                movieRepository.getAllMovies(),
                tvShowRepository.getAllTvShows(),
                gameRepository.getAllGames()
            ) { movies, tvShows, games ->
                WatchlistUiState.Success(
                    watchlistedMovies = movies.filter { it.isWatchlisted },
                    favoriteMovies = movies.filter { it.isFavorite },
                    watchedMovies = movies.filter { it.isWatched },
                    watchlistedTvShows = tvShows.filter { it.isWatchlisted },
                    favoriteTvShows = tvShows.filter { it.isFavorite },
                    watchedTvShows = tvShows.filter { it.isWatched },
                    watchlistedGames = games.filter { it.isWatchlisted },
                    favoriteGames = games.filter { it.isFavorite },
                    playedGames = games.filter { it.isPlayed }
                )
            }.collect { _uiState.value = it }
        }
    }

    fun toggleMovieWatched(movieId: Int, isWatched: Boolean) {
        viewModelScope.launch {
            movieRepository.toggleWatched(movieId, isWatched)
            if (isWatched) movieRepository.toggleWatchlisted(movieId, false)
        }
    }

    fun deleteMovie(movieId: Int) {
        viewModelScope.launch { movieRepository.deleteMovie(movieId) }
    }

    fun toggleTvShowWatched(tvShowId: Int, isWatched: Boolean) {
        viewModelScope.launch {
            tvShowRepository.toggleWatched(tvShowId, isWatched)
            if (isWatched) tvShowRepository.toggleWatchlisted(tvShowId, false)
        }
    }

    fun deleteTvShow(tvShowId: Int) {
        viewModelScope.launch { tvShowRepository.deleteTvShow(tvShowId) }
    }

    fun toggleGamePlayed(gameId: Int, isPlayed: Boolean) {
        viewModelScope.launch {
            gameRepository.togglePlayed(gameId, isPlayed)
            if (isPlayed) gameRepository.toggleWatchlisted(gameId, false)
        }
    }

    fun deleteGame(gameId: Int) {
        viewModelScope.launch { gameRepository.deleteGame(gameId) }
    }
}

sealed class WatchlistUiState {
    object Loading : WatchlistUiState()
    data class Success(
        val watchlistedMovies: List<MovieEntity>,
        val favoriteMovies: List<MovieEntity>,
        val watchedMovies: List<MovieEntity>,
        val watchlistedTvShows: List<TvShowEntity>,
        val favoriteTvShows: List<TvShowEntity>,
        val watchedTvShows: List<TvShowEntity>,
        val watchlistedGames: List<GameEntity>,
        val favoriteGames: List<GameEntity>,
        val playedGames: List<GameEntity>
    ) : WatchlistUiState()
    data class Error(val message: String) : WatchlistUiState()
}
