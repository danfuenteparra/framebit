package com.example.moviebox.ui.screens.gamedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviebox.data.local.entity.GameEntity
import com.example.moviebox.data.local.entity.ReviewEntity
import com.example.moviebox.data.remote.dto.GameDetailDto
import com.example.moviebox.data.remote.dto.ScreenshotDto
import com.example.moviebox.data.repository.GameRepository
import com.example.moviebox.data.repository.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameDetailViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val reviewRepository: ReviewRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val gameId: Int = savedStateHandle["gameId"] ?: 0
    private val apiKey = "3391dac64bae44c1bed7a142c6008538"

    private val _uiState = MutableStateFlow<GameDetailUiState>(GameDetailUiState.Loading)
    val uiState: StateFlow<GameDetailUiState> = _uiState

    private val _isWatchlisted = MutableStateFlow(false)
    val isWatchlisted: StateFlow<Boolean> = _isWatchlisted

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite

    private val _isPlayed = MutableStateFlow(false)
    val isPlayed: StateFlow<Boolean> = _isPlayed

    private val _reviews = MutableStateFlow<List<ReviewEntity>>(emptyList())
    val reviews: StateFlow<List<ReviewEntity>> = _reviews

    private val _screenshots = MutableStateFlow<List<ScreenshotDto>>(emptyList())
    val screenshots: StateFlow<List<ScreenshotDto>> = _screenshots

    init {
        loadGameDetail()
        loadScreenshots()
        checkLocalStatus()
        loadReviews()
    }

    private fun loadGameDetail() {
        viewModelScope.launch {
            _uiState.value = GameDetailUiState.Loading
            val result = gameRepository.getGameDetailsFromApi(apiKey, gameId)
            if (result.isSuccess) _uiState.value = GameDetailUiState.Success(result.getOrThrow())
            else _uiState.value = GameDetailUiState.Error("Error al cargar el juego")
        }
    }

    private fun loadScreenshots() {
        viewModelScope.launch {
            val result = gameRepository.getGameScreenshots(apiKey, gameId)
            if (result.isSuccess) {
                _screenshots.value = result.getOrThrow()
            }
        }
    }

    private fun checkLocalStatus() {
        viewModelScope.launch {
            val existing = gameRepository.getGameById(gameId)
            _isWatchlisted.value = existing?.isWatchlisted ?: false
            _isFavorite.value = existing?.isFavorite ?: false
            _isPlayed.value = existing?.isPlayed ?: false
        }
    }

    private fun loadReviews() {
        viewModelScope.launch {
            reviewRepository.getReviewsForMedia(gameId, "game").collect { _reviews.value = it }
        }
    }

    fun addReview(rating: Float, comment: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val title = if (state is GameDetailUiState.Success) state.game.name else ""
            reviewRepository.insertReview(
                ReviewEntity(
                    mediaId = gameId,
                    mediaType = "game",
                    mediaTitle = title,
                    rating = rating,
                    comment = comment
                )
            )
        }
    }

    fun deleteReview(reviewId: Int) {
        viewModelScope.launch { reviewRepository.deleteReviewById(reviewId) }
    }

    private suspend fun ensureInRoom() {
        if (gameRepository.getGameById(gameId) == null) {
            val state = _uiState.value
            if (state is GameDetailUiState.Success) {
                val d = state.game
                gameRepository.insertGame(
                    GameEntity(
                        id = d.id,
                        name = d.name,
                        backgroundImage = d.backgroundImage,
                        released = d.released,
                        rating = d.rating,
                        ratingsCount = d.ratingsCount,
                        metacritic = d.metacritic,
                        genres = d.genres?.joinToString(", ") { it.name },
                        platforms = d.platforms?.joinToString(", ") { it.platform.name }
                    )
                )
            }
        }
    }

    private suspend fun cleanupIfOrphan() {
        val existing = gameRepository.getGameById(gameId)
        if (existing != null && !existing.isWatchlisted && !existing.isFavorite && !existing.isPlayed) {
            gameRepository.deleteGame(gameId)
        }
    }

    fun toggleWatchlisted() {
        viewModelScope.launch {
            ensureInRoom()
            val newVal = !_isWatchlisted.value
            gameRepository.toggleWatchlisted(gameId, newVal)
            _isWatchlisted.value = newVal
            if (!newVal) cleanupIfOrphan()
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            ensureInRoom()
            val newVal = !_isFavorite.value
            gameRepository.toggleFavorite(gameId, newVal)
            _isFavorite.value = newVal
            if (!newVal) cleanupIfOrphan()
        }
    }

    fun togglePlayed() {
        viewModelScope.launch {
            ensureInRoom()
            val newVal = !_isPlayed.value
            gameRepository.togglePlayed(gameId, newVal)
            _isPlayed.value = newVal
            if (newVal && _isWatchlisted.value) {
                gameRepository.toggleWatchlisted(gameId, false)
                _isWatchlisted.value = false
            }
            if (!newVal) cleanupIfOrphan()
        }
    }
}

sealed class GameDetailUiState {
    object Loading : GameDetailUiState()
    data class Success(val game: GameDetailDto) : GameDetailUiState()
    data class Error(val message: String) : GameDetailUiState()
}
