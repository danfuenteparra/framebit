package com.example.moviebox.ui.screens.watchlist

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
import com.example.moviebox.data.local.entity.MovieEntity
import com.example.moviebox.data.local.entity.TvShowEntity
import com.example.moviebox.ui.screens.home.SectionTitle
import com.example.moviebox.ui.theme.MovieBoxBackground
import com.example.moviebox.ui.theme.MovieBoxOnBackground
import com.example.moviebox.ui.theme.MovieBoxPrimary
import com.example.moviebox.ui.theme.MovieBoxSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    onBack: () -> Unit,
    onMovieClick: (Int) -> Unit,
    onTvShowClick: (Int) -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

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
            TabRow(selectedTabIndex = selectedTab, containerColor = MovieBoxBackground, contentColor = MovieBoxPrimary) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Pendientes") }, icon = { Icon(Icons.Filled.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp)) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Favoritas") }, icon = { Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(16.dp)) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Vistas") }, icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) })
            }

            when (uiState) {
                is WatchlistUiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MovieBoxPrimary) }
                is WatchlistUiState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text((uiState as WatchlistUiState.Error).message, color = MovieBoxOnBackground) }
                is WatchlistUiState.Success -> {
                    val data = uiState as WatchlistUiState.Success

                    val movies = when (selectedTab) { 0 -> data.watchlistedMovies; 1 -> data.favoriteMovies; else -> data.watchedMovies }
                    val tvShows = when (selectedTab) { 0 -> data.watchlistedTvShows; 1 -> data.favoriteTvShows; else -> data.watchedTvShows }

                    if (movies.isEmpty() && tvShows.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = when (selectedTab) { 0 -> "No tienes nada pendiente"; 1 -> "No tienes favoritas"; else -> "No has visto nada aún" },
                                color = MovieBoxOnBackground.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (movies.isNotEmpty()) {
                                item { SectionTitle(title = "Películas") }
                                items(movies) { movie ->
                                    WatchlistMovieItem(
                                        movie = movie,
                                        showWatchedToggle = selectedTab == 0,
                                        onClick = { onMovieClick(movie.id) },
                                        onToggleWatched = { viewModel.toggleMovieWatched(movie.id, !movie.isWatched) },
                                        onDelete = { viewModel.deleteMovie(movie.id) }
                                    )
                                }
                            }
                            if (tvShows.isNotEmpty()) {
                                item { SectionTitle(title = "Series") }
                                items(tvShows) { tvShow ->
                                    WatchlistTvShowItem(
                                        tvShow = tvShow,
                                        showWatchedToggle = selectedTab == 0,
                                        onClick = { onTvShowClick(tvShow.id) },
                                        onToggleWatched = { viewModel.toggleTvShowWatched(tvShow.id, !tvShow.isWatched) },
                                        onDelete = { viewModel.deleteTvShow(tvShow.id) }
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

@Composable
fun WatchlistMovieItem(movie: MovieEntity, showWatchedToggle: Boolean, onClick: () -> Unit, onToggleWatched: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MovieBoxSurface), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
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
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
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