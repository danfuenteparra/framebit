package com.example.moviebox.ui.screens.moviedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviebox.auth.AuthManager
import com.example.moviebox.data.local.entity.MovieEntity
import com.example.moviebox.data.local.entity.ReviewEntity
import com.example.moviebox.data.remote.dto.CastDto
import com.example.moviebox.data.remote.dto.MovieDetailDto
import com.example.moviebox.data.remote.model.FriendReview
import com.example.moviebox.data.repository.MovieRepository
import com.example.moviebox.data.repository.ReviewRepository
import com.example.moviebox.data.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private val _friendReviews = MutableStateFlow<List<FriendReview>>(emptyList())
    val friendReviews: StateFlow<List<FriendReview>> = _friendReviews

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
        loadAllReviews()
    }

    private fun loadAllReviews() {
        viewModelScope.launch {
            try {
                socialRepository.ensureSignedIn()
                val myId = authManager.getCachedUserId() ?: ""
                val result = socialRepository.getAllReviewsForMedia("movie", movieId, myId)
                android.util.Log.d("ReviewsDebug", "OK movieId=$movieId myId=$myId count=${result.size}")
                result.forEach { android.util.Log.d("ReviewsDebug", "  -> ${it.userId} / ${it.userName} / rating=${it.rating}") }
                _friendReviews.value = result
            } catch (e: Exception) {
                android.util.Log.e("ReviewsDebug", "ERROR cargando reviews para movieId=$movieId", e)
            }
        }
    }

    /** Llamar para refrescar reseñas remotas (tras añadir/borrar la propia, p.ej.). */
    fun refreshFriendReviews() = loadAllReviews()

    /** Devuelve el userId del usuario actual (vacío si no hay sesión). */
    fun getMyUserId(): String = authManager.getCachedUserId() ?: ""

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

    fun checkLocalStatus() {
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
            val movie = (state as? MovieDetailUiState.Success)?.movie
            val title = movie?.title ?: ""
            val posterPath = movie?.posterPath
            val releaseYear = movie?.releaseDate?.take(4) ?: ""

            // 1) Guardar en Room (offline)
            reviewRepository.insertReview(
                ReviewEntity(
                    mediaId = movieId,
                    mediaType = "movie",
                    mediaTitle = title,
                    rating = rating,
                    comment = comment
                )
            )

            // 2) Marcar como visto localmente (auto)
            ensureInRoom()
            movieRepository.toggleWatched(movieId, true)
            _isWatched.value = true
            if (_isWatchlisted.value) {
                movieRepository.toggleWatchlisted(movieId, false)
                _isWatchlisted.value = false
            }

            // 3) Sincronizar con Firestore (review + library auto-watched dentro de upsertReview)
            val userId = authManager.getCachedUserId()
            if (!userId.isNullOrBlank()) {
                try {
                    socialRepository.ensureSignedIn()
                    socialRepository.syncReview(
                        userId = userId,
                        userName = authManager.getCachedName() ?: "",
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
                    loadAllReviews()
                } catch (_: Exception) { }
            }
        }
    }

    fun deleteReview(reviewId: Int) {
        viewModelScope.launch {
            reviewRepository.deleteReviewById(reviewId)
            // Sync remoto: borrar review pero mantener "visto" en library
            val userId = authManager.getCachedUserId()
            if (!userId.isNullOrBlank()) {
                try {
                    socialRepository.deleteReviewRemote(userId, "movie", movieId)
                    loadAllReviews()
                } catch (_: Exception) { }
            }
        }
    }

    private suspend fun ensureInRoom() {
        if (movieRepository.getMovieById(movieId) == null) {
            val state = _uiState.value
            if (state is MovieDetailUiState.Success) {
                val d = state.movie
                movieRepository.insertMovie(
                    MovieEntity(
                        id = d.id,
                        title = d.title,
                        overview = d.overview,
                        posterPath = d.posterPath,
                        backdropPath = d.backdropPath,
                        releaseDate = d.releaseDate,
                        voteAverage = d.voteAverage,
                        voteCount = d.voteCount
                    )
                )
            }
        }
    }

    private suspend fun cleanupIfOrphan() {
        val existing = movieRepository.getMovieById(movieId)
        if (existing != null && !existing.isWatchlisted && !existing.isFavorite && !existing.isWatched) {
            movieRepository.deleteMovie(movieId)
            // Sync remoto: borrar entrada de library (perdió todos los flags)
            val userId = authManager.getCachedUserId()
            if (!userId.isNullOrBlank()) {
                try {
                    socialRepository.removeLibraryEntry(userId, "movie", movieId)
                } catch (_: Exception) { }
            }
        }
    }

    private fun mediaMeta(): Triple<String, String?, String> {
        val movie = (_uiState.value as? MovieDetailUiState.Success)?.movie
        return Triple(
            movie?.title ?: "",
            movie?.posterPath,
            movie?.releaseDate?.take(4) ?: ""
        )
    }

    fun toggleWatchlisted() {
        viewModelScope.launch {
            ensureInRoom()
            val newVal = !_isWatchlisted.value
            movieRepository.toggleWatchlisted(movieId, newVal)
            _isWatchlisted.value = newVal

            val userId = authManager.getCachedUserId()
            if (!userId.isNullOrBlank()) {
                try {
                    socialRepository.ensureSignedIn()
                    if (newVal) {
                        val (title, poster, year) = mediaMeta()
                        socialRepository.setLibraryStatus(
                            userId, "movie", movieId,
                            status = "watchlist",
                            isFavorite = _isFavorite.value,
                            title = title, posterPath = poster, releaseYear = year
                        )
                    } else if (!_isWatched.value && !_isFavorite.value) {
                        socialRepository.removeLibraryEntry(userId, "movie", movieId)
                    }
                } catch (_: Exception) { }
            }

            if (!newVal) cleanupIfOrphan()
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            ensureInRoom()
            val newVal = !_isFavorite.value
            movieRepository.toggleFavorite(movieId, newVal)
            _isFavorite.value = newVal

            val userId = authManager.getCachedUserId()
            if (!userId.isNullOrBlank()) {
                try {
                    socialRepository.ensureSignedIn()
                    val (title, poster, year) = mediaMeta()
                    socialRepository.setLibraryFavorite(
                        userId, "movie", movieId,
                        isFavorite = newVal,
                        title = title, posterPath = poster, releaseYear = year
                    )
                    if (!newVal && !_isWatched.value && !_isWatchlisted.value) {
                        socialRepository.removeLibraryEntry(userId, "movie", movieId)
                    }
                } catch (_: Exception) { }
            }

            if (!newVal) cleanupIfOrphan()
        }
    }

    fun toggleWatched() {
        viewModelScope.launch {
            ensureInRoom()
            val newVal = !_isWatched.value
            movieRepository.toggleWatched(movieId, newVal)
            _isWatched.value = newVal
            // Al marcar como vista, quitar de watchlist
            if (newVal && _isWatchlisted.value) {
                movieRepository.toggleWatchlisted(movieId, false)
                _isWatchlisted.value = false
            }

            val userId = authManager.getCachedUserId()
            if (!userId.isNullOrBlank()) {
                try {
                    socialRepository.ensureSignedIn()
                    if (newVal) {
                        val (title, poster, year) = mediaMeta()
                        socialRepository.setLibraryStatus(
                            userId, "movie", movieId,
                            status = "watched",
                            isFavorite = _isFavorite.value,
                            title = title, posterPath = poster, releaseYear = year
                        )
                    } else if (!_isFavorite.value) {
                        // Si no es favorita y se desmarca visto, quitar entrada
                        socialRepository.removeLibraryEntry(userId, "movie", movieId)
                    } else {
                        // Mantener entrada (es favorita) pero pasar a watchlist
                        val (title, poster, year) = mediaMeta()
                        socialRepository.setLibraryStatus(
                            userId, "movie", movieId,
                            status = "watchlist",
                            isFavorite = true,
                            title = title, posterPath = poster, releaseYear = year
                        )
                    }
                } catch (_: Exception) { }
            }

            if (!newVal) cleanupIfOrphan()
        }
    }

    /**
     * Alterna el "me gusta" sobre una reseña con actualización
     * optimista: cambia el estado local al instante y revierte si
     * la llamada al backend falla.
     */
    fun toggleLike(review: FriendReview) {
        val myId = authManager.getCachedUserId() ?: return

        // 1) Update optimista en memoria (UI instantánea)
        val current = _friendReviews.value
        val optimistic = current.map { fr ->
            if (fr.reviewId == review.reviewId) {
                fr.copy(
                    likedByMe = !fr.likedByMe,
                    likesCount = (fr.likesCount + if (fr.likedByMe) -1 else 1)
                        .coerceAtLeast(0)
                )
            } else fr
        }
        _friendReviews.value = optimistic

        // 2) Llamada al backend; si falla, revertimos
        viewModelScope.launch {
            try {
                socialRepository.ensureSignedIn()
                val parts = FriendReview.parseId(review.reviewId) ?: return@launch
                val (authorId, mediaType, mediaId) = parts
                socialRepository.toggleLike(authorId, mediaType, mediaId, myId)
            } catch (_: Exception) {
                // Revertir si algo falló
                _friendReviews.value = current
            }
        }
    }
}

sealed class MovieDetailUiState {
    object Loading : MovieDetailUiState()
    data class Success(val movie: MovieDetailDto) : MovieDetailUiState()
    data class Error(val message: String) : MovieDetailUiState()
}