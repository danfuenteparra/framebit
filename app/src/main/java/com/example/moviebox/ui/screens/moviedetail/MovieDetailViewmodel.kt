package com.example.moviebox.ui.screens.moviedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviebox.auth.AuthManager
import com.example.moviebox.data.local.entity.MovieEntity
import com.example.moviebox.data.local.entity.ReviewEntity
import com.example.moviebox.data.remote.dto.CastDto
import com.example.moviebox.data.remote.dto.MovieDetailDto
import com.example.moviebox.data.repository.MovieRepository
import com.example.moviebox.data.repository.ReviewRepository
import com.example.moviebox.data.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val reviewRepository: ReviewRepository,
    private val socialRepository: SocialRepository,
    private val authManager: AuthManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val movieId: Int = savedStateHandle["movieId"] ?: 0
    private val apiKey = "3ec3dbb22f2043fa67e0ddf84266ad61"

    private val _uiState = MutableStateFlow<MovieDetailUiState>(MovieDetailUiState.Loading)
    val uiState: StateFlow<MovieDetailUiState> = _uiState

    private val _isWatchlisted = MutableStateFlow(false)
    val isWatchlisted: StateFlow<Boolean> = _isWatchlisted

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite

    private val _isWatched = MutableStateFlow(false)
    val isWatched: StateFlow<Boolean> = _isWatched

    private val _reviews = MutableStateFlow<List<ReviewEntity>>(emptyList())
    val reviews: StateFlow<List<ReviewEntity>> = _reviews

    private val _cast = MutableStateFlow<List<CastDto>>(emptyList())
    val cast: StateFlow<List<CastDto>> = _cast

    private val _director = MutableStateFlow<String?>(null)
    val director: StateFlow<String?> = _director

    private val _trailerKey = MutableStateFlow<String?>(null)
    val trailerKey: StateFlow<String?> = _trailerKey

    init {
        loadMovieDetail()
        loadCredits()
        loadVideos()
        checkLocalStatus()
        loadReviews()
    }

    private fun loadMovieDetail() {
        viewModelScope.launch {
            _uiState.value = MovieDetailUiState.Loading
            val result = movieRepository.getMovieDetailsFromApi(apiKey, movieId)
            if (result.isSuccess) _uiState.value = MovieDetailUiState.Success(result.getOrThrow())
            else _uiState.value = MovieDetailUiState.Error("Error al cargar la película")
        }
    }

    private fun loadCredits() {
        viewModelScope.launch {
            val result = movieRepository.getMovieCredits(apiKey, movieId)
            if (result.isSuccess) {
                val credits = result.getOrThrow()
                _cast.value = credits.cast.sortedBy { it.order }.take(20)
                _director.value = credits.crew.firstOrNull { it.job == "Director" }?.name
            }
        }
    }

    private fun loadVideos() {
        viewModelScope.launch {
            val result = movieRepository.getMovieVideos(apiKey, movieId)
            if (result.isSuccess) {
                val videos = result.getOrThrow().results
                _trailerKey.value = videos.firstOrNull { it.type == "Trailer" && it.site == "YouTube" && it.official }?.key
                    ?: videos.firstOrNull { it.type == "Trailer" && it.site == "YouTube" }?.key
                            ?: videos.firstOrNull { it.site == "YouTube" }?.key
            }
        }
    }

    private fun checkLocalStatus() {
        viewModelScope.launch {
            val existing = movieRepository.getMovieById(movieId)
            _isWatchlisted.value = existing?.isWatchlisted ?: false
            _isFavorite.value = existing?.isFavorite ?: false
            _isWatched.value = existing?.isWatched ?: false
        }
    }

    private fun loadReviews() {
        viewModelScope.launch {
            reviewRepository.getReviewsForMedia(movieId, "movie").collect { _reviews.value = it }
        }
    }

    fun addReview(rating: Float, comment: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val title = if (state is MovieDetailUiState.Success) state.movie.title else ""
            val posterPath = if (state is MovieDetailUiState.Success) state.movie.posterPath else null
            val releaseYear = if (state is MovieDetailUiState.Success) state.movie.releaseDate.take(4) else ""

            reviewRepository.insertReview(
                ReviewEntity(mediaId = movieId, mediaType = "movie", mediaTitle = title, rating = rating, comment = comment)
            )
            // Sincronizar reseña a Firestore
            val userId = authManager.getCachedUserId()
            if (userId != null) {
                try {
                    socialRepository.syncReview(
                        userId = userId,
                        userName = authManager.getCachedName().orEmpty(),
                        userPicture = authManager.getCachedPictureUrl(),
                        mediaType = "movie",
                        mediaId = movieId,
                        title = title,
                        posterPath = posterPath,
                        releaseYear = releaseYear,
                        rating = rating,
                        comment = comment,
                        isFavorite = _isFavorite.value
                    )
                } catch (_: Exception) { }
            }
        }
    }

    fun deleteReview(reviewId: Int) {
        viewModelScope.launch {
            reviewRepository.deleteReviewById(reviewId)
            val remaining = reviewRepository.getReviewsForMedia(movieId, "movie").first()
            if (remaining.isEmpty()) {
                val userId = authManager.getCachedUserId()
                if (userId != null) {
                    try {
                        socialRepository.deleteReviewRemote(userId, "movie", movieId)
                    } catch (_: Exception) { }
                }
            }
        }
    }

    private suspend fun ensureInRoom() {
        if (movieRepository.getMovieById(movieId) == null) {
            val state = _uiState.value
            if (state is MovieDetailUiState.Success) {
                val d = state.movie
                movieRepository.insertMovie(MovieEntity(id = d.id, title = d.title, overview = d.overview, posterPath = d.posterPath, backdropPath = d.backdropPath, releaseDate = d.releaseDate, voteAverage = d.voteAverage, voteCount = d.voteCount))
            }
        }
    }

    private suspend fun cleanupIfOrphan() {
        val existing = movieRepository.getMovieById(movieId)
        if (existing != null && !existing.isWatchlisted && !existing.isFavorite && !existing.isWatched) {
            movieRepository.deleteMovie(movieId)
        }
    }

    fun toggleWatchlisted() {
        viewModelScope.launch {
            ensureInRoom()
            val newVal = !_isWatchlisted.value
            movieRepository.toggleWatchlisted(movieId, newVal)
            _isWatchlisted.value = newVal
            if (!newVal) cleanupIfOrphan()
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            ensureInRoom()
            val newVal = !_isFavorite.value
            movieRepository.toggleFavorite(movieId, newVal)
            _isFavorite.value = newVal
            if (!newVal) cleanupIfOrphan()
        }
    }

    fun toggleWatched() {
        viewModelScope.launch {
            ensureInRoom()
            val newVal = !_isWatched.value
            movieRepository.toggleWatched(movieId, newVal)
            _isWatched.value = newVal
            if (newVal && _isWatchlisted.value) {
                movieRepository.toggleWatchlisted(movieId, false)
                _isWatchlisted.value = false
            }
            if (!newVal) cleanupIfOrphan()
        }
    }
}

sealed class MovieDetailUiState {
    object Loading : MovieDetailUiState()
    data class Success(val movie: MovieDetailDto) : MovieDetailUiState()
    data class Error(val message: String) : MovieDetailUiState()
}