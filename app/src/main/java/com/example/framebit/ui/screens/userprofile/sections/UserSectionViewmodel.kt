package com.example.framebit.ui.screens.userprofile.sections

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.framebit.data.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Origen de los items que muestra la pantalla.
 *  - WATCHED   : LibraryEntry con status="watched", ordenadas por updatedAt desc.
 *                Si tiene reseña asociada, se rellena el rating para el badge.
 *  - REVIEWS   : Solo items con reseña, ordenadas por createdAt desc.
 *                Siempre rellena el rating.
 *  - WATCHLIST : LibraryEntry con status="watchlist", ordenadas por updatedAt desc.
 *                Sin badge (nunca tiene reseña en este estado).
 */
enum class SectionSource(val key: String) {
    WATCHED("watched"),
    REVIEWS("reviews"),
    WATCHLIST("watchlist");

    companion object {
        fun fromKey(key: String?): SectionSource = when (key) {
            "watched" -> WATCHED
            "reviews" -> REVIEWS
            "watchlist" -> WATCHLIST
            else -> WATCHED
        }
    }
}

/**
 * Items de las 3 secciones (movies/tv/games) ya construidos para la UI.
 */
data class UserSectionState(
    val movies: List<SectionMediaItem> = emptyList(),
    val tvShows: List<SectionMediaItem> = emptyList(),
    val games: List<SectionMediaItem> = emptyList(),
    val loading: Boolean = true
)

@HiltViewModel
class UserSectionViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val targetUserId: String = savedStateHandle["userId"] ?: ""

    private val _state = MutableStateFlow(UserSectionState())
    val state: StateFlow<UserSectionState> = _state

    /**
     * Carga los items para la sección [source]. La pantalla concreta llama a
     * load() en su LaunchedEffect inicial, pasando su propia source.
     */
    fun load(source: SectionSource) {
        if (targetUserId.isBlank()) {
            _state.value = UserSectionState(loading = false)
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            try {
                socialRepository.ensureSignedIn()

                // Cargamos siempre las reseñas para poder pintar el badge en
                // los pósters de "Visto/Jugado". En "Watchlist" no se usan.
                val reviews = if (source != SectionSource.WATCHLIST) {
                    socialRepository.getReviewsByUser(targetUserId)
                } else emptyList()

                // Mapa rápido (mediaType+mediaId) -> rating, para cruzar con library.
                val ratingByKey = reviews.associate {
                    "${it.mediaType}_${it.mediaId}" to it.rating
                }

                when (source) {
                    SectionSource.REVIEWS -> {
                        // Una entrada por reseña directamente
                        val items = reviews.map { r ->
                            SectionMediaItem(
                                mediaType = r.mediaType,
                                mediaId = r.mediaId,
                                title = r.mediaTitle,
                                posterPath = r.mediaPosterPath,
                                rating = r.rating
                            )
                        }
                        _state.value = bucketize(items)
                    }
                    SectionSource.WATCHED, SectionSource.WATCHLIST -> {
                        val library = socialRepository.getLibrary(targetUserId)
                        val targetStatus = if (source == SectionSource.WATCHED) "watched" else "watchlist"
                        val items = library
                            .filter { it.status == targetStatus }
                            .map { entry ->
                                val key = "${entry.mediaType}_${entry.mediaId}"
                                SectionMediaItem(
                                    mediaType = entry.mediaType,
                                    mediaId = entry.mediaId,
                                    title = entry.title,
                                    posterPath = entry.posterPath,
                                    // En watchlist no hay reseña por definición
                                    rating = if (source == SectionSource.WATCHED) ratingByKey[key] else null
                                )
                            }
                        _state.value = bucketize(items)
                    }
                }
            } catch (_: Exception) {
                _state.value = UserSectionState(loading = false)
            }
        }
    }

    /** Distribuye una lista plana en las 3 cubetas por mediaType. */
    private fun bucketize(items: List<SectionMediaItem>): UserSectionState {
        return UserSectionState(
            movies = items.filter { it.mediaType == "movie" },
            tvShows = items.filter { it.mediaType == "tv" },
            games = items.filter { it.mediaType == "game" },
            loading = false
        )
    }
}