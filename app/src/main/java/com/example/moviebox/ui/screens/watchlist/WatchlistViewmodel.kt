package com.example.moviebox.ui.screens.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviebox.data.local.entity.MovieEntity
import com.example.moviebox.data.local.entity.TvShowEntity
import com.example.moviebox.data.repository.MovieRepository
import com.example.moviebox.data.repository.TvShowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val tvShowRepository: TvShowRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<WatchlistUiState>(WatchlistUiState.Loading)
    val uiState: StateFlow<WatchlistUiState> = _uiState

    init { loadWatchlist() }

    private fun loadWatchlist() {
        viewModelScope.launch {
            combine(
                movieRepository.getAllMovies(),
                tvShowRepository.getAllTvShows()
            ) { movies, tvShows ->
                WatchlistUiState.Success(
                    watchlistedMovies = movies.filter { it.isWatchlisted },
                    watchlistedTvShows = tvShows.filter { it.isWatchlisted },
                    favoriteMovies = movies.filter { it.isFavorite },
                    favoriteTvShows = tvShows.filter { it.isFavorite },
                    watchedMovies = movies.filter { it.isWatched },
                    watchedTvShows = tvShows.filter { it.isWatched }
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

    fun toggleTvShowWatched(tvShowId: Int, isWatched: Boolean) {
        viewModelScope.launch {
            tvShowRepository.toggleWatched(tvShowId, isWatched)
            if (isWatched) tvShowRepository.toggleWatchlisted(tvShowId, false)
        }
    }

    fun deleteMovie(movieId: Int) {
        viewModelScope.launch { movieRepository.deleteMovie(movieId) }
    }

    fun deleteTvShow(tvShowId: Int) {
        viewModelScope.launch { tvShowRepository.deleteTvShow(tvShowId) }
    }
}

sealed class WatchlistUiState {
    object Loading : WatchlistUiState()
    data class Success(
        val watchlistedMovies: List<MovieEntity>,
        val watchlistedTvShows: List<TvShowEntity>,
        val favoriteMovies: List<MovieEntity>,
        val favoriteTvShows: List<TvShowEntity>,
        val watchedMovies: List<MovieEntity>,
        val watchedTvShows: List<TvShowEntity>
    ) : WatchlistUiState()
    data class Error(val message: String) : WatchlistUiState()
}