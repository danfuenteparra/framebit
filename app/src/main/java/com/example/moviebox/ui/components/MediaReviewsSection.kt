package com.example.moviebox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.moviebox.data.remote.model.FriendReview
import com.example.moviebox.ui.components.mediareviews.MediaReviewsViewModel
import com.example.moviebox.ui.theme.MovieBoxOnBackground
import com.example.moviebox.ui.theme.MovieBoxPrimary
import com.example.moviebox.ui.theme.MovieBoxSurface

/**
 * Bloque para insertar dentro del scroll de un DetailScreen tras "Mis reseñas".
 * Muestra "Reseñas de amigos" y "Todas las reseñas" (top likes).
 */
@Composable
fun MediaReviewsSection(
    mediaType: String,
    mediaId: Int,
    onReviewClick: (reviewId: String) -> Unit,
    onUserClick: (userId: String) -> Unit,
    viewModel: MediaReviewsViewModel = hiltViewModel()
) {
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val all by viewModel.all.collectAsStateWithLifecycle()

    LaunchedEffect(mediaType, mediaId) {
        viewModel.load(mediaType, mediaId)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Reseñas de amigos",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MovieBoxOnBackground
        )
        Spacer(Modifier.height(8.dp))
        if (friends.isEmpty()) {
            Text(
                text = "Ninguno de tus amigos ha reseñado esto todavía.",
                color = MovieBoxOnBackground.copy(alpha = 0.5f),
                fontSize = 13.sp
            )
        } else {
            friends.forEach { r ->
                FriendReviewItem(
                    review = r,
                    onClick = { onReviewClick(r.reviewId) },
                    onAvatarClick = { onUserClick(r.userId) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "Todas las reseñas",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MovieBoxOnBackground
        )
        Spacer(Modifier.height(8.dp))
        if (all.isEmpty()) {
            Text(
                text = "Aún no hay reseñas.",
                color = MovieBoxOnBackground.copy(alpha = 0.5f),
                fontSize = 13.sp
            )
        } else {
            all.forEach { r ->
                FriendReviewItem(
                    review = r,
                    onClick = { onReviewClick(r.reviewId) },
                    onAvatarClick = { onUserClick(r.userId) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun FriendReviewItem(
    review: FriendReview,
    onClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MovieBoxSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MovieBoxSurface)
                        .clickable { onAvatarClick() },
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
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MovieBoxOnBackground,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = review.userName.ifBlank { "Usuario" },
                    color = MovieBoxOnBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                review.rating?.let {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = MovieBoxPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = String.format("%.1f", it),
                        color = MovieBoxOnBackground,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (review.comment.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = review.comment,
                    color = MovieBoxOnBackground,
                    fontSize = 13.sp,
                    maxLines = 3
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = MovieBoxPrimary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${review.likesCount}",
                    color = MovieBoxOnBackground.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "${review.commentsCount} comentarios",
                    color = MovieBoxOnBackground.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}