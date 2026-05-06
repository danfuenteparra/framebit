package com.example.moviebox.ui.screens.userprofile

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
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
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.moviebox.data.remote.api.TmdbApiService
import com.example.moviebox.data.remote.model.BlockRelation
import com.example.moviebox.data.remote.model.LibraryEntry
import com.example.moviebox.data.remote.model.PublicUser
import com.example.moviebox.ui.theme.MovieBoxBackground
import com.example.moviebox.ui.theme.MovieBoxOnBackground
import com.example.moviebox.ui.theme.MovieBoxPrimary
import com.example.moviebox.ui.theme.MovieBoxSurface

/**
 * Pantalla de perfil de OTRO usuario (cuando es propio se ve por ProfileScreen).
 *
 * Tres estados principales según BlockRelation:
 *  - NotBlocked   : todo el perfil visible (top items, library, follow, etc.).
 *  - IBlockedThem : header mínimo + tarjeta "has bloqueado" + botón desbloquear.
 *  - TheyBlockedMe: header mínimo + tarjeta "no puedes ver este perfil".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    onBack: () -> Unit,
    onNavigateToFollowers: (userId: String) -> Unit,
    onNavigateToFollowing: (userId: String) -> Unit,
    onMovieClick: (Int) -> Unit,
    onTvShowClick: (Int) -> Unit,
    onGameClick: (Int) -> Unit,
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val isFollowing by viewModel.isFollowing.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val blockRelation by viewModel.blockRelation.collectAsStateWithLifecycle()
    val topMovies by viewModel.topMovies.collectAsStateWithLifecycle()
    val topTvShows by viewModel.topTvShows.collectAsStateWithLifecycle()
    val topGames by viewModel.topGames.collectAsStateWithLifecycle()
    val watchedMovies by viewModel.watchedMovies.collectAsStateWithLifecycle()
    val watchedTvShows by viewModel.watchedTvShows.collectAsStateWithLifecycle()
    val watchedGames by viewModel.watchedGames.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Estado UI local: menú de tres puntos y diálogo de confirmación de bloqueo
    var showMenu by remember { mutableStateOf(false) }
    var showBlockConfirm by remember { mutableStateOf(false) }

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
                title = { Text("Perfil", color = MovieBoxOnBackground, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = MovieBoxOnBackground)
                    }
                },
                actions = {
                    // Menú de tres puntos: bloquear / desbloquear.
                    // Solo visible si no es nuestro propio perfil y conocemos al usuario.
                    // En estado TheyBlockedMe NO se muestra: si me bloquearon, no puedo
                    // hacer nada útil contra ellos desde aquí.
                    if (!viewModel.isOwnProfile && user != null
                        && blockRelation !is BlockRelation.TheyBlockedMe) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Más opciones",
                                    tint = MovieBoxOnBackground
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                containerColor = MovieBoxSurface
                            ) {
                                when (blockRelation) {
                                    BlockRelation.IBlockedThem -> {
                                        DropdownMenuItem(
                                            text = {
                                                Text("Desbloquear usuario", color = MovieBoxOnBackground)
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Block,
                                                    contentDescription = null,
                                                    tint = MovieBoxOnBackground
                                                )
                                            },
                                            onClick = {
                                                showMenu = false
                                                viewModel.unblockUser()
                                            }
                                        )
                                    }
                                    BlockRelation.NotBlocked -> {
                                        DropdownMenuItem(
                                            text = {
                                                Text("Bloquear usuario", color = MovieBoxOnBackground)
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Block,
                                                    contentDescription = null,
                                                    tint = MovieBoxOnBackground
                                                )
                                            },
                                            onClick = {
                                                showMenu = false
                                                showBlockConfirm = true
                                            }
                                        )
                                    }
                                    else -> { /* TheyBlockedMe: no debería entrar aquí */ }
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MovieBoxBackground)
            )
        },
        containerColor = MovieBoxBackground
    ) { innerPadding ->
        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MovieBoxPrimary)
            }
            return@Scaffold
        }

        val u = user
        if (u == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Usuario no encontrado", color = MovieBoxOnBackground.copy(alpha = 0.6f))
            }
            return@Scaffold
        }

        // Branching principal según relación de bloqueo
        when (blockRelation) {
            BlockRelation.TheyBlockedMe -> {
                BlockedHeaderOnly(
                    user = u,
                    innerPadding = innerPadding,
                    message = "No puedes ver el perfil de este usuario."
                )
            }
            BlockRelation.IBlockedThem -> {
                BlockedHeaderOnly(
                    user = u,
                    innerPadding = innerPadding,
                    message = "Has bloqueado a este usuario. Mientras esté bloqueado no veréis vuestra actividad mutuamente.",
                    showUnblockButton = true,
                    onUnblock = { viewModel.unblockUser() }
                )
            }
            BlockRelation.NotBlocked -> {
                FullProfileContent(
                    user = u,
                    isFollowing = isFollowing,
                    isOwnProfile = viewModel.isOwnProfile,
                    topMovies = topMovies,
                    topTvShows = topTvShows,
                    topGames = topGames,
                    watchedMovies = watchedMovies,
                    watchedTvShows = watchedTvShows,
                    watchedGames = watchedGames,
                    innerPadding = innerPadding,
                    onToggleFollow = { viewModel.toggleFollow() },
                    onNavigateToFollowers = onNavigateToFollowers,
                    onNavigateToFollowing = onNavigateToFollowing,
                    onLinkClick = { link ->
                        runCatching {
                            val normalized = if (!link.startsWith("http")) "https://$link" else link
                            context.startActivity(Intent(Intent.ACTION_VIEW, normalized.toUri()))
                        }
                    },
                    onMediaClick = { mediaType, mediaId -> navigateToMedia(mediaType, mediaId) }
                )
            }
        }
    }

    // Diálogo de confirmación al pulsar "Bloquear"
    if (showBlockConfirm) {
        AlertDialog(
            onDismissRequest = { showBlockConfirm = false },
            containerColor = MovieBoxSurface,
            title = { Text("Bloquear a ${user?.name ?: "este usuario"}", color = MovieBoxOnBackground) },
            text = {
                Text(
                    "Si lo bloqueas, dejaréis de seguiros y no veréis vuestra actividad mutuamente. " +
                            "Podrás desbloquearlo cuando quieras.",
                    color = MovieBoxOnBackground.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBlockConfirm = false
                        viewModel.blockUser()
                    }
                ) {
                    Text("Bloquear", color = MovieBoxPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirm = false }) {
                    Text("Cancelar", color = MovieBoxOnBackground)
                }
            }
        )
    }
}

/**
 * Cabecera mínima (avatar, nombre) + tarjeta con mensaje.
 * Se usa tanto cuando yo bloqueo (showUnblockButton=true) como cuando me bloquean.
 */
@Composable
private fun BlockedHeaderOnly(
    user: PublicUser,
    innerPadding: PaddingValues,
    message: String,
    showUnblockButton: Boolean = false,
    onUnblock: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        if (user.pictureUrl != null) {
            AsyncImage(
                model = user.pictureUrl,
                contentDescription = user.name,
                modifier = Modifier.size(80.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MovieBoxOnBackground,
                modifier = Modifier.size(80.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(user.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MovieBoxOnBackground)

        Spacer(modifier = Modifier.height(24.dp))

        // Tarjeta con el mensaje
        Card(
            colors = CardDefaults.cardColors(containerColor = MovieBoxSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Block,
                    contentDescription = null,
                    tint = MovieBoxPrimary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    color = MovieBoxOnBackground,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                if (showUnblockButton) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onUnblock,
                        colors = ButtonDefaults.buttonColors(containerColor = MovieBoxPrimary)
                    ) {
                        Text("Desbloquear", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Contenido completo del perfil cuando no hay bloqueo.
 * Extraído del Composable principal para que el branching de bloqueo se lea claro.
 */
@Composable
private fun FullProfileContent(
    user: PublicUser,
    isFollowing: Boolean,
    isOwnProfile: Boolean,
    topMovies: List<TopItemDisplay?>,
    topTvShows: List<TopItemDisplay?>,
    topGames: List<TopItemDisplay?>,
    watchedMovies: List<LibraryEntry>,
    watchedTvShows: List<LibraryEntry>,
    watchedGames: List<LibraryEntry>,
    innerPadding: PaddingValues,
    onToggleFollow: () -> Unit,
    onNavigateToFollowers: (String) -> Unit,
    onNavigateToFollowing: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onMediaClick: (String, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Avatar
        if (user.pictureUrl != null) {
            AsyncImage(
                model = user.pictureUrl,
                contentDescription = user.name,
                modifier = Modifier.size(80.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MovieBoxOnBackground,
                modifier = Modifier.size(80.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(user.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MovieBoxOnBackground)

        // Bio
        if (user.bio.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                user.bio,
                fontSize = 14.sp,
                color = MovieBoxOnBackground.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        // Enlaces
        if (user.links.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                user.links.forEach { link ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable { onLinkClick(link) }
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

        Spacer(modifier = Modifier.height(16.dp))

        // Contadores de seguidores / siguiendo
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onNavigateToFollowers(user.userId) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(user.followersCount.toString(), color = MovieBoxOnBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Seguidores", color = MovieBoxOnBackground.copy(alpha = 0.7f), fontSize = 13.sp)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onNavigateToFollowing(user.userId) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(user.followingCount.toString(), color = MovieBoxOnBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Siguiendo", color = MovieBoxOnBackground.copy(alpha = 0.7f), fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón seguir/dejar de seguir (solo si no es propio perfil)
        if (!isOwnProfile) {
            Button(
                onClick = onToggleFollow,
                modifier = Modifier.padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) MovieBoxSurface else MovieBoxPrimary
                )
            ) {
                Icon(
                    imageVector = if (isFollowing) Icons.Default.PersonRemove else Icons.Default.PersonAdd,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isFollowing) "Dejar de seguir" else "Seguir", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = MovieBoxSurface, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(12.dp))

        // Top 3
        Text(
            text = "Top 3 de " + user.name,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MovieBoxOnBackground,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(12.dp))

        ClickableTopRow("Películas", topMovies, "movie") { id -> onMediaClick("movie", id) }
        Spacer(modifier = Modifier.height(16.dp))
        ClickableTopRow("Series", topTvShows, "tv") { id -> onMediaClick("tv", id) }
        Spacer(modifier = Modifier.height(16.dp))
        ClickableTopRow("Juegos", topGames, "game") { id -> onMediaClick("game", id) }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = MovieBoxSurface, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(12.dp))

        // Visto/jugado
        Text(
            text = "Visto/jugado de " + user.name,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MovieBoxOnBackground,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(8.dp))

        WatchedRow("Películas", watchedMovies, "movie") { id -> onMediaClick("movie", id) }
        Spacer(modifier = Modifier.height(12.dp))
        WatchedRow("Series", watchedTvShows, "tv") { id -> onMediaClick("tv", id) }
        Spacer(modifier = Modifier.height(12.dp))
        WatchedRow("Juegos", watchedGames, "game") { id -> onMediaClick("game", id) }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Fila de Top 3 (3 huecos fijos, vacíos cuando el usuario no ha colocado nada).
 */
@Composable
private fun ClickableTopRow(
    label: String,
    items: List<TopItemDisplay?>,
    mediaType: String,
    onItemClick: (Int) -> Unit
) {
    val aspectRatio = 2f / 3f

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
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(aspectRatio)
                        .then(
                            if (item != null) Modifier.clickable { onItemClick(item.mediaId) }
                            else Modifier
                        ),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MovieBoxSurface.copy(alpha = 0.5f))
                ) {
                    if (item != null) {
                        val imageUrl = if (mediaType == "game") item.posterPath
                        else TmdbApiService.getImageUrl(item.posterPath)
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("-", color = MovieBoxOnBackground.copy(alpha = 0.3f), fontSize = 24.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Fila de pelis/series/juegos vistos. Se oculta entera si la lista está vacía.
 */
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
            Text(
                text = label,
                fontSize = 14.sp,
                color = MovieBoxPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${entries.size}",
                fontSize = 12.sp,
                color = MovieBoxOnBackground.copy(alpha = 0.5f)
            )
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