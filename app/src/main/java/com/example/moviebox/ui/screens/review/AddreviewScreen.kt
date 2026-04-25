package com.example.moviebox.ui.screens.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.moviebox.data.remote.api.TmdbApiService
import com.example.moviebox.ui.screens.review.AddReviewSearchState
import com.example.moviebox.ui.screens.review.AddReviewViewModel
import com.example.moviebox.ui.screens.review.ReviewMediaType
import com.example.moviebox.ui.theme.MovieBoxBackground
import com.example.moviebox.ui.theme.MovieBoxOnBackground
import com.example.moviebox.ui.theme.MovieBoxPrimary
import com.example.moviebox.ui.theme.MovieBoxSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReviewScreen(
    onBack: () -> Unit,
    onReviewSaved: () -> Unit,
    viewModel: AddReviewViewModel = hiltViewModel()
) {
    val mediaType by viewModel.mediaType.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val reviewSaved by viewModel.reviewSaved.collectAsStateWithLifecycle()

    // Cuando se guarda la reseña, navegar atrás
    LaunchedEffect(reviewSaved) {
        if (reviewSaved) {
            viewModel.consumeReviewSaved()
            onReviewSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva reseña", color = MovieBoxOnBackground, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = MovieBoxOnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MovieBoxBackground)
            )
        },
        containerColor = MovieBoxBackground
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)) {

            // Selector de tipo
            Text(
                text = "¿Qué quieres reseñar?",
                color = MovieBoxOnBackground,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MediaTypeChip(
                    label = "Película",
                    icon = Icons.Default.Movie,
                    selected = mediaType == ReviewMediaType.MOVIE,
                    onClick = { viewModel.onMediaTypeChange(ReviewMediaType.MOVIE) },
                    modifier = Modifier.weight(1f)
                )
                MediaTypeChip(
                    label = "Serie",
                    icon = Icons.Default.Tv,
                    selected = mediaType == ReviewMediaType.TV,
                    onClick = { viewModel.onMediaTypeChange(ReviewMediaType.TV) },
                    modifier = Modifier.weight(1f)
                )
                MediaTypeChip(
                    label = "Juego",
                    icon = Icons.Default.SportsEsports,
                    selected = mediaType == ReviewMediaType.GAME,
                    onClick = { viewModel.onMediaTypeChange(ReviewMediaType.GAME) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Barra de búsqueda
            val placeholder = when (mediaType) {
                ReviewMediaType.MOVIE -> "Buscar película..."
                ReviewMediaType.TV -> "Buscar serie..."
                ReviewMediaType.GAME -> "Buscar juego..."
            }
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.onQueryChange(it) },
                placeholder = { Text(placeholder, color = MovieBoxOnBackground.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MovieBoxOnBackground) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MovieBoxOnBackground,
                    unfocusedTextColor = MovieBoxOnBackground,
                    focusedBorderColor = MovieBoxPrimary,
                    unfocusedBorderColor = MovieBoxOnBackground.copy(alpha = 0.3f),
                    focusedContainerColor = MovieBoxSurface,
                    unfocusedContainerColor = MovieBoxSurface
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Resultados
            Box(modifier = Modifier.fillMaxSize()) {
                when (val s = searchState) {
                    is AddReviewSearchState.Idle -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "Escribe para buscar",
                                color = MovieBoxOnBackground.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }
                    }
                    is AddReviewSearchState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MovieBoxPrimary)
                        }
                    }
                    is AddReviewSearchState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(s.message, color = MovieBoxOnBackground)
                        }
                    }
                    is AddReviewSearchState.MovieResults -> {
                        if (s.items.isEmpty()) EmptyResults() else LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(s.items) { movie ->
                                MediaResultRow(
                                    title = movie.title,
                                    subtitle = movie.releaseDate.take(4),
                                    posterPath = TmdbApiService.getImageUrl(movie.posterPath, TmdbApiService.POSTER_SIZE),
                                    onClick = { viewModel.onSelectMovie(movie) }
                                )
                            }
                        }
                    }
                    is AddReviewSearchState.TvShowResults -> {
                        if (s.items.isEmpty()) EmptyResults() else LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(s.items) { tv ->
                                MediaResultRow(
                                    title = tv.name,
                                    subtitle = tv.firstAirDate.take(4),
                                    posterPath = TmdbApiService.getImageUrl(tv.posterPath, TmdbApiService.POSTER_SIZE),
                                    onClick = { viewModel.onSelectTvShow(tv) }
                                )
                            }
                        }
                    }
                    is AddReviewSearchState.GameResults -> {
                        if (s.items.isEmpty()) EmptyResults() else LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(s.items) { game ->
                                MediaResultRow(
                                    title = game.name,
                                    subtitle = game.released?.take(4) ?: "",
                                    posterPath = game.backgroundImage,
                                    onClick = { viewModel.onSelectGame(game) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de review cuando hay item seleccionado
    selected?.let { sel ->
        ReviewFormDialog(
            mediaTitle = sel.title,
            onDismiss = { viewModel.clearSelection() },
            onConfirm = { rating, comment ->
                viewModel.saveReview(rating, comment)
            }
        )
    }
}

@Composable
private fun MediaTypeChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) MovieBoxPrimary else MovieBoxSurface
    val fg = if (selected) MovieBoxBackground else MovieBoxOnBackground
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = fg)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MediaResultRow(
    title: String,
    subtitle: String,
    posterPath: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MovieBoxSurface)
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = posterPath,
            contentDescription = title,
            modifier = Modifier
                .width(56.dp)
                .height(80.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MovieBoxOnBackground,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = MovieBoxOnBackground.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyResults() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Sin resultados", color = MovieBoxOnBackground.copy(alpha = 0.5f))
    }
}

@Composable
private fun ReviewFormDialog(
    mediaTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (Float, String) -> Unit
) {
    var rating by remember { mutableFloatStateOf(3f) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MovieBoxSurface,
        title = {
            Column {
                Text("Escribir reseña", color = MovieBoxOnBackground, fontWeight = FontWeight.Bold)
                Text(
                    mediaTitle,
                    color = MovieBoxOnBackground.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Puntuación", color = MovieBoxOnBackground, fontSize = 14.sp)
                Row {
                    (1..5).forEach { star ->
                        IconButton(
                            onClick = { rating = star.toFloat() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "$star estrellas",
                                tint = if (star <= rating) MovieBoxPrimary else MovieBoxOnBackground.copy(alpha = 0.3f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Tu opinión") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MovieBoxOnBackground,
                        unfocusedTextColor = MovieBoxOnBackground,
                        focusedBorderColor = MovieBoxPrimary,
                        unfocusedBorderColor = MovieBoxOnBackground.copy(alpha = 0.3f)
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (comment.isNotBlank()) onConfirm(rating, comment) },
                enabled = comment.isNotBlank()
            ) {
                Text("Guardar", color = MovieBoxPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MovieBoxOnBackground)
            }
        }
    )
}