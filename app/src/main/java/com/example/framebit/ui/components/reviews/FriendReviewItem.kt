package com.example.framebit.ui.components.reviews

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.framebit.data.remote.model.FriendReview
import com.example.framebit.ui.theme.MovieBoxOnBackground
import com.example.framebit.ui.theme.MovieBoxPrimary
import com.example.framebit.ui.theme.MovieBoxSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.remember

/**
 * Tarjeta de reseña reutilizable para Movie/TvShow/Game detail.
 *
 * @param review datos de la reseña
 * @param isOwn true si la reseña es del usuario actual (muestra botón eliminar)
 * @param onClick navega al detalle de la reseña
 * @param onLikeClick alterna el like (con actualización optimista en el ViewModel)
 * @param onDelete callback de borrado (solo se invoca si isOwn)
 */
@Composable
fun FriendReviewItem(
    review: FriendReview,
    isOwn: Boolean,
    onClick: () -> Unit,
    onLikeClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MovieBoxSurface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: avatar + nombre + fecha + (eliminar si es propia)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (review.userPicture != null) {
                    AsyncImage(
                        model = review.userPicture,
                        contentDescription = review.userName,
                        modifier = Modifier.size(36.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MovieBoxOnBackground
                        )
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
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MovieBoxOnBackground.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Estrellas
            review.rating?.let { rating ->
                Row {
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (index < rating) MovieBoxPrimary
                            else MovieBoxOnBackground.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Comentario
            if (review.comment.isNotBlank()) {
                Text(text = review.comment, color = MovieBoxOnBackground, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Footer: like + contador + comentarios
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onLikeClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (review.likedByMe) Icons.Filled.Favorite
                        else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (review.likedByMe) "Quitar me gusta" else "Me gusta",
                        tint = if (review.likedByMe) MovieBoxPrimary
                        else MovieBoxOnBackground.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = review.likesCount.toString(),
                    color = MovieBoxOnBackground.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Comment,
                    contentDescription = "Comentarios",
                    tint = MovieBoxOnBackground.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = review.commentsCount.toString(),
                    color = MovieBoxOnBackground.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
        }
    }
}
