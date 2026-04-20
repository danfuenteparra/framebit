package com.example.moviebox.ui.screens.tvshows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviebox.data.remote.dto.TvShowDto
import com.example.moviebox.data.repository.TvShowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TvShowsViewModel @Inject constructor(
    private val tvShowRepository: TvShowRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TvShowsUiState>(TvShowsUiState.Loading)
    val uiState: StateFlow<TvShowsUiState> = _uiState

    private val apiKey = "3ec3dbb22f2043fa67e0ddf84266ad61"

    init {
        loadContent()
    }

    fun loadContent() {
        viewModelScope.launch {
            _uiState.value = TvShowsUiState.Loading
            try {
                coroutineScope {
                    val onTheAirDeferred = async { tvShowRepository.getOnTheAirTvShowsFromApi(apiKey) }
                    val popularDeferred = async { tvShowRepository.getPopularTvShowsFromApi(apiKey) }
                    val topRatedDeferred = async { tvShowRepository.getTopRatedTvShowsFromApi(apiKey) }

                    awaitAll(onTheAirDeferred, popularDeferred, topRatedDeferred)

                    _uiState.value = TvShowsUiState.Success(
                        onTheAirTvShows = onTheAirDeferred.await().getOrDefault(emptyList()),
                        popularTvShows = popularDeferred.await().getOrDefault(emptyList()),
                        topRatedTvShows = topRatedDeferred.await().getOrDefault(emptyList())
                    )
                }
            } catch (e: Exception) {
                _uiState.value = TvShowsUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}

sealed class TvShowsUiState {
    object Loading : TvShowsUiState()
    data class Success(
        val onTheAirTvShows: List<TvShowDto>,
        val popularTvShows: List<TvShowDto>,
        val topRatedTvShows: List<TvShowDto>
    ) : TvShowsUiState()
    data class Error(val message: String) : TvShowsUiState()
}