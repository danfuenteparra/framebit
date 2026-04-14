package com.example.moviebox.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviebox.data.remote.dto.MovieDto
import com.example.moviebox.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val movieRepository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    private val apiKey = "3ec3dbb22f2043fa67e0ddf84266ad61"

    init {
        loadContent()
    }

    fun loadContent() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val popularMovies = movieRepository.getPopularMoviesFromApi(apiKey)
                val topRatedMovies = movieRepository.getTopRatedMoviesFromApi(apiKey)

                _uiState.value = HomeUiState.Success(
                    popularMovies = popularMovies.getOrDefault(emptyList()),
                    topRatedMovies = topRatedMovies.getOrDefault(emptyList())
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val popularMovies: List<MovieDto>,
        val topRatedMovies: List<MovieDto>
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
