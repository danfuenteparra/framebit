package com.example.framebit.ui.screens.watchlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.framebit.data.local.entity.GameEntity
import com.example.framebit.data.local.entity.MovieEntity
import com.example.framebit.data.local.entity.TvShowEntity
import com.example.framebit.ui.theme.MovieBoxBackground
import com.example.framebit.ui.theme.MovieBoxOnBackground
import com.example.framebit.ui.theme.MovieBoxPrimary
import com.example.framebit.ui.theme.MovieBoxSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    onBack: () -> Unit,
    onMovieClick: (Int) -> Unit,
    onTvShowClick: (Int) -> Unit,
    onGameClick: (Int) -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Tab superior: tipo de media
    var selectedTypeTab by remember { mutableIntStateOf(0) }
    // Tab inferior: estado (pendiente/favorito/visto)
    var selectedStatusTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Lista", color = MovieBoxOnBackground, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = MovieBoxOnBackground) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MovieBoxBackground)
            )
        },
        containerColor = MovieBoxBackground
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // Tabs de tipo: Películas / Series / Juegos
            TabRow(
                selectedTabIndex = selectedTypeTab,
                containerColor = MovieBoxBackground,
                contentColor = MovieBoxPrimary
            ) {
                Tab(selected = selectedTypeTab == 0, onClick = { selectedTypeTab = 0 },
                    text = { Text("Películas") },
                    icon = { Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(selected = selectedTypeTab == 1, onClick = { selectedTypeTab = 1 },
                    text = { Text("Series") },
                    icon = { Icon(Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(selected = selectedTypeTab == 2, onClick = { selectedTypeTab = 2 },
                    text = { Text("Juegos") },
                    icon = { Icon(Icons.Default.SportsEsports, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            // Tabs de estado: Pendientes / Favoritos / Vistos(Jugados)
            val thirdLabel = if (selectedTypeTab == 2) "Jugados" else "Vistos"
            TabRow(
                selectedTabIndex = selectedStatusTab,
                containerColor = MovieBoxSurface,
                contentColor = MovieBoxPrimary
            ) {
                Tab(selected = selectedStatusTab == 0, onClick = { selectedStatusTab = 0 },
                    text = { Text("Pendientes", fontSize = 12.sp) },
                    icon = { Icon(Icons.Filled.Bookmark, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
                Tab(selected = selectedStatusTab == 1, onClick = { selectedStatusTab = 1 },
                    text = { Text("Favoritos", fontSize = 12.sp) },
                    icon = { Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
                Tab(selected = selectedStatusTab == 2, onClick = { selectedStatusTab = 2 },
                    text = { Text(thirdLabel, fontSize = 12.sp) },
                    icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }

            // Contenido
            when (uiState) {
                is WatchlistUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MovieBoxPrimary)
                    }
                }
                is WatchlistUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text((uiState as WatchlistUiState.Error).message, color = MovieBoxOnBackground)
                    }
                }
                is WatchlistUiState.Success -> {
                    val data = uiState as WatchlistUiState.Success

                    when (selectedTypeTab) {
                        // Películas
                        0 -> {
                            val items = when (selectedStatusTab) {
                                0 -> data.watchlistedMovies
                                1 -> data.favoriteMovies
                                else -> data.watchedMovies
                            }
                            if (items.isEmpty()) {
                                EmptyListMessage(selectedStatusTab)
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(items) { movie ->
                                        WatchlistMovieItem(
                                            movie = movie,
                                            showWatchedToggle = selectedStatusTab == 0,
                                            onClick = { onMovieClick(movie.id) },
                                            onToggleWatched = { viewModel.toggleMovieWatched(movie.id, !movie.isWatched) },
                                            onDelete = { viewModel.deleteMovie(movie.id) }
                                        )
                                    }
                                }
                            }
                        }
                        // Series
                        1 -> {
                            val items = when (selectedStatusTab) {
                                0 -> data.watchlistedTvShows
                                1 -> data.favoriteTvShows
                                else -> data.watchedTvShows
                            }
                            if (items.isEmpty()) {
                                EmptyListMessage(selectedStatusTab)
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(items) { tvShow ->
                                        WatchlistTvShowItem(
                                            tvShow = tvShow,
                                            showWatchedToggle = selectedStatusTab == 0,
                                            onClick = { onTvShowClick(tvShow.id) },
                                            onToggleWatched = { viewModel.toggleTvShowWatched(tvShow.id, !tvShow.isWatched) },
                                            onDelete = { viewModel.deleteTvShow(tvShow.id) }
                                        )
                                    }
                                }
                            }
                        }
                        // Juegos
                        2 -> {
                            val items = when (selectedStatusTab) {
                                0 -> data.watchlistedGames
                                1 -> data.favoriteGames
                                else -> data.playedGames
                            }
                            if (items.isEmpty()) {
                                EmptyListMessage(selectedStatusTab, isGame = true)
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(items) { game ->
                                        WatchlistGameItem(
                                            game = game,
                                            showPlayedToggle = selectedStatusTab == 0,
                                            onClick = { onGameClick(game.id) },
                                            onTogglePlayed = { viewModel.toggleGamePlayed(game.id, !game.isPlayed) },
                                            onDelete = { viewModel.deleteGame(game.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyListMessage(selectedStatusTab: Int, isGame: Boolean = false) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = when (selectedStatusTab) {
                0 -> "No tienes nada pendiente"
                1 -> "No tienes favoritos"
                else -> if (isGame) "No has jugado nada aún" else "No has visto nada aún"
            },
            color = MovieBoxOnBackground.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun WatchlistMovieItem(movie: MovieEntity, showWatchedToggle: Boolean, onClick: () -> Unit, onToggleWatched: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MovieBoxSurface), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = movie.title, color = MovieBoxOnBackground, fontWeight = FontWeight.Bold)
                Text(text = "\u2B50 ${String.format("%.1f", movie.voteAverage)}", color = MovieBoxPrimary, fontSize = 12.sp)
            }
            Row {
                if (showWatchedToggle) {
                    IconButton(onClick = onToggleWatched) {
                        Icon(imageVector = if (movie.isWatched) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle, contentDescription = "Marcar vista", tint = if (movie.isWatched) MovieBoxPrimary else MovieBoxOnBackground)
                    }
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MovieBoxOnBackground) }
            }
        }
    }
}

@Composable
fun WatchlistTvShowItem(tvShow: TvShowEntity, showWatchedToggle: Boolean, onClick: () -> Unit, onToggleWatched: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MovieBoxSurface), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = tvShow.name, color = MovieBoxOnBackground, fontWeight = FontWeight.Bold)
                Text(text = "\u2B50 ${String.format("%.1f", tvShow.voteAverage)}", color = MovieBoxPrimary, fontSize = 12.sp)
            }
            Row {
                if (showWatchedToggle) {
                    IconButton(onClick = onToggleWatched) {
                        Icon(imageVector = if (tvShow.isWatched) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle, contentDescription = "Marcar vista", tint = if (tvShow.isWatched) MovieBoxPrimary else MovieBoxOnBackground)
                    }
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MovieBoxOnBackground) }
            }
        }
    }
}

@Composable
fun WatchlistGameItem(game: GameEntity, showPlayedToggle: Boolean, onClick: () -> Unit, onTogglePlayed: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MovieBoxSurface), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = game.name, color = MovieBoxOnBackground, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "\u2B50 ${String.format("%.1f", game.rating)}", color = MovieBoxPrimary, fontSize = 12.sp)
                    game.metacritic?.let { Text(text = "MC: $it", color = MovieBoxOnBackground.copy(alpha = 0.6f), fontSize = 12.sp) }
                }
            }
            Row {
                if (showPlayedToggle) {
                    IconButton(onClick = onTogglePlayed) {
                        Icon(imageVector = if (game.isPlayed) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle, contentDescription = "Marcar jugado", tint = if (game.isPlayed) MovieBoxPrimary else MovieBoxOnBackground)
                    }
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MovieBoxOnBackground) }
            }
        }
    }
}
