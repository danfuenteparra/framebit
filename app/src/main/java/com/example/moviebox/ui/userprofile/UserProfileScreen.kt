package com.example.moviebox.ui.screens.userprofile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.moviebox.data.remote.api.TmdbApiService
import com.example.moviebox.ui.theme.MovieBoxBackground
import com.example.moviebox.ui.theme.MovieBoxOnBackground
import com.example.moviebox.ui.theme.MovieBoxPrimary
import com.example.moviebox.ui.theme.MovieBoxSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    onBack: () -> Unit,
    onNavigateToFollowers: (userId: String) -> Unit,
    onNavigateToFollowing: (userId: String) -> Unit,
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val isFollowing by viewModel.isFollowing.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val topMovies by viewModel.topMovies.collectAsStateWithLifecycle()
    val topTvShows by viewModel.topTvShows.collectAsStateWithLifecycle()
    val topGames by viewModel.topGames.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil", color = MovieBoxOnBackground, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = MovieBoxOnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MovieBoxBackground)
            )
        },
        containerColor = MovieBoxBackground
    ) { innerPadding ->
        if (loading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MovieBoxPrimary)
            }
            return@Scaffold
        }
        val u = user
        if (u == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Usuario no encontrado", color = MovieBoxOnBackground.copy(alpha = 0.6f))
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (u.pictureUrl != null) {
                AsyncImage(
                    model = u.pictureUrl,
                    contentDescription = u.name,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
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
            Text(u.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MovieBoxOnBackground)

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigateToFollowers(u.userId) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(u.followersCount.toString(), color = MovieBoxOnBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Seguidores", color = MovieBoxOnBackground.copy(alpha = 0.7f), fontSize = 13.sp)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigateToFollowing(u.userId) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(u.followingCount.toString(), color = MovieBoxOnBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Siguiendo", color = MovieBoxOnBackground.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón seguir/dejar de seguir (solo si no es propio perfil)
            if (!viewModel.isOwnProfile) {
                Button(
                    onClick = { viewModel.toggleFollow() },
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
                text = "Top 3",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MovieBoxOnBackground,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(12.dp))

            ReadOnlyTopRow("Películas", topMovies, "movie")
            Spacer(modifier = Modifier.height(16.dp))
            ReadOnlyTopRow("Series", topTvShows, "tv")
            Spacer(modifier = Modifier.height(16.dp))
            ReadOnlyTopRow("Juegos", topGames, "game")

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ReadOnlyTopRow(
    label: String,
    items: List<TopItemDisplay?>,
    mediaType: String
) {
    val aspectRatio = if (mediaType == "game") 16f / 9f else 2f / 3f

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
                        .aspectRatio(aspectRatio),
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
