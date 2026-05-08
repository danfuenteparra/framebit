package com.example.moviebox.ui.screens.userprofile.sections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moviebox.data.remote.model.FriendReview

/**
 * Las tres pantallas siguientes son finas: sólo eligen título y source para
 * el componente compartido [UserSectionScreen]. El ViewModel se comparte
 * entre las 3 (instancia distinta por NavBackStackEntry, no se mezclan).
 *
 * Cada una recibe los callbacks de navegación que necesita:
 *   - onBack
 *   - onItemClick(mediaType, mediaId): donde el tap del póster manda al usuario.
 *   - onSeeAll(mediaType): abre UserMediaList con la lista completa.
 *
 * Para las 3, el tap del póster va a ReviewDetail con un reviewId
 * sintético "{userId}::{mediaType}::{mediaId}" (ver ReviewDetailViewModel).
 * La pantalla de detalle ya sabe construir un FriendReview "minimal" si no
 * hay doc en Firestore (por ejemplo: visto sin reseña, o watchlist).
 */

// ============================================================
// UserWatchedScreen — total + 3 secciones de Visto / Jugado
// ============================================================

@Composable
fun UserWatchedScreen(
    userName: String,
    onBack: () -> Unit,
    onItemClick: (mediaType: String, mediaId: Int) -> Unit,
    onSeeAll: (mediaType: String) -> Unit,
    viewModel: UserSectionViewModel = hiltViewModel()
) {
    LaunchedEffect(viewModel.targetUserId) {
        viewModel.load(SectionSource.WATCHED)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val title = if (userName.isBlank()) "Visto / Jugado" else "Visto / Jugado de $userName"

    UserSectionScreen(
        title = title,
        movies = state.movies,
        tvShows = state.tvShows,
        games = state.games,
        loading = state.loading,
        onBack = onBack,
        onItemClick = onItemClick,
        onSeeAll = onSeeAll
    )
}

// ============================================================
// UserReviewsScreen — total + 3 secciones de Reseñas
// ============================================================

@Composable
fun UserReviewsScreen(
    userName: String,
    onBack: () -> Unit,
    onItemClick: (mediaType: String, mediaId: Int) -> Unit,
    onSeeAll: (mediaType: String) -> Unit,
    viewModel: UserSectionViewModel = hiltViewModel()
) {
    LaunchedEffect(viewModel.targetUserId) {
        viewModel.load(SectionSource.REVIEWS)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val title = if (userName.isBlank()) "Reseñas" else "Reseñas de $userName"

    UserSectionScreen(
        title = title,
        movies = state.movies,
        tvShows = state.tvShows,
        games = state.games,
        loading = state.loading,
        onBack = onBack,
        onItemClick = onItemClick,
        onSeeAll = onSeeAll
    )
}

// ============================================================
// UserWatchlistScreen — total + 3 secciones de Watchlist
// ============================================================

@Composable
fun UserWatchlistScreen(
    userName: String,
    onBack: () -> Unit,
    onItemClick: (mediaType: String, mediaId: Int) -> Unit,
    onSeeAll: (mediaType: String) -> Unit,
    viewModel: UserSectionViewModel = hiltViewModel()
) {
    LaunchedEffect(viewModel.targetUserId) {
        viewModel.load(SectionSource.WATCHLIST)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val title = if (userName.isBlank()) "Watchlist" else "Watchlist de $userName"

    UserSectionScreen(
        title = title,
        movies = state.movies,
        tvShows = state.tvShows,
        games = state.games,
        loading = state.loading,
        onBack = onBack,
        onItemClick = onItemClick,
        onSeeAll = onSeeAll
    )
}

/**
 * Helper para construir reviewIds sintéticos. Mismo formato que
 * FriendReview.composeId, pero centralizado aquí para que las pantallas
 * de sección no tengan que importar el modelo completo.
 */
fun buildReviewId(userId: String, mediaType: String, mediaId: Int): String =
    FriendReview.composeId(userId, mediaType, mediaId)