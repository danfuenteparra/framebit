package com.example.moviebox.ui.screens.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviebox.data.remote.dto.GameDto
import com.example.moviebox.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
                val today = LocalDate.now()
                val yearAgo = today.minusYears(1)
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val dates = "${yearAgo.format(formatter)},${today.format(formatter)}"

                val popularGames = gameRepository.getPopularGamesFromApi(apiKey)
                val topRatedGames = gameRepository.getTopRatedGamesFromApi(apiKey)
                val recentGames = gameRepository.getRecentGamesFromApi(apiKey, dates)

                _uiState.value = GamesUiState.Success(
                    popularGames = popularGames.getOrDefault(emptyList()),
                    topRatedGames = topRatedGames.getOrDefault(emptyList()),
                    recentGames = recentGames.getOrDefault(emptyList())
                )
            } catch (e: Exception) {
                _uiState.value = GamesUiState.Error(e.message ?: "Error desconocido")
            }
        }
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