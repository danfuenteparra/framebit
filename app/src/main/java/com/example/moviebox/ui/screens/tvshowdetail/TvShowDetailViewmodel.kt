package com.example.moviebox.ui.screens.tvshowdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviebox.auth.AuthManager
import com.example.moviebox.data.local.entity.ReviewEntity
import com.example.moviebox.data.local.entity.TvShowEntity
import com.example.moviebox.data.remote.dto.CastDto
import com.example.moviebox.data.remote.dto.TvShowDetailDto
import com.example.moviebox.data.remote.model.FriendReview
import com.example.moviebox.data.repository.ReviewRepository
import com.example.moviebox.data.repository.SocialRepository
import com.example.moviebox.data.repository.TvShowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TvShowDetailViewModel @Inject constructor(
    private val tvShowRepository: TvShowRepository,
    private val reviewRepository: ReviewRepository,
    private val socialRepository: SocialRepository,
    private val authManager: AuthManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tvShowId: Int = savedStateHandle["tvShowId"] ?: 0
    private val apiKey = "3ec3dbb22f2043fa67e0ddf84266ad61"

    private val _uiState = MutableStateFlow<TvShowDetailUiState>(TvShowDetailUiState.Loading)
    val uiState: StateFlow<TvShowDetailUiState> = _uiState

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

    private val _trailerKey = MutableStateFlow<String?>(null)
    val trailerKey: StateFlow<String?> = _trailerKey

    init {
        loadTvShowDetail()
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
                _friendReviews.value = socialRepository.getAllReviewsForMedia("tv", tvShowId, myId)
            } catch (_: Exception) { }
        }
    }

    fun refreshFriendReviews() = loadAllReviews()

    fun getMyUserId(): String = authManager.getCachedUserId() ?: ""

    private fun loadTvShowDetail() {
        viewModelScope.launch {
            _uiState.value = TvShowDetailUiState.Loading
            val result = tvShowRepository.getTvShowDetailsFromApi(apiKey, tvShowId)
            if (result.isSuccess) _uiState.value = TvShowDetailUiState.Success(result.getOrThrow())
            else _uiState.value = TvShowDetailUiState.Error("Error al cargar la serie")
        }
    }

    private fun loadCredits() {
        viewModelScope.launch {
            val result = tvShowRepository.getTvShowCredits(apiKey, tvShowId)
            if (result.isSuccess) _cast.value = result.getOrThrow().cast.sortedBy { it.order }.take(20)
        }
    }

    private fun loadVideos() {
        viewModelScope.launch {
            val result = tvShowRepository.getTvShowVideos(apiKey, tvShowId)
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
            val existing = tvShowRepository.getTvShowById(tvShowId)
            _isWatchlisted.value = existing?.isWatchlisted ?: false
            _isFavorite.value = existing?.isFavorite ?: false
            _isWatched.value = existing?.isWatched ?: false
        }
    }

    private fun loadReviews() {
        viewModelScope.launch {
            reviewRepository.getReviewsForMedia(tvShowId, "tv").collect { _reviews.value = it }
        }
    }

    fun addReview(rating: Float, comment: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val tv = (state as? TvShowDetailUiState.Success)?.tvShow
            val title = tv?.name ?: ""
            val posterPath = tv?.posterPath
            val releaseYear = tv?.firstAirDate?.take(4) ?: ""

            // 1) Guardar en Room
            reviewRepository.insertReview(
                ReviewEntity(
                    mediaId = tvShowId,
                    mediaType = "tv",
                    mediaTitle = title,
                    rating = rating,
                    comment = comment
                )
            )

            // 2) Auto-marcar como vista
            ensureInRoom()
            tvShowRepository.toggleWatched(tvShowId, true)
            _isWatched.value = true
            if (_isWatchlisted.value) {
                tvShowRepository.toggleWatchlisted(tvShowId, false)
                _isWatchlisted.value = false
            }

            // 3) Sincronizar con Firestore
            val userId = authManager.getCachedUserId()
            if (!userId.isNullOrBlank()) {
                try {
                    socialRepository.ensureSignedIn()
                    socialRepository.syncReview(
                        userId = userId,
                        userName = authManager.getCachedName() ?: "",
                        userPicture = authManager.getCachedPictureUrl(),
                        mediaType = "tv",
                        mediaId = tvShowId,
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
            val userId = authManager.getCachedUserId()
            if (!userId.isNullOrBlank()) {
                try {
                    socialRepository.deleteReviewRemote(userId, "tv", tvShowId)
                    loadAllReviews()
                } catch (_: Exception) { }
            }
        }
    }

    private suspend fun ensureInRoom() {
        if (tvShowRepository.getTvShowById(tvShowId) == null) {
            val state = _uiState.value
            if (state is TvShowDetailUiState.Success) {
                val d = state.tvShow
                tvShowRepository.insertTvShow(
                    TvShowEntity(
                        id = d.id,
                        name = d.name,
                        overview = d.overview,
                        posterPath = d.posterPath,
                        backdropPath = d.backdropPath,
                        firstAirDate = d.firstAirDate,
                        voteAverage = d.voteAverage,
                        voteCount = d.voteCount
                    )
                )
            }
        }
    }

    private suspend fun cleanupIfOrphan() {
        val existing = tvShowRepository.getTvShowById(tvShowId)
        if (existing != null && !existing.isWatchlisted && !existing.isFavorite && !existing.isWatched) {
            tvShowRepository.deleteTvShow(tvShowId)
            val userId = authManager.getCachedUserId()
            if (!userId.isNullOrBlank()) {
                try {
                    socialRepository.removeLibraryEntry(userId, "tv", tvShowId)
                } catch (_: Exception) { }
            }
        }
    }

    private fun mediaMeta(): Triple<String, String?, String> {
        val tv = (_uiState.value as? TvShowDetailUiState.Success)?.tvShow
        return Triple(
            tv?.name ?: "",
            tv?.posterPath,
            tv?.firstAirDate?.take(4) ?: ""
        )
    }

    fun toggleWatchlisted() {
        viewModelScope.launch {
            ensureInRoom()
            val newVal = !_isWatchlisted.value
            tvShowRepository.toggleWatchlisted(tvShowId, newVal)
            _isWatchlisted.value = newVal

            val userId = authManager.getCachedUserId()
            if (!userId.isNullOrBlank()) {
                try {
                    socialRepository.ensureSignedIn()
                    if (newVal) {
                        val (title, poster, year) = mediaMeta()
                        socialRepository.setLibraryStatus(
                            userId, "tv", tvShowId,
                            status = "watchlist",
                            isFavorite = _isFavorite.value,
                            title = title, posterPath = poster, releaseYear = year
                        )
                    } else if (!_isWatched.value && !_isFavorite.value) {
                        socialRepository.removeLibraryEntry(userId, "tv", tvShowId)
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
            tvShowRepository.toggleFavorite(tvShowId, newVal)
            _isFavorite.value = newVal

            val userId = authManager.getCachedUserId()
            if (!userId.isNullOrBlank()) {
                try {
                    socialRepository.ensureSignedIn()
                    val (title, poster, year) = mediaMeta()
                    socialRepository.setLibraryFavorite(
                        userId, "tv", tvShowId,
                        isFavorite = newVal,
                        title = title, posterPath = poster, releaseYear = year
                    )
                    if (!newVal && !_isWatched.value && !_isWatchlisted.value) {
                        socialRepository.removeLibraryEntry(userId, "tv", tvShowId)
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
            tvShowRepository.toggleWatched(tvShowId, newVal)
            _isWatched.value = newVal
            if (newVal && _isWatchlisted.value) {
                tvShowRepository.toggleWatchlisted(tvShowId, false)
                _isWatchlisted.value = false
            }

            val userId = authManager.getCachedUserId()
            if (!userId.isNullOrBlank()) {
                try {
                    socialRepository.ensureSignedIn()
                    if (newVal) {
                        val (title, poster, year) = mediaMeta()
                        socialRepository.setLibraryStatus(
                            userId, "tv", tvShowId,
                            status = "watched",
                            isFavorite = _isFavorite.value,
                            title = title, posterPath = poster, releaseYear = year
                        )
                    } else if (!_isFavorite.value) {
                        socialRepository.removeLibraryEntry(userId, "tv", tvShowId)
                    } else {
                        val (title, poster, year) = mediaMeta()
                        socialRepository.setLibraryStatus(
                            userId, "tv", tvShowId,
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

sealed class TvShowDetailUiState {
    object Loading : TvShowDetailUiState()
    data class Success(val tvShow: TvShowDetailDto) : TvShowDetailUiState()
    data class Error(val message: String) : TvShowDetailUiState()
}