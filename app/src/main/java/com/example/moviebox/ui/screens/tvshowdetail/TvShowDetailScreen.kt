package com.example.moviebox.ui.screens.tvshowdetail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.moviebox.data.remote.api.TmdbApiService
import com.example.moviebox.ui.screens.moviedetail.AddReviewDialog
import com.example.moviebox.ui.screens.moviedetail.CastRow
import com.example.moviebox.ui.screens.moviedetail.ReviewItem
import com.example.moviebox.ui.theme.MovieBoxBackground
import com.example.moviebox.ui.theme.MovieBoxOnBackground
import com.example.moviebox.ui.theme.MovieBoxPrimary
import com.example.moviebox.ui.theme.MovieBoxSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvShowDetailScreen(
    onBack: () -> Unit,
    viewModel: TvShowDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isWatchlisted by viewModel.isWatchlisted.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val isWatched by viewModel.isWatched.collectAsStateWithLifecycle()
    val reviews by viewModel.reviews.collectAsStateWithLifecycle()
    val cast by viewModel.cast.collectAsStateWithLifecycle()
    val trailerKey by viewModel.trailerKey.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showReviewDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle", color = MovieBoxOnBackground, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = MovieBoxOnBackground) } },
                actions = {
                    IconButton(onClick = { viewModel.toggleWatchlisted() }) {
                        Icon(imageVector = if (isWatchlisted) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = "Watchlist", tint = if (isWatchlisted) MovieBoxPrimary else MovieBoxOnBackground)
                    }
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = "Favorita", tint = if (isFavorite) MovieBoxPrimary else MovieBoxOnBackground)
                    }
                    IconButton(onClick = { viewModel.toggleWatched() }) {
                        Icon(imageVector = if (isWatched) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle, contentDescription = "Vista", tint = if (isWatched) MovieBoxPrimary else MovieBoxOnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MovieBoxBackground)
            )
        },
        containerColor = MovieBoxBackground
    ) { innerPadding ->
        when (uiState) {
            is TvShowDetailUiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MovieBoxPrimary) }
            is TvShowDetailUiState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text((uiState as TvShowDetailUiState.Error).message, color = MovieBoxOnBackground) }
            is TvShowDetailUiState.Success -> {
                val tvShow = (uiState as TvShowDetailUiState.Success).tvShow
                Column(modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState())) {
                    Box {
                        AsyncImage(model = TmdbApiService.getImageUrl(tvShow.backdropPath, TmdbApiService.BACKDROP_SIZE), contentDescription = tvShow.name, modifier = Modifier.fillMaxWidth().height(220.dp), contentScale = ContentScale.Crop)
                        if (trailerKey != null) {
                            FloatingActionButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TmdbApiService.getYouTubeUrl(trailerKey!!)))) }, modifier = Modifier.align(Alignment.Center).size(56.dp), containerColor = MovieBoxPrimary.copy(alpha = 0.9f)) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Ver trailer", tint = MovieBoxOnBackground, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = tvShow.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MovieBoxOnBackground)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "\u2B50 ${String.format("%.1f", tvShow.voteAverage)}", color = MovieBoxPrimary, fontSize = 14.sp)
                            Text(text = tvShow.firstAirDate.take(4), color = MovieBoxOnBackground.copy(alpha = 0.7f), fontSize = 14.sp)
                            tvShow.numberOfSeasons?.let { Text(text = "$it temporadas", color = MovieBoxOnBackground.copy(alpha = 0.7f), fontSize = 14.sp) }
                            tvShow.numberOfEpisodes?.let { Text(text = "$it episodios", color = MovieBoxOnBackground.copy(alpha = 0.7f), fontSize = 14.sp) }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (tvShow.genres.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                tvShow.genres.take(4).forEach { genre -> SuggestionChip(onClick = {}, label = { Text(genre.name, fontSize = 12.sp) }, colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MovieBoxSurface, labelColor = MovieBoxOnBackground)) }
                            }
                        }
                        if (trailerKey != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TmdbApiService.getYouTubeUrl(trailerKey!!)))) }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp), tint = MovieBoxPrimary); Spacer(modifier = Modifier.width(8.dp)); Text("Ver Trailer", color = MovieBoxPrimary)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Sinopsis", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MovieBoxOnBackground)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = tvShow.overview.ifEmpty { "Sin descripción disponible." }, color = MovieBoxOnBackground.copy(alpha = 0.8f), fontSize = 14.sp, lineHeight = 22.sp)
                        if (cast.isNotEmpty()) { Spacer(modifier = Modifier.height(24.dp)); Text(text = "Reparto", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MovieBoxOnBackground); Spacer(modifier = Modifier.height(8.dp)) }
                    }
                    if (cast.isNotEmpty()) CastRow(cast = cast)
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.toggleWatchlisted() },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isWatchlisted) MovieBoxSurface else MovieBoxPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = if (isWatchlisted) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "",
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                            Button(onClick = { viewModel.toggleFavorite() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (isFavorite) MovieBoxSurface else MovieBoxPrimary)) {
                                Icon(imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp)); Text(
                                text = "",
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            }
                            Button(onClick = { viewModel.toggleWatched() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (isWatched) MovieBoxSurface else MovieBoxPrimary)) {
                                Icon(imageVector = if (isWatched) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp)); Text(
                                text = "",
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Mis reseñas", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MovieBoxOnBackground)
                            IconButton(onClick = { showReviewDialog = true }) { Icon(Icons.Default.Add, contentDescription = "Añadir reseña", tint = MovieBoxPrimary) }
                        }
                        if (reviews.isEmpty()) Text(text = "No has escrito ninguna reseña.", color = MovieBoxOnBackground.copy(alpha = 0.5f), fontSize = 14.sp)
                        else reviews.forEach { review -> ReviewItem(review = review, onDelete = { viewModel.deleteReview(review.id) }); Spacer(modifier = Modifier.height(8.dp)) }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
    if (showReviewDialog) AddReviewDialog(onDismiss = { showReviewDialog = false }, onConfirm = { rating, comment -> viewModel.addReview(rating, comment); showReviewDialog = false })
}