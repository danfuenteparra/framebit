package com.example.framebit.ui.screens.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.framebit.auth.AuthManager
import com.example.framebit.data.local.entity.GameEntity
import com.example.framebit.data.local.entity.MovieEntity
import com.example.framebit.data.local.entity.ReviewEntity
import com.example.framebit.data.local.entity.TvShowEntity
import com.example.framebit.data.remote.dto.GameDto
import com.example.framebit.data.remote.dto.MovieDto
import com.example.framebit.data.remote.dto.TvShowDto
import com.example.framebit.data.repository.GameRepository
import com.example.framebit.data.repository.MovieRepository
import com.example.framebit.data.repository.ReviewRepository
import com.example.framebit.data.repository.SocialRepository
import com.example.framebit.data.repository.TvShowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ReviewMediaType { MOVIE, TV, GAME }

sealed class AddReviewSearchState {
    object Idle : AddReviewSearchState()
    object Loading : AddReviewSearchState()
    data class MovieResults(val items: List<MovieDto>) : AddReviewSearchState()
    data class TvShowResults(val items: List<TvShowDto>) : AddReviewSearchState()
    data class GameResults(val items: List<GameDto>) : AddReviewSearchState()
    data class Error(val message: String) : AddReviewSearchState()
}

data class SelectedMedia(
    val id: Int,
    val title: String,
    val posterUrl: String?,
    val releaseYear: String,
    val type: ReviewMediaType
)

@HiltViewModel
class AddReviewViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val tvShowRepository: TvShowRepository,
    private val gameRepository: GameRepository,
    private val reviewRepository: ReviewRepository,
    private val socialRepository: SocialRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val tmdbApiKey = "3ec3dbb22f2043fa67e0ddf84266ad61"
    private val rawgApiKey = "3391dac64bae44c1bed7a142c6008538"

    private val _mediaType = MutableStateFlow(ReviewMediaType.MOVIE)
    val mediaType: StateFlow<ReviewMediaType> = _mediaType

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _searchState = MutableStateFlow<AddReviewSearchState>(AddReviewSearchState.Idle)
    val searchState: StateFlow<AddReviewSearchState> = _searchState

    private val _selected = MutableStateFlow<SelectedMedia?>(null)
    val selected: StateFlow<SelectedMedia?> = _selected

    private val _reviewSaved = MutableStateFlow(false)
    val reviewSaved: StateFlow<Boolean> = _reviewSaved

    fun onMediaTypeChange(type: ReviewMediaType) {
        if (_mediaType.value == type) return
        _mediaType.value = type
        _query.value = ""
        _searchState.value = AddReviewSearchState.Idle
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        if (newQuery.length >= 2) search(newQuery)
        else _searchState.value = AddReviewSearchState.Idle
    }

    private fun search(query: String) {
        viewModelScope.launch {
            _searchState.value = AddReviewSearchState.Loading
            try {
                when (_mediaType.value) {
                    ReviewMediaType.MOVIE -> {
                        val r = movieRepository.searchMoviesFromApi(tmdbApiKey, query).getOrDefault(emptyList())
                        _searchState.value = AddReviewSearchState.MovieResults(r)
                    }
                    ReviewMediaType.TV -> {
                        val r = tvShowRepository.searchTvShowsFromApi(tmdbApiKey, query).getOrDefault(emptyList())
                        _searchState.value = AddReviewSearchState.TvShowResults(r)
                    }
                    ReviewMediaType.GAME -> {
                        val r = gameRepository.searchGamesFromApi(rawgApiKey, query).getOrDefault(emptyList())
                        _searchState.value = AddReviewSearchState.GameResults(r)
                    }
                }
            } catch (e: Exception) {
                _searchState.value = AddReviewSearchState.Error(e.message ?: "Error de búsqueda")
            }
        }
    }

    fun onSelectMovie(movie: MovieDto) {
        _selected.value = SelectedMedia(
            id = movie.id,
            title = movie.title,
            posterUrl = movie.posterPath,
            releaseYear = movie.releaseDate.take(4),
            type = ReviewMediaType.MOVIE
        )
    }

    fun onSelectTvShow(tvShow: TvShowDto) {
        _selected.value = SelectedMedia(
            id = tvShow.id,
            title = tvShow.name,
            posterUrl = tvShow.posterPath,
            releaseYear = tvShow.firstAirDate.take(4),
            type = ReviewMediaType.TV
        )
    }

    fun onSelectGame(game: GameDto) {
        _selected.value = SelectedMedia(
            id = game.id,
            title = game.name,
            posterUrl = game.backgroundImage,
            releaseYear = game.released?.take(4) ?: "",
            type = ReviewMediaType.GAME
        )
    }

    fun clearSelection() {
        _selected.value = null
    }

    fun saveReview(rating: Float, comment: String) {
        val sel = _selected.value ?: return
        viewModelScope.launch {
            ensureMediaInRoom(sel)
            val mediaTypeStr = when (sel.type) {
                ReviewMediaType.MOVIE -> "movie"
                ReviewMediaType.TV -> "tv"
                ReviewMediaType.GAME -> "game"
            }
            // Guardar en Room
            reviewRepository.insertReview(
                ReviewEntity(
                    mediaId = sel.id,
                    mediaType = mediaTypeStr,
                    mediaTitle = sel.title,
                    rating = rating,
                    comment = comment
                )
            )
            // Marcar como visto/jugado y sacar de watchlist (Room)
            when (sel.type) {
                ReviewMediaType.MOVIE -> {
                    movieRepository.toggleWatched(sel.id, true)
                    movieRepository.toggleWatchlisted(sel.id, false)
                }
                ReviewMediaType.TV -> {
                    tvShowRepository.toggleWatched(sel.id, true)
                    tvShowRepository.toggleWatchlisted(sel.id, false)
                }
                ReviewMediaType.GAME -> {
                    gameRepository.togglePlayed(sel.id, true)
                    gameRepository.toggleWatchlisted(sel.id, false)
                }
            }
            // Sincronizar a Firestore (la review hace auto-watched en library dentro de upsertReview)
            val userId = authManager.getCachedUserId()
            if (userId != null) {
                try {
                    socialRepository.syncReview(
                        userId = userId,
                        userName = authManager.getCachedName().orEmpty(),
                        userPicture = authManager.getCachedPictureUrl(),
                        mediaType = mediaTypeStr,
                        mediaId = sel.id,
                        title = sel.title,
                        posterPath = sel.posterUrl,
                        releaseYear = sel.releaseYear,
                        rating = rating,
                        comment = comment,
                        isFavorite = false
                    )
                } catch (_: Exception) { }
            }
            _reviewSaved.value = true
        }
    }

    private suspend fun ensureMediaInRoom(sel: SelectedMedia) {
        when (sel.type) {
            ReviewMediaType.MOVIE -> {
                if (movieRepository.getMovieById(sel.id) == null) {
                    movieRepository.insertMovie(
                        MovieEntity(
                            id = sel.id, title = sel.title, overview = "",
                            posterPath = sel.posterUrl, backdropPath = null,
                            releaseDate = "", voteAverage = 0.0, voteCount = 0
                        )
                    )
                }
            }
            ReviewMediaType.TV -> {
                if (tvShowRepository.getTvShowById(sel.id) == null) {
                    tvShowRepository.insertTvShow(
                        TvShowEntity(
                            id = sel.id, name = sel.title, overview = "",
                            posterPath = sel.posterUrl, backdropPath = null,
                            firstAirDate = "", voteAverage = 0.0, voteCount = 0
                        )
                    )
                }
            }
            ReviewMediaType.GAME -> {
                if (gameRepository.getGameById(sel.id) == null) {
                    gameRepository.insertGame(
                        GameEntity(
                            id = sel.id, name = sel.title,
                            backgroundImage = sel.posterUrl, released = null,
                            rating = 0.0, ratingsCount = 0, metacritic = null,
                            genres = null, platforms = null
                        )
                    )
                }
            }
        }
    }

    fun consumeReviewSaved() {
        _reviewSaved.value = false
    }
}