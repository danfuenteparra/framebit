package com.example.moviebox.ui.screens.tvshows

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
import com.example.moviebox.data.remote.dto.TvShowDto
import com.example.moviebox.ui.components.FriendsActivityRow
import com.example.moviebox.ui.theme.MovieBoxBackground
import com.example.moviebox.ui.theme.MovieBoxOnBackground
import com.example.moviebox.ui.theme.MovieBoxPrimary
import com.example.moviebox.ui.theme.MovieBoxSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvShowsScreen(
    onTvShowClick: (Int) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToWatchlist: () -> Unit,
    onReviewClick: (reviewId: String) -> Unit,
    viewModel: TvShowsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val friendsTvShows by viewModel.friendsTvShows.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadFriendsActivity()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Series", color = MovieBoxPrimary, fontWeight = FontWeight.Bold) },
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
            is TvShowsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MovieBoxPrimary)
                }
            }

            is TvShowsUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = (uiState as TvShowsUiState.Error).message, color = MovieBoxOnBackground)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadContent() }) { Text("Reintentar") }
                    }
                }
            }

            is TvShowsUiState.Success -> {
                val data = uiState as TvShowsUiState.Success

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    if (friendsTvShows.isNotEmpty()) {
                        item {
                            TvShowSectionTitle(title = "De tus amigos")
                            FriendsActivityRow(
                                activities = friendsTvShows,
                                mediaType = "tv",
                                onItemClick = onTvShowClick,
                                onReviewClick = { activity ->
                                    activity.reviewId?.let(onReviewClick)
                                }
                            )
                        }
                    }

                    item {
                        TvShowSectionTitle(title = "Series populares")
                        TvShowRow(tvShows = data.popularTvShows, onTvShowClick = onTvShowClick)
                    }

                    item {
                        TvShowSectionTitle(title = "Series mejor valoradas")
                        TvShowRow(tvShows = data.topRatedTvShows, onTvShowClick = onTvShowClick)
                    }
                }
            }
        }
    }
}

@Composable
fun TvShowRow(tvShows: List<TvShowDto>, onTvShowClick: (Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tvShows) { tvShow ->
            TvShowCard(tvShow = tvShow, onClick = { onTvShowClick(tvShow.id) })
        }
    }
}

@Composable
fun TvShowSectionTitle(title: String) {
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
fun TvShowCard(tvShow: TvShowDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MovieBoxSurface)
    ) {
        Column {
            AsyncImage(
                model = TmdbApiService.getImageUrl(tvShow.posterPath),
                contentDescription = tvShow.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )
        }
    }
}