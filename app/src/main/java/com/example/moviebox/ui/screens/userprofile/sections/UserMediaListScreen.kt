package com.example.moviebox.ui.screens.userprofile.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.moviebox.data.repository.SocialRepository
import com.example.moviebox.ui.theme.MovieBoxBackground
import com.example.moviebox.ui.theme.MovieBoxOnBackground
import com.example.moviebox.ui.theme.MovieBoxPrimary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pantalla con la lista COMPLETA de un mediaType concreto dentro de una sección.
 *
 * Recibe vía nav args:
 *   - userId    : a quién pertenece la lista
 *   - source    : "watched" | "reviews" | "watchlist"
 *   - mediaType : "movie" | "tv" | "game"
 *
 * Renderiza un grid 3 columnas. Cada póster con badge de nota si el item
 * tiene reseña asociada (en watched y reviews; en watchlist nunca hay).
 * Tap en póster → callback onItemClick para que el caller decida ruta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserMediaListScreen(
    onBack: () -> Unit,
    onItemClick: (mediaType: String, mediaId: Int) -> Unit,
    viewModel: UserMediaListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.title,
                        color = MovieBoxOnBackground,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MovieBoxOnBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MovieBoxBackground)
            )
        },
        containerColor = MovieBoxBackground
    ) { innerPadding ->
        when {
            state.loading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MovieBoxPrimary)
            }
            state.items.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No hay nada por aquí.",
                    color = MovieBoxOnBackground.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
            else -> {
                // Grid 3 columnas. El MediaPosterCard tiene width fija de 100dp
                // pero, dentro del grid, se ajusta porque le quitamos esa width
                // a través del modifier. Lo más simple es llamar directamente al
                // contenido y que el grid controle el tamaño.
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = state.items,
                        key = { "${it.mediaType}_${it.mediaId}" }
                    ) { item ->
                        // Reusamos el componente MediaPosterCard pero forzando
                        // el width a fillMaxWidth dentro de la celda del grid.
                        MediaPosterCard(
                            item = item,
                            onClick = { onItemClick(item.mediaType, item.mediaId) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// ViewModel
// ============================================================

data class UserMediaListState(
    val title: String = "",
    val items: List<SectionMediaItem> = emptyList(),
    val loading: Boolean = true
)

@HiltViewModel
class UserMediaListViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val targetUserId: String = savedStateHandle["userId"] ?: ""
    private val source: SectionSource = SectionSource.fromKey(savedStateHandle["source"])
    private val mediaType: String = savedStateHandle["mediaType"] ?: "movie"

    private val _state = MutableStateFlow(UserMediaListState(title = computeTitle()))
    val state: StateFlow<UserMediaListState> = _state

    init { load() }

    /** Título de la pantalla a partir de source + mediaType. */
    private fun computeTitle(): String {
        val sectionLabel = when (source) {
            SectionSource.WATCHED -> "Visto / Jugado"
            SectionSource.REVIEWS -> "Reseñas"
            SectionSource.WATCHLIST -> "Watchlist"
        }
        val mediaLabel = when (mediaType) {
            "movie" -> "películas"
            "tv" -> "series"
            "game" -> "juegos"
            else -> ""
        }
        return "$sectionLabel · $mediaLabel"
    }

    private fun load() {
        if (targetUserId.isBlank()) {
            _state.value = _state.value.copy(loading = false)
            return
        }
        viewModelScope.launch {
            try {
                socialRepository.ensureSignedIn()

                val reviews = if (source != SectionSource.WATCHLIST) {
                    socialRepository.getReviewsByUser(targetUserId)
                } else emptyList()

                val ratingByKey = reviews.associate {
                    "${it.mediaType}_${it.mediaId}" to it.rating
                }

                val items = when (source) {
                    SectionSource.REVIEWS -> reviews
                        .filter { it.mediaType == mediaType }
                        .map { r ->
                            SectionMediaItem(
                                mediaType = r.mediaType,
                                mediaId = r.mediaId,
                                title = r.mediaTitle,
                                posterPath = r.mediaPosterPath,
                                rating = r.rating
                            )
                        }
                    SectionSource.WATCHED, SectionSource.WATCHLIST -> {
                        val library = socialRepository.getLibrary(targetUserId)
                        val targetStatus = if (source == SectionSource.WATCHED) "watched" else "watchlist"
                        library
                            .filter { it.status == targetStatus && it.mediaType == mediaType }
                            .map { entry ->
                                val key = "${entry.mediaType}_${entry.mediaId}"
                                SectionMediaItem(
                                    mediaType = entry.mediaType,
                                    mediaId = entry.mediaId,
                                    title = entry.title,
                                    posterPath = entry.posterPath,
                                    rating = if (source == SectionSource.WATCHED) ratingByKey[key] else null
                                )
                            }
                    }
                }

                _state.value = UserMediaListState(
                    title = computeTitle(),
                    items = items,
                    loading = false
                )
            } catch (_: Exception) {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }
}