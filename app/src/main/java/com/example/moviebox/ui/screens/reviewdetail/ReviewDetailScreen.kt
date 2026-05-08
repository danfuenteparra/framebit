package com.example.moviebox.ui.screens.reviewdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.moviebox.data.remote.api.TmdbApiService
import com.example.moviebox.data.remote.model.FriendReview
import com.example.moviebox.data.remote.model.ReviewComment
import com.example.moviebox.ui.theme.MovieBoxBackground
import com.example.moviebox.ui.theme.MovieBoxOnBackground
import com.example.moviebox.ui.theme.MovieBoxPrimary
import com.example.moviebox.ui.theme.MovieBoxSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewDetailScreen(
    onBack: () -> Unit,
    onNavigateToMedia: (mediaId: Int, mediaType: String) -> Unit,
    onNavigateToUser: (userId: String) -> Unit,
    viewModel: ReviewDetailViewModel = hiltViewModel()
) {
    val review by viewModel.review.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()

    var commentText by remember { mutableStateOf("") }

    // Si la reseña es minimal (no existe doc en Firestore, solo entrada de
    // library), ocultamos la barra de comentarios y el botón de like:
    // no hay doc al que asociar likes ni comentarios.
    val r = review
    val isMinimal = r?.isMinimal == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        // Cuando es minimal cambiamos el título para que el
                        // usuario entienda que no es una "reseña" propiamente.
                        text = if (isMinimal) "Detalle" else "Reseña",
                        color = MovieBoxOnBackground,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = MovieBoxOnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MovieBoxBackground)
            )
        },
        bottomBar = {
            // Barra de comentar SOLO en reseñas reales con doc en Firestore.
            if (r != null && !isMinimal) {
                CommentInputBar(
                    value = commentText,
                    onValueChange = { commentText = it },
                    onSend = {
                        viewModel.addComment(commentText)
                        commentText = ""
                    }
                )
            }
        },
        containerColor = MovieBoxBackground
    ) { padding ->
        when {
            loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MovieBoxPrimary)
            }
            r == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Reseña no disponible", color = MovieBoxOnBackground)
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        ReviewHeader(
                            review = r,
                            onMediaClick = { onNavigateToMedia(r.mediaId, r.mediaType) },
                            onUserClick = { onNavigateToUser(r.userId) },
                            onLikeClick = { viewModel.toggleLike() }
                        )
                        // Bloque de comentarios solo en modo no-minimal
                        if (!isMinimal) {
                            HorizontalDivider(color = MovieBoxSurface)
                            Text(
                                text = "Comentarios (${comments.size})",
                                color = MovieBoxOnBackground,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    if (!isMinimal) {
                        if (comments.isEmpty()) {
                            item {
                                Text(
                                    text = "Sé el primero en comentar.",
                                    color = MovieBoxOnBackground.copy(alpha = 0.5f),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        } else {
                            items(items = comments, key = { it.commentId }) { c ->
                                CommentRow(
                                    comment = c,
                                    isOwn = viewModel.isOwnComment(c),
                                    onUserClick = { onNavigateToUser(c.userId) },
                                    onDelete = { viewModel.deleteComment(c.commentId) }
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
private fun ReviewHeader(
    review: FriendReview,
    onMediaClick: () -> Unit,
    onUserClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val isGame = review.mediaType == "game"
    val posterUrl = if (isGame) review.mediaPosterPath
    else TmdbApiService.getImageUrl(review.mediaPosterPath)

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        // Cabecera: portada + título + año + fecha
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AsyncImage(
                model = posterUrl,
                contentDescription = review.mediaTitle,
                modifier = Modifier
                    .size(width = 90.dp, height = 130.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onMediaClick() },
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = review.mediaTitle,
                    color = MovieBoxOnBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (review.releaseYear.isNotBlank()) {
                    Text(
                        text = review.releaseYear,
                        color = MovieBoxOnBackground.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Visto el ${dateFormat.format(Date(review.createdAt))}",
                    color = MovieBoxOnBackground.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
                if (review.isFavorite) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = MovieBoxPrimary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Marcado como favorito", color = MovieBoxPrimary, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Autor
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onUserClick() }
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(MovieBoxSurface),
                contentAlignment = Alignment.Center
            ) {
                if (review.userPicture != null) {
                    AsyncImage(
                        model = review.userPicture,
                        contentDescription = review.userName,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MovieBoxOnBackground, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = review.userName.ifBlank { "Usuario" },
                color = MovieBoxOnBackground,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }

        // Rating + comentario + like SOLO si no es minimal
        if (!review.isMinimal) {
            Spacer(Modifier.height(12.dp))
            review.rating?.let { rating ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { i ->
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (i < rating) MovieBoxPrimary else MovieBoxOnBackground.copy(alpha = 0.25f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f", rating),
                        color = MovieBoxOnBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (review.comment.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = review.comment,
                    color = MovieBoxOnBackground,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // Like
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLikeClick) {
                    Icon(
                        imageVector = if (review.likedByMe) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Me gusta",
                        tint = if (review.likedByMe) MovieBoxPrimary else MovieBoxOnBackground
                    )
                }
                Text(
                    text = "${review.likesCount} me gusta",
                    color = MovieBoxOnBackground.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun CommentRow(
    comment: ReviewComment,
    isOwn: Boolean,
    onUserClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MovieBoxSurface)
                .clickable { onUserClick() },
            contentAlignment = Alignment.Center
        ) {
            if (comment.userPicture != null) {
                AsyncImage(
                    model = comment.userPicture,
                    contentDescription = comment.userName,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = null, tint = MovieBoxOnBackground, modifier = Modifier.size(18.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.userName.ifBlank { "Usuario" },
                    color = MovieBoxOnBackground,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onUserClick() }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = dateFormat.format(Date(comment.createdAt)),
                    color = MovieBoxOnBackground.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
            Text(
                text = comment.text,
                color = MovieBoxOnBackground,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
        if (isOwn) {
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Borrar",
                    tint = MovieBoxOnBackground.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CommentInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(color = MovieBoxBackground, tonalElevation = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Escribe un comentario...", color = MovieBoxOnBackground.copy(alpha = 0.5f)) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MovieBoxOnBackground,
                    unfocusedTextColor = MovieBoxOnBackground,
                    focusedBorderColor = MovieBoxPrimary,
                    unfocusedBorderColor = MovieBoxSurface
                )
            )
            IconButton(onClick = onSend, enabled = value.isNotBlank()) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar",
                    tint = if (value.isNotBlank()) MovieBoxPrimary else MovieBoxOnBackground.copy(alpha = 0.3f)
                )
            }
        }
    }
}