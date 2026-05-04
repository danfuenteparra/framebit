package com.example.moviebox.ui.components.reviews

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moviebox.data.remote.model.FriendReview
import com.example.moviebox.ui.theme.MovieBoxOnBackground
import com.example.moviebox.ui.theme.MovieBoxPrimary

/**
 * Sección "Reseñas" reutilizable para Movie/TvShow/Game detail.
 * Contiene el título, el botón de añadir y la lista de reseñas.
 *
 * @param reviews lista de reseñas a mostrar
 * @param myUserId id del usuario actual (para detectar reseñas propias)
 * @param onAddClick acción al pulsar el botón "+"
 * @param onReviewClick navegación al detalle de la reseña (recibe el reviewId)
 * @param onLikeClick alternar like en una reseña
 * @param onDeleteOwnReview borrar la reseña propia (solo se invoca si la review es propia)
 */
@Composable
fun ReviewsSection(
    reviews: List<FriendReview>,
    myUserId: String,
    onAddClick: () -> Unit,
    onReviewClick: (reviewId: String) -> Unit,
    onLikeClick: (FriendReview) -> Unit,
    onDeleteOwnReview: (FriendReview) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Reseñas",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MovieBoxOnBackground
        )
        IconButton(onClick = onAddClick) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Añadir reseña",
                tint = MovieBoxPrimary
            )
        }
    }

    if (reviews.isEmpty()) {
        Text(
            text = "Aún no hay reseñas. ¡Sé el primero!",
            color = MovieBoxOnBackground.copy(alpha = 0.5f),
            fontSize = 14.sp
        )
    } else {
        reviews.forEach { fr ->
            FriendReviewItem(
                review = fr,
                isOwn = fr.userId == myUserId,
                onClick = { onReviewClick(fr.reviewId) },
                onLikeClick = { onLikeClick(fr) },
                onDelete = { onDeleteOwnReview(fr) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
