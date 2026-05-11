package com.example.moviebox.ui.screens.userprofile

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.moviebox.data.remote.model.PublicUser
import com.example.moviebox.ui.theme.MovieBoxBackground
import com.example.moviebox.ui.theme.MovieBoxOnBackground
import com.example.moviebox.ui.theme.MovieBoxPrimary
import com.example.moviebox.ui.theme.MovieBoxSurface

/**
 * Pantalla de perfil de OTRO usuario (cuando es propio se ve por ProfileScreen).
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
    onNavigateToWatched: (userId: String) -> Unit,
    onNavigateToReviews: (userId: String) -> Unit,
    onNavigateToWatchlist: (userId: String) -> Unit,
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val isFollowing by viewModel.isFollowing.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val blockRelation by viewModel.blockRelation.collectAsStateWithLifecycle()
    val topMovies by viewModel.topMovies.collectAsStateWithLifecycle()
    val topTvShows by viewModel.topTvShows.collectAsStateWithLifecycle()
    val topGames by viewModel.topGames.collectAsStateWithLifecycle()
    val counts by viewModel.counts.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
                    if (!viewModel.isOwnProfile && user != null
                        && blockRelation != BlockRelation.TheyBlockedMe) {
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
                    counts = counts,
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
                    onMediaClick = { mediaType, mediaId -> navigateToMedia(mediaType, mediaId) },
                    onWatchedClick = { onNavigateToWatched(u.userId) },
                    onReviewsClick = { onNavigateToReviews(u.userId) },
                    onWatchlistClick = { onNavigateToWatchlist(u.userId) }
                )
            }
        }
    }

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

@Composable
private fun FullProfileContent(
    user: PublicUser,
    isFollowing: Boolean,
    isOwnProfile: Boolean,
    topMovies: List<TopItemDisplay?>,
    topTvShows: List<TopItemDisplay?>,
    topGames: List<TopItemDisplay?>,
    counts: ProfileCounts,
    innerPadding: PaddingValues,
    onToggleFollow: () -> Unit,
    onNavigateToFollowers: (String) -> Unit,
    onNavigateToFollowing: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onMediaClick: (String, Int) -> Unit,
    onWatchedClick: () -> Unit,
    onReviewsClick: () -> Unit,
    onWatchlistClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

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
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionButton(
                icon = Icons.Default.Visibility,
                label = "Visto / Jugado",
                count = counts.totalWatched,
                onClick = onWatchedClick
            )
            SectionButton(
                icon = Icons.Default.RateReview,
                label = "Reseñas",
                count = counts.totalReviews,
                onClick = onReviewsClick
            )
            SectionButton(
                icon = Icons.Default.Bookmark,
                label = "Watchlist",
                count = counts.totalWatchlist,
                onClick = onWatchlistClick
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SectionButton(
    icon: ImageVector,
    label: String,
    count: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MovieBoxSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MovieBoxPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                color = MovieBoxOnBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = count.toString(),
                color = MovieBoxOnBackground.copy(alpha = 0.7f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MovieBoxOnBackground.copy(alpha = 0.5f)
            )
        }
    }
}

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