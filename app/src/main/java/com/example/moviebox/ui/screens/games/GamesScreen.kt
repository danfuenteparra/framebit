package com.example.moviebox.ui.screens.games

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.moviebox.data.remote.dto.GameDto
import com.example.moviebox.ui.components.FriendsActivityRow
import com.example.moviebox.ui.theme.MovieBoxBackground
import com.example.moviebox.ui.theme.MovieBoxOnBackground
import com.example.moviebox.ui.theme.MovieBoxPrimary
import com.example.moviebox.ui.theme.MovieBoxSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesScreen(
    onGameClick: (Int) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToWatchlist: () -> Unit,
    onReviewClick: (reviewId: String) -> Unit,
    viewModel: GamesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val friendsGames by viewModel.friendsGames.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadFriendsActivity()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Juegos", color = MovieBoxPrimary, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar", tint = MovieBoxOnBackground)
                    }
                    IconButton(onClick = onNavigateToWatchlist) {
                        Icon(Icons.Default.Favorite, contentDescription = "Mi Lista", tint = MovieBoxOnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MovieBoxBackground)
            )
        },
        containerColor = MovieBoxBackground
    ) { innerPadding ->

        when (uiState) {
            is GamesUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MovieBoxPrimary)
                }
            }

            is GamesUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = (uiState as GamesUiState.Error).message, color = MovieBoxOnBackground)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadContent() }) { Text("Reintentar") }
                    }
                }
            }

            is GamesUiState.Success -> {
                val data = uiState as GamesUiState.Success

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp, top = 8.dp)
                ) {
                    if (friendsGames.isNotEmpty()) {
                        item {
                            GameSectionTitle(title = "De tus amigos")
                            FriendsActivityRow(
                                activities = friendsGames,
                                mediaType = "game",
                                onItemClick = onGameClick,
                                onReviewClick = { activity ->
                                    activity.reviewId?.let(onReviewClick)
                                }
                            )
                        }
                    }

                    item {
                        GameSectionTitle(title = "Juegos populares")
                        GameRow(games = data.popularGames, onGameClick = onGameClick)
                    }

                    item {
                        GameSectionTitle(title = "Mejor valorados")
                        GameRow(games = data.topRatedGames, onGameClick = onGameClick)
                    }

                    item {
                        GameSectionTitle(title = "Lanzamientos recientes")
                        GameRow(games = data.recentGames, onGameClick = onGameClick)
                    }
                }
            }
        }
    }
}

@Composable
fun GameSectionTitle(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MovieBoxOnBackground)
        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MovieBoxOnBackground.copy(alpha = 0.5f))
    }
}

@Composable
fun GameRow(games: List<GameDto>, onGameClick: (Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(games) { game ->
            GameCard(game = game, onClick = { onGameClick(game.id) })
        }
    }
}

@Composable
fun GameCard(game: GameDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(160.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MovieBoxSurface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            AsyncImage(
                model = game.backgroundImage,
                contentDescription = game.name,
                modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = game.name, color = MovieBoxOnBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "\u2B50 ${String.format("%.1f", game.rating)}", color = MovieBoxPrimary, fontSize = 11.sp)
                    game.metacritic?.let { Text(text = "MC: $it", color = MovieBoxOnBackground.copy(alpha = 0.6f), fontSize = 11.sp) }
                }
                game.genres?.take(2)?.let { genres ->
                    if (genres.isNotEmpty()) {
                        Text(text = genres.joinToString(", ") { it.name }, color = MovieBoxOnBackground.copy(alpha = 0.5f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}