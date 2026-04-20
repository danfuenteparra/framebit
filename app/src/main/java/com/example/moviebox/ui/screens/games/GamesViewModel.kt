package com.example.moviebox.ui.screens.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviebox.data.remote.dto.GameDto
import com.example.moviebox.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class GamesViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GamesUiState>(GamesUiState.Loading)
    val uiState: StateFlow<GamesUiState> = _uiState

    private val apiKey = "3391dac64bae44c1bed7a142c6008538"

    init {
        loadContent()
    }

    fun loadContent() {
        viewModelScope.launch {
            _uiState.value = GamesUiState.Loading
            try {
                val dates = buildLastYearRange()

                coroutineScope {
                    val popularDeferred = async { gameRepository.getPopularGamesFromApi(apiKey) }
                    val topRatedDeferred = async { gameRepository.getTopRatedGamesFromApi(apiKey) }
                    val recentDeferred = async { gameRepository.getRecentGamesFromApi(apiKey, dates) }

                    awaitAll(popularDeferred, topRatedDeferred, recentDeferred)

                    _uiState.value = GamesUiState.Success(
                        popularGames = popularDeferred.await().getOrDefault(emptyList()),
                        topRatedGames = topRatedDeferred.await().getOrDefault(emptyList()),
                        recentGames = recentDeferred.await().getOrDefault(emptyList())
                    )
                }
            } catch (e: Exception) {
                _uiState.value = GamesUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Construye un rango de fechas "yyyy-MM-dd,yyyy-MM-dd" desde hace un año hasta hoy.
     * Compatible con API 24+ (no usa java.time).
     */
    private fun buildLastYearRange(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = Calendar.getInstance()
        val yearAgo = Calendar.getInstance().apply { add(Calendar.YEAR, -1) }
        return "${formatter.format(yearAgo.time)},${formatter.format(today.time)}"
    }
}

sealed class GamesUiState {
    object Loading : GamesUiState()
    data class Success(
        val popularGames: List<GameDto>,
        val topRatedGames: List<GameDto>,
        val recentGames: List<GameDto>
    ) : GamesUiState()
    data class Error(val message: String) : GamesUiState()
}