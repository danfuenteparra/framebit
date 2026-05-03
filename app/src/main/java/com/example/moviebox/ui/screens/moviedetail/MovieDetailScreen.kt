package com.example.moviebox.ui.screens.moviedetail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Person
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.moviebox.data.local.entity.ReviewEntity
import com.example.moviebox.data.remote.api.TmdbApiService
import com.example.moviebox.data.remote.dto.CastDto
import com.example.moviebox.data.remote.model.FriendReview
import com.example.moviebox.ui.theme.MovieBoxBackground
import com.example.moviebox.ui.theme.MovieBoxOnBackground
import com.example.moviebox.ui.theme.MovieBoxPrimary
import com.example.moviebox.ui.theme.MovieBoxSurface
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    onBack: () -> Unit,
    viewModel: MovieDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isWatchlisted by viewModel.isWatchlisted.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val isWatched by viewModel.isWatched.collectAsStateWithLifecycle()
    val reviews by viewModel.reviews.collectAsStateWithLifecycle()
    val friendReviews by viewModel.friendReviews.collectAsStateWithLifecycle()
    val cast by viewModel.cast.collectAsStateWithLifecycle()
    val director by viewModel.director.collectAsStateWithLifecycle()
    val trailerKey by viewModel.trailerKey.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showReviewDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle", color = MovieBoxOnBackground, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = MovieBoxOnBackground) }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleWatchlisted() }) {
                        Icon(
                            imageVector = if (isWatchlisted) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Watchlist",
                            tint = if (isWatchlisted) MovieBoxPrimary else MovieBoxOnBackground
                        )
                    }
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorita",
                            tint = if (isFavorite) MovieBoxPrimary else MovieBoxOnBackground
                        )
                    }
                    IconButton(onClick = { viewModel.toggleWatched() }) {
                        Icon(
                            imageVector = if (isWatched) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                            contentDescription = "Vista",
                            tint = if (isWatched) MovieBoxPrimary else MovieBoxOnBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MovieBoxBackground)
            )
        },
        containerColor = MovieBoxBackground
    ) { innerPadding ->
        when (uiState) {
            is MovieDetailUiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MovieBoxPrimary) }
            is MovieDetailUiState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text((uiState as MovieDetailUiState.Error).message, color = MovieBoxOnBackground) }
            is MovieDetailUiState.Success -> {
                val movie = (uiState as MovieDetailUiState.Success).movie
                Column(modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState())) {
                    // Backdrop
                    Box {
                        AsyncImage(model = TmdbApiService.getImageUrl(movie.backdropPath, TmdbApiService.BACKDROP_SIZE), contentDescription = movie.title, modifier = Modifier.fillMaxWidth().height(220.dp), contentScale = ContentScale.Crop)
                        if (trailerKey != null) {
                            FloatingActionButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TmdbApiService.getYouTubeUrl(trailerKey!!)))) }, modifier = Modifier.align(Alignment.Center).size(56.dp), containerColor = MovieBoxPrimary.copy(alpha = 0.9f)) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Ver trailer", tint = MovieBoxOnBackground, modifier = Modifier.size(32.dp))
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = movie.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MovieBoxOnBackground)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "\u2B50 ${String.format("%.1f", movie.voteAverage)}", color = MovieBoxPrimary, fontSize = 14.sp)
                            Text(text = movie.releaseDate.take(4), color = MovieBoxOnBackground.copy(alpha = 0.7f), fontSize = 14.sp)
                            movie.runtime?.let { Text(text = "${it} min", color = MovieBoxOnBackground.copy(alpha = 0.7f), fontSize = 14.sp) }
                        }
                        if (director != null) { Spacer(modifier = Modifier.height(4.dp)); Text(text = "Director: $director", color = MovieBoxOnBackground.copy(alpha = 0.7f), fontSize = 14.sp) }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (movie.genres.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                movie.genres.take(4).forEach { genre -> SuggestionChip(onClick = {}, label = { Text(genre.name, fontSize = 12.sp) }, colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MovieBoxSurface, labelColor = MovieBoxOnBackground)) }
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
                        Text(text = movie.overview.ifEmpty { "Sin descripción disponible." }, color = MovieBoxOnBackground.copy(alpha = 0.8f), fontSize = 14.sp, lineHeight = 22.sp)
                        if (cast.isNotEmpty()) { Spacer(modifier = Modifier.height(24.dp)); Text(text = "Reparto", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MovieBoxOnBackground); Spacer(modifier = Modifier.height(8.dp)) }
                    }
                    if (cast.isNotEmpty()) CastRow(cast = cast)
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        // 3 botones
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.toggleWatchlisted() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (isWatchlisted) MovieBoxSurface else MovieBoxPrimary)) {
                                Icon(imageVector = if (isWatchlisted) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp)); Text("", fontSize = 12.sp)
                            }
                            Button(onClick = { viewModel.toggleFavorite() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (isFavorite) MovieBoxSurface else MovieBoxPrimary)) {
                                Icon(imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp)); Text("", fontSize = 12.sp)
                            }
                            Button(onClick = { viewModel.toggleWatched() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (isWatched) MovieBoxSurface else MovieBoxPrimary)) {
                                Icon(imageVector = if (isWatched) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp)); Text("", fontSize = 12.sp)
                            }
                        }
                        // Reseñas
                        Spacer(modifier = Modifier.height(32.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Reseñas", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MovieBoxOnBackground)
                            IconButton(onClick = { showReviewDialog = true }) { Icon(Icons.Default.Add, contentDescription = "Añadir reseña", tint = MovieBoxPrimary) }
                        }
                        val myId = viewModel.getMyUserId()
                        if (friendReviews.isEmpty()) Text(text = "Aún no hay reseñas. ¡Sé el primero!", color = MovieBoxOnBackground.copy(alpha = 0.5f), fontSize = 14.sp)
                        else friendReviews.forEach { fr ->
                            FriendReviewItem(
                                review = fr,
                                isOwn = fr.userId == myId,
                                onDelete = {
                                    // Encontrar la review local correspondiente y borrarla
                                    val local = reviews.firstOrNull()
                                    if (local != null) viewModel.deleteReview(local.id)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
    if (showReviewDialog) AddReviewDialog(onDismiss = { showReviewDialog = false }, onConfirm = { rating, comment -> viewModel.addReview(rating, comment); showReviewDialog = false })
}

@Composable
fun CastRow(cast: List<CastDto>) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(cast) { actor ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
                AsyncImage(model = TmdbApiService.getImageUrl(actor.profilePath, TmdbApiService.PROFILE_SIZE), contentDescription = actor.name, modifier = Modifier.size(70.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = actor.name, color = MovieBoxOnBackground, fontSize = 11.sp, maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(text = actor.character, color = MovieBoxOnBackground.copy(alpha = 0.6f), fontSize = 10.sp, maxLines = 1, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun FriendReviewItem(
    review: FriendReview,
    isOwn: Boolean,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    Card(colors = CardDefaults.cardColors(containerColor = MovieBoxSurface), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (review.userPicture != null) {
                    AsyncImage(
                        model = review.userPicture,
                        contentDescription = review.userName,
                        modifier = Modifier.size(36.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MovieBoxOnBackground)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.userName.ifBlank { "Usuario" },
                        color = MovieBoxOnBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (review.createdAt > 0L) {
                        Text(
                            text = dateFormat.format(Date(review.createdAt)),
                            color = MovieBoxOnBackground.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }
                if (isOwn) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MovieBoxOnBackground.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            review.rating?.let { rating ->
                Row {
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (index < rating) MovieBoxPrimary else MovieBoxOnBackground.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            if (review.comment.isNotBlank()) {
                Text(text = review.comment, color = MovieBoxOnBackground, fontSize = 14.sp)
            }
        }
    }
}

@Composable
@Deprecated("Usa FriendReviewItem", ReplaceWith("FriendReviewItem(review, isOwn, onDelete)"))
fun ReviewItem(review: ReviewEntity, onDelete: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    Card(colors = CardDefaults.cardColors(containerColor = MovieBoxSurface), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row { repeat(5) { index -> Icon(imageVector = Icons.Filled.Star, contentDescription = null, tint = if (index < review.rating) MovieBoxPrimary else MovieBoxOnBackground.copy(alpha = 0.3f), modifier = Modifier.size(16.dp)) } }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = dateFormat.format(Date(review.createdAt)), color = MovieBoxOnBackground.copy(alpha = 0.5f), fontSize = 11.sp)
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MovieBoxOnBackground.copy(alpha = 0.5f), modifier = Modifier.size(16.dp)) }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = review.comment, color = MovieBoxOnBackground, fontSize = 14.sp)
        }
    }
}

@Composable
fun AddReviewDialog(onDismiss: () -> Unit, onConfirm: (Float, String) -> Unit) {
    var rating by remember { mutableFloatStateOf(3f) }
    var comment by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, containerColor = MovieBoxSurface,
        title = { Text("Escribir reseña", color = MovieBoxOnBackground) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Puntuación", color = MovieBoxOnBackground, fontSize = 14.sp)
                Row { (1..5).forEach { star -> IconButton(onClick = { rating = star.toFloat() }, modifier = Modifier.size(40.dp)) { Icon(imageVector = Icons.Filled.Star, contentDescription = "$star estrellas", tint = if (star <= rating) MovieBoxPrimary else MovieBoxOnBackground.copy(alpha = 0.3f), modifier = Modifier.size(28.dp)) } } }
                OutlinedTextField(value = comment, onValueChange = { comment = it }, label = { Text("Tu opinión") }, modifier = Modifier.fillMaxWidth(), minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MovieBoxOnBackground, unfocusedTextColor = MovieBoxOnBackground, focusedBorderColor = MovieBoxPrimary, unfocusedBorderColor = MovieBoxOnBackground.copy(alpha = 0.3f)))
            }
        },
        confirmButton = { TextButton(onClick = { if (comment.isNotBlank()) onConfirm(rating, comment) }, enabled = comment.isNotBlank()) { Text("Guardar", color = MovieBoxPrimary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = MovieBoxOnBackground) } }
    )
}