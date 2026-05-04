package com.example.moviebox.ui.screens.profile

import android.content.Intent
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.example.moviebox.data.local.entity.TopItemEntity
import com.example.moviebox.data.remote.api.TmdbApiService
import com.example.moviebox.data.remote.model.LibraryEntry
import com.example.moviebox.ui.theme.MovieBoxBackground
import com.example.moviebox.ui.theme.MovieBoxOnBackground
import com.example.moviebox.ui.theme.MovieBoxPrimary
import com.example.moviebox.ui.theme.MovieBoxSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToFollowers: (String) -> Unit,
    onNavigateToFollowing: (String) -> Unit,
    onNavigateToUserSearch: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onMovieClick: (Int) -> Unit,
    onTvShowClick: (Int) -> Unit,
    onGameClick: (Int) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val publicUser by viewModel.publicUser.collectAsStateWithLifecycle()
    val topMovies by viewModel.topMovies.collectAsStateWithLifecycle()
    val topTvShows by viewModel.topTvShows.collectAsStateWithLifecycle()
    val topGames by viewModel.topGames.collectAsStateWithLifecycle()
    val watchedMovies by viewModel.watchedMovies.collectAsStateWithLifecycle()
    val watchedTvShows by viewModel.watchedTvShows.collectAsStateWithLifecycle()
    val watchedGames by viewModel.watchedGames.collectAsStateWithLifecycle()
    val followersCount by viewModel.followersCount.collectAsStateWithLifecycle()
    val followingCount by viewModel.followingCount.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Refrescar al volver de EditProfile / WatchlistScreen / etc.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshAll()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Estado del diálogo de búsqueda
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchMediaType by remember { mutableStateOf("") }
    var searchPosition by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState) {
        if (uiState is ProfileUiState.LoggedOut) {
            onLogout()
        }
    }

    fun navigateToMedia(mediaType: String, mediaId: Int) {
        when (mediaType) {
            "movie" -> onMovieClick(mediaId)
            "tv" -> onTvShowClick(mediaId)
            "game" -> onGameClick(mediaId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi perfil", color = MovieBoxOnBackground, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToUserSearch) {
                        Icon(Icons.Default.PersonSearch, contentDescription = "Buscar usuarios", tint = MovieBoxPrimary)
                    }
                    IconButton(onClick = onNavigateToEditProfile) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar perfil", tint = MovieBoxPrimary)
                    }
                    IconButton(onClick = { viewModel.logout(context) }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar sesión", tint = MovieBoxPrimary)
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
                    val authProfile = (uiState as ProfileUiState.Success).userProfile

                    // Preferimos foto/nombre del PublicUser (foto custom) y caemos a Auth0
                    val displayName = publicUser?.name?.takeIf { it.isNotBlank() } ?: authProfile.name ?: "Usuario"
                    val displayPicture = publicUser?.pictureUrl ?: authProfile.pictureURL
                    val bio = publicUser?.bio.orEmpty()
                    val links = publicUser?.links.orEmpty()

                    Spacer(modifier = Modifier.height(16.dp))

                    if (displayPicture != null) {
                        AsyncImage(
                            model = displayPicture,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
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
                        text = displayName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MovieBoxOnBackground
                    )

                    if (bio.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            bio,
                            fontSize = 14.sp,
                            color = MovieBoxOnBackground.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }

                    if (links.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            links.forEach { link ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.clickable {
                                        runCatching {
                                            val normalized = if (!link.startsWith("http")) "https://$link" else link
                                            context.startActivity(Intent(Intent.ACTION_VIEW, normalized.toUri()))
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Link,
                                        contentDescription = null,
                                        tint = MovieBoxPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        link,
                                        fontSize = 13.sp,
                                        color = MovieBoxPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Contadores sociales
                    val myId = viewModel.getCurrentUserId()
                    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { if (myId.isNotBlank()) onNavigateToFollowers(myId) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(followersCount.toString(), color = MovieBoxOnBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Seguidores", color = MovieBoxOnBackground.copy(alpha = 0.7f), fontSize = 13.sp)
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { if (myId.isNotBlank()) onNavigateToFollowing(myId) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(followingCount.toString(), color = MovieBoxOnBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Siguiendo", color = MovieBoxOnBackground.copy(alpha = 0.7f), fontSize = 13.sp)
                        }
                    }
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MovieBoxSurface, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Mis top 3",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MovieBoxOnBackground,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(12.dp))

            EditableTopRow(
                label = "Películas",
                items = topMovies,
                mediaType = "movie",
                onItemClick = { id -> onMovieClick(id) },
                onAdd = { row ->
                    searchMediaType = "movie"
                    searchPosition = row
                    showSearchDialog = true
                },
                onRemove = { row -> viewModel.removeTopItem("movie", row) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            EditableTopRow(
                label = "Series",
                items = topTvShows,
                mediaType = "tv",
                onItemClick = { id -> onTvShowClick(id) },
                onAdd = { row ->
                    searchMediaType = "tv"
                    searchPosition = row
                    showSearchDialog = true
                },
                onRemove = { row -> viewModel.removeTopItem("tv", row) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            EditableTopRow(
                label = "Juegos",
                items = topGames,
                mediaType = "game",
                onItemClick = { id -> onGameClick(id) },
                onAdd = { row ->
                    searchMediaType = "game"
                    searchPosition = row
                    showSearchDialog = true
                },
                onRemove = { row -> viewModel.removeTopItem("game", row) }
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MovieBoxSurface, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Visto/Jugado",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MovieBoxOnBackground,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(8.dp))

            WatchedRow("Películas", watchedMovies, "movie") { id -> navigateToMedia("movie", id) }
            Spacer(modifier = Modifier.height(12.dp))
            WatchedRow("Series", watchedTvShows, "tv") { id -> navigateToMedia("tv", id) }
            Spacer(modifier = Modifier.height(12.dp))
            WatchedRow("Juegos", watchedGames, "game") { id -> navigateToMedia("game", id) }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Diálogo de búsqueda
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
private fun EditableTopRow(
    label: String,
    items: List<TopItemEntity?>,
    mediaType: String,
    onItemClick: (Int) -> Unit,
    onAdd: (Int) -> Unit,
    onRemove: (Int) -> Unit
) {
    val aspectRatio = 2f / 3f // siempre 2:3 para coherencia

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
                val item = items.getOrNull(pos)
                Box(modifier = Modifier.weight(1f)) {
                    if (item != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(aspectRatio)
                                .clickable { onItemClick(item.mediaId) },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MovieBoxSurface)
                        ) {
                            val imageUrl = if (mediaType == "game") item.posterPath
                            else TmdbApiService.getImageUrl(item.posterPath)
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = item.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        IconButton(
                            onClick = { onRemove(pos) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Quitar",
                                tint = MovieBoxOnBackground,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                            )
                        }
                    } else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(aspectRatio)
                                .clickable { onAdd(pos) },
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
    onClick: (Int) -> Unit,
    onRemove: () -> Unit
) {
    val aspectRatio = 2f / 3f

    if (item != null) {
        Box(modifier = modifier) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .clickable { onClick(item.mediaId) },
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
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Quitar",
                    tint = MovieBoxOnBackground,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
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

@Composable
private fun WatchedRow(
    label: String,
    entries: List<LibraryEntry>,
    mediaType: String,
    onItemClick: (Int) -> Unit
) {
    if (entries.isEmpty()) return

    val aspectRatio = 2f / 3f
    val itemWidth = 100.dp

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 14.sp, color = MovieBoxPrimary, fontWeight = FontWeight.SemiBold)
            Text("${entries.size}", fontSize = 12.sp, color = MovieBoxOnBackground.copy(alpha = 0.5f))
        }
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(entries, key = { it.mediaId }) { entry ->
                Card(
                    modifier = Modifier
                        .width(itemWidth)
                        .aspectRatio(aspectRatio)
                        .clickable { onItemClick(entry.mediaId) },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MovieBoxSurface)
                ) {
                    val imageUrl = if (mediaType == "game") entry.posterPath
                    else TmdbApiService.getImageUrl(entry.posterPath)
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = entry.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
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