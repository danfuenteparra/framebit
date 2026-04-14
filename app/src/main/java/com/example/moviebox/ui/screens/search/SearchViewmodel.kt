package com.example.moviebox.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviebox.data.remote.dto.GenreDto
import com.example.moviebox.data.remote.dto.MovieDto
import com.example.moviebox.data.remote.dto.PersonCreditDto
import com.example.moviebox.data.remote.dto.PersonDto
import com.example.moviebox.data.remote.dto.TvShowDto
import com.example.moviebox.data.repository.MovieRepository
import com.example.moviebox.data.repository.TvShowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchFilter { TITLE, PERSON, GENRE }
enum class MediaTypeFilter { MOVIES, TV_SHOWS }

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val tvShowRepository: TvShowRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _filter = MutableStateFlow(SearchFilter.TITLE)
    val filter: StateFlow<SearchFilter> = _filter

    private val _mediaTypeFilter = MutableStateFlow(MediaTypeFilter.MOVIES)
    val mediaTypeFilter: StateFlow<MediaTypeFilter> = _mediaTypeFilter

    private val _genres = MutableStateFlow<List<GenreDto>>(emptyList())
    val genres: StateFlow<List<GenreDto>> = _genres

    private val apiKey = "3ec3dbb22f2043fa67e0ddf84266ad61"

    fun onFilterChange(newFilter: SearchFilter) {
        _filter.value = newFilter
        _uiState.value = SearchUiState.Idle
        _query.value = ""
        if (newFilter == SearchFilter.GENRE) loadGenresForCurrentType()
    }

    fun onMediaTypeChange(type: MediaTypeFilter) {
        _mediaTypeFilter.value = type
        if (_filter.value == SearchFilter.GENRE) loadGenresForCurrentType()
    }

    private fun loadGenresForCurrentType() {
        viewModelScope.launch {
            val result = when (_mediaTypeFilter.value) {
                MediaTypeFilter.MOVIES -> movieRepository.getMovieGenres(apiKey)
                MediaTypeFilter.TV_SHOWS -> tvShowRepository.getTvShowGenres(apiKey)
            }
            _genres.value = result.getOrDefault(emptyList()).sortedBy { it.name }
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        if (newQuery.length >= 2) search(newQuery) else _uiState.value = SearchUiState.Idle
    }

    fun onGenreSelected(genre: GenreDto) {
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            try {
                when (_mediaTypeFilter.value) {
                    MediaTypeFilter.MOVIES -> {
                        val movies = movieRepository.discoverMoviesByGenre(apiKey, genre.id).getOrDefault(emptyList())
                        _uiState.value = SearchUiState.GenreMovieResults(label = genre.name, movies = movies)
                    }
                    MediaTypeFilter.TV_SHOWS -> {
                        val tvShows = tvShowRepository.discoverTvShowsByGenre(apiKey, genre.id).getOrDefault(emptyList())
                        _uiState.value = SearchUiState.GenreTvShowResults(label = genre.name, tvShows = tvShows)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun onPersonSelected(person: PersonDto) {
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            try {
                val result = movieRepository.getPersonCombinedCredits(apiKey, person.id)
                if (result.isSuccess) {
                    val credits = result.getOrThrow()

                    val actedMovies = credits.cast.filter { it.mediaType == "movie" && it.posterPath != null }
                        .sortedByDescending { it.popularity }.distinctBy { it.id }
                    val actedTv = credits.cast.filter { it.mediaType == "tv" && it.posterPath != null }
                        .sortedByDescending { it.popularity }.distinctBy { it.id }
                    val directedMovies = credits.crew.filter { it.job == "Director" && it.mediaType == "movie" && it.posterPath != null }
                        .sortedByDescending { it.popularity }.distinctBy { it.id }
                    val directedTv = credits.crew.filter { it.job == "Director" && it.mediaType == "tv" && it.posterPath != null }
                        .sortedByDescending { it.popularity }.distinctBy { it.id }

                    _uiState.value = SearchUiState.PersonDetail(
                        person = person,
                        actedMovies = actedMovies,
                        actedTvShows = actedTv,
                        directedMovies = directedMovies,
                        directedTvShows = directedTv
                    )
                } else {
                    _uiState.value = SearchUiState.Error("Error al cargar filmografía")
                }
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    // Expandir una categoría para ver todas
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
                // Guardar estado anterior para poder volver
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
                        val movies = movieRepository.searchMoviesFromApi(apiKey, query).getOrDefault(emptyList()).take(5)
                        val tvShows = tvShowRepository.searchTvShowsFromApi(apiKey, query).getOrDefault(emptyList()).take(5)
                        _uiState.value = SearchUiState.TitleResults(movies = movies, tvShows = tvShows)
                    }
                    SearchFilter.PERSON -> {
                        val persons = movieRepository.searchPerson(apiKey, query).getOrDefault(emptyList()).take(10)
                        _uiState.value = SearchUiState.PersonList(persons = persons)
                    }
                    SearchFilter.GENRE -> {}
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
    data class TitleResults(val movies: List<MovieDto>, val tvShows: List<TvShowDto>) : SearchUiState()
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
    data class GenreMovieResults(val label: String, val movies: List<MovieDto>) : SearchUiState()
    data class GenreTvShowResults(val label: String, val tvShows: List<TvShowDto>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}