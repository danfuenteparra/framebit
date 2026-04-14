package com.example.moviebox.ui.screens.tvshowdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviebox.data.local.entity.ReviewEntity
import com.example.moviebox.data.local.entity.TvShowEntity
import com.example.moviebox.data.remote.dto.CastDto
import com.example.moviebox.data.remote.dto.TvShowDetailDto
import com.example.moviebox.data.repository.ReviewRepository
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
    }

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

    private fun checkLocalStatus() {
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
            val title = if (state is TvShowDetailUiState.Success) state.tvShow.name else ""
            reviewRepository.insertReview(ReviewEntity(mediaId = tvShowId, mediaType = "tv", mediaTitle = title, rating = rating, comment = comment))
        }
    }

    fun deleteReview(reviewId: Int) {
        viewModelScope.launch { reviewRepository.deleteReviewById(reviewId) }
    }

    private suspend fun ensureInRoom() {
        if (tvShowRepository.getTvShowById(tvShowId) == null) {
            val state = _uiState.value
            if (state is TvShowDetailUiState.Success) {
                val d = state.tvShow
                tvShowRepository.insertTvShow(TvShowEntity(id = d.id, name = d.name, overview = d.overview, posterPath = d.posterPath, backdropPath = d.backdropPath, firstAirDate = d.firstAirDate, voteAverage = d.voteAverage, voteCount = d.voteCount))
            }
        }
    }

    private suspend fun cleanupIfOrphan() {
        val existing = tvShowRepository.getTvShowById(tvShowId)
        if (existing != null && !existing.isWatchlisted && !existing.isFavorite && !existing.isWatched) {
            tvShowRepository.deleteTvShow(tvShowId)
        }
    }

    fun toggleWatchlisted() {
        viewModelScope.launch {
            ensureInRoom()
            val newVal = !_isWatchlisted.value
            tvShowRepository.toggleWatchlisted(tvShowId, newVal)
            _isWatchlisted.value = newVal
            if (!newVal) cleanupIfOrphan()
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            ensureInRoom()
            val newVal = !_isFavorite.value
            tvShowRepository.toggleFavorite(tvShowId, newVal)
            _isFavorite.value = newVal
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
            if (!newVal) cleanupIfOrphan()
        }
    }
}

sealed class TvShowDetailUiState {
    object Loading : TvShowDetailUiState()
    data class Success(val tvShow: TvShowDetailDto) : TvShowDetailUiState()
    data class Error(val message: String) : TvShowDetailUiState()
}