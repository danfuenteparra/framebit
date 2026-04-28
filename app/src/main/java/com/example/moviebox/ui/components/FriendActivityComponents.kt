package com.example.moviebox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.moviebox.data.remote.api.TmdbApiService
import com.example.moviebox.data.remote.model.FriendActivity
import com.example.moviebox.ui.theme.MovieBoxOnBackground
import com.example.moviebox.ui.theme.MovieBoxPrimary
import com.example.moviebox.ui.theme.MovieBoxSurface

/**
 * Card con portada arriba (click → detalle del contenido) y footer
 * con avatar + nota + iconos (click → detalle de la reseña).
 */
@Composable
fun FriendActivityCard(
    activity: FriendActivity,
    mediaType: String,
    onPosterClick: () -> Unit,
    onReviewClick: () -> Unit
) {
    val isGame = mediaType == "game"
    val cardWidth = if (isGame) 160.dp else 130.dp
    val imageHeight = if (isGame) 100.dp else 180.dp
    val imageUrl = if (isGame) activity.posterPath
    else TmdbApiService.getImageUrl(activity.posterPath)

    Card(
        modifier = Modifier.width(cardWidth),
        colors = CardDefaults.cardColors(containerColor = MovieBoxSurface),
        shape = RoundedCornerShape(8.dp)
    ) {
        // Portada
        AsyncImage(
            model = imageUrl,
            contentDescription = activity.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
                .clickable { onPosterClick() },
            contentScale = ContentScale.Crop
        )

        // Footer con info del amigo + reseña
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onReviewClick() }
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MovieBoxSurface),
                contentAlignment = Alignment.Center
            ) {
                if (activity.friendPicture != null) {
                    AsyncImage(
                        model = activity.friendPicture,
                        contentDescription = activity.friendName,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MovieBoxOnBackground,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Nota (si existe)
            if (activity.rating != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MovieBoxPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = String.format("%.1f", activity.rating),
                        color = MovieBoxOnBackground,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Icono de texto si tiene comentario
            if (activity.hasComment) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Comment,
                    contentDescription = "Tiene comentario",
                    tint = MovieBoxOnBackground.copy(alpha = 0.7f),
                    modifier = Modifier.size(12.dp)
                )
            }

            // Estrella si es favorito
            if (activity.isFavorite) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Favorito",
                    tint = MovieBoxPrimary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun FriendsActivityRow(
    activities: List<FriendActivity>,
    mediaType: String,
    onItemClick: (mediaId: Int) -> Unit,
    onReviewClick: (activity: FriendActivity) -> Unit
) {
    if (activities.isEmpty()) return
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = activities,
            key = { "${it.friendUserId}_${it.mediaId}_${it.watchedAt}" }
        ) { act ->
            FriendActivityCard(
                activity = act,
                mediaType = mediaType,
                onPosterClick = { onItemClick(act.mediaId) },
                onReviewClick = { onReviewClick(act) }
            )
        }
    }
}

@Composable
fun EmptyFriendsActivityHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MovieBoxOnBackground.copy(alpha = 0.5f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal
        )
    }
}