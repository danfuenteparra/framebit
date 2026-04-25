package com.example.moviebox.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.moviebox.data.local.entity.TopItemEntity
import com.example.moviebox.data.remote.api.TmdbApiService
import com.example.moviebox.ui.theme.MovieBoxBackground
import com.example.moviebox.ui.theme.MovieBoxOnBackground
import com.example.moviebox.ui.theme.MovieBoxPrimary
import com.example.moviebox.ui.theme.MovieBoxSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToFollowers: (userId: String) -> Unit,
    onNavigateToFollowing: (userId: String) -> Unit,
    onNavigateToUserSearch: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val topMovies by viewModel.topMovies.collectAsStateWithLifecycle()
    val topTvShows by viewModel.topTvShows.collectAsStateWithLifecycle()
    val topGames by viewModel.topGames.collectAsStateWithLifecycle()
    val followersCount by viewModel.followersCount.collectAsStateWithLifecycle()
    val followingCount by viewModel.followingCount.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showSearchDialog by remember { mutableStateOf(false) }
    var searchMediaType by remember { mutableStateOf("") }
    var searchPosition by remember { mutableIntStateOf(0) }

    // Refrescar contadores cada vez que se vuelve a la pantalla
    LaunchedEffect(Unit) {
        viewModel.refreshSocialCounts()
    }

    LaunchedEffect(uiState) {
        if (uiState is ProfileUiState.LoggedOut) {
            onLogout()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil", color = MovieBoxOnBackground, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToUserSearch) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Buscar usuarios", tint = MovieBoxPrimary)
                    }
                    IconButton(onClick = { viewModel.logout(context) }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Cerrar sesión", tint = MovieBoxPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MovieBoxBackground)
            )
        },
        containerColor = MovieBoxBackground
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Info del usuario
            when (uiState) {
                is ProfileUiState.Loading -> {
                    Spacer(modifier = Modifier.height(24.dp))
                    CircularProgressIndicator(color = MovieBoxPrimary)
                }
                is ProfileUiState.Error -> {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text((uiState as ProfileUiState.Error).message, color = MovieBoxOnBackground)
                }
                is ProfileUiState.Success -> {
                    val profile = (uiState as ProfileUiState.Success).userProfile
                    val userId = viewModel.getCurrentUserId()

                    Spacer(modifier = Modifier.height(16.dp))

                    if (profile.pictureURL != null) {
                        AsyncImage(
                            model = profile.pictureURL,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Perfil",
                            tint = MovieBoxOnBackground,
                            modifier = Modifier.size(80.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = profile.name ?: "Usuario",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MovieBoxOnBackground
                    )
                    Text(
                        text = profile.email ?: "",
                        fontSize = 14.sp,
                        color = MovieBoxOnBackground.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Contadores de seguidores / seguidos
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        SocialStat(
                            count = followersCount,
                            label = "Seguidores",
                            onClick = { if (userId.isNotBlank()) onNavigateToFollowers(userId) }
                        )
                        SocialStat(
                            count = followingCount,
                            label = "Siguiendo",
                            onClick = { if (userId.isNotBlank()) onNavigateToFollowing(userId) }
                        )
                    }
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MovieBoxSurface, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(16.dp))

            // Título del Top
            Text(
                text = "Mi Top 3",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MovieBoxOnBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ===== TOP 3 HORIZONTAL =====
            // 3 filas (películas, series, juegos), cada una con 3 slots horizontales
            TopRowSection(
                label = "Películas",
                items = topMovies,
                mediaType = "movie",
                onAdd = { pos ->
                    searchMediaType = "movie"
                    searchPosition = pos
                    showSearchDialog = true
                },
                onRemove = { pos -> viewModel.removeTopItem("movie", pos) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            TopRowSection(
                label = "Series",
                items = topTvShows,
                mediaType = "tv",
                onAdd = { pos ->
                    searchMediaType = "tv"
                    searchPosition = pos
                    showSearchDialog = true
                },
                onRemove = { pos -> viewModel.removeTopItem("tv", pos) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            TopRowSection(
                label = "Juegos",
                items = topGames,
                mediaType = "game",
                onAdd = { pos ->
                    searchMediaType = "game"
                    searchPosition = pos
                    showSearchDialog = true
                },
                onRemove = { pos -> viewModel.removeTopItem("game", pos) }
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MovieBoxSurface, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.logout(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MovieBoxPrimary)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar Sesión", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showSearchDialog) {
        TopItemSearchDialog(
            mediaType = searchMediaType,
            viewModel = viewModel,
            onDismiss = {
                showSearchDialog = false
                viewModel.clearSearch()
            },
            onSelect = { mediaId, title, posterPath ->
                viewModel.setTopItem(searchMediaType, searchPosition, mediaId, title, posterPath)
                showSearchDialog = false
                viewModel.clearSearch()
            }
        )
    }
}

@Composable
private fun SocialStat(count: Int, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = count.toString(),
            color = MovieBoxOnBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = MovieBoxOnBackground.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
    }
}

/**
 * Una fila horizontal de 3 slots para un tipo de media.
 */
@Composable
private fun TopRowSection(
    label: String,
    items: List<TopItemEntity?>,
    mediaType: String,
    onAdd: (position: Int) -> Unit,
    onRemove: (position: Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MovieBoxPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (pos in 0..2) {
                TopItemSlot(
                    item = items.getOrNull(pos),
                    mediaType = mediaType,
                    modifier = Modifier.weight(1f),
                    onAdd = { onAdd(pos) },
                    onRemove = { onRemove(pos) }
                )
            }
        }
    }
}

@Composable
fun TopItemSlot(
    item: TopItemEntity?,
    mediaType: String,
    modifier: Modifier = Modifier,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    val aspectRatio = if (mediaType == "game") 16f / 9f else 2f / 3f

    if (item != null) {
        Box(modifier = modifier) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .clickable { onRemove() },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MovieBoxSurface)
            ) {
                val imageUrl = when (mediaType) {
                    "game" -> item.posterPath
                    else -> TmdbApiService.getImageUrl(item.posterPath)
                }
                AsyncImage(
                    model = imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    } else {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clickable { onAdd() },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MovieBoxSurface.copy(alpha = 0.5f)),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(MovieBoxOnBackground.copy(alpha = 0.2f))
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Añadir",
                    tint = MovieBoxOnBackground.copy(alpha = 0.4f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopItemSearchDialog(
    mediaType: String,
    viewModel: ProfileViewModel,
    onDismiss: () -> Unit,
    onSelect: (mediaId: Int, title: String, posterPath: String?) -> Unit
) {
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val title = when (mediaType) {
        "movie" -> "Buscar película"
        "tv" -> "Buscar serie"
        "game" -> "Buscar juego"
        else -> "Buscar"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MovieBoxBackground)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(title, color = MovieBoxOnBackground, fontSize = 16.sp) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = MovieBoxOnBackground)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MovieBoxBackground)
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchForType(it, mediaType) },
                    placeholder = { Text("Escribe para buscar...", color = MovieBoxOnBackground.copy(alpha = 0.5f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MovieBoxOnBackground) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MovieBoxOnBackground,
                        unfocusedTextColor = MovieBoxOnBackground,
                        focusedBorderColor = MovieBoxPrimary,
                        unfocusedBorderColor = MovieBoxSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                when (searchResults) {
                    is SearchResults.Empty -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Escribe para buscar...", color = MovieBoxOnBackground.copy(alpha = 0.5f), fontSize = 14.sp)
                        }
                    }
                    is SearchResults.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MovieBoxPrimary)
                        }
                    }
                    is SearchResults.Movies -> {
                        val movies = (searchResults as SearchResults.Movies).movies
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(movies) { movie ->
                                SearchResultRow(
                                    title = movie.title,
                                    subtitle = movie.releaseDate.take(4),
                                    posterUrl = TmdbApiService.getImageUrl(movie.posterPath),
                                    onClick = { onSelect(movie.id, movie.title, movie.posterPath) }
                                )
                            }
                        }
                    }
                    is SearchResults.TvShows -> {
                        val tvShows = (searchResults as SearchResults.TvShows).tvShows
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(tvShows) { tvShow ->
                                SearchResultRow(
                                    title = tvShow.name,
                                    subtitle = tvShow.firstAirDate.take(4),
                                    posterUrl = TmdbApiService.getImageUrl(tvShow.posterPath),
                                    onClick = { onSelect(tvShow.id, tvShow.name, tvShow.posterPath) }
                                )
                            }
                        }
                    }
                    is SearchResults.Games -> {
                        val games = (searchResults as SearchResults.Games).games
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(games) { game ->
                                SearchResultRow(
                                    title = game.name,
                                    subtitle = game.released?.take(4) ?: "",
                                    posterUrl = game.backgroundImage,
                                    onClick = { onSelect(game.id, game.name, game.backgroundImage) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultRow(
    title: String,
    subtitle: String,
    posterUrl: String?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MovieBoxSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = posterUrl,
                contentDescription = title,
                modifier = Modifier
                    .size(width = 40.dp, height = 56.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = MovieBoxOnBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = MovieBoxOnBackground.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
