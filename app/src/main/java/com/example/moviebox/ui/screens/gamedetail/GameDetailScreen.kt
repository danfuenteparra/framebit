package com.example.moviebox.ui.screens.gamedetail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.moviebox.data.remote.dto.ScreenshotDto
import com.example.moviebox.ui.components.reviews.AddReviewDialog
import com.example.moviebox.ui.components.reviews.ReviewsSection
import com.example.moviebox.ui.theme.MovieBoxBackground
import com.example.moviebox.ui.theme.MovieBoxOnBackground
import com.example.moviebox.ui.theme.MovieBoxPrimary
import com.example.moviebox.ui.theme.MovieBoxSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    onBack: () -> Unit,
    onReviewClick: (reviewId: String) -> Unit,
    viewModel: GameDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isWatchlisted by viewModel.isWatchlisted.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val isPlayed by viewModel.isPlayed.collectAsStateWithLifecycle()
    val reviews by viewModel.reviews.collectAsStateWithLifecycle()
    val friendReviews by viewModel.friendReviews.collectAsStateWithLifecycle()
    val screenshots by viewModel.screenshots.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showReviewDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkLocalStatus()
                viewModel.refreshFriendReviews()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle", color = MovieBoxOnBackground, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = MovieBoxOnBackground)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleWatchlisted() }) {
                        Icon(
                            imageVector = if (isWatchlisted) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Pendiente",
                            tint = if (isWatchlisted) MovieBoxPrimary else MovieBoxOnBackground
                        )
                    }
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (isFavorite) MovieBoxPrimary else MovieBoxOnBackground
                        )
                    }
                    IconButton(onClick = { viewModel.togglePlayed() }) {
                        Icon(
                            imageVector = if (isPlayed) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                            contentDescription = "Jugado",
                            tint = if (isPlayed) MovieBoxPrimary else MovieBoxOnBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MovieBoxBackground)
            )
        },
        containerColor = MovieBoxBackground
    ) { innerPadding ->
        when (uiState) {
            is GameDetailUiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MovieBoxPrimary) }
            is GameDetailUiState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text((uiState as GameDetailUiState.Error).message, color = MovieBoxOnBackground) }
            is GameDetailUiState.Success -> {
                val game = (uiState as GameDetailUiState.Success).game
                Column(modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState())) {
                    AsyncImage(
                        model = game.backgroundImage,
                        contentDescription = game.name,
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        contentScale = ContentScale.Crop
                    )

                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = game.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MovieBoxOnBackground)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "\u2B50 ${String.format("%.1f", game.rating)}", color = MovieBoxPrimary, fontSize = 14.sp)
                            game.released?.let { Text(text = it.take(4), color = MovieBoxOnBackground.copy(alpha = 0.7f), fontSize = 14.sp) }
                            game.metacritic?.let { Text(text = "Metacritic: $it", color = MovieBoxOnBackground.copy(alpha = 0.7f), fontSize = 14.sp) }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        game.developers?.let { devs ->
                            if (devs.isNotEmpty()) {
                                Text(
                                    text = "Desarrollador: ${devs.joinToString(", ") { it.name }}",
                                    color = MovieBoxOnBackground.copy(alpha = 0.7f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                        game.publishers?.let { pubs ->
                            if (pubs.isNotEmpty()) {
                                Text(
                                    text = "Publisher: ${pubs.joinToString(", ") { it.name }}",
                                    color = MovieBoxOnBackground.copy(alpha = 0.7f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                        game.playtime?.let { if (it > 0) Text(text = "Tiempo medio: ${it}h", color = MovieBoxOnBackground.copy(alpha = 0.7f), fontSize = 14.sp) }

                        Spacer(modifier = Modifier.height(8.dp))

                        game.genres?.let { genres ->
                            if (genres.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    genres.take(4).forEach { genre ->
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(genre.name, fontSize = 12.sp) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MovieBoxSurface, labelColor = MovieBoxOnBackground)
                                        )
                                    }
                                }
                            }
                        }

                        game.platforms?.let { platforms ->
                            if (platforms.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    platforms.take(5).forEach { pw ->
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(pw.platform.name, fontSize = 11.sp) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MovieBoxPrimary.copy(alpha = 0.2f), labelColor = MovieBoxPrimary)
                                        )
                                    }
                                }
                            }
                        }

                        game.website?.let { url ->
                            if (url.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp), tint = MovieBoxPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Web oficial", color = MovieBoxPrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Descripción", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MovieBoxOnBackground)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = game.descriptionRaw?.ifEmpty { "Sin descripción disponible." } ?: "Sin descripción disponible.",
                            color = MovieBoxOnBackground.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )

                        if (screenshots.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(text = "Capturas", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MovieBoxOnBackground)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    if (screenshots.isNotEmpty()) {
                        ScreenshotsRow(screenshots = screenshots)
                    }

                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.toggleWatchlisted() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (isWatchlisted) MovieBoxSurface else MovieBoxPrimary)) {
                                Icon(imageVector = if (isWatchlisted) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Button(onClick = { viewModel.toggleFavorite() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (isFavorite) MovieBoxSurface else MovieBoxPrimary)) {
                                Icon(imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Button(onClick = { viewModel.togglePlayed() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (isPlayed) MovieBoxSurface else MovieBoxPrimary)) {
                                Icon(imageVector = if (isPlayed) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        ReviewsSection(
                            reviews = friendReviews,
                            myUserId = viewModel.getMyUserId(),
                            onAddClick = { showReviewDialog = true },
                            onReviewClick = onReviewClick,
                            onLikeClick = { fr -> viewModel.toggleLike(fr) },
                            onDeleteOwnReview = {
                                val local = reviews.firstOrNull()
                                if (local != null) viewModel.deleteReview(local.id)
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    if (showReviewDialog) {
        AddReviewDialog(
            onDismiss = { showReviewDialog = false },
            onConfirm = { rating, comment ->
                viewModel.addReview(rating, comment)
                showReviewDialog = false
            }
        )
    }
}

@Composable
fun ScreenshotsRow(screenshots: List<ScreenshotDto>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(screenshots) { screenshot ->
            AsyncImage(
                model = screenshot.image,
                contentDescription = "Screenshot",
                modifier = Modifier
                    .width(240.dp)
                    .height(135.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}