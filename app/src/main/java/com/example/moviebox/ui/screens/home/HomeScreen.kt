package com.example.moviebox.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.moviebox.data.remote.api.TmdbApiService
import com.example.moviebox.data.remote.dto.MovieDto
import com.example.moviebox.ui.components.FriendsActivityRow
import com.example.moviebox.ui.theme.MovieBoxBackground
import com.example.moviebox.ui.theme.MovieBoxOnBackground
import com.example.moviebox.ui.theme.MovieBoxPrimary
import com.example.moviebox.ui.theme.MovieBoxSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMovieClick: (Int) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToWatchlist: () -> Unit,
    onReviewClick: (reviewId: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val friendsMovies by viewModel.friendsMovies.collectAsStateWithLifecycle()

    // Recargar actividad de amigos al volver a la pantalla
    LaunchedEffect(Unit) {
        viewModel.loadFriendsActivity()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Películas", color = MovieBoxPrimary, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar", tint = MovieBoxOnBackground)
                    }
                    IconButton(onClick = onNavigateToWatchlist) {
                        Icon(Icons.Default.Favorite, contentDescription = "Watchlist", tint = MovieBoxOnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MovieBoxBackground)
            )
        },
        containerColor = MovieBoxBackground
    ) { innerPadding ->

        when (uiState) {
            is HomeUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MovieBoxPrimary)
                }
            }

            is HomeUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = (uiState as HomeUiState.Error).message, color = MovieBoxOnBackground)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadContent() }) { Text("Reintentar") }
                    }
                }
            }

            is HomeUiState.Success -> {
                val data = uiState as HomeUiState.Success

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Sección "De tus amigos" (solo si hay actividad)
                    if (friendsMovies.isNotEmpty()) {
                        item {
                            SectionTitle(title = "De tus amigos")
                            FriendsActivityRow(
                                activities = friendsMovies,
                                mediaType = "movie",
                                onItemClick = onMovieClick,
                                onReviewClick = { activity ->
                                    activity.reviewId?.let(onReviewClick)
                                }
                            )
                        }
                    }

                    item {
                        SectionTitle(title = "Películas populares")
                        MovieRow(movies = data.popularMovies, onMovieClick = onMovieClick)
                    }

                    item {
                        SectionTitle(title = "Películas mejor valoradas")
                        MovieRow(movies = data.topRatedMovies, onMovieClick = onMovieClick)
                    }
                }
            }
        }
    }
}

@Composable
fun MovieRow(movies: List<MovieDto>, onMovieClick: (Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(movies) { movie ->
            MovieCard(movie = movie, onClick = { onMovieClick(movie.id) })
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MovieBoxOnBackground
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MovieBoxOnBackground.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun MovieCard(movie: MovieDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MovieBoxSurface)
    ) {
        Column {
            AsyncImage(
                model = TmdbApiService.getImageUrl(movie.posterPath),
                contentDescription = movie.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )
        }
    }
}