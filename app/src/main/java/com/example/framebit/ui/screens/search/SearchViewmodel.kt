package com.example.framebit.ui.screens.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.framebit.data.remote.dto.GameDto
import com.example.framebit.data.remote.dto.MovieDto
import com.example.framebit.data.remote.dto.PersonCreditDto
import com.example.framebit.data.remote.dto.PersonDto
import com.example.framebit.data.remote.dto.TvShowDto
import com.example.framebit.data.repository.GameRepository
import com.example.framebit.data.repository.MovieRepository
import com.example.framebit.data.repository.TvShowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchFilter { TITLE, PERSON }

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val tvShowRepository: TvShowRepository,
    private val gameRepository: GameRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // "movie", "tv" o "game" — determina qué se busca
    val mediaType: String = savedStateHandle["mediaType"] ?: "movie"

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _filter = MutableStateFlow(SearchFilter.TITLE)
    val filter: StateFlow<SearchFilter> = _filter

    private val tmdbApiKey = "3ec3dbb22f2043fa67e0ddf84266ad61"
    private val rawgApiKey = "3391dac64bae44c1bed7a142c6008538"

    fun onFilterChange(newFilter: SearchFilter) {
        _filter.value = newFilter
        _uiState.value = SearchUiState.Idle
        _query.value = ""
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        if (newQuery.length >= 2) search(newQuery) else _uiState.value = SearchUiState.Idle
    }

    fun onPersonSelected(person: PersonDto) {
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            try {
                val result = movieRepository.getPersonCombinedCredits(tmdbApiKey, person.id)
                if (result.isSuccess) {
                    val credits = result.getOrThrow()

                    // Filtrar según mediaType
                    val actedMovies = credits.cast.filter { it.mediaType == "movie" && it.posterPath != null }
                        .sortedByDescending { it.popularity }.distinctBy { it.id }
                    val actedTv = credits.cast.filter { it.mediaType == "tv" && it.posterPath != null }
                        .sortedByDescending { it.popularity }.distinctBy { it.id }
                    val directedMovies = credits.crew.filter { it.job == "Director" && it.mediaType == "movie" && it.posterPath != null }
                        .sortedByDescending { it.popularity }.distinctBy { it.id }
                    val directedTv = credits.crew.filter { it.job == "Director" && it.mediaType == "tv" && it.posterPath != null }
                        .sortedByDescending { it.popularity }.distinctBy { it.id }

                    // Si estamos en pestaña películas, mostrar solo pelis; si series, solo series
                    val filteredActed = when (mediaType) {
                        "movie" -> actedMovies
                        "tv" -> actedTv
                        else -> actedMovies + actedTv
                    }
                    val filteredDirected = when (mediaType) {
                        "movie" -> directedMovies
                        "tv" -> directedTv
                        else -> directedMovies + directedTv
                    }

                    _uiState.value = SearchUiState.PersonDetail(
                        person = person,
                        actedMovies = if (mediaType != "tv") actedMovies else emptyList(),
                        actedTvShows = if (mediaType != "movie") actedTv else emptyList(),
                        directedMovies = if (mediaType != "tv") directedMovies else emptyList(),
                        directedTvShows = if (mediaType != "movie") directedTv else emptyList()
                    )
                } else {
                    _uiState.value = SearchUiState.Error("Error al cargar filmografía")
                }
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun onExpandCategory(category: FilmCategory) {
        val current = _uiState.value
        if (current is SearchUiState.PersonDetail) {
            _uiState.value = SearchUiState.ExpandedCategory(
                person = current.person,
                category = category,
                items = when (category) {
                    FilmCategory.ACTED_MOVIES -> current.actedMovies
                    FilmCategory.ACTED_TV -> current.actedTvShows
                    FilmCategory.DIRECTED_MOVIES -> current.directedMovies
                    FilmCategory.DIRECTED_TV -> current.directedTvShows
                },
                previousState = current
            )
        }
    }

    fun onBackFromExpanded() {
        val current = _uiState.value
        if (current is SearchUiState.ExpandedCategory) {
            _uiState.value = current.previousState
        }
    }

    private fun search(query: String) {
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            try {
                when (_filter.value) {
                    SearchFilter.TITLE -> {
                        when (mediaType) {
                            "movie" -> {
                                val movies = movieRepository.searchMoviesFromApi(tmdbApiKey, query).getOrDefault(emptyList())
                                _uiState.value = SearchUiState.MovieResults(movies = movies)
                            }
                            "tv" -> {
                                val tvShows = tvShowRepository.searchTvShowsFromApi(tmdbApiKey, query).getOrDefault(emptyList())
                                _uiState.value = SearchUiState.TvShowResults(tvShows = tvShows)
                            }
                            "game" -> {
                                val games = gameRepository.searchGamesFromApi(rawgApiKey, query).getOrDefault(emptyList())
                                _uiState.value = SearchUiState.GameResults(games = games)
                            }
                        }
                    }
                    SearchFilter.PERSON -> {
                        val persons = movieRepository.searchPerson(tmdbApiKey, query).getOrDefault(emptyList()).take(10)
                        _uiState.value = SearchUiState.PersonList(persons = persons)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}

enum class FilmCategory { ACTED_MOVIES, ACTED_TV, DIRECTED_MOVIES, DIRECTED_TV }

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class MovieResults(val movies: List<MovieDto>) : SearchUiState()
    data class TvShowResults(val tvShows: List<TvShowDto>) : SearchUiState()
    data class GameResults(val games: List<GameDto>) : SearchUiState()
    data class PersonList(val persons: List<PersonDto>) : SearchUiState()
    data class PersonDetail(
        val person: PersonDto,
        val actedMovies: List<PersonCreditDto>,
        val actedTvShows: List<PersonCreditDto>,
        val directedMovies: List<PersonCreditDto>,
        val directedTvShows: List<PersonCreditDto>
    ) : SearchUiState()
    data class ExpandedCategory(
        val person: PersonDto,
        val category: FilmCategory,
        val items: List<PersonCreditDto>,
        val previousState: PersonDetail
    ) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}
